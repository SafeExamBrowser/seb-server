/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.configs;

import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.API.BatchActionType;
import ch.ethz.seb.sebserver.gbl.model.BatchAction;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationStatus;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.AbstractBatchActionWizard;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.push.ServerPushService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;

@Lazy
@Component
@GuiProfile
public class SEBExamConfigBatchStateChangePopup extends AbstractBatchActionWizard {

    private static final String ATTR_SELECTED_TARGET_STATE = "selectedTargetState";

    private final static LocTextKey FORM_TITLE =
            new LocTextKey("sebserver.examconfig.list.batch.statechange.title");
    private final static LocTextKey ACTION_DO_STATE_CHANGE =
            new LocTextKey("sebserver.examconfig.list.batch.action.statechange");
    private final static LocTextKey FORM_INFO =
            new LocTextKey("sebserver.examconfig.list.batch.action.statechange.info");
    private final static LocTextKey FORM_STATUS_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.list.batch.action.status");

    protected SEBExamConfigBatchStateChangePopup(
            final PageService pageService,
            final ServerPushService serverPushService) {

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
        return ACTION_DO_STATE_CHANGE;
    }

    @Override
    protected BatchActionType getBatchActionType() {
        return BatchActionType.EXAM_CONFIG_STATE_CHANGE;
    }

    @Override
    public FormBuilder buildSpecificFormFields(
            final PageContext formContext,
            final FormBuilder formHead,
            final boolean readonly) {

        final String targetStateName = readonly
                ? formContext.getAttribute(ATTR_SELECTED_TARGET_STATE)
                : ConfigurationStatus.CONSTRUCTION.name();

        return formHead.addField(FormBuilder.singleSelection(
                Domain.CONFIGURATION_NODE.ATTR_STATUS,
                FORM_STATUS_TEXT_KEY,
                targetStateName,
                () -> this.pageService.getResourceService()
                        .examConfigStatusResources(false))
                .readonly(readonly));
    }

    @Override
    protected Supplier<PageContext> createResultPageSupplier(
            final PageContext pageContext,
            final FormHandle<ConfigurationNode> formHandle) {

        return () -> pageContext.withAttribute(
                ATTR_SELECTED_TARGET_STATE,
                formHandle.getForm().getFieldValue(Domain.CONFIGURATION_NODE.ATTR_STATUS));
    }

    @Override
    protected void extendBatchActionRequest(
            final PageContext pageContext,
            final RestCall<BatchAction>.RestCallBuilder batchActionRequestBuilder) {

        final String targetStateName = pageContext.getAttribute(ATTR_SELECTED_TARGET_STATE);
        if (StringUtils.isBlank(targetStateName)) {
            throw new IllegalArgumentException("missing " + ATTR_SELECTED_TARGET_STATE + " form pageContext");
        }

        batchActionRequestBuilder.withFormParam(BatchAction.ACTION_ATTRIBUT_TARGET_STATE, targetStateName);
    }

}
