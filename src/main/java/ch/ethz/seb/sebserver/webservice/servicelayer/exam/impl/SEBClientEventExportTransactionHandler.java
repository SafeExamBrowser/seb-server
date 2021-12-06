/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.equalTo;
import static org.mybatis.dynamic.sql.SqlBuilder.isEqualToWhenPresent;

import java.util.Collection;
import java.util.function.Predicate;

import org.mybatis.dynamic.sql.SqlBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientConnectionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientConnectionRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ClientEventRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientConnectionRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;

@Lazy
@Component
@WebServiceProfile
public class SEBClientEventExportTransactionHandler {

    private final ClientEventRecordMapper clientEventRecordMapper;
    private final ClientConnectionRecordMapper clientConnectionRecordMapper;
    private final ExamDAO examDAO;

    public SEBClientEventExportTransactionHandler(
            final ClientEventRecordMapper clientEventRecordMapper,
            final ClientConnectionRecordMapper clientConnectionRecordMapper,
            final ExamDAO examDAO) {

        this.clientEventRecordMapper = clientEventRecordMapper;
        this.clientConnectionRecordMapper = clientConnectionRecordMapper;
        this.examDAO = examDAO;
    }

    @Transactional(readOnly = true)
    public Result<Collection<ClientEventRecord>> allMatching(
            final FilterMap filterMap,
            final Predicate<ClientEvent> predicate) {

        return Result.tryCatch(() -> this.clientEventRecordMapper
                .selectByExample()
                .leftJoin(ClientConnectionRecordDynamicSqlSupport.clientConnectionRecord)
                .on(
                        ClientConnectionRecordDynamicSqlSupport.id,
                        equalTo(ClientEventRecordDynamicSqlSupport.clientConnectionId))
                .where(
                        ClientConnectionRecordDynamicSqlSupport.institutionId,
                        isEqualToWhenPresent(filterMap.getInstitutionId()))
                .and(
                        ClientConnectionRecordDynamicSqlSupport.examId,
                        isEqualToWhenPresent(filterMap.getClientEventExamId()))
                .and(
                        ClientConnectionRecordDynamicSqlSupport.examUserSessionId,
                        SqlBuilder.isLikeWhenPresent(filterMap.getSQLWildcard(ClientConnection.FILTER_ATTR_SESSION_ID)))
                .and(
                        ClientEventRecordDynamicSqlSupport.clientConnectionId,
                        isEqualToWhenPresent(filterMap.getClientEventConnectionId()))
                .and(
                        ClientEventRecordDynamicSqlSupport.type,
                        isEqualToWhenPresent(filterMap.getClientEventTypeId()))
                .and(
                        ClientEventRecordDynamicSqlSupport.type,
                        SqlBuilder.isNotEqualTo(5)) //  formerly defined as EventType.LAST_PING
                .and(
                        ClientEventRecordDynamicSqlSupport.clientTime,
                        SqlBuilder.isGreaterThanOrEqualToWhenPresent(filterMap.getClientEventClientTimeFrom()))
                .and(
                        ClientEventRecordDynamicSqlSupport.clientTime,
                        SqlBuilder.isLessThanOrEqualToWhenPresent(filterMap.getClientEventClientTimeTo()))
                .and(
                        ClientEventRecordDynamicSqlSupport.serverTime,
                        SqlBuilder.isGreaterThanOrEqualToWhenPresent(filterMap.getClientEventServerTimeFrom()))
                .and(
                        ClientEventRecordDynamicSqlSupport.serverTime,
                        SqlBuilder.isLessThanOrEqualToWhenPresent(filterMap.getClientEventServerTimeTo()))
                .and(
                        ClientEventRecordDynamicSqlSupport.text,
                        SqlBuilder.isLikeWhenPresent(filterMap.getClientEventText()))
                .build()
                .execute());
    }

    @Transactional(readOnly = true)
    public Result<ClientConnectionRecord> clientConnectionById(final Long id) {
        return Result.tryCatch(() -> this.clientConnectionRecordMapper.selectByPrimaryKey(id));
    }

    public Result<Exam> examById(final Long id) {
        return this.examDAO.byPK(id);
    }

}
