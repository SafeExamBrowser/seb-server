package ch.ethz.seb.sebserver.webservice.datalayer.checks;

import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.DBIntegrityCheck;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationAttributeRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ConfigurationAttributeRecord;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class UpdateSEBServerVersionSEBSetting implements DBIntegrityCheck  {

    private final ConfigurationAttributeRecordMapper configurationAttributeRecordMapper;
    private final Environment environment;

    public UpdateSEBServerVersionSEBSetting(
            final ConfigurationAttributeRecordMapper configurationAttributeRecordMapper,
            final Environment environment) {

        this.configurationAttributeRecordMapper = configurationAttributeRecordMapper;
        this.environment = environment;
    }


    @Override
    public String name() {
        return "UpdateSEBServerVersionSEBSetting";
    }

    @Override
    public String description() {
        return "Check current SEB Server Version for SEB Setting originatorVersion (1000) and update it id needed";
    }

    @Override
    public Result<String> applyCheck(boolean tryFix) {
        return Result.tryCatch(() -> {

            String currentVersion = environment.getProperty("sebserver.version", "");
            final ConfigurationAttributeRecord record = configurationAttributeRecordMapper
                    .selectByPrimaryKey(1000L);
            final String defaultValue = record.getDefaultValue();

            if (currentVersion.endsWith("-SNAPSHOT")) {
                currentVersion = currentVersion.substring(0, currentVersion.length() - "-SNAPSHOT".length());
            }

            if (!defaultValue.endsWith(currentVersion)) {

                final String newVersion = "SEB_Server_"  + currentVersion;
                configurationAttributeRecordMapper.updateByPrimaryKeySelective(new ConfigurationAttributeRecord(
                        record.getId(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        newVersion
                ));

                return "Updated out-dated SEB Server Version for SEB Setting originatorVersion (1000) from: " + defaultValue + " to: " + newVersion;
            }

            return "OK";
        });
    }
}
