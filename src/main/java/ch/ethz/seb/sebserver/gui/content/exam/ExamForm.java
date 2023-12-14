/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.exam;

import static ch.ethz.seb.sebserver.gbl.FeatureService.ConfigurableFeature.SCREEN_PROCTORING;

import java.util.*;
import java.util.function.Function;

import ch.ethz.seb.sebserver.gbl.FeatureService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.*;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.ErrorMessage;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamTemplate;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.exam.ScreenProctoringSettings;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult.ErrorType;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.Form;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.form.FormPostException;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEvent;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.download.DownloadService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCallError;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.template.GetDefaultExamTemplate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.template.GetExamTemplate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.TestLmsSetup;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.quiz.GetQuizData;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.quiz.ImportAsExam;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser.EntityGrantCheck;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

@Lazy
@Component
@GuiProfile
public class ExamForm implements TemplateComposer {

    private static final Logger log = LoggerFactory.getLogger(ExamForm.class);

    protected static final String ATTR_READ_GRANT = "ATTR_READ_GRANT";
    protected static final String ATTR_EDITABLE = "ATTR_EDITABLE";
    protected static final String ATTR_EXAM_STATUS = "ATTR_EXAM_STATUS";

    public static final LocTextKey EXAM_FORM_TITLE_KEY =
            new LocTextKey("sebserver.exam.form.title");
    public static final LocTextKey EXAM_FORM_TITLE_IMPORT_KEY =
            new LocTextKey("sebserver.exam.form.title.import");

    private static final LocTextKey FORM_SUPPORTER_TEXT_KEY =
            new LocTextKey("sebserver.exam.form.supporter");
    private static final LocTextKey FORM_STATUS_TEXT_KEY =
            new LocTextKey("sebserver.exam.form.status");
    private static final LocTextKey FORM_TYPE_TEXT_KEY =
            new LocTextKey("sebserver.exam.form.type");
    private static final LocTextKey FORM_END_TIME_TEXT_KEY =
            new LocTextKey("sebserver.exam.form.endtime");
    private static final LocTextKey FORM_START_TIME_TEXT_KEY =
            new LocTextKey("sebserver.exam.form.starttime");
    private static final LocTextKey FORM_DESCRIPTION_TEXT_KEY =
            new LocTextKey("sebserver.exam.form.description");
    private static final LocTextKey FORM_NAME_TEXT_KEY =
            new LocTextKey("sebserver.exam.form.name");
    private static final LocTextKey FORM_QUIZ_ID_TEXT_KEY =
            new LocTextKey("sebserver.exam.form.quizid");
    private static final LocTextKey FORM_QUIZ_URL_TEXT_KEY =
            new LocTextKey("sebserver.exam.form.quizurl");
    private static final LocTextKey FORM_LMSSETUP_TEXT_KEY =
            new LocTextKey("sebserver.exam.form.lmssetup");
    private final static LocTextKey ACTION_MESSAGE_SEB_RESTRICTION_RELEASE =
            new LocTextKey("sebserver.exam.action.sebrestriction.release.confirm");
    private static final LocTextKey FORM_EXAM_TEMPLATE_TEXT_KEY =
            new LocTextKey("sebserver.exam.form.examTemplate");
    private static final LocTextKey FORM_EXAM_TEMPLATE_ERROR =
            new LocTextKey("sebserver.exam.form.examTemplate.error");
    private static final LocTextKey EXAM_ARCHIVE_CONFIRM =
            new LocTextKey("sebserver.exam.action.archive.confirm");

    private final static LocTextKey CONSISTENCY_MESSAGE_TITLE =
            new LocTextKey("sebserver.exam.consistency.title");
    private final static LocTextKey CONSISTENCY_MESSAGE_MISSING_SUPPORTER =
            new LocTextKey("sebserver.exam.consistency.missing-supporter");
    private final static LocTextKey CONSISTENCY_MESSAGE_MISSING_INDICATOR =
            new LocTextKey("sebserver.exam.consistency.missing-indicator");
    private final static LocTextKey CONSISTENCY_MESSAGE_MISSING_CONFIG =
            new LocTextKey("sebserver.exam.consistency.missing-config");
    private final static LocTextKey CONSISTENCY_MESSAGE_MISSING_SEB_RESTRICTION =
            new LocTextKey("sebserver.exam.consistency.missing-seb-restriction");
    private final static LocTextKey CONSISTENCY_MESSAGE_VALIDATION_LMS_CONNECTION =
            new LocTextKey("sebserver.exam.consistency.no-lms-connection");
    private final static LocTextKey CONSISTENCY_MESSAGEINVALID_ID_REFERENCE =
            new LocTextKey("sebserver.exam.consistency.invalid-lms-id");
    private final static LocTextKey CONSISTENCY_MESSAGE_SEB_RESTRICTION_MISMATCH =
            new LocTextKey("sebserver.exam.consistencyseb-restriction-mismatch");

