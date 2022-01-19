package ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper;

import java.sql.JDBCType;
import javax.annotation.Generated;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

public final class CertificateRecordDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.212+01:00", comments="Source Table: certificate")
    public static final CertificateRecord certificateRecord = new CertificateRecord();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.212+01:00", comments="Source field: certificate.id")
    public static final SqlColumn<Long> id = certificateRecord.id;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.213+01:00", comments="Source field: certificate.institution_id")
    public static final SqlColumn<Long> institutionId = certificateRecord.institutionId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.213+01:00", comments="Source field: certificate.aliases")
    public static final SqlColumn<String> aliases = certificateRecord.aliases;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.213+01:00", comments="Source field: certificate.cert_store")
    public static final SqlColumn<byte[]> certStore = certificateRecord.certStore;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2022-01-18T17:36:21.212+01:00", comments="Source Table: certificate")
    public static final class CertificateRecord extends SqlTable {
        public final SqlColumn<Long> id = column("id", JDBCType.BIGINT);

        public final SqlColumn<Long> institutionId = column("institution_id", JDBCType.BIGINT);

        public final SqlColumn<String> aliases = column("aliases", JDBCType.VARCHAR);

        public final SqlColumn<byte[]> certStore = column("cert_store", JDBCType.BLOB);

        public CertificateRecord() {
            super("certificate");
        }
    }
}