/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.admin;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEvent;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputWizard;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputWizard.WizardAction;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputWizard.WizardPage;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.logs.DeleteAllUserLogs;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

@Lazy
@Component
@GuiProfile
public class UserActivityLogsDeletePopup {

    private static final Logger log = LoggerFactory.getLogger(UserActivityLogsDeletePopup.class);

    private final static LocTextKey FORM_TITLE =
            new LocTextKey("sebserver.userlogs.delete.form.title");
    private final static LocTextKey ACTION_DELETE =
            new LocTextKey("sebserver.userlogs.delete.action.delete");
    private final static LocTextKey DELETE_CONFIRM_TITLE =
            new LocTextKey("sebserver.userlogs.delete.confirm.title");

    private final PageService pageService;

    protected UserActivityLogsDeletePopup(final PageService pageService) {
        this.pageService = pageService;
    }

    public Function<PageAction, PageAction> deleteWizardFunction(final PageContext pageContext) {
        return action -> {

            final ModalInputWizard<PageContext> wizard =
                    new ModalInputWizard<PageContext>(
                            action.pageContext().getParent().getShell(),
                            this.pageService.getWidgetFactory())
                                    .setVeryLargeDialogWidth();

            final String page1Id = "DELETE_PAGE";
            final Predicate<PageContext> callback = pc -> doDelete(this.pageService, pc);
            final BiFunction<PageContext, Composite, Supplier<PageContext>> composePage1 =
                    (prefPageContext, content) -> composeDeleteDialog(content,
                            (prefPageContext != null) ? prefPageContext : pageContext);

            final WizardPage<PageContext> page1 = new WizardPage<>(
                    page1Id,
                    true,
                    composePage1,
                    new WizardAction<>(ACTION_DELETE, callback));

            wizard.open(FORM_TITLE, Utils.EMPTY_EXECUTION, page1);

            return action;
        };
    }

    private boolean doDelete(
            final PageService pageService,
            final PageContext pageContext) {

        try {
            final String idsToDelete = pageContext.getAttribute(PageContext.AttributeKeys.ENTITY_ID_LIST);

            final RestCall<EntityProcessingReport>.RestCallBuilder restCallBuilder = this.pageService.getRestService()
                    .getBuilder(DeleteAllUserLogs.class)
                    .withFormParam(API.PARAM_MODEL_ID_LIST, idsToDelete)
                    .withFormParam(API.PARAM_BULK_ACTION_TYPE, BulkActionType.HARD_DELETE.name());

            final EntityProcessingReport report = restCallBuilder.call().getOrThrow();

            final PageAction action = this.pageService.pageActionBuilder(pageContext)
                    .newAction(ActionDefinition.LOGS_USER_ACTIVITY_LIST)
                    .create();

            this.pageService.firePageEvent(
                    new ActionEvent(action),
                    action.pageContext());

            pageContext.publishPageMessage(
                    DELETE_CONFIRM_TITLE,
                    new LocTextKey(
                            "sebserver.userlogs.delete.confirm.message",
                            report.results.size(),
                            (report.errors.isEmpty()) ? "no" : String.valueOf((report.errors.size()))));

            return true;
        } catch (final Exception e) {
            log.error("Unexpected error while trying to delete user activity logs:", e);
            pageContext.notifyUnexpectedError(e);
            return false;
        }
    }

    private Supplier<PageContext> composeDeleteDialog(
            final Composite parent,
            final PageContext pageContext) {

        final String idsToDelete = pageContext.getAttribute(PageContext.AttributeKeys.ENTITY_ID_LIST);
        final int number = (StringUtils.isNotBlank(idsToDelete))
                ? idsToDelete.split(Constants.LIST_SEPARATOR).length
                : 0;

        final Composite grid = this.pageService.getWidgetFactory()
                .createPopupScrollComposite(parent);

        final Label title = this.pageService.getWidgetFactory().labelLocalized(
                grid,
                CustomVariant.TEXT_H3,
                new LocTextKey("sebserver.userlogs.delete.form.info", number));
        final GridData gridData = new GridData();
        gridData.horizontalIndent = 10;
        gridData.verticalIndent = 10;
        title.setLayoutData(gridData);

        return () -> pageContext;
    }

}