    private final static LocTextKey AUTO_GEN_CONFIG_ERROR_TITLE =
            new LocTextKey("sebserver.exam.autogen.error.config.title");
    private final static LocTextKey AUTO_GEN_CONFIG_ERROR_TEXT =
            new LocTextKey("sebserver.exam.autogen.error.config.text");

    private final Map<String, LocTextKey> consistencyMessageMapping;
    private final PageService pageService;
    private final ResourceService resourceService;
    private final ExamSEBRestrictionSettings examSEBRestrictionSettings;
    private final ProctoringSettingsPopup proctoringSettingsPopup;
    private final ScreenProctoringSettingsPopup screenProctoringSettingsPopup;
    private final WidgetFactory widgetFactory;
    private final RestService restService;
    private final ExamDeletePopup examDeletePopup;
    private final ExamFormConfigs examFormConfigs;
    private final ExamIndicatorsList examIndicatorsList;
    private final ExamClientGroupList examClientGroupList;
    private final ExamCreateClientConfigPopup examCreateClientConfigPopup;
    private final FeatureService featureService;

    protected ExamForm(
            final PageService pageService,
            final ExamSEBRestrictionSettings examSEBRestrictionSettings,
            final ProctoringSettingsPopup proctoringSettingsPopup,
            final ScreenProctoringSettingsPopup screenProctoringSettingsPopup,
            final ExamToConfigBindingForm examToConfigBindingForm,
            final DownloadService downloadService,
            final ExamDeletePopup examDeletePopup,
            final ExamFormConfigs examFormConfigs,
            final ExamIndicatorsList examIndicatorsList,
            final ExamClientGroupList examClientGroupList,
            final ExamCreateClientConfigPopup examCreateClientConfigPopup,
            final FeatureService featureService) {

        this.pageService = pageService;
        this.resourceService = pageService.getResourceService();
        this.examSEBRestrictionSettings = examSEBRestrictionSettings;
        this.screenProctoringSettingsPopup = screenProctoringSettingsPopup;
        this.proctoringSettingsPopup = proctoringSettingsPopup;
        this.widgetFactory = pageService.getWidgetFactory();
        this.restService = this.resourceService.getRestService();
        this.examDeletePopup = examDeletePopup;
        this.examFormConfigs = examFormConfigs;
        this.examIndicatorsList = examIndicatorsList;
        this.examClientGroupList = examClientGroupList;
        this.examCreateClientConfigPopup = examCreateClientConfigPopup;
        this.featureService = featureService;

        this.consistencyMessageMapping = new HashMap<>();
        this.consistencyMessageMapping.put(
                APIMessage.ErrorMessage.EXAM_CONSISTENCY_VALIDATION_SUPPORTER.messageCode,
                CONSISTENCY_MESSAGE_MISSING_SUPPORTER);
        this.consistencyMessageMapping.put(
                APIMessage.ErrorMessage.EXAM_CONSISTENCY_VALIDATION_INDICATOR.messageCode,
                CONSISTENCY_MESSAGE_MISSING_INDICATOR);
        this.consistencyMessageMapping.put(
                APIMessage.ErrorMessage.EXAM_CONSISTENCY_VALIDATION_CONFIG.messageCode,
                CONSISTENCY_MESSAGE_MISSING_CONFIG);
        this.consistencyMessageMapping.put(
                APIMessage.ErrorMessage.EXAM_CONSISTENCY_VALIDATION_SEB_RESTRICTION.messageCode,
                CONSISTENCY_MESSAGE_MISSING_SEB_RESTRICTION);
        this.consistencyMessageMapping.put(
                APIMessage.ErrorMessage.EXAM_CONSISTENCY_VALIDATION_LMS_CONNECTION.messageCode,
                CONSISTENCY_MESSAGE_VALIDATION_LMS_CONNECTION);
        this.consistencyMessageMapping.put(
                APIMessage.ErrorMessage.EXAM_CONSISTENCY_VALIDATION_INVALID_ID_REFERENCE.messageCode,
                CONSISTENCY_MESSAGEINVALID_ID_REFERENCE);
    }

