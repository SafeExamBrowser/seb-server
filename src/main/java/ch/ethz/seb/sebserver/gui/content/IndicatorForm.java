/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.form.PageFormService;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageAction;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExam;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetIndicator;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.NewIndicator;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.SaveIndicator;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class IndicatorForm implements TemplateComposer {

    private static final Logger log = LoggerFactory.getLogger(IndicatorForm.class);

    private final PageFormService pageFormService;
    private final ResourceService resourceService;

    protected IndicatorForm(
            final PageFormService pageFormService,
            final ResourceService resourceService) {

        this.pageFormService = pageFormService;
        this.resourceService = resourceService;
    }

    @Override
    public void compose(final PageContext pageContext) {
        final RestService restService = this.resourceService.getRestService();
        final WidgetFactory widgetFactory = this.pageFormService.getWidgetFactory();
        final EntityKey entityKey = pageContext.getEntityKey();
        final EntityKey parentEntityKey = pageContext.getParentEntityKey();
        final boolean isNew = entityKey == null;
        final boolean isReadonly = pageContext.isReadonly();

        final Exam exam = restService
                .getBuilder(GetExam.class)
                .withURIVariable(API.PARAM_MODEL_ID, parentEntityKey.modelId)
                .call()
                .get(pageContext::notifyError);

        // get data or create new. Handle error if happen
        final Indicator indicator = (isNew)
                ? Indicator.createNew(exam.getInstitutionId(), exam)
                : restService
                        .getBuilder(GetIndicator.class)
                        .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                        .call()
                        .get(pageContext::notifyError);

        if (indicator == null) {
            log.error("Failed to get Indicator. "
                    + "Error was notified to the User. "
                    + "See previous logs for more infomation");
            return;
        }

        // new PageContext with actual EntityKey
        final PageContext formContext = pageContext.withEntityKey(indicator.getEntityKey());

        // the default page layout
        final LocTextKey titleKey = new LocTextKey(
                (isNew)
                        ? "sebserver.exam.indicator.form.title.new"
                        : "sebserver.exam.indicator.form.title");
        final Composite content = widgetFactory.defaultPageLayout(
                formContext.getParent(),
                titleKey);

        final FormHandle<Indicator> formHandle = this.pageFormService.getBuilder(
                formContext.copyOf(content), 4)
                .readonly(isReadonly)
                .putStaticValueIf(() -> !isNew,
                        Domain.INDICATOR.ATTR_ID,
                        indicator.getModelId())
                .putStaticValue(
                        Domain.EXAM.ATTR_INSTITUTION_ID,
                        exam.getModelId())
                .putStaticValue(
                        Domain.INDICATOR.ATTR_EXAM_ID,
                        parentEntityKey.getModelId())
                .addField(FormBuilder.text(
                        "examName",
                        "sebserver.exam.indicator.form.exam",
                        exam.name)
                        .readonly(true))
                .addField(FormBuilder.text(
                        Domain.INDICATOR.ATTR_NAME,
                        "sebserver.exam.indicator.form.name",
                        indicator.name))
                .addField(FormBuilder.singleSelection(
                        Domain.INDICATOR.ATTR_TYPE,
                        "sebserver.exam.indicator.form.type",
                        (indicator.type != null) ? indicator.type.name() : null,
                        this.resourceService::indicatorTypeResources))
                .addField(FormBuilder.colorSelection(
                        Domain.INDICATOR.ATTR_COLOR,
                        "sebserver.exam.indicator.form.color",
                        indicator.defaultColor))
                .addField(FormBuilder.thresholdList(
                        Domain.THRESHOLD.REFERENCE_NAME,
                        "sebserver.exam.indicator.form.thresholds",
                        indicator.getThresholds()))
                .buildFor((isNew)
                        ? restService.getRestCall(NewIndicator.class)
                        : restService.getRestCall(SaveIndicator.class));

        // propagate content actions to action-pane
        formContext.clearEntityKeys()
                .createAction(ActionDefinition.EXAM_INDICATOR_SAVE)
                .withEntityKey(parentEntityKey)
                .withExec(formHandle::processFormSave)
                .publishIf(() -> !isReadonly)

                .createAction(ActionDefinition.EXAM_INDICATOR_CANCEL_MODIFY)
                .withEntityKey(parentEntityKey)
                .withExec(PageAction::onEmptyEntityKeyGoToActivityHome)
                .withConfirm("sebserver.overall.action.modify.cancel.confirm")
                .publishIf(() -> !isReadonly);

    }

}
