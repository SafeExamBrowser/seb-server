/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.exam;

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.exam.*;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.clientgroup.*;
import org.eclipse.swt.widgets.Composite;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroupData.ClientGroupType;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
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
    private static final LocTextKey FORM_UNAME_START_KEY =
            new LocTextKey("sebserver.exam.clientgroup.form.usernamestart");
    private static final LocTextKey FORM_UNAME_END_KEY =
            new LocTextKey("sebserver.exam.clientgroup.form.usernameend");

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

        if (pageService.isLightSetup()) {
            pageService.applyFullVersionNote(pageContext.getParent(), pageContext);
            this.pageService.pageActionBuilder(pageContext.clearEntityKeys())
                    .newAction(ActionDefinition.EXAM_CLIENT_GROUP_CANCEL_MODIFY)
                    .withEntityKey(pageContext.getParentEntityKey())
                    .withExec(this.pageService.backToCurrentFunction())
                    .ignoreMoveAwayFromEdit()
                    .publish();
            return;
        }

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

        // new PageContext with actual EntityKey
        final PageContext formContext = pageContext.withEntityKey(clientGroup.getEntityKey());

        // the default page layout
        final LocTextKey titleKey = (isNew)
                ? NEW_CLIENT_GROUP_TILE_TEXT_KEY
                : CLIENT_GROUP_TILE_TEXT_KEY;
        final Composite content = widgetFactory.defaultPageLayout(
                formContext.getParent(),
                titleKey);

        final Composite formRoot = widgetFactory.voidComposite(content);
        final FormHandleAnchor formHandleAnchor = new FormHandleAnchor();
        formHandleAnchor.formContext = pageContext
                .copyOf(formRoot)
                .clearEntityKeys();
        
        buildFormAccordingToSelection(
                clientGroup.type != null ? clientGroup.type.name() : null, 
                exam,
                clientGroup, 
                formHandleAnchor, 
                pageContext,
                true);

        // propagate content actions to action-pane
        this.pageService.pageActionBuilder(formContext.clearEntityKeys())

                .newAction(ActionDefinition.EXAM_CLIENT_GROUP_SAVE)
                .withEntityKey(parentEntityKey)
                .withExec(formHandleAnchor::processFormSave)
                .ignoreMoveAwayFromEdit()
                .publishIf(() -> !isReadonly)

                .newAction(ActionDefinition.EXAM_CLIENT_GROUP_CANCEL_MODIFY)
                .withEntityKey(parentEntityKey)
                .withExec(this.pageService.backToCurrentFunction())
                .publishIf(() -> !isReadonly);
    }

    <T extends Entity> void buildFormAccordingToSelection(
            final String selection,
            final Exam exam,
            final ClientGroup clientGroup,
            final FormHandleAnchor formHandleAnchor,
            final PageContext pageContext,
            final boolean init) {
        
        final String name = init 
                ? clientGroup.getName() 
                : formHandleAnchor.formHandle.getForm().getFieldValue(Domain.CLIENT_GROUP.ATTR_NAME);
        final String color = init
                ? clientGroup.getColor()
                : formHandleAnchor.formHandle.getForm().getFieldValue(Domain.CLIENT_GROUP.ATTR_COLOR);
        
        if (!init) {
            PageService.clearComposite(formHandleAnchor.formContext.getParent());
        }
        
        final RestService restService = this.resourceService.getRestService();
        final EntityKey entityKey = pageContext.getEntityKey();
        final EntityKey parentEntityKey = pageContext.getParentEntityKey();
        final boolean isNew = entityKey == null;
        final boolean isReadonly = pageContext.isReadonly();

        final ClientGroupType type = selection != null ? ClientGroupType.valueOf(selection) : null;
        final String typeDescription = (type != null)
                ? Utils.formatLineBreaks(
                this.i18nSupport.getText(CLIENT_GROUP_TYPE_DESC_PREFIX + type.name()))
                : Constants.EMPTY_NOTE;

        formHandleAnchor.formHandle = this.pageService.formBuilder(formHandleAnchor.formContext)
                .readonly(isReadonly)
                .putStaticValueIf(() -> !isNew,
                        Domain.CLIENT_GROUP.ATTR_ID,
                        clientGroup.getModelId())
                .putStaticValue(
                        Domain.EXAM.ATTR_INSTITUTION_ID,
                        String.valueOf(exam.institutionId))
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
                                name)
                        .mandatory(!isReadonly))

                .addField(FormBuilder.colorSelection(
                                Domain.CLIENT_GROUP.ATTR_COLOR,
                                FORM_COLOR_TEXT_KEY,
                                color)
                        .withEmptyCellSeparation(false))

                .addField(FormBuilder.singleSelection(
                                Domain.CLIENT_GROUP.ATTR_TYPE,
                                FORM_TYPE_TEXT_KEY,
                                selection,
                                this.resourceService::clientGroupTypeResources)
                        .withSelectionListener(form -> buildFormAccordingToSelection(
                                form.getFieldValue(Domain.CLIENT_GROUP.ATTR_TYPE),
                                exam,
                                clientGroup,
                                formHandleAnchor,
                                pageContext,
                                false))
                        .mandatory(!isReadonly))

                .addField(FormBuilder.text(
                                TYPE_DESCRIPTION_FIELD_NAME,
                                FORM_DESC_TEXT_KEY,
                                typeDescription)
                        .asArea()
                        .readonly(true))

                .addFieldIf(() -> type == ClientGroupType.IP_V4_RANGE,
                        () -> FormBuilder.text(
                                ClientGroup.ATTR_IP_RANGE_START,
                                FORM_IP_START_KEY,
                                clientGroup::getIpRangeStart)
                        .mandatory(!isReadonly))

                .addFieldIf(() -> type == ClientGroupType.IP_V4_RANGE,
                        () -> FormBuilder.text(
                                ClientGroup.ATTR_IP_RANGE_END,
                                FORM_IP_END_KEY,
                                clientGroup::getIpRangeEnd)
                        .mandatory(!isReadonly))

                .addFieldIf(() -> type == ClientGroupType.CLIENT_OS,
                        () -> FormBuilder.singleSelection(
                                ClientGroupTemplate.ATTR_CLIENT_OS,
                                FORM_OS_TYPE_KEY,
                                        clientGroup.getClientOS() != null ? clientGroup.getClientOS().name() : null,
                                this.resourceService::clientClientOSResources)
                        .mandatory(!isReadonly))

                .addFieldIf(() -> type == ClientGroupType.NAME_ALPHABETICAL_RANGE,
                        () -> FormBuilder.text(
                                ClientGroup.ATTR_NAME_RANGE_START_LETTER,
                                FORM_UNAME_START_KEY,
                                clientGroup::getNameRangeStartLetter)
                        .mandatory(!isReadonly))

                .addFieldIf(() -> type == ClientGroupType.NAME_ALPHABETICAL_RANGE,
                        () -> FormBuilder.text(
                                ClientGroup.ATTR_NAME_RANGE_END_LETTER,
                                FORM_UNAME_END_KEY,
                                clientGroup::getNameRangeEndLetter)
                        .mandatory(!isReadonly))

                .buildFor((isNew)
                        ? restService.getRestCall( NewClientGroup.class)
                        : restService.getRestCall(SaveClientGroup.class));

        formHandleAnchor.formContext.getParent().layout();
    }

    static final class FormHandleAnchor {
        FormHandle<ClientGroup> formHandle = null;
        PageContext formContext = null;

        public PageAction processFormSave(final PageAction action) {
            if (this.formHandle != null) {
                return formHandle.processFormSave(action);
            }
            return action;
        }
    }

}
