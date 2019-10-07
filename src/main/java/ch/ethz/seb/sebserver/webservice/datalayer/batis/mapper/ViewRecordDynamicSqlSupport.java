package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class ViewRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-10-07T11:14:30.892+02:00", comments="Source Table: view")
    public static final ViewRecord viewRecord = new ViewRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-10-07T11:14:30.892+02:00", comments="Source field: view.id")
    public static final SqlColumn<Long> id = viewRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-10-07T11:14:30.892+02:00", comments="Source field: view.name")
    public static final SqlColumn<String> name = viewRecord.name;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-10-07T11:14:30.892+02:00", comments="Source field: view.columns")
    public static final SqlColumn<Integer> columns = viewRecord.columns;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-10-07T11:14:30.892+02:00", comments="Source field: view.position")
    public static final SqlColumn<Integer> position = viewRecord.position;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2019-10-07T11:14:30.892+02:00", comments="Source Table: view")
    public static final class ViewRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<String> name = column("name", JDBCType.VARCHAR);

        public final SqlColumn<Integer> columns = column("columns", JDBCType.INTEGER);

        public final SqlColumn<Integer> position = column("position", JDBCType.INTEGER);

        public ViewRecord() {
            super("view");
        }
    }
}