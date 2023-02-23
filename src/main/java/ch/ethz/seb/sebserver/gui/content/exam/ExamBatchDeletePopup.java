/*
 * Copyright (c) 2023 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.exam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.tomcat.util.buf.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.API.BatchActionType;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.BatchAction;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.AbstractBatchActionWizard;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.push.ServerPushService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamsByIds;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;

@Lazy
@Component
@GuiProfile
public class ExamBatchDeletePopup extends AbstractBatchActionWizard {

    private final static LocTextKey FORM_TITLE =
            new LocTextKey("sebserver.exam.list.batch.delete.title");
    private final static LocTextKey ACTION_DO_RESET =
            new LocTextKey("sebserver.exam.list.batch.action.delete");
    private final static LocTextKey FORM_INFO =
            new LocTextKey("sebserver.exam.list.batch.action.delete.info");

    protected ExamBatchDeletePopup(final PageService pageService, final ServerPushService serverPushService) {
        super(pageService, serverPushService);
    }

    @Override
    protected LocTextKey getTitle() {
        return FORM_TITLE;
    }

    @Override
    protected LocTextKey getBatchActionInfo() {
        return FORM_INFO;
    }

    @Override
    protected LocTextKey getBatchActionTitle() {
        return ACTION_DO_RESET;
    }

    @Override
    protected BatchActionType getBatchActionType() {
        return BatchActionType.DELETE_EXAM;
    }

    @Override
    protected Supplier<PageContext> createResultPageSupplier(
            final PageContext pageContext,
            final FormHandle<ConfigurationNode> formHandle) {
        // No specific fields for this action
        return () -> pageContext;
    }

    @Override
    protected void extendBatchActionRequest(final PageContext pageContext,
            final RestCall<BatchAction>.RestCallBuilder batchActionRequestBuilder) {
        // Nothing to do here
    }

    @Override
    protected FormBuilder buildSpecificFormFields(
            final PageContext formContext,
            final FormBuilder formHead,
            final boolean readonly) {

        // No specific fields for this action
        return formHead;
    }

    @Override
    protected void processUpdateListAction(final PageContext formContext) {
        this.pageService.executePageAction(this.pageService.pageActionBuilder(formContext)
                .newAction(ActionDefinition.EXAM_VIEW_LIST)
                .create());
    }

    @Override
    protected void applySelectionList(
            final PageContext formContext,
            final Set<EntityKey> multiSelection) {

        final ResourceService resourceService = this.pageService.getResourceService();

        final String ids = StringUtils.join(
                multiSelection.stream().map(EntityKey::getModelId).collect(Collectors.toList()),
                Constants.LIST_SEPARATOR_CHAR);

        final RestService restService = this.pageService.getRestService();
        final List<Exam> selected = new ArrayList<>(restService.getBuilder(GetExamsByIds.class)
                .withQueryParam(API.PARAM_MODEL_ID_LIST, ids)
                .call()
                .getOr(Collections.emptyList()));

        selected.sort((exam1, exam2) -> exam1.name.compareTo(exam2.name));

        this.pageService.staticListTableBuilder(selected, EntityType.EXAM)
                .withPaging(10)
                .withColumn(new ColumnDefinition<>(
                        Domain.EXAM.ATTR_LMS_SETUP_ID,
                        ExamList.COLUMN_TITLE_LMS_KEY,
                        ExamList.examLmsSetupNameFunction(resourceService)))

                .withColumn(new ColumnDefinition<>(
                        Domain.EXAM.ATTR_QUIZ_NAME,
                        ExamList.COLUMN_TITLE_NAME_KEY,
                        Exam::getName))

                .withColumn(new ColumnDefinition<>(
                        Domain.EXAM.ATTR_STATUS,
                        ExamList.COLUMN_TITLE_STATE_KEY,
                        resourceService::localizedExamStatusName))

                .compose(formContext);
    }

}