    @Override
    public void compose(final PageContext pageContext) {
        final CurrentUser currentUser = this.resourceService.getCurrentUser();
        final I18nSupport i18nSupport = this.resourceService.getI18nSupport();
        final boolean readonly = pageContext.isReadonly();
        final boolean newExamNoLMS = BooleanUtils.toBoolean(
                pageContext.getAttribute(AttributeKeys.NEW_EXAM_NO_LMS));
        final boolean importFromQuizData = BooleanUtils.toBoolean(
                pageContext.getAttribute(AttributeKeys.IMPORT_FROM_QUIZ_DATA));

        // get or create model data
        final Exam exam = newExamNoLMS
                ? this.newExamNoLMS()
                : (importFromQuizData
                    ? createExamFromQuizData(pageContext)
                    : getExistingExam(pageContext))
                            .onError(error -> pageContext.notifyLoadError(EntityType.EXAM, error))
                            .getOrThrow();

        // new PageContext with actual EntityKey
        final EntityKey entityKey = (readonly || !newExamNoLMS) ? pageContext.getEntityKey() : null;
        final PageContext formContext = pageContext.withEntityKey(exam.getEntityKey());
        final EntityGrantCheck entityGrantCheck = currentUser.entityGrantCheck(exam);
        final boolean modifyGrant = entityGrantCheck.m();
        final boolean writeGrant = entityGrantCheck.w();
        final boolean editable = modifyGrant &&
                (exam.getStatus() == ExamStatus.UP_COMING || exam.getStatus() == ExamStatus.RUNNING);
        final boolean signatureKeyCheckEnabled = BooleanUtils.toBoolean(
                exam.additionalAttributes.get(Exam.ADDITIONAL_ATTR_SIGNATURE_KEY_CHECK_ENABLED));
        final boolean sebRestrictionAvailable = readonly && testSEBRestrictionAPI(exam);
        final boolean isRestricted = readonly && sebRestrictionAvailable && this.restService
                .getBuilder(CheckSEBRestriction.class)
                .withURIVariable(API.PARAM_MODEL_ID, exam.getModelId())
                .call()
                .onError(e -> log.error("Unexpected error while trying to verify seb restriction settings: ", e))
                .getOr(exam.sebRestriction);
        final boolean sebRestrictionMismatch = readonly &&
                sebRestrictionAvailable &&
                isRestricted != exam.sebRestriction &&
                exam.status == ExamStatus.RUNNING;

        // check exam consistency and inform the user if needed
        Collection<APIMessage> warnings = null;
        if (readonly) {
            warnings = this.restService.getBuilder(CheckExamConsistency.class)
                    .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                    .call()
                    .getOr(Collections.emptyList());
            if (sebRestrictionMismatch || (warnings != null && !warnings.isEmpty())) {
                showConsistencyChecks(warnings, sebRestrictionMismatch, formContext.getParent());
            }
        }

        // the default page layout with title
        final LocTextKey titleKey = importFromQuizData
                ? EXAM_FORM_TITLE_IMPORT_KEY
                : EXAM_FORM_TITLE_KEY;
        final Composite content = this.widgetFactory.defaultPageLayout(
                formContext.getParent(),
                titleKey);
        if ((warnings != null && !warnings.isEmpty()) || sebRestrictionMismatch) {
            final GridData gridData = (GridData) content.getLayoutData();
            gridData.verticalIndent = 10;
        }

        // The Exam form
        final FormHandle<Exam> formHandle = readonly
                ? createReadOnlyForm(formContext, content, exam)
                : createEditForm(formContext, content, exam);

        if (importFromQuizData) {
            this.processTemplateSelection(formHandle.getForm(), formContext);
        }

        final boolean proctoringEnabled = readonly && this.restService
                .getBuilder(GetExamProctoringSettings.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .call()
                .map(ProctoringServiceSettings::getEnableProctoring)
                .getOr(false);

        final boolean spsFeatureEnabled = this.featureService.isEnabled(SCREEN_PROCTORING);
        final boolean screenProctoringEnabled = readonly && spsFeatureEnabled && this.restService
                .getBuilder(GetScreenProctoringSettings.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .call()
                .map(ScreenProctoringSettings::getEnableScreenProctoring)
                .getOr(false);

        final PageActionBuilder actionBuilder = this.pageService.pageActionBuilder(formContext
                .clearEntityKeys()
                .removeAttribute(AttributeKeys.IMPORT_FROM_QUIZ_DATA));


        // propagate content actions to action-pane
        actionBuilder

                .newAction(ActionDefinition.EXAM_MODIFY)
                .withEntityKey(entityKey)
                .publishIf(() -> modifyGrant && readonly && editable)

                .newAction(ActionDefinition.EXAM_DELETE)
                .withEntityKey(entityKey)
                .withExec(this.examDeletePopup.deleteWizardFunction(pageContext))
                .publishIf(() -> writeGrant && readonly)

                .newAction(ActionDefinition.EXAM_ARCHIVE)
                .withEntityKey(entityKey)
                .withConfirm(() -> EXAM_ARCHIVE_CONFIRM)
                .withExec(this::archiveExam)
                .publishIf(() -> writeGrant && readonly && exam.status == ExamStatus.FINISHED)

                .newAction(ActionDefinition.EXAM_SAVE)
                .withExec(action -> (importFromQuizData)
                        ? importExam(action, formHandle, sebRestrictionAvailable && exam.status == ExamStatus.RUNNING)
                        : formHandle.processFormSave(action))
                .ignoreMoveAwayFromEdit()
                .publishIf(() -> !readonly && modifyGrant)

                .newAction(ActionDefinition.EXAM_CANCEL_MODIFY)
                .withEntityKey(entityKey)
                .withAttribute(AttributeKeys.IMPORT_FROM_QUIZ_DATA, String.valueOf(importFromQuizData))
                .withExec(this.cancelModifyFunction())
                .publishIf(() -> !readonly)

                .newAction(ActionDefinition.EXAM_SEB_CLIENT_CONFIG_EXPORT)
                .withEntityKey(entityKey)
                .withExec(this.examCreateClientConfigPopup.exportFunction(
                        exam.institutionId,
                        exam.getName()))
                .publishIf(() -> editable && readonly)

                .newAction(ActionDefinition.EXAM_SECURITY_KEY_ENABLED)
                .withEntityKey(entityKey)
                .publishIf(() -> signatureKeyCheckEnabled && readonly)

                .newAction(ActionDefinition.EXAM_SECURITY_KEY_DISABLED)
                .withEntityKey(entityKey)
                .publishIf(() -> !signatureKeyCheckEnabled && readonly)

                .newAction(ActionDefinition.EXAM_MODIFY_SEB_RESTRICTION_DETAILS)
                .withEntityKey(entityKey)
                .withExec(this.examSEBRestrictionSettings.settingsFunction(this.pageService))
                .withAttribute(ExamSEBRestrictionSettings.PAGE_CONTEXT_ATTR_LMS_ID, String.valueOf(exam.lmsSetupId))
                .withAttribute(PageContext.AttributeKeys.FORCE_READ_ONLY, String.valueOf(!modifyGrant || !editable))
                .noEventPropagation()
                .publishIf(() -> sebRestrictionAvailable && readonly)

                .newAction(ActionDefinition.EXAM_ENABLE_SEB_RESTRICTION)
                .withEntityKey(entityKey)
                .withExec(action -> this.examSEBRestrictionSettings.setSEBRestriction(action, true, this.restService))
                .publishIf(() -> sebRestrictionAvailable && readonly && modifyGrant && !importFromQuizData
                        && BooleanUtils.isFalse(isRestricted))

                .newAction(ActionDefinition.EXAM_DISABLE_SEB_RESTRICTION)
                .withConfirm(() -> ACTION_MESSAGE_SEB_RESTRICTION_RELEASE)
                .withEntityKey(entityKey)
                .withExec(action -> this.examSEBRestrictionSettings.setSEBRestriction(action, false, this.restService))
                .publishIf(() -> sebRestrictionAvailable && readonly && modifyGrant && !importFromQuizData
                        && BooleanUtils.isTrue(isRestricted))

                .newAction(ActionDefinition.EXAM_PROCTORING_ON)
                .withEntityKey(entityKey)
                .withExec(this.proctoringSettingsPopup.settingsFunction(this.pageService, modifyGrant && editable))
                .noEventPropagation()
                .publishIf(() -> proctoringEnabled && readonly)

                .newAction(ActionDefinition.EXAM_PROCTORING_OFF)
                .withEntityKey(entityKey)
                .withExec(this.proctoringSettingsPopup.settingsFunction(this.pageService, modifyGrant && editable))
                .noEventPropagation()
                .publishIf(() -> !proctoringEnabled && readonly)

                .newAction(ActionDefinition.SCREEN_PROCTORING_ON)
                .withEntityKey(entityKey)
                .withExec(
                        this.screenProctoringSettingsPopup.settingsFunction(this.pageService, modifyGrant && editable))
                .noEventPropagation()
                .publishIf(() -> spsFeatureEnabled && screenProctoringEnabled && readonly)

                .newAction(ActionDefinition.SCREEN_PROCTORING_OFF)
                .withEntityKey(entityKey)
                .withExec(
                        this.screenProctoringSettingsPopup.settingsFunction(this.pageService, modifyGrant && editable))
                .noEventPropagation()
                .publishIf(
                        () -> spsFeatureEnabled && !screenProctoringEnabled && readonly)
        ;

        // additional data in read-only view
        if (readonly && !importFromQuizData) {
            // Configurations
            this.examFormConfigs.compose(
                    formContext
                            .copyOf(content)
                            .withAttribute(ATTR_READ_GRANT, String.valueOf(entityGrantCheck.r()))
                            .withAttribute(ATTR_EDITABLE, String.valueOf(editable))
                            .withAttribute(ATTR_EXAM_STATUS, exam.status.name()));

            // Indicators
            this.examIndicatorsList.compose(
                    formContext
                            .copyOf(content)
                            .withAttribute(ATTR_READ_GRANT, String.valueOf(entityGrantCheck.r()))
                            .withAttribute(ATTR_EDITABLE, String.valueOf(editable))
                            .withAttribute(ATTR_EXAM_STATUS, exam.status.name()));

            // Client Groups
            this.examClientGroupList.compose(
                    formContext
                            .copyOf(content)
                            .withAttribute(ATTR_READ_GRANT, String.valueOf(entityGrantCheck.r()))
                            .withAttribute(ATTR_EDITABLE, String.valueOf(editable))
                            .withAttribute(ATTR_EXAM_STATUS, exam.status.name()));
        }
    }

    private FormHandle<Exam> createReadOnlyForm(
            final PageContext formContext,
            final Composite content,
            final Exam exam) {

        final I18nSupport i18nSupport = formContext.getI18nSupport();
        return this.pageService.formBuilder(
                formContext.copyOf(content), 8)
                .withDefaultSpanLabel(1)
                .withDefaultSpanInput(4)
                .withDefaultSpanEmptyCell(3)
                .readonly(true)
                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_NAME,
                        FORM_NAME_TEXT_KEY,
                        exam.name)
                        .readonly(true)
                        .withInputSpan(3)
                        .withEmptyCellSeparation(false))

                .addField(FormBuilder.singleSelection(
                        Domain.EXAM.ATTR_LMS_SETUP_ID,
                        FORM_LMSSETUP_TEXT_KEY,
                        String.valueOf(exam.lmsSetupId),
                        this.resourceService::lmsSetupResource)
                        .readonly(true)
                        .withInputSpan(3)
                        .withEmptyCellSeparation(false))

                .addField(FormBuilder.text(
                                Domain.EXAM.ATTR_STATUS + "_display",
                                FORM_STATUS_TEXT_KEY,
                                i18nSupport.getText(new LocTextKey("sebserver.exam.status." + exam.status.name())))
                        .readonly(true)
                        .withInputSpan(3)
                        .withEmptyCellSeparation(false))

                .addField(FormBuilder.text(
                                Domain.EXAM.ATTR_EXTERNAL_ID,
                                FORM_QUIZ_ID_TEXT_KEY,
                                exam.externalId)
                        .readonly(true)
                        .withInputSpan(3)
                        .withEmptyCellSeparation(false))

                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_START_TIME,
                        FORM_START_TIME_TEXT_KEY,
                        i18nSupport.formatDisplayDateWithTimeZone(exam.startTime))
                        .readonly(true)
                        .withInputSpan(3)
                        .withEmptyCellSeparation(false))

                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_END_TIME,
                        FORM_END_TIME_TEXT_KEY,
                        i18nSupport.formatDisplayDateWithTimeZone(exam.endTime))
                        .readonly(true)
                        .withInputSpan(3)
                        .withEmptyCellSeparation(false))

                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_START_URL,
                        FORM_QUIZ_URL_TEXT_KEY,
                        exam.getStartURL())
                        .readonly(true)
                        .withInputSpan(7)
                        .withEmptyCellSeparation(false))

                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_DESCRIPTION,
                        FORM_DESCRIPTION_TEXT_KEY,
                        exam.getDescription())
                        .asHTML(50)
                        .readonly(true)
                        .withInputSpan(7)
                        .withEmptyCellSeparation(false))

                .addField(FormBuilder.singleSelection(
                                Domain.EXAM.ATTR_TYPE,
                                FORM_TYPE_TEXT_KEY,
                                (exam.type != null) ? String.valueOf(exam.type) : Exam.ExamType.UNDEFINED.name(),
                                this.resourceService::examTypeResources)
                        .withInputSpan(7)
                        .withEmptyCellSeparation(false))

                .addField(FormBuilder.multiComboSelection(
                        Domain.EXAM.ATTR_SUPPORTER,
                        FORM_SUPPORTER_TEXT_KEY,
                        StringUtils.join(exam.supporter, Constants.LIST_SEPARATOR_CHAR),
                        this.resourceService::examSupporterResources)
                        .withInputSpan(7)
                        .withEmptyCellSeparation(false))
                .build();
    }

    private FormHandle<Exam> createEditForm(
            final PageContext formContext,
            final Composite content,
            final Exam exam) {

        final I18nSupport i18nSupport = formContext.getI18nSupport();
        final boolean newExam = exam.id == null;
        final boolean hasLMS = exam.lmsSetupId != null;
        final boolean importFromLMS = newExam && hasLMS;
        final LocTextKey statusTitle = new LocTextKey("sebserver.exam.status." + exam.status.name());

        return this.pageService.formBuilder(formContext.copyOf(content))
                .putStaticValueIf(() -> !newExam,
                        Domain.EXAM.ATTR_ID,
                        exam.getModelId())
                .putStaticValue(
                        Domain.EXAM.ATTR_INSTITUTION_ID,
                        String.valueOf(exam.getInstitutionId()))
                .putStaticValueIf(() -> exam.lmsSetupId != null,
                        Domain.EXAM.ATTR_LMS_SETUP_ID,
                        String.valueOf(exam.lmsSetupId))
                .putStaticValueIf(() -> exam.lmsSetupId != null,
                        QuizData.QUIZ_ATTR_LMS_SETUP_ID,
                        String.valueOf(exam.lmsSetupId))
                .putStaticValueIf(() -> exam.externalId != null,
                        Domain.EXAM.ATTR_EXTERNAL_ID,
                        exam.externalId)
                .putStaticValueIf(() -> exam.lmsSetupId != null,
                        QuizData.QUIZ_ATTR_ID,
                        exam.externalId)

                .addField(FormBuilder.text(
                                Domain.EXAM.ATTR_STATUS + "_display",
                                FORM_STATUS_TEXT_KEY,
                                i18nSupport.getText(statusTitle))
                        .readonly(true))

                .addFieldIf( () -> hasLMS,
                        () -> FormBuilder.singleSelection(
                                        Domain.EXAM.ATTR_LMS_SETUP_ID,
                                        FORM_LMSSETUP_TEXT_KEY,
                                        String.valueOf(exam.lmsSetupId),
                                        this.resourceService::lmsSetupResource)
                                .readonly(true))

                .addFieldIf(() -> exam.id == null,
                        () -> FormBuilder.singleSelection(
                                Domain.EXAM.ATTR_EXAM_TEMPLATE_ID,
                                FORM_EXAM_TEMPLATE_TEXT_KEY,
                                (exam.examTemplateId == null)
                                        ? getDefaultExamTemplateId()
                                        : String.valueOf(exam.examTemplateId),
                                this.resourceService::examTemplateResources)
                        .withSelectionListener(form -> this.processTemplateSelection(form, formContext)))

                .addField(FormBuilder.text(
                                Domain.EXAM.ATTR_QUIZ_NAME,
                                FORM_NAME_TEXT_KEY,
                                exam.name)
                        .readonly(hasLMS)
                        .mandatory(!hasLMS))

                .addField(FormBuilder.text(
                                QuizData.QUIZ_ATTR_DESCRIPTION,
                                FORM_DESCRIPTION_TEXT_KEY,
                                exam.getDescription())
                        .asArea()
                        .readonly(hasLMS))
                .withAdditionalValueMapping(QuizData.QUIZ_ATTR_DESCRIPTION)

                .addField(FormBuilder.dateTime(
                                Domain.EXAM.ATTR_QUIZ_START_TIME,
                                FORM_START_TIME_TEXT_KEY,
                                exam.startTime)
                        .readonly(hasLMS)
                        .mandatory(!hasLMS))

                .addField(FormBuilder.dateTime(
                                Domain.EXAM.ATTR_QUIZ_END_TIME,
                                FORM_END_TIME_TEXT_KEY,
                                exam.endTime)
                        .readonly(hasLMS))

                .addField(FormBuilder.text(
                                QuizData.QUIZ_ATTR_START_URL,
                                FORM_QUIZ_URL_TEXT_KEY,
                                exam.getStartURL())
                        .readonly(hasLMS)
                        .mandatory(!hasLMS))
                .withAdditionalValueMapping(QuizData.QUIZ_ATTR_START_URL)

                .addField(FormBuilder.singleSelection(
                                Domain.EXAM.ATTR_TYPE,
                                FORM_TYPE_TEXT_KEY,
                                (exam.type != null) ? String.valueOf(exam.type) : Exam.ExamType.UNDEFINED.name(),
                                this.resourceService::examTypeResources)
                        .mandatory(true))

                .addField(FormBuilder.multiComboSelection(
                                Domain.EXAM.ATTR_SUPPORTER,
                                FORM_SUPPORTER_TEXT_KEY,
                                StringUtils.join(exam.supporter, Constants.LIST_SEPARATOR_CHAR),
                                this.resourceService::examSupporterResources))

                .buildFor(importFromLMS
                        ? this.restService.getRestCall(ImportAsExam.class)
                        : newExam
                            ? this.restService.getRestCall(NewExam.class)
                            : this.restService.getRestCall(SaveExam.class));
    }

    private Exam newExamNoLMS() {
        final DateTimeZone timeZone = this.pageService.getCurrentUser().get().timeZone;
        return new Exam(
            null,
            this.pageService.getCurrentUser().get().institutionId,
            null,
            UUID.randomUUID().toString(),
            true,
            null,
                DateTime.now(timeZone),
                DateTime.now(timeZone).plusHours(1),
            Exam.ExamType.UNDEFINED,
            null,
            null,
            ExamStatus.UP_COMING,
            false,
            null,
            true,
            null,
            null,
            null,
            null);
    }

    private PageAction archiveExam(final PageAction action) {

        this.restService.getBuilder(ArchiveExam.class)
                .withURIVariable(API.PARAM_MODEL_ID, action.getEntityKey().modelId)
                .call()
                .onError(error -> action.pageContext().notifyUnexpectedError(error));

        return action;
    }

    private String getDefaultExamTemplateId() {
        return this.restService.getBuilder(GetDefaultExamTemplate.class)
                .call()
                .map(ExamTemplate::getId)
                .map(Object::toString)
                .getOr(StringUtils.EMPTY);
    }

    private void processTemplateSelection(final Form form, final PageContext context) {
        try {
            final String templateId = form.getFieldValue(Domain.EXAM.ATTR_EXAM_TEMPLATE_ID);
            if (StringUtils.isNotBlank(templateId)) {
                final ExamTemplate examTemplate = this.pageService.getRestService().getBuilder(GetExamTemplate.class)
                        .withURIVariable(API.PARAM_MODEL_ID, templateId)
                        .call()
                        .getOrThrow();

                form.setFieldValue(Domain.EXAM.ATTR_TYPE, examTemplate.examType.name());
                form.setFieldValue(
                        Domain.EXAM.ATTR_SUPPORTER,
                        StringUtils.join(examTemplate.supporter, Constants.LIST_SEPARATOR));
            } else {
                form.setFieldValue(Domain.EXAM.ATTR_TYPE, Exam.ExamType.UNDEFINED.name());
                form.setFieldValue(Domain.EXAM.ATTR_SUPPORTER, null);
            }
        } catch (final Exception e) {
            context.notifyError(FORM_EXAM_TEMPLATE_ERROR, e);
        }
    }

    private PageAction importExam(
            final PageAction action,
            final FormHandle<Exam> formHandle,
            final boolean applySEBRestriction) {

        try {
            // process normal save first
            final PageAction processFormSave = formHandle.processFormSave(action);

            // when okay and the exam sebRestriction is true
            if (applySEBRestriction) {
                this.examSEBRestrictionSettings.setSEBRestriction(
                        processFormSave,
                        true,
                        this.restService,
                        t -> log.error("Failed to initially restrict the course for SEB on LMS: {}", t.getMessage()));
            }

            return processFormSave;

        } catch (final Exception e) {
            return handleExamImportSetupFailure(action, e);
        }
    }

    private PageAction handleExamImportSetupFailure(final PageAction action, final Exception e) {
        Throwable error = e;
        if (e instanceof FormPostException) {
            error = ((FormPostException) e).getCause();
        }
        if (error instanceof RestCallError) {
            final List<APIMessage> apiMessages = ((RestCallError) error).getAPIMessages();
            if (apiMessages != null && !apiMessages.isEmpty()) {
                final APIMessage apiMessage = apiMessages.remove(0);
                if (ErrorMessage.EXAM_IMPORT_ERROR_AUTO_SETUP.isOf(apiMessage)) {
                    final String examIdAttr = apiMessage.attributes
                            .stream()
                            .filter(attr -> attr.startsWith(API.PARAM_MODEL_ID))
                            .findFirst().orElse(null);
                    if (examIdAttr != null) {
                        final String[] split = StringUtils.split(
                                examIdAttr,
                                Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR);
                        if (API.PARAM_MODEL_ID.equals(split[0])) {
                            final String additionlMessages = apiMessages.stream()
                                    .reduce(
                                            "",
                                            (acc, msg) -> acc + "<br/>&nbsp;&nbsp;&nbsp;" + msg.systemMessage,
                                            (acc1, acc2) -> acc1 + acc2);
                            action.pageContext().publishPageMessage(
                                    AUTO_GEN_CONFIG_ERROR_TITLE,
                                    new LocTextKey(AUTO_GEN_CONFIG_ERROR_TEXT.name, additionlMessages));
                            return action.withEntityKey(new EntityKey(split[1], EntityType.EXAM));
                        }
                    }
                }
            }
        }
        throw new RuntimeException("Error while handle exam import setup failure:", e);
    }

    private boolean testSEBRestrictionAPI(final Exam exam) {
        if (exam.lmsSetupId == null || !exam.isLmsAvailable() || exam.status == ExamStatus.ARCHIVED) {
            return false;
        }

        // Call the testing endpoint with the specified data to test
        final Result<LmsSetupTestResult> result = this.restService.getBuilder(TestLmsSetup.class)
                .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(exam.lmsSetupId))
                .call();

        if (result.hasError()) {
            return false;
        }

        final LmsSetupTestResult lmsSetupTestResult = result.get();
        if (!lmsSetupTestResult.lmsType.features.contains(LmsSetup.Features.SEB_RESTRICTION)) {
            return false;
        }
        return !lmsSetupTestResult.hasError(ErrorType.QUIZ_RESTRICTION_API_REQUEST);
    }

    private void showConsistencyChecks(
            final Collection<APIMessage> result,
            final boolean sebRestrictionMismatch,
            final Composite parent) {

        final Composite warningPanel = this.widgetFactory.createWarningPanel(parent);
        this.widgetFactory.labelLocalized(
                warningPanel,
                CustomVariant.TITLE_LABEL,
                CONSISTENCY_MESSAGE_TITLE);

        final String restrMessageCode = APIMessage.ErrorMessage.EXAM_CONSISTENCY_VALIDATION_SEB_RESTRICTION.messageCode;

        result
                .stream()
                .filter(message -> !(sebRestrictionMismatch && message.messageCode.equals(restrMessageCode)))
                .map(message -> this.consistencyMessageMapping.get(message.messageCode))
                .filter(Objects::nonNull)
                .forEach(message -> this.widgetFactory.labelLocalized(
                        warningPanel,
                        CustomVariant.MESSAGE,
                        message));

        if (sebRestrictionMismatch) {
            this.widgetFactory.labelLocalized(
                    warningPanel,
                    CustomVariant.MESSAGE,
                    CONSISTENCY_MESSAGE_SEB_RESTRICTION_MISMATCH);
        }
    }

    private Result<Exam> getExistingExam(final PageContext pageContext) {
        final EntityKey entityKey = pageContext.getEntityKey();
        return this.restService.getBuilder(GetExam.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .call();
    }

    private Result<Exam> createExamFromQuizData(final PageContext pageContext) {
        final EntityKey entityKey = pageContext.getEntityKey();
        final EntityKey parentEntityKey = pageContext.getParentEntityKey();
        return this.restService.getBuilder(GetQuizData.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .withQueryParam(QuizData.QUIZ_ATTR_LMS_SETUP_ID, parentEntityKey.modelId)
                .call()
                .map(Exam::new)
                .onError(error -> pageContext.notifyLoadError(EntityType.EXAM, error));
    }

    private Function<PageAction, PageAction> cancelModifyFunction() {
        final Function<PageAction, PageAction> backToCurrentFunction = this.pageService.backToCurrentFunction();
        return action -> {
            final boolean importFromQuizData = BooleanUtils.toBoolean(

                    action.pageContext().getAttribute(AttributeKeys.IMPORT_FROM_QUIZ_DATA));
            if (importFromQuizData) {
                final PageActionBuilder actionBuilder = this.pageService.pageActionBuilder(action.pageContext());
                final PageAction activityHomeAction = actionBuilder
                        .newAction(ActionDefinition.QUIZ_DISCOVERY_VIEW_LIST)
                        .create();
                this.pageService.firePageEvent(new ActionEvent(activityHomeAction), action.pageContext());
                return activityHomeAction;
            }

            return backToCurrentFunction.apply(action);
        };
    }

}
