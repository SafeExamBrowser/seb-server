/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import java.util.function.BooleanSupplier;
import java.util.function.Function;

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
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageUtils;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.GetInstitution;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.ActivateLmsSetup;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.DeactivateLmsSetup;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.GetLmsSetup;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.NewLmsSetup;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.SaveLmsSetup;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.TestLmsSetup;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.TestLmsSetupAdHoc;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser.EntityGrantCheck;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class LmsSetupForm implements TemplateComposer {

    private static final Logger log = LoggerFactory.getLogger(LmsSetupForm.class);

    private static final LocTextKey FORM_SECRET_LMS_TEXT_KEY =
            new LocTextKey("sebserver.lmssetup.form.secret.lms");
    private static final LocTextKey FORM_CLIENTNAME_LMS_TEXT_KEY =
            new LocTextKey("sebserver.lmssetup.form.clientname.lms");
    private static final LocTextKey FORM_URL_TEXT_KEY =
            new LocTextKey("sebserver.lmssetup.form.url");
    private static final LocTextKey FORM_TYPE_TEXT_KEY =
            new LocTextKey("sebserver.lmssetup.form.type");
    private static final LocTextKey FORM_NAME_TEXT_KEY =
            new LocTextKey("sebserver.lmssetup.form.name");
    private static final LocTextKey FORM_INSTITUTION_TEXT_KEY =
            new LocTextKey("sebserver.lmssetup.form.institution");

    private final PageService pageService;
    private final ResourceService resourceService;

    protected LmsSetupForm(
            final PageService pageService,
            final ResourceService resourceService) {

        this.pageService = pageService;
        this.resourceService = resourceService;
    }

    @Override
    public void compose(final PageContext pageContext) {
        final CurrentUser currentUser = this.resourceService.getCurrentUser();
        final RestService restService = this.resourceService.getRestService();
        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();

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
        final FormHandle<LmsSetup> formHandle = this.pageService.formBuilder(
                formContext.copyOf(content), 4)
                .readonly(readonly)
                .putStaticValueIf(isNotNew,
                        Domain.LMS_SETUP.ATTR_ID,
                        lmsSetup.getModelId())
                .putStaticValue(
                        Domain.LMS_SETUP.ATTR_INSTITUTION_ID,
                        String.valueOf(lmsSetup.getInstitutionId()))
                .putStaticValueIf(isNotNew,
                        Domain.LMS_SETUP.ATTR_LMS_TYPE,
                        String.valueOf(lmsSetup.getLmsType()))
                .addFieldIf(
                        isSEBAdmin,
                        () -> FormBuilder.singleSelection(
                                Domain.LMS_SETUP.ATTR_INSTITUTION_ID,
                                FORM_INSTITUTION_TEXT_KEY,
                                String.valueOf(lmsSetup.getInstitutionId()),
                                () -> this.resourceService.institutionResource())
                                .readonly(true))
                .addField(FormBuilder.text(
                        Domain.LMS_SETUP.ATTR_NAME,
                        FORM_NAME_TEXT_KEY,
                        lmsSetup.getName()))
                .addField(FormBuilder.singleSelection(
                        Domain.LMS_SETUP.ATTR_LMS_TYPE,
                        FORM_TYPE_TEXT_KEY,
                        (lmsType != null) ? lmsType.name() : null,
                        this.resourceService::lmsTypeResources)
                        .readonlyIf(isNotNew))
                .addField(FormBuilder.text(
                        Domain.LMS_SETUP.ATTR_LMS_URL,
                        FORM_URL_TEXT_KEY,
                        lmsSetup.getLmsApiUrl()))
                .addField(FormBuilder.text(
                        Domain.LMS_SETUP.ATTR_LMS_CLIENTNAME,
                        FORM_CLIENTNAME_LMS_TEXT_KEY,
                        (lmsSetup.getLmsAuthName() != null) ? lmsSetup.getLmsAuthName().toString() : null))
                .addField(FormBuilder.text(
                        Domain.LMS_SETUP.ATTR_LMS_CLIENTSECRET,
                        FORM_SECRET_LMS_TEXT_KEY)
                        .asPasswordField())

                .buildFor((entityKey == null)
                        ? restService.getRestCall(NewLmsSetup.class)
                        : restService.getRestCall(SaveLmsSetup.class));

        ;

        // propagate content actions to action-pane
        this.pageService.pageActionBuilder(formContext.clearEntityKeys())

                .newAction(ActionDefinition.LMS_SETUP_NEW)
                .publishIf(() -> writeGrant && readonly && institutionActive)

                .newAction(ActionDefinition.LMS_SETUP_MODIFY)
                .withEntityKey(entityKey)
                .publishIf(() -> modifyGrant && readonly && institutionActive)

                .newAction(ActionDefinition.LMS_SETUP_SAVE_AND_TEST)
                .withEntityKey(entityKey)
                .withExec(action -> this.testLmsSetup(action, formHandle))
                .ignoreMoveAwayFromEdit()
                .publishIf(() -> modifyGrant && isNotNew.getAsBoolean() && !readonly)

                .newAction(ActionDefinition.LMS_SETUP_TEST_AND_SAVE)
                .withEntityKey(entityKey)
                .withExec(action -> this.testAdHoc(action, formHandle))
                .ignoreMoveAwayFromEdit()
                .publishIf(() -> modifyGrant && isNew.getAsBoolean() && !readonly)

                .newAction(ActionDefinition.LMS_SETUP_DEACTIVATE)
                .withEntityKey(entityKey)
                .withSimpleRestCall(restService, DeactivateLmsSetup.class)
                .withConfirm(PageUtils.confirmDeactivation(lmsSetup, restService))
                .publishIf(() -> writeGrant && readonly && institutionActive && lmsSetup.isActive())

                .newAction(ActionDefinition.LMS_SETUP_ACTIVATE)
                .withEntityKey(entityKey)
                .withExec(action -> activate(action, formHandle))
                .publishIf(() -> writeGrant && readonly && institutionActive && !lmsSetup.isActive())

                .newAction(ActionDefinition.LMS_SETUP_SAVE)
                .withEntityKey(entityKey)
                .withExec(formHandle::processFormSave)
                .ignoreMoveAwayFromEdit()
                .publishIf(() -> !readonly)

                .newAction(ActionDefinition.LMS_SETUP_CANCEL_MODIFY)
                .withEntityKey(entityKey)
                .withExec(action -> this.pageService.onEmptyEntityKeyGoTo(
                        action,
                        ActionDefinition.LMS_SETUP_VIEW_LIST))
                .publishIf(() -> !readonly);
    }

    /** Save and test connection before activation */
    private PageAction activate(final PageAction action, final FormHandle<LmsSetup> formHandle) {
        // first test the LMS Setup. If this fails the action execution will stops
        final PageAction testLmsSetup = this.testLmsSetup(action, formHandle);
        // if LMS Setup test was successful, the activation action applies
        this.resourceService.getRestService().getBuilder(ActivateLmsSetup.class)
                .withURIVariable(
                        API.PARAM_MODEL_ID,
                        action.pageContext().getAttribute(AttributeKeys.ENTITY_ID))
                .call()
                .onErrorDo(t -> action.pageContext().notifyError(t));

        return testLmsSetup;
    }

    /** LmsSetup test action implementation */
    private PageAction testAdHoc(final PageAction action, final FormHandle<LmsSetup> formHandle) {

        // reset previous errors
        formHandle.process(
                name -> true,
                fieldAccessor -> fieldAccessor.resetError());

        // first test the connection on ad hoc object
        final Result<LmsSetupTestResult> result = this.resourceService.getRestService()
                .getBuilder(TestLmsSetupAdHoc.class)
                .withFormBinding(formHandle.getFormBinding())
                .call();

        // ... and handle the response
        if (result.hasError()) {
            if (formHandle.handleError(result.getError())) {
                throw new PageMessageException(
                        new LocTextKey("sebserver.lmssetup.action.test.missingParameter"));
            }
        }

        return handleTestResult(
                action,
                a -> {
                    // try to save the LmsSetup
                    final PageAction processFormSave = formHandle.processFormSave(a);
                    processFormSave.pageContext().publishInfo(
                            new LocTextKey("sebserver.lmssetup.action.test.ok"));
                    return processFormSave;
                },
                result.getOrThrow());
    }

    /** LmsSetup test action implementation */
    private PageAction testLmsSetup(final PageAction action, final FormHandle<LmsSetup> formHandle) {
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

        return handleTestResult(
                action,
                a -> {
                    action.pageContext().publishInfo(
                            new LocTextKey("sebserver.lmssetup.action.test.ok"));

                    return action;
                },
                result.getOrThrow());
    }

    private PageAction handleTestResult(
            final PageAction action,
            final Function<PageAction, PageAction> onOK,
            final LmsSetupTestResult testResult) {

        if (testResult.isOk()) {
            return onOK.apply(action);
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
