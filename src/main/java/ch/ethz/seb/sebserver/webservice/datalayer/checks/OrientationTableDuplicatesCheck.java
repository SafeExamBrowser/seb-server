/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.datalayer.checks;

import java.util.*;
import java.util.stream.Collectors;

import org.mybatis.dynamic.sql.SqlBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.DBIntegrityCheck;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.OrientationRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.OrientationRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.OrientationRecord;

@Lazy
@Component
@WebServiceProfile
public class OrientationTableDuplicatesCheck implements DBIntegrityCheck {

    private final OrientationRecordMapper orientationRecordMapper;

    public OrientationTableDuplicatesCheck(final OrientationRecordMapper orientationRecordMapper) {
        this.orientationRecordMapper = orientationRecordMapper;
    }

    @Override
    public String name() {
        return "OrientationTableDuplicatesCheck";
    }

    @Override
    public String description() {
        return "Checks if there are duplicate entries in the orientation table by using the config_attribute_id and template_id to identify duplicates.";
    }

    @Override
    @Transactional
    public Result<String> applyCheck(final boolean tryFix) {
        return Result.tryCatch(() -> {
            final List<OrientationRecord> records = this.orientationRecordMapper
                    .selectByExample()
                    .build()
                    .execute();

            final Set<String> once = new HashSet<>();
            final Set<Long> toDelete = new HashSet<>();
            for (final OrientationRecord record : records) {
                final String id = record.getConfigAttributeId().toString() + record.getTemplateId().toString();
                if (once.contains(id)) {
                    toDelete.add(record.getId());
                } else {
                    once.add(id);
                }
            }

            final List<Long> checkedToDelete = toDelete
                    .stream()
                    .map(this::getOldestForDeletion)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (checkedToDelete == null || checkedToDelete.isEmpty()) {
                return "OK";
            }

            if (tryFix) {

                checkedToDelete
                        .stream()
                        .forEach(this.orientationRecordMapper::deleteByPrimaryKey);
                return "Fixed duplicates by deletion: " + checkedToDelete;
            } else {
                return "Found duplicates: " + checkedToDelete;
            }

        });
    }

    private Long getOldestForDeletion(final Long id) {
        try {
            final OrientationRecord selectByPrimaryKey = this.orientationRecordMapper.selectByPrimaryKey(id);
            final List<OrientationRecord> records = this.orientationRecordMapper.selectByExample()
                    .where(
                            OrientationRecordDynamicSqlSupport.configAttributeId,
                            SqlBuilder.isEqualTo(selectByPrimaryKey.getConfigAttributeId()))
                    .and(
                            OrientationRecordDynamicSqlSupport.templateId,
                            SqlBuilder.isEqualTo(selectByPrimaryKey.getTemplateId()))
                    .orderBy(OrientationRecordDynamicSqlSupport.id)
                    .build()
                    .execute();

            // get latest entry of duplicates
            if (records != null && records.size() > 1) {
                Long result = null;
                for (int i = 0; i < records.size(); i++) {
                    final OrientationRecord rec = records.get(i);
                    if (result == null) {
                        result = rec.getId();
                        continue;
                    }
                    if (result > rec.getId()) {
                        result = rec.getId();
                    }
                }
                return result;
            }
            return null;
        } catch (final Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("OrientationTableDuplicatesCheck [name()=");
        builder.append(name());
        builder.append(", description()=");
        builder.append(description());
        builder.append("]");
        return builder.toString();
    }

}
