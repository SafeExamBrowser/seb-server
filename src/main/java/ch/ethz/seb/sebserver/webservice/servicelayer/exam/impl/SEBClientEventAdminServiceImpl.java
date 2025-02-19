/*
 * Copyright (c) 2021 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport.ErrorEntry;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.ExportType;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientConnectionRecord;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientEventRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientEventDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.SEBClientEventAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.SEBClientEventExporter;

@Lazy
@Service
@WebServiceProfile
public class SEBClientEventAdminServiceImpl implements SEBClientEventAdminService {

    private static final Logger log = LoggerFactory.getLogger(SEBClientEventAdminServiceImpl.class);
    
    private final ClientEventDAO clientEventDAO;
    private final SEBClientEventExportTransactionHandler sebClientEventExportTransactionHandler;
    private final EnumMap<ExportType, SEBClientEventExporter> exporter;

    public SEBClientEventAdminServiceImpl(
            final ClientEventDAO clientEventDAO,
            final SEBClientEventExportTransactionHandler sebClientEventExportTransactionHandler,
            final Collection<SEBClientEventExporter> exporter) {
        
        this.clientEventDAO = clientEventDAO;
        this.sebClientEventExportTransactionHandler = sebClientEventExportTransactionHandler;

        this.exporter = new EnumMap<>(ExportType.class);
        exporter.forEach(exp -> this.exporter.putIfAbsent(exp.exportType(), exp));
    }

    @Override
    public Result<EntityProcessingReport> deleteAllClientEvents(final Collection<String> ids) {
        return Result.tryCatch(() -> {

            if (ids == null || ids.isEmpty()) {
                return EntityProcessingReport.ofEmptyError();
            }

            final Set<EntityKey> sources = ids.stream()
                    .map(id -> new EntityKey(id, EntityType.CLIENT_EVENT))
                    .collect(Collectors.toSet());

            final Result<Collection<EntityKey>> delete = this.clientEventDAO.delete(sources);
            if (delete.hasError()) {
                return new EntityProcessingReport(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Arrays.asList(new ErrorEntry(null, APIMessage.ErrorMessage.UNEXPECTED.of(delete.getError()))),
                        BulkActionType.HARD_DELETE);
            } else {
                return new EntityProcessingReport(
                        sources,
                        delete.get(),
                        Collections.emptyList(),
                        BulkActionType.HARD_DELETE);
            }
        });
    }

    @Override
    public void exportSEBClientLogs(
            final OutputStream output,
            final FilterMap filterMap,
            final String sort,
            final ExportType exportType,
            final boolean includeConnectionDetails,
            final boolean includeExamDetails,
            final SEBServerUser currentUser) {

        try {

            new exportRunner(
                    this.exporter.get(exportType),
                    includeConnectionDetails,
                    includeExamDetails,
                    filterMap, sort,
                    output).run(currentUser);
            
        } catch (final Exception e) {
            log.error("Unexpected error during export SEB logs: ", e);
        } finally {
            try {
                output.flush();
            } catch (final IOException e) {
                log.error("Failed to flush export data: ", e);
            }
            IOUtils.closeQuietly(output);
        }

    }

    private class exportRunner {

        private final SEBClientEventExporter exporter;
        private final boolean includeConnectionDetails;
        private final boolean includeExamDetails;
        private final FilterMap filterMap;
        private final String sort;
        private final OutputStream output;

        private final Map<Long, Exam> examCache;
        private final Map<Long, ClientConnectionRecord> connectionCache;

        public exportRunner(
                final SEBClientEventExporter exporter,
                final boolean includeConnectionDetails,
                final boolean includeExamDetails,
                final FilterMap filterMap,
                final String sort,
                final OutputStream output) {

            this.exporter = exporter;
            this.includeConnectionDetails = includeConnectionDetails;
            this.includeExamDetails = includeExamDetails;
            this.filterMap = filterMap;
            this.sort = sort;
            this.output = output;

            this.connectionCache = new HashMap<>();
            this.examCache = new HashMap<>();
        }

        public void run(final SEBServerUser currentUser) {

            final EnumSet<UserRole> userRoles = currentUser.getUserRoles();
            final boolean isSupporterOnly = userRoles.size() == 1 && userRoles.contains(UserRole.EXAM_SUPPORTER);

            // first stream header line
            this.exporter.streamHeader(this.output, this.includeConnectionDetails, this.includeExamDetails);
            
            // get all involved connection ids
            sebClientEventExportTransactionHandler
                    .getConnectionIds(filterMap, sort)
                    .map(cIds -> {
                        
                        if (log.isDebugEnabled()) {
                            log.debug("Export for {} SEB clients connection", cIds.size());
                        }

                        cIds.forEach( cid -> {

                            final ClientConnectionRecord connection = getConnection(cid);
                            final Collection<ClientEventRecord> records = sebClientEventExportTransactionHandler
                                    .getEvents(cid, filterMap, sort)
                                    .onError(error -> log.error("Failed to get SEB Events for connection: {}", connection))
                                    .getOr(Collections.emptyList());

                            records.forEach( rec -> {
                                this.exporter.streamData(
                                this.output,
                                rec,
                                this.includeConnectionDetails ? connection : null,
                                this.includeExamDetails ? getExam(rec.getClientConnectionId()) : null);
                            });
                        });
                        
                        return cIds;
                    })
                    .onError(error -> log.error("Failed to export SEB event: ", error));
        }

        private ClientConnectionRecord getConnection(final Long connectionId) {
            if (!this.connectionCache.containsKey(connectionId)) {
                SEBClientEventAdminServiceImpl.this.sebClientEventExportTransactionHandler
                        .clientConnectionById(connectionId)
                        .onSuccess(rec -> this.connectionCache.put(rec.getId(), rec))
                        .onError(error -> log.error("Failed to get ClientConnectionRecord for id: {}",
                                connectionId,
                                error));
            }

            return this.connectionCache.get(connectionId);
        }

        private Exam getExam(final Long connectionId) {
            final ClientConnectionRecord connection = getConnection(connectionId);
            final Long examId = connection.getExamId();
            if (!this.examCache.containsKey(examId)) {
                SEBClientEventAdminServiceImpl.this.sebClientEventExportTransactionHandler
                        .examById(examId)
                        .onSuccess(e -> this.examCache.put(examId, e))
                        .onError(error -> log.error("Failed to get Exam for id: {}",
                                examId,
                                error));
            }

            return this.examCache.get(examId);
        }
    }

}
