/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.swt.widgets.Composite;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityDependency;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.form.Form;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModelInputWizard;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModelInputWizard.WizardAction;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModelInputWizard.WizardPage;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.GetUserAccount;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.GetUserDependency;

@Lazy
@Component
@GuiProfile
public class UserAccountDeletePopup {

    private final static String ARG_WITH_CONFIGS = "WITH_CONFIGS";
    private final static String ARG_WITH_EXAMS = "WITH_EXAMS";

    private final static LocTextKey FORM_TITLE =
            new LocTextKey("sebserver.useraccount.delete.form.title");
    private final static LocTextKey FORM_INFO =
            new LocTextKey("sebserver.useraccount.delete.form.info");
    private final static LocTextKey FORM_NAME =
            new LocTextKey("sebserver.useraccount.delete.form.accountName");
    private final static LocTextKey FORM_CONFIGS =
            new LocTextKey("sebserver.useraccount.delete.form.deleteAlsoConfigs");
    private final static LocTextKey FORM_EXAMS =
            new LocTextKey("sebserver.useraccount.delete.form.deleteAlsoExams");
    private final static LocTextKey ACTION_DELETE =
            new LocTextKey("sebserver.useraccount.delete.form.action.delete");
    private final static LocTextKey ACTION_REPORT =
            new LocTextKey("sebserver.useraccount.delete.form.action.report");

    private final PageService pageService;

    protected UserAccountDeletePopup(final PageService pageService) {
        this.pageService = pageService;
    }

    public Function<PageAction, PageAction> deleteWizardFunction(final PageContext pageContext) {
        return action -> {

            final ModelInputWizard<PageContext> wizard =
                    new ModelInputWizard<PageContext>(
                            action.pageContext().getParent().getShell(),
                            this.pageService.getWidgetFactory())
                                    .setLargeDialogWidth();

            final String page1Id = "DELETE_PAGE";
            final String page2Id = "REPORT_PAGE";
            final Predicate<PageContext> callback = pc -> doDelete(this.pageService, pc);
            final BiFunction<PageContext, Composite, Supplier<PageContext>> composePage1 =
                    (formHandle, content) -> composeDeleteDialog(content, pageContext);
            final BiFunction<PageContext, Composite, Supplier<PageContext>> composePage2 =
                    (formHandle, content) -> composeReportDialog(content, pageContext);

            final WizardPage<PageContext> page1 = new WizardPage<>(
                    page1Id,
                    true,
                    composePage1,
                    new WizardAction<>(ACTION_DELETE, callback),
                    new WizardAction<>(ACTION_REPORT, page2Id));

            final WizardPage<PageContext> page2 = new WizardPage<>(
                    page2Id,
                    false,
                    composePage2,
                    new WizardAction<>(ACTION_DELETE, callback));

            wizard.open(FORM_TITLE, Utils.EMPTY_EXECUTION, page1, page2);

            return action;
        };
    }

    private boolean doDelete(
            final PageService pageService,
            final PageContext pageContext) {

        final boolean withConfigs = BooleanUtils.toBoolean(pageContext.getAttribute(ARG_WITH_CONFIGS));
        final boolean withExams = BooleanUtils.toBoolean(pageContext.getAttribute(ARG_WITH_EXAMS));
        return true;
    }

    private Supplier<PageContext> composeDeleteDialog(
            final Composite parent,
            final PageContext pageContext) {

        final EntityKey entityKey = pageContext.getEntityKey();
        final UserInfo userInfo = this.pageService.getRestService()
                .getBuilder(GetUserAccount.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .call()
                .get();

        final FormHandle<?> formHandle = this.pageService.formBuilder(
                pageContext.copyOf(parent))
                .readonly(false)
                .addField(FormBuilder.text(
                        "USE_NAME",
                        FORM_NAME,
                        userInfo.toName().name)
                        .readonly(true))

                .addField(FormBuilder.checkbox(
                        ARG_WITH_CONFIGS,
                        FORM_CONFIGS))

                .addField(FormBuilder.checkbox(
                        ARG_WITH_EXAMS,
                        FORM_EXAMS))
                .build();

        final Form form = formHandle.getForm();
        return () -> pageContext
                .withAttribute(ARG_WITH_CONFIGS, form.getFieldValue(ARG_WITH_CONFIGS))
                .withAttribute(ARG_WITH_EXAMS, form.getFieldValue(ARG_WITH_EXAMS));
    }

    private Supplier<PageContext> composeReportDialog(
            final Composite parent,
            final PageContext pageContext) {

        // get selection
        final boolean withConfigs = BooleanUtils.toBoolean(pageContext.getAttribute(ARG_WITH_CONFIGS));
        final boolean withExams = BooleanUtils.toBoolean(pageContext.getAttribute(ARG_WITH_EXAMS));

        // get dependencies
        final EntityKey entityKey = pageContext.getEntityKey();
        final RestCall<Set<EntityDependency>>.RestCallBuilder restCallBuilder = this.pageService.getRestService()
                .getBuilder(GetUserDependency.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId);

        if (withConfigs) {
            restCallBuilder.withQueryParam(
                    API.PARAM_BULK_ACTION_INCLUDES,
                    EntityType.CONFIGURATION_NODE.name());
        }
        if (withExams) {
            restCallBuilder.withQueryParam(
                    API.PARAM_BULK_ACTION_INCLUDES,
                    EntityType.EXAM.name());
        }

//        final EntityTable<ConfigurationNode> configTable =
//                this.pageService.entityTableBuilder(GetUserDependency.class)
//                    .

        final Set<EntityDependency> dependencies = restCallBuilder.call().getOrThrow();

        // TODO get dependencies in case of selection and show all in a list (type / name)

        return () -> pageContext;
    }

}
