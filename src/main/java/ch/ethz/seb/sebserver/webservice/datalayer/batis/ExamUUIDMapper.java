/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.datalayer.batis;

import java.util.Collection;

import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordDynamicSqlSupport;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.dynamic.sql.select.MyBatis3SelectModelAdapter;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;
import org.mybatis.dynamic.sql.select.SelectDSL;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.mybatis.dynamic.sql.util.SqlProviderAdapter;

@Mapper
public interface ExamUUIDMapper {

    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    Long num(SelectStatementProvider selectStatement);

    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    @ResultType(ExamUUID.class)
    @ConstructorArgs({
            @Arg(column = "external_id", javaType = String.class, jdbcType = JdbcType.VARCHAR, id = true)
    })
    Collection<ExamUUID> selectMany(SelectStatementProvider select);

    default QueryExpressionDSL<MyBatis3SelectModelAdapter<Collection<ExamUUID>>> selectByExample() {
        return SelectDSL.selectWithMapper(
                        this::selectMany,
                        ExamRecordDynamicSqlSupport.externalId.as("uuid"))

                .from(ExamRecordDynamicSqlSupport.examRecord);
    }

    final class ExamUUID {

        public final String uuid;

        public ExamUUID(final String uuid) {
            this.uuid = uuid;
        }
    }
}
