/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import java.util.Arrays;

import org.eclipse.swt.widgets.Composite;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.TemplateAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.View;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.examconfig.ExamConfigurationService;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputField;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputFieldBuilder;
import ch.ethz.seb.sebserver.gui.service.examconfig.impl.AttributeMapping;
import ch.ethz.seb.sebserver.gui.service.examconfig.impl.ViewContext;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetConfigurations;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetTemplateAttribute;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class ConfigTemplateAttributeForm implements TemplateComposer {

    private static final LocTextKey FORM_TITLE =
            new LocTextKey("sebserver.configtemplate.attr.form.title");
    private static final LocTextKey FORM_NAME_TEXT_KEY =
            new LocTextKey("sebserver.configtemplate.attr.form.name");
    private static final LocTextKey FORM_TYPE_TEXT_KEY =
            new LocTextKey("sebserver.configtemplate.attr.form.type");
    private static final LocTextKey FORM_VIEW_TEXT_KEY =
            new LocTextKey("sebserver.configtemplate.attr.form.view");
    private static final LocTextKey FORM_GROUP_TEXT_KEY =
            new LocTextKey("sebserver.configtemplate.attr.form.group");
    private static final LocTextKey FORM_VALUE_TEXT_KEY =
            new LocTextKey("sebserver.configtemplate.attr.form.value");

    private final PageService pageService;
    private final RestService restService;
    private final CurrentUser currentUser;
    private final ResourceService resourceService;
    private final ExamConfigurationService examConfigurationService;

    protected ConfigTemplateAttributeForm(
            final PageService pageService,
            final RestService restService,
            final CurrentUser currentUser,
            final ExamConfigurationService examConfigurationService) {

        this.pageService = pageService;
        this.restService = restService;
        this.currentUser = currentUser;
        this.resourceService = pageService.getResourceService();
        this.examConfigurationService = examConfigurationService;

    }

    @Override
    public void compose(final PageContext pageContext) {
        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();

        final UserInfo user = this.currentUser.get();
        final EntityKey attributeKey = pageContext.getEntityKey();
        final EntityKey templateKey = pageContext.getParentEntityKey();
        final Long templateId = Long.valueOf(templateKey.modelId);

        // the attribute
        final TemplateAttribute attribute = this.restService.getBuilder(GetTemplateAttribute.class)
                .withURIVariable(API.PARAM_PARENT_MODEL_ID, templateKey.modelId)
                .withURIVariable(API.PARAM_MODEL_ID, attributeKey.modelId)
                .call()
                .getOrThrow();

        // the follow-up configuration
        final Configuration configuration = this.restService.getBuilder(GetConfigurations.class)
                .withQueryParam(Configuration.FILTER_ATTR_CONFIGURATION_NODE_ID, templateKey.getModelId())
                .withQueryParam(Configuration.FILTER_ATTR_FOLLOWUP, Constants.TRUE_STRING)
                .call()
                .map(Utils::toSingleton)
                .onError(pageContext::notifyError)
                .getOrThrow();

        // the default page layout with title
        final Composite content = widgetFactory.defaultPageLayout(
                pageContext.getParent(),
                FORM_TITLE);

        final PageContext formContext = pageContext.copyOf(content);

        final boolean hasView = attribute.getOrientation() != null;

        final FormHandle<TemplateAttribute> formHandle = this.pageService.formBuilder(
                formContext, 4)
                .readonly(true) // TODO change this for next version
                .addField(FormBuilder.text(
                        Domain.CONFIGURATION_ATTRIBUTE.ATTR_NAME,
                        FORM_NAME_TEXT_KEY,
                        attribute::getName))
                .addField(FormBuilder.text(
                        Domain.CONFIGURATION_ATTRIBUTE.ATTR_TYPE,
                        FORM_TYPE_TEXT_KEY,
                        () -> attribute.getConfigAttribute().getType().name()))
                .addFieldIf(
                        () -> hasView,
                        () -> FormBuilder.singleSelection(
                                Domain.ORIENTATION.ATTR_VIEW_ID,
                                FORM_VIEW_TEXT_KEY,
                                attribute.getViewModelId(),
                                () -> this.resourceService.getViewResources(templateKey.modelId)))
                .addFieldIf(
                        () -> hasView,
                        () -> FormBuilder.text(
                                Domain.ORIENTATION.ATTR_GROUP_ID,
                                FORM_GROUP_TEXT_KEY,
                                attribute.getGroupId()))
                .build();

        widgetFactory.labelLocalizedTitle(
                content,
                FORM_VALUE_TEXT_KEY);

        final InputFieldBuilder inputFieldBuilder = this.examConfigurationService.getInputFieldBuilder(
                attribute.getConfigAttribute(),
                attribute.getOrientation());
        final AttributeMapping attributeMapping = this.examConfigurationService
                .getAttributes(templateId)
                .getOrThrow();

        final ViewContext viewContext = this.examConfigurationService.createViewContext(
                formContext,
                configuration,
                new View(-1L, "template", 10, 0, templateId),
                attributeMapping,
                1);

        final InputField createInputField = inputFieldBuilder.createInputField(
                content,
                attribute.getConfigAttribute(),
                viewContext);

        viewContext.registerInputField(createInputField);

        this.examConfigurationService.initInputFieldValues(
                configuration.id,
                Arrays.asList(viewContext));

        final PageActionBuilder pageActionBuilder = this.pageService
                .pageActionBuilder(formContext.clearEntityKeys());
        pageActionBuilder

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_TEMPLATE_ATTR_FORM_SET_DEFAULT)
                .withEntityKey(attributeKey)
                .withParentEntityKey(templateKey)
                .withExec(this.examConfigurationService::resetToDefaults)
                .ignoreMoveAwayFromEdit()
                .publish()

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_TEMPLATE_ATTR_FORM_EDIT_TEMPLATE)
                .withEntityKey(templateKey)
                .publish();

    }

}
