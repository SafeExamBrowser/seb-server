/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import java.util.function.BooleanSupplier;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.form.PageFormService;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.PageUtils;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.action.Action;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.GetInstitution;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.GetLmsSetup;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.NewLmsSetup;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.SaveLmsSetup;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.TestLmsSetup;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser.EntityGrantCheck;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class LmsSetupForm implements TemplateComposer {

    private static final Logger log = LoggerFactory.getLogger(LmsSetupForm.class);

    private final PageFormService pageFormService;
    private final ResourceService resourceService;

    protected LmsSetupForm(
            final PageFormService pageFormService,
            final ResourceService resourceService) {

        this.pageFormService = pageFormService;
        this.resourceService = resourceService;
    }

    @Override
    public void compose(final PageContext pageContext) {
        final CurrentUser currentUser = this.resourceService.getCurrentUser();
        final RestService restService = this.resourceService.getRestService();
        final WidgetFactory widgetFactory = this.pageFormService.getWidgetFactory();

        final UserInfo user = currentUser.get();
        final EntityKey entityKey = pageContext.getEntityKey();
        final EntityKey parentEntityKey = pageContext.getParentEntityKey();
        final boolean readonly = pageContext.isReadonly();

        final BooleanSupplier isNew = () -> entityKey == null;
        final BooleanSupplier isNotNew = () -> !isNew.getAsBoolean();
        final BooleanSupplier isSEBAdmin = () -> user.hasRole(UserRole.SEB_SERVER_ADMIN);

        // get data or create new. handle error if happen
        final LmsSetup lmsSetup = isNew.getAsBoolean()
                ? LmsSetup.createNew((parentEntityKey != null)
                        ? Long.valueOf(parentEntityKey.modelId)
                        : user.institutionId)
                : restService
                        .getBuilder(GetLmsSetup.class)
                        .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                        .call()
                        .get(pageContext::notifyError);

        if (lmsSetup == null) {
            log.error(
                    "Failed to get LmsSetup. "
                            + "Error was notified to the User. "
                            + "See previous logs for more infomation");
            return;
        }

        // new PageContext with actual EntityKey
        final PageContext formContext = pageContext.withEntityKey(lmsSetup.getEntityKey());
        // the default page layout with title
        final LocTextKey titleKey = new LocTextKey(
                isNotNew.getAsBoolean()
                        ? "sebserver.lmssetup.form.title"
                        : "sebserver.lmssetup.form.title.new");
        final Composite content = widgetFactory.defaultPageLayout(
                formContext.getParent(),
                titleKey);

        final EntityGrantCheck userGrantCheck = currentUser.entityGrantCheck(lmsSetup);
        final boolean writeGrant = userGrantCheck.w();
        final boolean modifyGrant = userGrantCheck.m();
        final boolean institutionActive = restService.getBuilder(GetInstitution.class)
                .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(lmsSetup.getInstitutionId()))
                .call()
                .map(inst -> inst.active)
                .getOr(false);

        // The LMS Setup form
        final LmsType lmsType = lmsSetup.getLmsType();
        final FormHandle<LmsSetup> formHandle = this.pageFormService.getBuilder(
                formContext.copyOf(content), 4)
                .readonly(readonly)
                .putStaticValueIf(isNotNew,
                        Domain.LMS_SETUP.ATTR_ID,
                        lmsSetup.getModelId())
                .putStaticValueIf(isNotNew,
                        Domain.LMS_SETUP.ATTR_INSTITUTION_ID,
                        String.valueOf(lmsSetup.getInstitutionId()))
                .putStaticValueIf(isNotNew,
                        Domain.LMS_SETUP.ATTR_LMS_TYPE,
                        String.valueOf(lmsSetup.getLmsType()))
                .addField(FormBuilder.singleSelection(
                        Domain.LMS_SETUP.ATTR_INSTITUTION_ID,
                        "sebserver.lmssetup.form.institution",
                        String.valueOf(lmsSetup.getInstitutionId()),
                        () -> this.resourceService.institutionResource())
                        .withCondition(isSEBAdmin)
                        .readonlyIf(isNotNew))
                .addField(FormBuilder.text(
                        Domain.LMS_SETUP.ATTR_NAME,
                        "sebserver.lmssetup.form.name",
                        lmsSetup.getName()))
                .addField(FormBuilder.singleSelection(
                        Domain.LMS_SETUP.ATTR_LMS_TYPE,
                        "sebserver.lmssetup.form.type",
                        (lmsType != null) ? lmsType.name() : null,
                        this.resourceService::lmsTypeResources)
                        .readonlyIf(isNotNew))
                .addField(FormBuilder.text(
                        Domain.LMS_SETUP.ATTR_LMS_URL,
                        "sebserver.lmssetup.form.url",
                        lmsSetup.getLmsApiUrl())
                        .withCondition(() -> isNotNew.getAsBoolean()))
                .addField(FormBuilder.text(
                        Domain.LMS_SETUP.ATTR_LMS_CLIENTNAME,
                        "sebserver.lmssetup.form.clientname.lms",
                        lmsSetup.getLmsAuthName())
                        .withCondition(() -> isNotNew.getAsBoolean()))
                .addField(FormBuilder.text(
                        Domain.LMS_SETUP.ATTR_LMS_CLIENTSECRET,
                        "sebserver.lmssetup.form.secret.lms")
                        .asPasswordField()
                        .withCondition(() -> isNotNew.getAsBoolean()))

                .buildFor((entityKey == null)
                        ? restService.getRestCall(NewLmsSetup.class)
                        : restService.getRestCall(SaveLmsSetup.class));

        ;

        // propagate content actions to action-pane
        formContext.clearEntityKeys()

                .createAction(ActionDefinition.LMS_SETUP_NEW)
                .publishIf(() -> writeGrant && readonly && institutionActive)

                .createAction(ActionDefinition.LMS_SETUP_MODIFY)
                .withEntityKey(entityKey)
                .publishIf(() -> modifyGrant && readonly && institutionActive)

                .createAction(ActionDefinition.LMS_SETUP_TEST)
                .withEntityKey(entityKey)
                .withExec(action -> this.testLmsSetup(action, formHandle))
                .publishIf(() -> modifyGrant && isNotNew.getAsBoolean() && institutionActive)

                .createAction(ActionDefinition.LMS_SETUP_DEACTIVATE)
                .withEntityKey(entityKey)
                .withExec(restService::activation)
                .withConfirm(PageUtils.confirmDeactivation(lmsSetup, restService))
                .publishIf(() -> writeGrant && readonly && institutionActive && lmsSetup.isActive())

                .createAction(ActionDefinition.LMS_SETUP_ACTIVATE)
                .withEntityKey(entityKey)
                .withExec(action -> activate(action, formHandle))
                .publishIf(() -> writeGrant && readonly && institutionActive && !lmsSetup.isActive())

                .createAction(ActionDefinition.LMS_SETUP_SAVE)
                .withExec(formHandle::processFormSave)
                .publishIf(() -> !readonly)

                .createAction(ActionDefinition.LMS_SETUP_CANCEL_MODIFY)
                .withEntityKey(entityKey)
                .withExec(Action::onEmptyEntityKeyGoToActivityHome)
                .withConfirm("sebserver.overall.action.modify.cancel.confirm")
                .publishIf(() -> !readonly);
    }

    /** Save and test connection before activation */
    private Action activate(final Action action, final FormHandle<LmsSetup> formHandle) {
        final RestService restService = this.resourceService.getRestService();
        final Action testLmsSetup = this.testLmsSetup(action, formHandle);
        final Action activation = restService.activation(testLmsSetup);
        return activation;
    }

    /** LmsSetup test action implementation */
    private Action testLmsSetup(final Action action, final FormHandle<LmsSetup> formHandle) {
        // If we are in edit-mode we have to save the form before testing
        if (!action.pageContext().isReadonly()) {
            final Result<LmsSetup> postResult = formHandle.doAPIPost();
            if (postResult.hasError()) {
                formHandle.handleError(postResult.getError());
                postResult.getOrThrow();
            }
        }

        // Call the testing endpoint with the specified data to test
        final EntityKey entityKey = action.getEntityKey();
        final RestService restService = this.resourceService.getRestService();
        final Result<LmsSetupTestResult> result = restService.getBuilder(TestLmsSetup.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.getModelId())
                .call();

        // ... and handle the response
        if (result.hasError()) {
            if (formHandle.handleError(result.getError())) {
                throw new PageMessageException(
                        new LocTextKey("sebserver.lmssetup.action.test.missingParameter"));
            }
        }

        final LmsSetupTestResult testResult = result.getOrThrow();

        if (testResult.isOk()) {
            action.pageContext().publishInfo(
                    new LocTextKey("sebserver.lmssetup.action.test.ok"));

            return action;
        } else if (StringUtils.isNoneBlank(testResult.tokenRequestError)) {
            throw new PageMessageException(
                    new LocTextKey("sebserver.lmssetup.action.test.tokenRequestError",
                            testResult.tokenRequestError));
        } else if (StringUtils.isNoneBlank(testResult.quizRequestError)) {
            throw new PageMessageException(
                    new LocTextKey("sebserver.lmssetup.action.test.quizRequestError", testResult.quizRequestError));
        } else {
            throw new PageMessageException(
                    new LocTextKey("sebserver.lmssetup.action.test.unknownError", testResult));
        }
    }

}
