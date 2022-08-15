/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.BatchAction;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.BatchActionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BatchActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.EntityDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserActivityLogDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.validation.BeanValidationService;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.BATCH_ACTION_ENDPOINT)
public class BatchActionController extends EntityController<BatchAction, BatchAction> {

    private final BatchActionService batchActionService;

    protected BatchActionController(
            final AuthorizationService authorization,
            final BulkActionService bulkActionService,
            final EntityDAO<BatchAction, BatchAction> entityDAO,
            final UserActivityLogDAO userActivityLogDAO,
            final PaginationService paginationService,
            final BeanValidationService beanValidationService,
            final BatchActionService batchActionService) {

        super(
                authorization,
                bulkActionService,
                entityDAO,
                userActivityLogDAO,
                paginationService,
                beanValidationService);

        this.batchActionService = batchActionService;
    }

    @Override
    protected BatchAction createNew(final POSTMapper postParams) {
        return new BatchAction(
                null,
                super.authorization.getUserService().getCurrentUser().getUserInfo().uuid,
                postParams);
    }

    @Override
    protected Result<BatchAction> validForCreate(final BatchAction entity) {
        return this.batchActionService.validate(entity);
    }

    @Override
    protected Result<BatchAction> validForSave(final BatchAction entity) {
        throw new UnsupportedOperationException("Save already existing BatchAction is not supported");
    }

    @Override
    protected Result<BatchAction> notifyCreated(final BatchAction entity) {
        return this.batchActionService.notifyNewBatchAction(entity);
    }

    @Override
    protected SqlTable getSQLTableOfEntity() {
        return BatchActionRecordDynamicSqlSupport.batchActionRecord;
    }

}
