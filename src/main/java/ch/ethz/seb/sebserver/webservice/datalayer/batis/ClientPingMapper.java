/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.datalayer.batis;

import static ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientIndicatorRecordDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;

import java.util.Collection;

import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.dynamic.sql.select.MyBatis3SelectModelAdapter;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;
import org.mybatis.dynamic.sql.select.SelectDSL;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.mybatis.dynamic.sql.update.UpdateDSL;
import org.mybatis.dynamic.sql.update.render.UpdateStatementProvider;
import org.mybatis.dynamic.sql.util.SqlProviderAdapter;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientIndicatorRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.indicator.ClientIndicatorType;

@Mapper
public interface ClientPingMapper {

    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    @ConstructorArgs({ @Arg(column = "value", javaType = Long.class, jdbcType = JdbcType.BIGINT) })
    Collection<Long> selectPingTimes(SelectStatementProvider selectStatement);

    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    @ConstructorArgs({ @Arg(column = "value", javaType = Long.class, jdbcType = JdbcType.BIGINT) })
    Long selectPingTime(SelectStatementProvider selectStatement);

    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    @ConstructorArgs({ @Arg(column = "id", javaType = Long.class, jdbcType = JdbcType.BIGINT, id = true) })
    Long selectPK(SelectStatementProvider selectStatement);

    @UpdateProvider(type = SqlProviderAdapter.class, method = "update")
    int update(UpdateStatementProvider updateStatement);

    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    @ResultType(ClientLastPingRecord.class)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class, jdbcType = JdbcType.BIGINT),
            @Arg(column = "value", javaType = Long.class, jdbcType = JdbcType.BIGINT)
    })
    Collection<ClientLastPingRecord> selectMany(SelectStatementProvider select);

    default Long selectPingTimeByPrimaryKey(final Long id_) {
        return SelectDSL.selectWithMapper(
                this::selectPingTime,
                value.as("value"))
                .from(clientIndicatorRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    default Long pingRecordIdByConnectionId(final Long connectionId) {
        return SelectDSL.selectDistinctWithMapper(
                this::selectPK,
                id.as("id"))
                .from(clientIndicatorRecord)
                .where(clientConnectionId, isEqualTo(connectionId))
                .and(type, isEqualTo(ClientIndicatorType.LAST_PING.id))
                .build()
                .execute();
    }

    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Collection<ClientLastPingRecord>>> selectByExample() {

        return SelectDSL.selectWithMapper(
                this::selectMany,
                id.as("id"),
                value.as("value"))
                .from(ClientIndicatorRecordDynamicSqlSupport.clientIndicatorRecord);
    }

    default int updatePingTime(final Long _id, final Long pingTime) {
        return UpdateDSL.updateWithMapper(this::update, clientIndicatorRecord)
                .set(value).equalTo(pingTime)
                .where(id, isEqualTo(_id))
                .build()
                .execute();
    }

    final class ClientLastPingRecord {

        public final Long id;
        public final Long lastPingTime;

        public ClientLastPingRecord(
                final Long id,
                final Long value) {

            this.id = id;
            this.lastPingTime = value;
        }
    }

}
