/*
 * Copyright (c) 2020 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.admin;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.ErrorMessage;
import ch.ethz.seb.sebserver.gbl.model.EntityDependency;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.institution.Institution;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEvent;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputWizard;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputWizard.WizardAction;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputWizard.WizardPage;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCallError;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.DeleteInstitution;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.GetInstitution;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.GetInstitutionDependency;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

@Lazy
@Component
@GuiProfile
public class InstitutionDeletePopup {

    private static final Logger log = LoggerFactory.getLogger(InstitutionDeletePopup.class);

    private final static LocTextKey FORM_TITLE =
            new LocTextKey("sebserver.institution.delete.form.title");
    private final static LocTextKey FORM_INFO =
            new LocTextKey("sebserver.institution.delete.form.info");
    private final static LocTextKey FORM_REPORT_INFO =
            new LocTextKey("sebserver.institution.delete.report.info");
    private final static LocTextKey FORM_REPORT_LIST_TYPE =
            new LocTextKey("sebserver.institution.delete.report.list.type");
    private final static LocTextKey FORM_REPORT_LIST_NAME =
            new LocTextKey("sebserver.institution.delete.report.list.name");
    private final static LocTextKey FORM_REPORT_LIST_DESC =
            new LocTextKey("sebserver.institution.delete.report.list.description");
    private final static LocTextKey FORM_REPORT_NONE =
            new LocTextKey("sebserver.institution.delete.report.list.empty");

    private final static LocTextKey ACTION_DELETE =
            new LocTextKey("sebserver.institution.delete.action.delete");

    private final static LocTextKey DELETE_CONFIRM_TITLE =
            new LocTextKey("sebserver.institution.delete.confirm.title");
    private final static LocTextKey DELETE_ERROR_CONSISTENCY =
            new LocTextKey("sebserver.institution.action.delete.consistency.error");

    private final PageService pageService;

    protected InstitutionDeletePopup(final PageService pageService) {
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
            final EntityKey entityKey = pageContext.getEntityKey();
            final Institution toDelete = this.pageService
                    .getRestService()
                    .getBuilder(GetInstitution.class)
                    .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                    .call()
                    .getOrThrow();

            final RestCall<EntityProcessingReport>.RestCallBuilder restCallBuilder = this.pageService.getRestService()
                    .getBuilder(DeleteInstitution.class)
                    .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                    .withQueryParam(API.PARAM_BULK_ACTION_TYPE, BulkActionType.HARD_DELETE.name());

            final Result<EntityProcessingReport> deleteCall = restCallBuilder.call();
            if (deleteCall.hasError()) {
                final Exception error = deleteCall.getError();
                if (error instanceof RestCallError) {
                    final APIMessage message = ((RestCallError) error)
                            .getAPIMessages()
                            .stream()
                            .findFirst()
                            .orElse(null);
                    if (message != null && ErrorMessage.INTEGRITY_VALIDATION.isOf(message)) {
                        pageContext.publishPageMessage(new PageMessageException(DELETE_ERROR_CONSISTENCY));
                        return false;
                    }
                }
            }

            final EntityProcessingReport report = deleteCall.getOrThrow();

            final PageAction action = this.pageService.pageActionBuilder(pageContext)
                    .newAction(ActionDefinition.INSTITUTION_VIEW_LIST)
                    .create();

            this.pageService.firePageEvent(
                    new ActionEvent(action),
                    action.pageContext());

            final List<EntityKey> dependencies = report.results.stream()
                    .filter(key -> !key.equals(entityKey))
                    .collect(Collectors.toList());
            pageContext.publishPageMessage(
                    DELETE_CONFIRM_TITLE,
                    new LocTextKey(
                            "sebserver.institution.delete.confirm.message",
                            toDelete.toName().name,
                            dependencies.size(),
                            (report.errors.isEmpty()) ? "no" : String.valueOf((report.errors.size()))));
            return true;
        } catch (final Exception e) {
            log.error("Unexpected error while trying to delete Institution:", e);
            pageContext.notifyUnexpectedError(e);
            return false;
        }
    }

    private Supplier<PageContext> composeDeleteDialog(
            final Composite parent,
            final PageContext pageContext) {

        final I18nSupport i18nSupport = this.pageService.getI18nSupport();
        final Composite grid = this.pageService.getWidgetFactory()
                .createPopupScrollComposite(parent);

        final Label title = this.pageService.getWidgetFactory()
                .labelLocalized(grid, CustomVariant.TEXT_H3, FORM_INFO);
        final GridData gridData = new GridData();
        gridData.horizontalIndent = 10;
        gridData.verticalIndent = 10;
        title.setLayoutData(gridData);

        final Label titleReport = this.pageService.getWidgetFactory()
                .labelLocalized(grid, CustomVariant.TEXT_H3, FORM_REPORT_INFO);
        final GridData gridDataReport = new GridData();
        gridDataReport.horizontalIndent = 10;
        gridDataReport.verticalIndent = 10;
        titleReport.setLayoutData(gridDataReport);

        try {

            // get dependencies
            final EntityKey entityKey = pageContext.getEntityKey();
            final RestCall<Set<EntityDependency>>.RestCallBuilder restCallBuilder = this.pageService.getRestService()
                    .getBuilder(GetInstitutionDependency.class)
                    .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                    .withQueryParam(API.PARAM_BULK_ACTION_TYPE, BulkActionType.HARD_DELETE.name());

            final Set<EntityDependency> dependencies = restCallBuilder
                    .call()
                    .getOrThrow();
            final List<EntityDependency> list = dependencies
                    .stream()
                    .sorted()
                    .collect(Collectors.toList());

            this.pageService.<EntityDependency> staticListTableBuilder(list, null)
                    .withEmptyMessage(FORM_REPORT_NONE)
                    .withColumn(new ColumnDefinition<>(
                            "FORM_REPORT_LIST_TYPE",
                            FORM_REPORT_LIST_TYPE,
                            dep -> i18nSupport
                                    .getText("sebserver.overall.types.entityType." + dep.self.entityType.name())))
                    .withColumn(new ColumnDefinition<>(
                            "FORM_REPORT_LIST_NAME",
                            FORM_REPORT_LIST_NAME,
                            dep -> dep.name))
                    .withColumn(new ColumnDefinition<EntityDependency>(
                            "FORM_REPORT_LIST_DESC",
                            FORM_REPORT_LIST_DESC,
                            dep -> dep.description))
                    .compose(pageContext.copyOf(grid));

            return () -> pageContext;
        } catch (final Exception e) {
            log.error("Error while trying to compose Institution delete report page: ", e);
            pageContext.notifyUnexpectedError(e);
            throw e;
        }
    }

}
