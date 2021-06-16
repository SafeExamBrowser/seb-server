/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.datalayer.checks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.DBIntegrityCheck;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ViewRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ViewRecord;

@Lazy
@Component
@WebServiceProfile
public class ViewTableDuplicatesCheck implements DBIntegrityCheck {

    private final ViewRecordMapper viewRecordMapper;

    public ViewTableDuplicatesCheck(final ViewRecordMapper viewRecordMapper) {
        this.viewRecordMapper = viewRecordMapper;
    }

    @Override
    public String name() {
        return "ViewTableDuplicatesCheck";
    }

    @Override
    public String description() {
        return "Checks if there are duplicate entries in the view table by using the name and template_id to identify duplicates.";
    }

    @Override
    @Transactional
    public Result<String> applyCheck(final boolean tryFix) {
        return Result.tryCatch(() -> {
            final List<ViewRecord> records = this.viewRecordMapper
                    .selectByExample()
                    .build()
                    .execute();

            final Set<String> once = new HashSet<>();
            final Set<Long> toDelete = new HashSet<>();
            for (final ViewRecord record : records) {
                final String id = record.getName() + record.getTemplateId().toString();
                if (once.contains(id)) {
                    toDelete.add(record.getId());
                } else {
                    once.add(id);
                }
            }

            if (toDelete.isEmpty()) {
                return "OK";
            }

            if (tryFix) {
                toDelete
                        .stream()
                        .forEach(this.viewRecordMapper::deleteByPrimaryKey);
                return "Fixed duplicates by deletion: " + toDelete;
            } else {
                return "Found duplicates: " + toDelete;
            }

        });
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ViewTableDuplicatesCheck [name()=");
        builder.append(name());
        builder.append(", description()=");
        builder.append(description());
        builder.append("]");
        return builder.toString();
    }

}
