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
import ch.ethz.seb.sebserver.webservice.WebserviceInitException;
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
    private final String versionAttributeIds =
            "1,2,4,8,10,11,12,13,14,15,16,18,19,20,21,22,23,24,25,26,27,28,29,31,32,33,34,35,42,43,44,45,46,47,48,50,51," +
            "52,53,54,55,56,57,58,59,60,61,972,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,81,82,85,86,87," +
            "88,89,90,91,92,93,94,95,96,97,98,99,100,200,201,202,203,204,205,206,210,220,221,222,223,231,233,234," +
            "235,236,237,238,239,240,241,242,243,244,245,246,247,248,249,250,251,252,253,254,255,256,257,258,259," +
            "260,261,262,263,264,265,300,301,302,303,305,306,307,308,309,310,311,312,313,314,315,316,317,318,319," +
            "320,321,322,400,401,402,403,404,405,406,407,408,500,501,502,503,504,505,506,507,508,509,510,511,512," +
            "513,514,515,516,517,518,519,520,804,812,900,901,942,940,941,1129,1130,1131,1132,1133,1102,1103,1106," +
            "1108,1104,1105,1100,1116,1101,1120,1122,1123,1125,1124,1121,1500,1503,1506,1508,1504,1505,1502,1516," +
            "1501,1530,1531,1532,1533,951,952,953,950,960,961,919,928,970,971,973,904,974,975,1578,1551,1300,1301," +
            "1302,1303,1305,1306,1320,1321,1322,1564,1580,1581,1582,1200,1590,1591,1568,1577,1567,1550,909,1552,948," +
            "1557,943,945,1558,1559,1560,1561,1562,905,1569,1570,1563,1595,947";
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

            final String[] split = StringUtils.split(versionAttributeIds, Constants.LIST_SEPARATOR_CHAR);
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
