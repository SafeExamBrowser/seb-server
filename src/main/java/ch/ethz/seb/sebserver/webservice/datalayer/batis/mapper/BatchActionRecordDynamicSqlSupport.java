package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class BatchActionRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.076+02:00", comments="Source Table: batch_action")
    public static final BatchActionRecord batchActionRecord = new BatchActionRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.076+02:00", comments="Source field: batch_action.id")
    public static final SqlColumn<Long> id = batchActionRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.076+02:00", comments="Source field: batch_action.institution_id")
    public static final SqlColumn<Long> institutionId = batchActionRecord.institutionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.076+02:00", comments="Source field: batch_action.owner")
    public static final SqlColumn<String> owner = batchActionRecord.owner;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.076+02:00", comments="Source field: batch_action.action_type")
    public static final SqlColumn<String> actionType = batchActionRecord.actionType;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.076+02:00", comments="Source field: batch_action.attributes")
    public static final SqlColumn<String> attributes = batchActionRecord.attributes;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.076+02:00", comments="Source field: batch_action.source_ids")
    public static final SqlColumn<String> sourceIds = batchActionRecord.sourceIds;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.076+02:00", comments="Source field: batch_action.successful")
    public static final SqlColumn<String> successful = batchActionRecord.successful;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.076+02:00", comments="Source field: batch_action.last_update")
    public static final SqlColumn<Long> lastUpdate = batchActionRecord.lastUpdate;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.076+02:00", comments="Source field: batch_action.processor_id")
    public static final SqlColumn<String> processorId = batchActionRecord.processorId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-04-06T16:51:31.076+02:00", comments="Source Table: batch_action")
    public static final class BatchActionRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> institutionId = column("institution_id", JDBCType.BIGINT);

        public final SqlColumn<String> owner = column("owner", JDBCType.VARCHAR);

        public final SqlColumn<String> actionType = column("action_type", JDBCType.VARCHAR);

        public final SqlColumn<String> attributes = column("attributes", JDBCType.VARCHAR);

        public final SqlColumn<String> sourceIds = column("source_ids", JDBCType.VARCHAR);

        public final SqlColumn<String> successful = column("successful", JDBCType.VARCHAR);

        public final SqlColumn<Long> lastUpdate = column("last_update", JDBCType.BIGINT);

        public final SqlColumn<String> processorId = column("processor_id", JDBCType.VARCHAR);

        public BatchActionRecord() {
            super("batch_action");
        }
    }
}