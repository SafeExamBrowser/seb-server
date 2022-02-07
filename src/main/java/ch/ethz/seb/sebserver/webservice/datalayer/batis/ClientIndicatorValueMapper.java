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
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.dynamic.sql.select.MyBatis3SelectModelAdapter;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;
import org.mybatis.dynamic.sql.select.SelectDSL;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.mybatis.dynamic.sql.update.UpdateDSL;
import org.mybatis.dynamic.sql.update.render.UpdateStatementProvider;
import org.mybatis.dynamic.sql.util.SqlProviderAdapter;

import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientIndicatorRecordDynamicSqlSupport;

@Mapper
public interface ClientIndicatorValueMapper {

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
    @ResultType(ClientIndicatorValueRecord.class)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class, jdbcType = JdbcType.BIGINT),
            @Arg(column = "value", javaType = Long.class, jdbcType = JdbcType.BIGINT)
    })
    Collection<ClientIndicatorValueRecord> selectMany(SelectStatementProvider select);

    default Long selectValueByPrimaryKey(final Long id_) {
        return SelectDSL.selectWithMapper(
                this::selectPingTime,
                value.as("value"))
                .from(clientIndicatorRecord)
                .where(id, isEqualTo(id_))
                .build()
                .execute();
    }

    default Long indicatorRecordIdByConnectionId(final Long connectionId, final IndicatorType indicatorType) {
        return SelectDSL.selectDistinctWithMapper(
                this::selectPK,
                id.as("id"))
                .from(clientIndicatorRecord)
                .where(clientConnectionId, isEqualTo(connectionId))
                .and(type, isEqualTo(indicatorType.id))
                .build()
                .execute();
    }

    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Collection<ClientIndicatorValueRecord>>> selectByExample() {

        return SelectDSL.selectWithMapper(
                this::selectMany,
                id.as("id"),
                value.as("value"))
                .from(ClientIndicatorRecordDynamicSqlSupport.clientIndicatorRecord);
    }

    @Update("UPDATE client_indicator SET value = value + 1 WHERE id =#{pk}")
    int incrementIndicatorValue(final Long pk);

    @Update("UPDATE client_indicator SET value = value - 1 WHERE id =#{pk}")
    int decrementIndicatorValue(final Long pk);

    default int updateIndicatorValue(final Long pk, final Long v) {
        return UpdateDSL.updateWithMapper(this::update, clientIndicatorRecord)
                .set(value).equalTo(v)
                .where(id, isEqualTo(pk))
                .build()
                .execute();
    }

    final class ClientIndicatorValueRecord {

        public final Long id;
        public final Long indicatorValue;

        public ClientIndicatorValueRecord(
                final Long id,
                final Long value) {

            this.id = id;
            this.indicatorValue = value;
        }
    }

}
