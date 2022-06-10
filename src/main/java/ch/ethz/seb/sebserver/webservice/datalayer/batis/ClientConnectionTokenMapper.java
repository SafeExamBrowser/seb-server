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

import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientConnectionRecordDynamicSqlSupport;

@Mapper
public interface ClientConnectionTokenMapper {

    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    Long num(SelectStatementProvider selectStatement);

    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    @ResultType(ClientConnectionTokenRecord.class)
    @ConstructorArgs({
            @Arg(column = "connection_token", javaType = String.class, jdbcType = JdbcType.VARCHAR, id = true)
    })
    Collection<ClientConnectionTokenRecord> selectMany(SelectStatementProvider select);

    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Collection<ClientConnectionTokenRecord>>> selectByExample() {
        return SelectDSL.selectWithMapper(
                this::selectMany,

                ClientConnectionRecordDynamicSqlSupport.connectionToken.as("connection_token"),
                ClientConnectionRecordDynamicSqlSupport.updateTime.as("update_time"))

                .from(ClientConnectionRecordDynamicSqlSupport.clientConnectionRecord);
    }

    final class ClientConnectionTokenRecord {

        public final String connection_token;

        public ClientConnectionTokenRecord(final String connection_token) {
            this.connection_token = connection_token;
        }
    }

}
