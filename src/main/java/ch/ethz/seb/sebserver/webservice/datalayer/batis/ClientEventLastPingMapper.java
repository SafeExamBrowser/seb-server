/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.datalayer.batis;

import java.util.Collection;

import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.dynamic.sql.select.MyBatis3SelectModelAdapter;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;
import org.mybatis.dynamic.sql.select.SelectDSL;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.mybatis.dynamic.sql.util.SqlProviderAdapter;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordDynamicSqlSupport;

@Mapper
public interface ClientEventLastPingMapper {

    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    Long num(SelectStatementProvider selectStatement);

    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    @ResultType(ClientEventLastPingRecord.class)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class, jdbcType = JdbcType.BIGINT),
            @Arg(column = "server_time", javaType = Long.class, jdbcType = JdbcType.BIGINT),
    })
    Collection<ClientEventLastPingRecord> selectMany(SelectStatementProvider select);

    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Collection<ClientEventLastPingRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(
                this::selectMany,

                ClientEventRecordDynamicSqlSupport.clientConnectionId.as("id"),
                ClientEventRecordDynamicSqlSupport.serverTime.as("server_time"))

                .from(ClientEventRecordDynamicSqlSupport.clientEventRecord);
    }

    final class ClientEventLastPingRecord {

        public final Long id;
        public final Long lastPingTime;

        public ClientEventLastPingRecord(
                final Long id,
                final Long server_time) {

            this.id = id;
            this.lastPingTime = server_time;
        }
    }

}
