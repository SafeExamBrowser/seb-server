/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.configs;

import java.util.Collections;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.TemplateAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.TitleOrientation;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.View;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.examconfig.ExamConfigurationService;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputField;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputFieldBuilder;
import ch.ethz.seb.sebserver.gui.service.examconfig.impl.AttributeMapping;
import ch.ethz.seb.sebserver.gui.service.examconfig.impl.ViewContext;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetConfigurations;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetExamConfigNode;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetTemplateAttribute;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser.EntityGrantCheck;
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
    private static final LocTextKey FORM_VALUE_TOOLTIP_TEXT_KEY =
            new LocTextKey("sebserver.configtemplate.attr.form.value" + Constants.TOOLTIP_TEXT_KEY_SUFFIX);

    private final PageService pageService;
    private final RestService restService;
    private final CurrentUser currentUser;
    private final ResourceService resourceService;
    private final ExamConfigurationService examConfigurationService;

    protected ConfigTemplateAttributeForm(
            final PageService pageService,
            final ExamConfigurationService examConfigurationService) {

        this.pageService = pageService;
        this.restService = pageService.getRestService();
        this.currentUser = pageService.getCurrentUser();
        this.resourceService = pageService.getResourceService();
        this.examConfigurationService = examConfigurationService;

    }

    @Override
    public void compose(final PageContext pageContext) {
        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();

        final EntityKey attributeKey = pageContext.getEntityKey();
        final EntityKey templateKey = pageContext.getParentEntityKey();
        final Long templateId = Long.valueOf(templateKey.modelId);

        try {

            final ConfigurationNode template = this.restService
                    .getBuilder(GetExamConfigNode.class)
                    .withURIVariable(API.PARAM_MODEL_ID, templateKey.modelId)
                    .call()
                    .onError(error -> pageContext.notifyLoadError(EntityType.CONFIGURATION_NODE, error))
                    .getOrThrow();

            final EntityGrantCheck entityGrant = this.currentUser.entityGrantCheck(template);
            final boolean modifyGrant = entityGrant.m();

            // the attribute
            final TemplateAttribute attribute = this.restService.getBuilder(GetTemplateAttribute.class)
                    .withURIVariable(API.PARAM_PARENT_MODEL_ID, templateKey.modelId)
                    .withURIVariable(API.PARAM_MODEL_ID, attributeKey.modelId)
                    .call()
                    .onError(error -> pageContext.notifyLoadError(EntityType.CONFIGURATION_NODE, error))
                    .getOrThrow();

            // the follow-up configuration
            final Configuration configuration = this.restService.getBuilder(GetConfigurations.class)
                    .withQueryParam(Configuration.FILTER_ATTR_CONFIGURATION_NODE_ID, templateKey.getModelId())
                    .withQueryParam(Configuration.FILTER_ATTR_FOLLOWUP, Constants.TRUE_STRING)
                    .call()
                    .map(Utils::toSingleton)
                    .onError(error -> pageContext.notifyLoadError(EntityType.CONFIGURATION, error))
                    .getOrThrow();

            // the default page layout with title
            final Composite content = widgetFactory.defaultPageLayout(
                    pageContext.getParent(),
                    FORM_TITLE);

            final PageContext formContext = pageContext.copyOf(content);

            final boolean hasView = attribute.getOrientation() != null;

            this.pageService.formBuilder(formContext)
                    .readonly(true) // TODO change this for next version
                    .addField(FormBuilder.text(
                            Domain.CONFIGURATION_ATTRIBUTE.ATTR_NAME,
                            FORM_NAME_TEXT_KEY,
                            attribute::getName))
                    .addField(FormBuilder.text(
                            Domain.CONFIGURATION_ATTRIBUTE.ATTR_TYPE,
                            FORM_TYPE_TEXT_KEY,
                            () -> this.resourceService.getAttributeTypeName(attribute)))
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

            final Composite valSpace = new Composite(content, SWT.NONE);
            valSpace.setLayout(new GridLayout());
            valSpace.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

            widgetFactory.addFormSubContextHeader(
                    valSpace,
                    FORM_VALUE_TEXT_KEY,
                    FORM_VALUE_TOOLTIP_TEXT_KEY);

            final Composite grid = new Composite(valSpace, SWT.NONE);
            grid.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
            grid.setLayout(new GridLayout(6, true));

            final PageContext valueContext = formContext.copyOf(grid);

            final Orientation defaultOrientation = getDefaultOrientation(attribute);
            final AttributeMapping attributeMapping = this.examConfigurationService
                    .getAttributes(attribute, defaultOrientation)
                    .getOrThrow();
            final ViewContext viewContext = this.examConfigurationService.createViewContext(
                    valueContext,
                    configuration,
                    new View(-1L, "template", 10, 0, templateId),
                    viewName -> null,
                    attributeMapping,
                    1, false, null);

            final InputFieldBuilder inputFieldBuilder = this.examConfigurationService.getInputFieldBuilder(
                    attribute.getConfigAttribute(),
                    defaultOrientation);

            final InputField createInputField = inputFieldBuilder.createInputField(
                    grid,
                    attribute.getConfigAttribute(),
                    viewContext);

            viewContext.registerInputField(createInputField);

            this.examConfigurationService.initInputFieldValues(
                    configuration.id,
                    Collections.singletonList(viewContext));

            this.pageService.pageActionBuilder(formContext.clearEntityKeys())

                    .newAction(ActionDefinition.SEB_EXAM_CONFIG_TEMPLATE_ATTR_FORM_SET_DEFAULT)
                    .withEntityKey(attributeKey)
                    .withParentEntityKey(templateKey)
                    .withExec(this.examConfigurationService::resetToDefaults)
                    .ignoreMoveAwayFromEdit()
                    .publishIf(() -> modifyGrant)

                    .newAction(ActionDefinition.SEB_EXAM_CONFIG_TEMPLATE_ATTR_REMOVE_VIEW)
                    .withEntityKey(attributeKey)
                    .withParentEntityKey(templateKey)
                    .withExec(this.examConfigurationService::removeFromView)
                    .ignoreMoveAwayFromEdit()
                    .publishIf(() -> modifyGrant && hasView)

                    .newAction(ActionDefinition.SEB_EXAM_CONFIG_TEMPLATE_ATTR_ATTACH_DEFAULT_VIEW)
                    .withEntityKey(attributeKey)
                    .withParentEntityKey(templateKey)
                    .withExec(this.examConfigurationService::attachToDefaultView)
                    .ignoreMoveAwayFromEdit()
                    .publishIf(() -> modifyGrant && !hasView)

                    .newAction(ActionDefinition.SEB_EXAM_CONFIG_TEMPLATE_ATTR_FORM_EDIT_TEMPLATE)
                    .withEntityKey(templateKey)
                    .ignoreMoveAwayFromEdit()
                    .publish();

        } catch (final Exception e) {
            pageContext.notifyUnexpectedError(e);
        }
    }

    private Orientation getDefaultOrientation(final TemplateAttribute attribute) {
        return new Orientation(
                -1L,
                attribute.getConfigAttribute().id,
                attribute.templateId,
                null,
                null,
                0,
                0,
                2,
                1,
                TitleOrientation.NONE);
    }

}
