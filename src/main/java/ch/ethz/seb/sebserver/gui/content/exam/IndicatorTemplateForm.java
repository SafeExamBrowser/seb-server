/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
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
import ch.ethz.seb.sebserver.gbl.model.exam.ExamTemplate;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.exam.IndicatorTemplate;
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
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamTemplate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetIndicatorTemplate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.NewIndicatorTemplate;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.SaveIndicatorTemplate;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class IndicatorTemplateForm implements TemplateComposer {

    private static final LocTextKey NEW_INDICATOR_TILE_TEXT_KEY =
            new LocTextKey("sebserver.exam.indicator.form.title.new");
    private static final LocTextKey INDICATOR_TILE_TEXT_KEY =
            new LocTextKey("sebserver.exam.indicator.form.title");
    private static final LocTextKey FORM_THRESHOLDS_TEXT_KEY =
            new LocTextKey("sebserver.exam.indicator.form.thresholds");
    private static final LocTextKey FORM_COLOR_TEXT_KEY =
            new LocTextKey("sebserver.exam.indicator.form.color");
    private static final LocTextKey FORM_TYPE_TEXT_KEY =
            new LocTextKey("sebserver.exam.indicator.form.type");
    private static final LocTextKey FORM_NAME_TEXT_KEY =
            new LocTextKey("sebserver.exam.indicator.form.name");
    private static final LocTextKey FORM_TAGS_TEXT_KEY =
            new LocTextKey("sebserver.exam.indicator.form.tags");
    private static final LocTextKey FORM_EXAM_TEXT_KEY =
            new LocTextKey("sebserver.exam.indicator.form.exam");
    private static final LocTextKey FORM_DESC_TEXT_KEY =
            new LocTextKey("sebserver.exam.indicator.form.description");

    private static final String INDICATOR_TYPE_DESC_PREFIX =
            "sebserver.exam.indicator.type.description.";
    private static final String TYPE_DESCRIPTION_FIELD_NAME =
            "typeDescription";

    private final PageService pageService;
    private final ResourceService resourceService;
    private final I18nSupport i18nSupport;

    protected IndicatorTemplateForm(final PageService pageService) {

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

        final ExamTemplate examTemplate = restService
                .getBuilder(GetExamTemplate.class)
                .withURIVariable(API.PARAM_MODEL_ID, parentEntityKey.modelId)
                .call()
                .onError(error -> pageContext.notifyLoadError(EntityType.EXAM, error))
                .getOrThrow();

        // get data or create new. Handle error if happen
        final IndicatorTemplate indicatorTemplate = (isNew)
                ? new IndicatorTemplate(null, Long.parseLong(parentEntityKey.modelId),
                        null, null, null, null, null, null)
                : restService
                        .getBuilder(GetIndicatorTemplate.class)
                        .withURIVariable(API.PARAM_PARENT_MODEL_ID, parentEntityKey.modelId)
                        .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                        .call()
                        .onError(error -> pageContext.notifyLoadError(EntityType.INDICATOR, error))
                        .getOrThrow();

        final boolean typeSet = indicatorTemplate.type != null;
        final String typeDescription = (typeSet)
                ? Utils.formatLineBreaks(
                        this.i18nSupport.getText(INDICATOR_TYPE_DESC_PREFIX + indicatorTemplate.type.name))
                : Constants.EMPTY_NOTE;

        // new PageContext with actual EntityKey
        final PageContext formContext = pageContext.withEntityKey(indicatorTemplate.getEntityKey());

        // the default page layout
        final LocTextKey titleKey = (isNew)
                ? NEW_INDICATOR_TILE_TEXT_KEY
                : INDICATOR_TILE_TEXT_KEY;
        final Composite content = widgetFactory.defaultPageLayout(
                formContext.getParent(),
                titleKey);

        final FormHandle<IndicatorTemplate> formHandle = this.pageService.formBuilder(
                formContext.copyOf(content))
                .readonly(isReadonly)
                .putStaticValueIf(() -> !isNew,
                        Domain.INDICATOR.ATTR_ID,
                        indicatorTemplate.getModelId())
                .putStaticValue(
                        Domain.EXAM.ATTR_INSTITUTION_ID,
                        String.valueOf(examTemplate.getInstitutionId()))
                .putStaticValue(
                        IndicatorTemplate.ATTR_EXAM_TEMPLATE_ID,
                        parentEntityKey.getModelId())
                .addField(FormBuilder.text(
                        Domain.EXAM_TEMPLATE.ATTR_NAME,
                        FORM_EXAM_TEXT_KEY,
                        examTemplate.name)
                        .readonly(true))
                .addField(FormBuilder.text(
                        Domain.INDICATOR.ATTR_NAME,
                        FORM_NAME_TEXT_KEY,
                        indicatorTemplate.name)
                        .mandatory(!isReadonly))

                .addField(FormBuilder.singleSelection(
                        Domain.INDICATOR.ATTR_TYPE,
                        FORM_TYPE_TEXT_KEY,
                        (indicatorTemplate.type != null) ? indicatorTemplate.type.name() : null,
                        this.resourceService::indicatorTypeResources)
                        .withSelectionListener(this::updateForm)
                        .mandatory(!isReadonly))

                .addField(FormBuilder.text(
                        TYPE_DESCRIPTION_FIELD_NAME,
                        FORM_DESC_TEXT_KEY,
                        typeDescription)
                        .asArea()
                        //.asHTML(true)
                        .readonly(true)
                        .withInputSpan(6))

                .addField(FormBuilder.colorSelection(
                        Domain.INDICATOR.ATTR_COLOR,
                        FORM_COLOR_TEXT_KEY,
                        indicatorTemplate.defaultColor)
                        .withEmptyCellSeparation(false))
                .addField(FormBuilder.text(
                        Domain.INDICATOR.ATTR_TAGS,
                        FORM_TAGS_TEXT_KEY,
                        indicatorTemplate.tags)
                        .visibleIf(indicatorTemplate.type != null && indicatorTemplate.type.tags
                                && !indicatorTemplate.type.tagsReadonly))
                .addField(FormBuilder.thresholdList(
                        Domain.THRESHOLD.REFERENCE_NAME,
                        FORM_THRESHOLDS_TEXT_KEY,
                        indicatorTemplate.thresholds))

                .buildFor((isNew)
                        ? restService.getRestCall(NewIndicatorTemplate.class)
                        : restService.getRestCall(SaveIndicatorTemplate.class));

        // propagate content actions to action-pane
        this.pageService.pageActionBuilder(formContext.clearEntityKeys())

                .newAction(ActionDefinition.INDICATOR_TEMPLATE_SAVE)
                .withEntityKey(parentEntityKey)
                .withExec(formHandle::processFormSave)
                .ignoreMoveAwayFromEdit()
                .publishIf(() -> !isReadonly)

                .newAction(ActionDefinition.INDICATOR_TEMPLATE_CANCEL_MODIFY)
                .withEntityKey(parentEntityKey)
                .withExec(this.pageService.backToCurrentFunction())
                .publishIf(() -> !isReadonly);

    }

    private void updateForm(final Form form) {
        final String typeValue = form.getFieldValue(Domain.INDICATOR.ATTR_TYPE);
        if (StringUtils.isNotBlank(typeValue)) {
            final String text = this.i18nSupport.getText(INDICATOR_TYPE_DESC_PREFIX + typeValue);
            form.setFieldValue(
                    TYPE_DESCRIPTION_FIELD_NAME,
                    Utils.formatLineBreaks(text));
            final IndicatorType type = IndicatorType.valueOf(typeValue);
            form.setFieldVisible(type.tags && !type.tagsReadonly, Domain.INDICATOR.ATTR_TAGS);
            if (!type.tags || type.tagsReadonly) {
                form.setFieldValue(Domain.INDICATOR.ATTR_TAGS, StringUtils.EMPTY);
            }
        } else {
            form.setFieldValue(TYPE_DESCRIPTION_FIELD_NAME, Constants.EMPTY_NOTE);
        }
    }

}
