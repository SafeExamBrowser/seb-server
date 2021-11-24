/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.datalayer.batis;

import static org.mybatis.dynamic.sql.SqlBuilder.equalTo;

import java.util.Collection;

import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.dynamic.sql.BasicColumn;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.select.MyBatis3SelectModelAdapter;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;
import org.mybatis.dynamic.sql.select.SelectDSL;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.mybatis.dynamic.sql.util.SqlProviderAdapter;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientConnectionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordDynamicSqlSupport;

@Mapper
public interface ClientEventExtensionMapper {

    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    Long num(SelectStatementProvider selectStatement);

    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> maxByExample(final BasicColumn column) {
        return SelectDSL.selectWithMapper(this::num, SqlBuilder.max(column))
                .from(ClientEventRecordDynamicSqlSupport.clientEventRecord);
    }

    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Long>> minByExample(final BasicColumn column) {
        return SelectDSL.selectWithMapper(this::num, SqlBuilder.min(column))
                .from(ClientEventRecordDynamicSqlSupport.clientEventRecord);
    }

    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    @ResultType(ClientEventExtensionMapper.ConnectionEventJoinRecord.class)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class, jdbcType = JdbcType.BIGINT, id = true),
            @Arg(column = "connection_id", javaType = Long.class, jdbcType = JdbcType.BIGINT),
            @Arg(column = "type", javaType = Integer.class, jdbcType = JdbcType.INTEGER),
            @Arg(column = "client_time", javaType = Long.class, jdbcType = JdbcType.BIGINT),
            @Arg(column = "server_time", javaType = Long.class, jdbcType = JdbcType.BIGINT),
            @Arg(column = "numeric_value", javaType = Double.class, jdbcType = JdbcType.DECIMAL),
            @Arg(column = "text", javaType = String.class, jdbcType = JdbcType.VARCHAR),

            @Arg(column = "institution_id", javaType = Long.class, jdbcType = JdbcType.BIGINT),
            @Arg(column = "exam_id", javaType = Long.class, jdbcType = JdbcType.BIGINT),
            @Arg(column = "exam_user_session_identifier", javaType = String.class, jdbcType = JdbcType.VARCHAR)
    })
    Collection<ConnectionEventJoinRecord> selectMany(SelectStatementProvider select);

    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Collection<ConnectionEventJoinRecord>>>.JoinSpecificationFinisher selectByExample() {
        return SelectDSL.selectWithMapper(
                this::selectMany,

                ClientEventRecordDynamicSqlSupport.id,
                ClientEventRecordDynamicSqlSupport.clientConnectionId.as("connection_id"),
                ClientEventRecordDynamicSqlSupport.type,
                ClientEventRecordDynamicSqlSupport.clientTime.as("client_time"),
                ClientEventRecordDynamicSqlSupport.serverTime.as("server_time"),
                ClientEventRecordDynamicSqlSupport.numericValue.as("numeric_value"),
                ClientEventRecordDynamicSqlSupport.text,

                ClientConnectionRecordDynamicSqlSupport.institutionId.as("institution_id"),
                ClientConnectionRecordDynamicSqlSupport.examId.as("exam_id"),
                ClientConnectionRecordDynamicSqlSupport.examUserSessionId.as("exam_user_session_identifier"))

                .from(ClientEventRecordDynamicSqlSupport.clientEventRecord)

                .join(ClientConnectionRecordDynamicSqlSupport.clientConnectionRecord)
                .on(
                        ClientEventRecordDynamicSqlSupport.clientEventRecord.clientConnectionId,
                        equalTo(ClientConnectionRecordDynamicSqlSupport.clientConnectionRecord.id));
    }

    final class ConnectionEventJoinRecord {

        public final Long id;
        public final Long connection_id;
        public final Integer type;
        public final Long client_time;
        public final Long server_time;
        public final Double numeric_value;
        public final String text;

        public final Long institution_id;
        public final Long exam_id;
        public final String exam_user_session_identifier;

        protected ConnectionEventJoinRecord(
                final Long id,
                final Long connection_id,
                final Integer type,
                final Long client_time,
                final Long server_time,
                final Double numeric_value,
                final String text,

                final Long institution_id,
                final Long exam_id,
                final String exam_user_session_identifier) {

            this.id = id;
            this.connection_id = connection_id;
            this.type = type;
            this.client_time = client_time;
            this.server_time = server_time;
            this.numeric_value = numeric_value;
            this.text = text;

            this.institution_id = institution_id;
            this.exam_id = exam_id;
            this.exam_user_session_identifier = exam_user_session_identifier;
        }
    }

}
