/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.datalayer.checks;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.DBIntegrityCheck;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.*;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ConfigurationAttributeRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.OrientationRecord;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
@WebServiceProfile
public class DowngradeSEBSettingsCheck implements DBIntegrityCheck {

    public static final Logger INIT_LOGGER = LoggerFactory.getLogger("ch.ethz.seb.SEB_SERVER_INIT");

    private final OrientationRecordMapper orientationRecordMapper;
    private final ConfigurationAttributeRecordMapper configurationAttributeRecordMapper;
    private final DataSource dataSource;
    private final String schemaName;
    private final Long lastMigrationVersion = 27L;
    private final String versionSEBSettingsAttributeIds =
            "";
    private final boolean fixDowngrade;

    public DowngradeSEBSettingsCheck(
            final OrientationRecordMapper orientationRecordMapper,
            final ConfigurationAttributeRecordMapper configurationAttributeRecordMapper,
            final DataSource dataSource,
            @Value("${sebserver.init.database.integrity.fix.downgrade:false}") final boolean fixDowngrade,
            @Value("${sebserver.init.database.integrity.check.schema:SEBServer}") final String schemaName) {

        this.orientationRecordMapper = orientationRecordMapper;
        this.configurationAttributeRecordMapper = configurationAttributeRecordMapper;
        this.dataSource = dataSource;
        this.fixDowngrade = fixDowngrade;
        this.schemaName = schemaName;
    }

    @Override
    public String name() {
        return "DowngradeSEBSettingsCheck";
    }

    @Override
    public String description() {
        return "Check if there are additional SEB Settings orientations within the database that do not match the once for the current SEB Server version.";
    }

    @Override
    public Result<String> applyCheck(final boolean tryFix) {
        return Result.tryCatch(() -> {

            if (StringUtils.isBlank(versionSEBSettingsAttributeIds)) {
                return  "No additional SEB Settings orientations for downgrading found.";
            }

            final String[] split = StringUtils.split(versionSEBSettingsAttributeIds, Constants.LIST_SEPARATOR_CHAR);
            final List<Long> config_attrs_ids = Arrays.stream(split).map(s -> {
                try {
                    return Long.valueOf(s.trim());
                } catch (final Exception e) {
                    return 0L;
                }
            }).collect(Collectors.toList());

            final List<Long> attributeIds = orientationRecordMapper.selectByExample()
                    .where(OrientationRecordDynamicSqlSupport.templateId, SqlBuilder.isEqualTo(0L))
                    .and(OrientationRecordDynamicSqlSupport.configAttributeId, SqlBuilder.isNotIn(config_attrs_ids))
                    .build()
                    .execute()
                    .stream()
                    .map(OrientationRecord::getConfigAttributeId)
                    .collect(Collectors.toList());

            if (attributeIds.isEmpty()) {
                return  "No additional SEB Settings orientations for downgrading found.";
            }

            final Set<String> allNames = configurationAttributeRecordMapper
                    .selectByExample()
                    .where(ConfigurationAttributeRecordDynamicSqlSupport.id, SqlBuilder.isIn(attributeIds))
                    .build()
                    .execute()
                    .stream()
                    .map(ConfigurationAttributeRecord::getName)
                    .collect(Collectors.toSet());

            if (!fixDowngrade) {
                INIT_LOGGER.error(" ---> !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                INIT_LOGGER.error(" ---> !!! Detected a Database version integrity violation, probably due to SEB Server version downgrade.");
                INIT_LOGGER.error(" ---> !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                INIT_LOGGER.error(" ---> !!!! Please check if there was a SEB Server downgrade that causes this or if this is caused by actual development. ");
                INIT_LOGGER.error(" ---> !!!! ");
                INIT_LOGGER.error(" ---> !!!! If new SEB Settings are added to the default SEB Settings view in actual development, just add the new attributes ids");
                INIT_LOGGER.error(" ---> !!!! To the list in class DowngradeSEBSettingsCheck to fix this issue.");
                INIT_LOGGER.error(" ---> !!!! ");
                INIT_LOGGER.error(" ---> !!!! If a SEB Server version downgrade is the cause of this issue, please check the following");
                INIT_LOGGER.error(" ---> !!!! correction and set 'sebserver.init.database.integrity.fix.downgrade' to true");
                INIT_LOGGER.error(" ---> !!!! to apply repair with next startup. Then SEB Server will apply the repair task");
                INIT_LOGGER.error(" ---> !!!! After successfully repair you can set 'sebserver.init.database.integrity.fix.downgrade' back to false ");
                INIT_LOGGER.error(" ---> !!!! ");
                INIT_LOGGER.error(" ---> !!!! NOTE: Repair will delete the following SEB Settings orientation for Exam Configuration default Views");
                INIT_LOGGER.error(" ---> !!!!       Exam Configurations built from Configuration Template will stay the same and might have incorrect View Tabs");
                INIT_LOGGER.error(" ---> !!!! ");
                INIT_LOGGER.error(" ---> !!!! Repair will remove following SEB Settings from default view:\n {}", allNames);
                INIT_LOGGER.error(" ---> !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                //throw new WebserviceInitException("Detected a Database version integrity violation, probably due to SEB Server version downgrade. See logs above");
                return "Downgrade SEB Settings correction would delete the following SEB Settings: " + allNames;
            } else {
                try {
                    final Integer deletedOrientation = orientationRecordMapper
                            .deleteByExample()
                            .where(OrientationRecordDynamicSqlSupport.configAttributeId, SqlBuilder.isIn(attributeIds))
                            .and(OrientationRecordDynamicSqlSupport.templateId, SqlBuilder.isEqualTo(0L))
                            .build()
                            .execute();

                    INIT_LOGGER.info(" ---> Deleted {} entries from table orientation", deletedOrientation);

                    INIT_LOGGER.info(" ---> Try delete migration task until this version...");

                    final Connection connection = this.dataSource.getConnection();
                    final PreparedStatement prepareStatement = connection.prepareStatement(
                            "DELETE FROM "+ schemaName +".flyway_schema_history WHERE version > " + lastMigrationVersion);
                    prepareStatement.execute();

                    INIT_LOGGER.info(" ---> Deleted {} entries from table flyway_schema_history", deletedOrientation);

                    return "Successfully deleted SEB Settings attributes: " + allNames;
                } catch (final Exception e) {
                    INIT_LOGGER.error("Failed to delete SEB Settings attributes: ", e);
                    return "Failed to delete SEB Settings attributes: " + allNames;
                }
            }
        });
    }
}
