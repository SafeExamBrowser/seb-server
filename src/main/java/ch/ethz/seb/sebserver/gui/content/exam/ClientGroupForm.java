/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.exam;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.widgets.Composite;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroup;
import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroupData.ClientGroupType;
import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroupTemplate;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.Form;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExam;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.clientgroup.GetClientGroup;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.clientgroup.NewClientGroup;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.clientgroup.SaveClientGroup;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class ClientGroupForm implements TemplateComposer {

    private static final LocTextKey NEW_CLIENT_GROUP_TILE_TEXT_KEY =
            new LocTextKey("sebserver.exam.clientgroup.form.title.new");
    private static final LocTextKey CLIENT_GROUP_TILE_TEXT_KEY =
            new LocTextKey("sebserver.exam.clientgroup.form.title");
    private static final LocTextKey FORM_COLOR_TEXT_KEY =
            new LocTextKey("sebserver.exam.clientgroup.form.color");
    private static final LocTextKey FORM_TYPE_TEXT_KEY =
            new LocTextKey("sebserver.exam.clientgroup.form.type");
    private static final LocTextKey FORM_NAME_TEXT_KEY =
            new LocTextKey("sebserver.exam.clientgroup.form.name");
    private static final LocTextKey FORM_EXAM_TEXT_KEY =
            new LocTextKey("sebserver.exam.clientgroup.form.exam");
    private static final LocTextKey FORM_DESC_TEXT_KEY =
            new LocTextKey("sebserver.exam.clientgroup.form.description");
    private static final LocTextKey FORM_IP_START_KEY =
            new LocTextKey("sebserver.exam.clientgroup.form.ipstart");
    private static final LocTextKey FORM_IP_END_KEY =
            new LocTextKey("sebserver.exam.clientgroup.form.ipend");
    private static final LocTextKey FORM_OS_TYPE_KEY =
            new LocTextKey("sebserver.exam.clientgroup.form.ostype");

    private static final String CLIENT_GROUP_TYPE_DESC_PREFIX =
            "sebserver.exam.clientgroup.type.description.";
    private static final String TYPE_DESCRIPTION_FIELD_NAME =
            "typeDescription";

    private final PageService pageService;
    private final ResourceService resourceService;
    private final I18nSupport i18nSupport;

    protected ClientGroupForm(final PageService pageService) {

        this.pageService = pageService;
        this.resourceService = pageService.getResourceService();
        this.i18nSupport = pageService.getI18nSupport();
    }

    @Override
    public void compose(final PageContext pageContext) {
        final RestService restService = this.resourceService.getRestService();
        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();
        final EntityKey entityKey = pageContext.getEntityKey();
        final EntityKey parentEntityKey = pageContext.getParentEntityKey();
        final boolean isNew = entityKey == null;
        final boolean isReadonly = pageContext.isReadonly();

        final Exam exam = restService
                .getBuilder(GetExam.class)
                .withURIVariable(API.PARAM_MODEL_ID, parentEntityKey.modelId)
                .call()
                .onError(error -> pageContext.notifyLoadError(EntityType.EXAM, error))
                .getOrThrow();

        // get data or create new. Handle error if happen
        final ClientGroup clientGroup = (isNew)
                ? ClientGroup.createNew(exam.getModelId())
                : restService
                        .getBuilder(GetClientGroup.class)
                        .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                        .call()
                        .onError(error -> pageContext.notifyLoadError(EntityType.CLIENT_GROUP, error))
                        .getOrThrow();

        final boolean typeSet = clientGroup.type != null;
        final String typeDescription = (typeSet)
                ? Utils.formatLineBreaks(
                        this.i18nSupport.getText(CLIENT_GROUP_TYPE_DESC_PREFIX + clientGroup.type.name()))
                : Constants.EMPTY_NOTE;

        // new PageContext with actual EntityKey
        final PageContext formContext = pageContext.withEntityKey(clientGroup.getEntityKey());

        // the default page layout
        final LocTextKey titleKey = (isNew)
                ? NEW_CLIENT_GROUP_TILE_TEXT_KEY
                : CLIENT_GROUP_TILE_TEXT_KEY;
        final Composite content = widgetFactory.defaultPageLayout(
                formContext.getParent(),
                titleKey);

        final FormHandle<ClientGroup> formHandle = this.pageService.formBuilder(
                formContext.copyOf(content))
                .readonly(isReadonly)
                .putStaticValueIf(() -> !isNew,
                        Domain.CLIENT_GROUP.ATTR_ID,
                        clientGroup.getModelId())
                .putStaticValue(
                        Domain.EXAM.ATTR_INSTITUTION_ID,
                        String.valueOf(exam.getInstitutionId()))
                .putStaticValue(
                        Domain.CLIENT_GROUP.ATTR_EXAM_ID,
                        parentEntityKey.getModelId())

                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_NAME,
                        FORM_EXAM_TEXT_KEY,
                        exam.name)
                        .readonly(true))

                .addField(FormBuilder.text(
                        Domain.CLIENT_GROUP.ATTR_NAME,
                        FORM_NAME_TEXT_KEY,
                        clientGroup.name)
                        .mandatory(!isReadonly))

                .addField(FormBuilder.colorSelection(
                        Domain.CLIENT_GROUP.ATTR_COLOR,
                        FORM_COLOR_TEXT_KEY,
                        clientGroup.color)
                        .withEmptyCellSeparation(false))

                .addField(FormBuilder.singleSelection(
                        Domain.CLIENT_GROUP.ATTR_TYPE,
                        FORM_TYPE_TEXT_KEY,
                        clientGroup.type.name(),
                        this.resourceService::clientGroupTypeResources)
                        .withSelectionListener(form -> updateForm(form, this.i18nSupport))
                        .mandatory(!isReadonly))

                .addField(FormBuilder.text(
                        TYPE_DESCRIPTION_FIELD_NAME,
                        FORM_DESC_TEXT_KEY,
                        typeDescription)
                        .asArea()
                        .readonly(true))

                .addField(FormBuilder.text(
                        ClientGroup.ATTR_IP_RANGE_START,
                        FORM_IP_START_KEY,
                        clientGroup::getIpRangeStart)
                        .mandatory(!isReadonly)
                        .visibleIf(clientGroup.type == ClientGroupType.IP_V4_RANGE))

                .addField(FormBuilder.text(
                        ClientGroup.ATTR_IP_RANGE_END,
                        FORM_IP_END_KEY,
                        clientGroup::getIpRangeEnd)
                        .mandatory(!isReadonly)
                        .visibleIf(clientGroup.type == ClientGroupType.IP_V4_RANGE))

                .addField(FormBuilder.singleSelection(
                        ClientGroupTemplate.ATTR_CLIENT_OS,
                        FORM_OS_TYPE_KEY,
                        clientGroup.clientOS.name(),
                        this.resourceService::clientClientOSResources)
                        .visibleIf(clientGroup.type == ClientGroupType.CLIENT_OS)
                        .mandatory(!isReadonly))

                .buildFor((isNew)
                        ? restService.getRestCall(NewClientGroup.class)
                        : restService.getRestCall(SaveClientGroup.class));

        // propagate content actions to action-pane
        this.pageService.pageActionBuilder(formContext.clearEntityKeys())

                .newAction(ActionDefinition.EXAM_CLIENT_GROUP_SAVE)
                .withEntityKey(parentEntityKey)
                .withExec(formHandle::processFormSave)
                .ignoreMoveAwayFromEdit()
                .publishIf(() -> !isReadonly)

                .newAction(ActionDefinition.EXAM_CLIENT_GROUP_CANCEL_MODIFY)
                .withEntityKey(parentEntityKey)
                .withExec(this.pageService.backToCurrentFunction())
                .publishIf(() -> !isReadonly);
    }

    public static void updateForm(final Form form, final I18nSupport i18nSupport) {
        final String typeValue = form.getFieldValue(Domain.CLIENT_GROUP.ATTR_TYPE);
        if (StringUtils.isNotBlank(typeValue)) {
            final String text = i18nSupport.getText(CLIENT_GROUP_TYPE_DESC_PREFIX + typeValue);
            form.setFieldValue(
                    TYPE_DESCRIPTION_FIELD_NAME,
                    Utils.formatLineBreaks(text));
            final ClientGroupType type = ClientGroupType.valueOf(typeValue);
            form.setFieldVisible(false, ClientGroup.ATTR_IP_RANGE_START);
            form.setFieldVisible(false, ClientGroup.ATTR_IP_RANGE_END);
            form.setFieldVisible(false, ClientGroupTemplate.ATTR_CLIENT_OS);
            if (type == ClientGroupType.IP_V4_RANGE) {
                form.setFieldVisible(true, ClientGroup.ATTR_IP_RANGE_START);
                form.setFieldVisible(true, ClientGroup.ATTR_IP_RANGE_END);
            }
            if (type == ClientGroupType.CLIENT_OS) {
                form.setFieldVisible(true, ClientGroupTemplate.ATTR_CLIENT_OS);
            }

        } else {
            form.setFieldValue(TYPE_DESCRIPTION_FIELD_NAME, Constants.EMPTY_NOTE);
        }
    }

}
