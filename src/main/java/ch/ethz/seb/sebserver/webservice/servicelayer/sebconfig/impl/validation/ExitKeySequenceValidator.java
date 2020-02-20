/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.validation;

import java.util.Arrays;
import java.util.List;

import org.mybatis.dynamic.sql.SqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationAttributeRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationValueRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationValueRecordMapper;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ConfigurationValueValidator;

@Lazy
@Component
@WebServiceProfile
public class ExitKeySequenceValidator implements ConfigurationValueValidator {

    private static final Logger log = LoggerFactory.getLogger(ExitKeySequenceValidator.class);

    public static final String NAME = "ExitKeySequenceValidator";
    private static final List<String> ATTR_NAMES =
            Arrays.asList("exitKey1", "exitKey2", "exitKey3");

    private final ConfigurationValueRecordMapper configurationValueRecordMapper;

    protected ExitKeySequenceValidator(
            final ConfigurationValueRecordMapper configurationValueRecordMapper) {

        this.configurationValueRecordMapper = configurationValueRecordMapper;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean validate(final ConfigurationValue value, final ConfigurationAttribute attribute) {
        if (!NAME.equals(attribute.validator)) {
            return true;
        }

        try {
            return this.configurationValueRecordMapper.selectByExample()
                    .join(ConfigurationAttributeRecordDynamicSqlSupport.configurationAttributeRecord)
                    .on(
                            ConfigurationAttributeRecordDynamicSqlSupport.id,
                            SqlBuilder.equalTo(ConfigurationValueRecordDynamicSqlSupport.configurationAttributeId))
                    .where(
                            ConfigurationAttributeRecordDynamicSqlSupport.name,
                            SqlBuilder.isIn(ATTR_NAMES))
                    .and(
                            ConfigurationValueRecordDynamicSqlSupport.institutionId,
                            SqlBuilder.isEqualTo(value.institutionId))
                    .and(
                            ConfigurationValueRecordDynamicSqlSupport.configurationId,
                            SqlBuilder.isEqualTo(value.configurationId))
                    .build()
                    .execute()
                    .stream()
                    .noneMatch(val -> value.value.equals(val.getValue()));
        } catch (final Exception e) {
            log.error("unexpected error while trying to validate SEB exam configuration attributes: {}", ATTR_NAMES, e);
            return true;
        }
    }

}
