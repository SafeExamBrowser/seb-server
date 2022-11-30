/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.exam;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.widgets.Composite;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.institution.AppSignatureKeyInfo;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityKey;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.Form;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExam;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.seckey.DeleteSecurityKeyGrant;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.seckey.GetAppSignatureKeyInfo;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.seckey.GetAppSignatureKeys;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.seckey.SaveAppSignatureKeySettings;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class ExamSignatureKeyForm implements TemplateComposer {

    private static final LocTextKey TILE =
            new LocTextKey("sebserver.exam.signaturekey.title");

    private static final LocTextKey FORM_ENABLED =
            new LocTextKey("sebserver.exam.signaturekey.form.enabled");
    private static final LocTextKey FORM_STAT_GRANT_THRESHOLD =
            new LocTextKey("sebserver.exam.signaturekey.form.grant.threshold");

    private static final LocTextKey APP_SIG_KEY_EMPTY_LIST_TEXT_KEY =
            new LocTextKey("sebserver.exam.signaturekey.keylist.empty");
    private static final LocTextKey APP_SIG_KEY_LIST_TITLE =
            new LocTextKey("sebserver.exam.signaturekey.keylist.title");
    private static final LocTextKey APP_SIG_KEY_LIST_TITLE_TOOLTIP =
            new LocTextKey("sebserver.exam.signaturekey.keylist.title" + Constants.TOOLTIP_TEXT_KEY_SUFFIX);
    private static final LocTextKey APP_SIG_KEY_LIST_KEY =
            new LocTextKey("sebserver.exam.signaturekey.keylist.key");
    private static final LocTextKey APP_SIG_KEY_LIST_NUM_CLIENTS =
            new LocTextKey("sebserver.exam.signaturekey.keylist.clients");
    private static final LocTextKey APP_SIG_KEY_LIST_EMPTY_SELECTION_TEXT_KEY =
            new LocTextKey("sebserver.exam.signaturekey.keylist.pleaseSelect");

    private static final LocTextKey GRANT_LIST_TITLE =
            new LocTextKey("sebserver.exam.signaturekey.grantlist.title");
    private static final LocTextKey GRANT_LIST_TITLE_TOOLTIP =
            new LocTextKey("sebserver.exam.signaturekey.grantlist.title" + Constants.TOOLTIP_TEXT_KEY_SUFFIX);
    private static final LocTextKey GRANT_LIST_EMPTY_LIST_TEXT_KEY =
            new LocTextKey("sebserver.exam.signaturekey.grantlist.empty");
    private static final LocTextKey GRANT_LIST_KEY =
            new LocTextKey("sebserver.exam.signaturekey.grantlist.key");
    private static final LocTextKey GRANT_LIST_TAG =
            new LocTextKey("sebserver.exam.signaturekey.grantlist.tag");
    private static final LocTextKey GRANT_LIST_EMPTY_SELECTION_TEXT_KEY =
            new LocTextKey("sebserver.exam.signaturekey.grantlist.pleaseSelect");
    private static final LocTextKey GRANT_LIST_DELETE_CONFORM =
            new LocTextKey("sebserver.exam.signaturekey.grantlist.delete.confirm");

    private final PageService pageService;
    private final ResourceService resourceService;
    private final AddSecurityKeyGrantPopup addSecurityKeyGrantPopup;
    private final SecurityKeyGrantPopup securityKeyGrantPopup;

    public ExamSignatureKeyForm(
            final PageService pageService,
            final ResourceService resourceService,
            final AddSecurityKeyGrantPopup addSecurityKeyGrantPopup,
            final SecurityKeyGrantPopup securityKeyGrantPopup) {

        this.pageService = pageService;
        this.resourceService = resourceService;
        this.addSecurityKeyGrantPopup = addSecurityKeyGrantPopup;
        this.securityKeyGrantPopup = securityKeyGrantPopup;
    }

    @Override
    public void compose(final PageContext pageContext) {
        final RestService restService = this.resourceService.getRestService();
        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();
        final EntityKey entityKey = pageContext.getEntityKey();
        final Exam exam = restService
                .getBuilder(GetExam.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .call()
                .getOrThrow();
        final boolean signatureKeyCheckEnabled = BooleanUtils.toBoolean(
                exam.additionalAttributes.get(Exam.ADDITIONAL_ATTR_SIGNATURE_KEY_CHECK_ENABLED));
        final String ct = exam.additionalAttributes.get(Exam.ADDITIONAL_ATTR_STATISTICAL_GRANT_COUNT_THRESHOLD);

        final Composite content = widgetFactory
                .defaultPageLayout(pageContext.getParent(), TILE);

        final PageActionBuilder actionBuilder = this.pageService
                .pageActionBuilder(pageContext.clearEntityKeys());

        final FormHandle<Entity> form = this.pageService
                .formBuilder(pageContext.copyOf(content))
                .withDefaultSpanLabel(3)
                .withDefaultSpanEmptyCell(2)

                .addField(FormBuilder.checkbox(
                        Exam.ADDITIONAL_ATTR_SIGNATURE_KEY_CHECK_ENABLED,
                        FORM_ENABLED,
                        String.valueOf(signatureKeyCheckEnabled))
                        .withInputSpan(1))

                .addField(FormBuilder.text(
                        Exam.ADDITIONAL_ATTR_STATISTICAL_GRANT_COUNT_THRESHOLD,
                        FORM_STAT_GRANT_THRESHOLD,
                        (ct != null) ? ct : "2")
                        .asNumber(number -> {
                            if (StringUtils.isNotBlank(number)) {
                                Integer.parseInt(number);
                            }
                        })
                        .mandatory()
                        .withInputSpan(1))

                .build();

        widgetFactory.addFormSubContextHeader(
                content,
                APP_SIG_KEY_LIST_TITLE,
                APP_SIG_KEY_LIST_TITLE_TOOLTIP);

        final EntityTable<AppSignatureKeyInfo> connectionInfoTable = this.pageService
                .remoteListTableBuilder(
                        restService.getRestCall(GetAppSignatureKeyInfo.class),
                        EntityType.SEB_SECURITY_KEY_REGISTRY)
                .withRestCallAdapter(builder -> builder.withURIVariable(API.PARAM_PARENT_MODEL_ID, entityKey.modelId))
                .withEmptyMessage(APP_SIG_KEY_EMPTY_LIST_TEXT_KEY)
                .withPaging(-1)
                .hideNavigation()

                .withColumn(new ColumnDefinition<>(
                        Domain.SEB_SECURITY_KEY_REGISTRY.ATTR_KEY_VALUE,
                        APP_SIG_KEY_LIST_KEY,
                        AppSignatureKeyInfo::getKey)
                                .widthProportion(2))

                .withColumn(new ColumnDefinition<>(
                        AppSignatureKeyInfo.ATTR_NUMBER_OF_CONNECTIONS,
                        APP_SIG_KEY_LIST_NUM_CLIENTS,
                        AppSignatureKeyInfo::getNumberOfConnections)
                                .widthProportion(1))

                .withDefaultAction(table -> actionBuilder
                        .newAction(ActionDefinition.EXAM_SECURITY_KEY_SHOW_ADD_GRANT_POPUP)
                        .withParentEntityKey(entityKey)
                        .withExec(action -> this.addSecurityKeyGrantPopup.showGrantPopup(
                                action,
                                table.getSingleSelectedROWData()))
                        .noEventPropagation()
                        .ignoreMoveAwayFromEdit()
                        .create())

                .withSelectionListener(this.pageService.getSelectionPublisher(
                        pageContext,
                        ActionDefinition.EXAM_SECURITY_KEY_SHOW_ADD_GRANT_POPUP))

                .compose(pageContext.copyOf(content));

        widgetFactory.addFormSubContextHeader(
                content,
                GRANT_LIST_TITLE,
                GRANT_LIST_TITLE_TOOLTIP);

        final EntityTable<SecurityKey> grantsList = this.pageService
                .remoteListTableBuilder(
                        restService.getRestCall(GetAppSignatureKeys.class),
                        EntityType.SEB_SECURITY_KEY_REGISTRY)
                .withRestCallAdapter(builder -> builder.withURIVariable(API.PARAM_PARENT_MODEL_ID, entityKey.modelId))
                .withEmptyMessage(GRANT_LIST_EMPTY_LIST_TEXT_KEY)
                .withPaging(-1)
                .hideNavigation()

                .withColumn(new ColumnDefinition<>(
                        Domain.SEB_SECURITY_KEY_REGISTRY.ATTR_KEY_VALUE,
                        GRANT_LIST_KEY,
                        SecurityKey::getKey).widthProportion(2))

                .withColumn(new ColumnDefinition<>(
                        Domain.SEB_SECURITY_KEY_REGISTRY.ATTR_TAG,
                        GRANT_LIST_TAG,
                        SecurityKey::getTag).widthProportion(1))

                .withDefaultAction(table -> actionBuilder
                        .newAction(ActionDefinition.EXAM_SECURITY_KEY_SHOW_GRANT_POPUP)
                        .withParentEntityKey(entityKey)
                        .withExec(action -> this.securityKeyGrantPopup.showGrantPopup(
                                action,
                                table.getSingleSelectedROWData()))
                        .noEventPropagation()
                        .ignoreMoveAwayFromEdit()
                        .create())

                .withSelectionListener(this.pageService.getSelectionPublisher(
                        pageContext,
                        ActionDefinition.EXAM_SECURITY_KEY_SHOW_GRANT_POPUP,
                        ActionDefinition.EXAM_SECURITY_KEY_DELETE_GRANT))

                .compose(pageContext.copyOf(content));

        actionBuilder.newAction(ActionDefinition.EXAM_SECURITY_KEY_SAVE_SETTINGS)
                .withEntityKey(entityKey)
                .withExec(action -> this.saveSettings(action, form.getForm()))
                .ignoreMoveAwayFromEdit()
                .publish()

                .newAction(ActionDefinition.EXAM_SECURITY_KEY_CANCEL_MODIFY)
                .withExec(this.pageService.backToCurrentFunction())
                .publish()

                .newAction(ActionDefinition.EXAM_SECURITY_KEY_SHOW_ADD_GRANT_POPUP)
                .withParentEntityKey(entityKey)
                .withSelect(
                        connectionInfoTable::getMultiSelection,
                        action -> this.addSecurityKeyGrantPopup.showGrantPopup(
                                action,
                                connectionInfoTable.getSingleSelectedROWData()),
                        APP_SIG_KEY_LIST_EMPTY_SELECTION_TEXT_KEY)
                .ignoreMoveAwayFromEdit()
                .noEventPropagation()
                .publish(false)

                .newAction(ActionDefinition.EXAM_SECURITY_KEY_SHOW_GRANT_POPUP)
                .withEntityKey(entityKey)
                .withSelect(
                        grantsList::getMultiSelection,
                        action -> this.securityKeyGrantPopup.showGrantPopup(action,
                                grantsList.getSingleSelectedROWData()),
                        GRANT_LIST_EMPTY_SELECTION_TEXT_KEY)
                .ignoreMoveAwayFromEdit()
                .noEventPropagation()
                .publish(false)

                .newAction(ActionDefinition.EXAM_SECURITY_KEY_DELETE_GRANT)
                .withConfirm(action -> GRANT_LIST_DELETE_CONFORM)
                .withParentEntityKey(entityKey)
                .withSelect(
                        grantsList::getMultiSelection,
                        this::deleteGrant,
                        GRANT_LIST_EMPTY_SELECTION_TEXT_KEY)
                .ignoreMoveAwayFromEdit()
                .publish(false)

        ;
    }

    private PageAction saveSettings(final PageAction action, final Form form) {
        final String enable = form.getFieldValue(Exam.ADDITIONAL_ATTR_SIGNATURE_KEY_CHECK_ENABLED);
        final String threshold = form.getFieldValue(Exam.ADDITIONAL_ATTR_STATISTICAL_GRANT_COUNT_THRESHOLD);
        final EntityKey entityKey = action.getEntityKey();

        this.pageService
                .getRestService()
                .getBuilder(SaveAppSignatureKeySettings.class)
                .withURIVariable(API.PARAM_PARENT_MODEL_ID, entityKey.modelId)
                .withFormParam(Exam.ADDITIONAL_ATTR_SIGNATURE_KEY_CHECK_ENABLED, enable)
                .withFormParam(Exam.ADDITIONAL_ATTR_STATISTICAL_GRANT_COUNT_THRESHOLD, threshold)
                .call()
                .onError(error -> action.pageContext().notifySaveError(EntityType.EXAM, error));
        return action;
    }

    private PageAction deleteGrant(final PageAction action) {
        final EntityKey parentEntityKey = action.getParentEntityKey();
        final EntityKey singleSelection = action.getSingleSelection();
        this.pageService.getRestService()
                .getBuilder(DeleteSecurityKeyGrant.class)
                .withURIVariable(API.PARAM_PARENT_MODEL_ID, parentEntityKey.modelId)
                .withURIVariable(API.PARAM_MODEL_ID, singleSelection.modelId)
                .call()
                .onError(error -> action.pageContext().notifyUnexpectedError(error));

        return action.withEntityKey(parentEntityKey);
    }

}
