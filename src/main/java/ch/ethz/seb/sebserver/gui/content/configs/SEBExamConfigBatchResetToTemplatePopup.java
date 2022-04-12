/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.configs;

import java.util.function.Supplier;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.API.BatchActionType;
import ch.ethz.seb.sebserver.gbl.model.BatchAction;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
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
public class SEBExamConfigBatchResetToTemplatePopup extends AbstractBatchActionWizard {

    private final static LocTextKey FORM_TITLE =
            new LocTextKey("sebserver.examconfig.list.batch.reset.title");
    private final static LocTextKey ACTION_DO_RESET =
            new LocTextKey("sebserver.examconfig.list.batch.action.reset");
    private final static LocTextKey FORM_INFO =
            new LocTextKey("sebserver.examconfig.list.batch.action.reset.info");

    protected SEBExamConfigBatchResetToTemplatePopup(
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
        return ACTION_DO_RESET;
    }

    @Override
    protected BatchActionType getBatchActionType() {
        return BatchActionType.EXAM_CONFIG_REST_TEMPLATE_SETTINGS;
    }

    @Override
    protected Supplier<PageContext> createResultPageSupplier(
            final PageContext pageContext,
            final FormHandle<ConfigurationNode> formHandle) {

        // No specific fields for this action
        return () -> pageContext;
    }

    @Override
    protected void extendBatchActionRequest(
            final PageContext pageContext,
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

}
