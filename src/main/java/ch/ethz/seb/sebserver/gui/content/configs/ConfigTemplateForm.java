/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.configs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.widgets.Composite;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationStatus;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.TemplateAttribute;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.examconfig.ExamConfigurationService;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.DeleteExamConfiguration;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetConfigurationValues;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetConfigurations;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetExamConfigNode;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetTemplateAttributePage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.NewExamConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.SaveExamConfig;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser.EntityGrantCheck;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition.TableFilterAttribute;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.table.TableFilter.CriteriaType;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class ConfigTemplateForm implements TemplateComposer {

    private static final LocTextKey FORM_TITLE_NEW =
            new LocTextKey("sebserver.configtemplate.form.title.new");
    private static final LocTextKey FORM_TITLE =
            new LocTextKey("sebserver.configtemplate.form.title");
    private static final LocTextKey FORM_NAME_TEXT_KEY =
            new LocTextKey("sebserver.configtemplate.form.name");
    private static final LocTextKey FORM_DESCRIPTION_TEXT_KEY =
            new LocTextKey("sebserver.configtemplate.form.description");
    private static final LocTextKey ATTRIBUTES_LIST_TITLE_TEXT_KEY =
            new LocTextKey("sebserver.configtemplate.attrs.list.title");
    private static final LocTextKey ATTRIBUTES_LIST_TITLE_TOOLTIP_TEXT_KEY =
            new LocTextKey("sebserver.configtemplate.attrs.list.title" + Constants.TOOLTIP_TEXT_KEY_SUFFIX);
    private static final LocTextKey ATTRIBUTES_LIST_NAME_TEXT_KEY =
            new LocTextKey("sebserver.configtemplate.attrs.list.name");
    private static final LocTextKey ATTRIBUTES_LIST_VIEW_TEXT_KEY =
            new LocTextKey("sebserver.configtemplate.attrs.list.view");
    private static final LocTextKey ATTRIBUTES_LIST_GROUP_TEXT_KEY =
            new LocTextKey("sebserver.configtemplate.attrs.list.group");
    private static final LocTextKey ATTRIBUTES_LIST_VALUE_TEXT_KEY =
            new LocTextKey("sebserver.configtemplate.attrs.list.value");
    private static final LocTextKey ATTRIBUTES_LIST_TYPE_TEXT_KEY =
            new LocTextKey("sebserver.configtemplate.attrs.list.type");
    private static final LocTextKey EMPTY_ATTRIBUTE_SELECTION_TEXT_KEY =
            new LocTextKey("sebserver.configtemplate.attr.info.pleaseSelect");

    static final LocTextKey CONFIRM_DELETE =
            new LocTextKey("sebserver.configtemplate.message.confirm.delete");
    static final LocTextKey DELETE_CONFIRM_TITLE =
            new LocTextKey("sebserver.dialog.confirm.title");

    private final static LocTextKey DELETE_ERROR_DEPENDENCY =
            new LocTextKey("sebserver.configtemplate.message.delete.partialerror");
    private final static LocTextKey DELETE_CONFIRM =
            new LocTextKey("sebserver.configtemplate.message.delete.confirm");

    private final PageService pageService;
    private final RestService restService;
    private final CurrentUser currentUser;
    private final SEBExamConfigCreationPopup sebxamConfigCreationPopup;
    private final I18nSupport i18nSupport;
    private final ResourceService resourceService;
    private final ExamConfigurationService examConfigurationService;

    private final TableFilterAttribute nameFilter =
            new TableFilterAttribute(CriteriaType.TEXT, TemplateAttribute.FILTER_ATTR_NAME);
    private final TableFilterAttribute groupFilter =
            new TableFilterAttribute(CriteriaType.TEXT, TemplateAttribute.FILTER_ATTR_GROUP);

    protected ConfigTemplateForm(
            final PageService pageService,
            final SEBExamConfigCreationPopup sebxamConfigCreationPopup,
            final ExamConfigurationService examConfigurationService) {

        this.pageService = pageService;
        this.restService = pageService.getRestService();
        this.currentUser = pageService.getCurrentUser();
        this.i18nSupport = pageService.getI18nSupport();
        this.resourceService = pageService.getResourceService();
        this.examConfigurationService = examConfigurationService;
        this.sebxamConfigCreationPopup = sebxamConfigCreationPopup;

    }

    @Override
    public void compose(final PageContext pageContext) {

        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();
        final ResourceService resourceService = this.pageService.getResourceService();

        final UserInfo user = this.currentUser.get();
        final EntityKey entityKey = pageContext.getEntityKey();
        final boolean isNew = entityKey == null;

        // get data or create new. Handle error if happen
        final ConfigurationNode examConfig = (isNew)
                ? ConfigurationNode.createNewTemplate(user.institutionId)
                : this.restService
                        .getBuilder(GetExamConfigNode.class)
                        .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                        .call()
                        .onError(error -> pageContext.notifyLoadError(EntityType.CONFIGURATION_NODE, error))
                        .getOrThrow();

        final EntityGrantCheck entityGrant = this.currentUser.entityGrantCheck(examConfig);
        final boolean writeGrant = entityGrant.w();
        final boolean modifyGrant = entityGrant.m();
        final boolean isReadonly = pageContext.isReadonly();

        // new PageContext with actual EntityKey
        final PageContext formContext = pageContext
                .withEntityKey(examConfig.getEntityKey());

        // the default page layout with interactive title
        final LocTextKey titleKey = (isNew)
                ? FORM_TITLE_NEW
                : FORM_TITLE;
        final Composite content = widgetFactory.defaultPageLayout(
                formContext.getParent(),
                titleKey);

        // The SEBClientConfig form
        final FormHandle<ConfigurationNode> formHandle = this.pageService.formBuilder(
                formContext.copyOf(content))
                .readonly(isReadonly)
                .putStaticValueIf(() -> !isNew,
                        Domain.CONFIGURATION_NODE.ATTR_ID,
                        examConfig.getModelId())
                .putStaticValue(
                        Domain.CONFIGURATION_NODE.ATTR_INSTITUTION_ID,
                        String.valueOf(examConfig.getInstitutionId()))
                .putStaticValue(
                        Domain.CONFIGURATION_NODE.ATTR_TYPE,
                        ConfigurationType.TEMPLATE.name())
                .putStaticValue(
                        Domain.CONFIGURATION_NODE.ATTR_STATUS,
                        ConfigurationStatus.IN_USE.name())
                .addField(FormBuilder.text(
                        Domain.CONFIGURATION_NODE.ATTR_NAME,
                        FORM_NAME_TEXT_KEY,
                        examConfig.name)
                        .mandatory(!isReadonly))
                .addField(FormBuilder.text(
                        Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION,
                        FORM_DESCRIPTION_TEXT_KEY,
                        examConfig.description)
                        .asArea())

                .buildFor((isNew)
                        ? this.restService.getRestCall(NewExamConfig.class)
                        : this.restService.getRestCall(SaveExamConfig.class));

        final PageActionBuilder pageActionBuilder = this.pageService
                .pageActionBuilder(formContext.clearEntityKeys());

        if (isReadonly) {

            widgetFactory.addFormSubContextHeader(
                    content,
                    ATTRIBUTES_LIST_TITLE_TEXT_KEY,
                    ATTRIBUTES_LIST_TITLE_TOOLTIP_TEXT_KEY);

            final TableFilterAttribute viewFilter = new TableFilterAttribute(
                    CriteriaType.SINGLE_SELECTION,
                    TemplateAttribute.FILTER_ATTR_VIEW,
                    () -> this.resourceService.getViewResources(entityKey.modelId));
            final TableFilterAttribute typeFilter = new TableFilterAttribute(
                    CriteriaType.SINGLE_SELECTION,
                    TemplateAttribute.FILTER_ATTR_TYPE,
                    this.resourceService::getAttributeTypeResources);

            // TODO move this to an supplier that also can be updated
            // the follow-up configuration
            final Configuration configuration = this.restService
                    .getBuilder(GetConfigurations.class)
                    .withQueryParam(Configuration.FILTER_ATTR_CONFIGURATION_NODE_ID, examConfig.getModelId())
                    .withQueryParam(Configuration.FILTER_ATTR_FOLLOWUP, Constants.TRUE_STRING)
                    .call()
                    .map(Utils::toSingleton)
                    .onError(error -> pageContext.notifyLoadError(EntityType.CONFIGURATION, error))
                    .getOrThrow();
            final AttributeValueSupplier attributeValueSupplier = new AttributeValueSupplier(
                    this.pageService,
                    configuration.getModelId());

            final EntityTable<TemplateAttribute> attrTable =
                    this.pageService.entityTableBuilder(
                            Domain.CONFIGURATION_NODE.TYPE_NAME + "_Template",
                            this.restService.getRestCall(GetTemplateAttributePage.class))
                            .withRestCallAdapter(restCall -> restCall.withURIVariable(
                                    API.PARAM_PARENT_MODEL_ID,
                                    entityKey.modelId))
                            .withPaging(15)

                            .withColumn(new ColumnDefinition<>(
                                    Domain.CONFIGURATION_ATTRIBUTE.ATTR_NAME,
                                    ATTRIBUTES_LIST_NAME_TEXT_KEY,
                                    this::getAttributeName)
                                            .withFilter(this.nameFilter)
                                            .sortable()
                                            .widthProportion(3))

                            .withColumn(new ColumnDefinition<TemplateAttribute>(
                                    Domain.CONFIGURATION_ATTRIBUTE.ATTR_TYPE,
                                    ATTRIBUTES_LIST_TYPE_TEXT_KEY,
                                    resourceService::getAttributeTypeName)
                                            .withFilter(typeFilter)
                                            .sortable()
                                            .widthProportion(1))

                            .withColumn(new ColumnDefinition<>(
                                    Domain.ORIENTATION.ATTR_VIEW_ID,
                                    ATTRIBUTES_LIST_VIEW_TEXT_KEY,
                                    resourceService.getViewNameFunction(entityKey.modelId))
                                            .withFilter(viewFilter)
                                            .sortable()
                                            .widthProportion(1))

                            .withColumn(new ColumnDefinition<>(
                                    Domain.ORIENTATION.ATTR_GROUP_ID,
                                    ATTRIBUTES_LIST_GROUP_TEXT_KEY,
                                    TemplateAttribute::getGroupId)
                                            .withFilter(this.groupFilter)
                                            .sortable()
                                            .widthProportion(1))

                            .withColumn(new ColumnDefinition<TemplateAttribute>(
                                    Domain.CONFIGURATION_VALUE.ATTR_VALUE,
                                    ATTRIBUTES_LIST_VALUE_TEXT_KEY,
                                    attr -> attributeValueSupplier.getAttributeValue(attr.getConfigAttribute().id))
                                            .widthProportion(1))

                            .withDefaultActionIf(
                                    () -> modifyGrant,
                                    () -> pageActionBuilder
                                            .newAction(ActionDefinition.SEB_EXAM_CONFIG_TEMPLATE_ATTR_EDIT)
                                            .withParentEntityKey(entityKey)
                                            .create())

                            .withSelectionListener(this.pageService.getSelectionPublisher(
                                    pageContext,
                                    ActionDefinition.SEB_EXAM_CONFIG_TEMPLATE_ATTR_EDIT,
                                    ActionDefinition.SEB_EXAM_CONFIG_TEMPLATE_ATTR_SET_DEFAULT,
                                    ActionDefinition.SEB_EXAM_CONFIG_TEMPLATE_ATTR_LIST_REMOVE_VIEW,
                                    ActionDefinition.SEB_EXAM_CONFIG_TEMPLATE_ATTR_LIST_ATTACH_DEFAULT_VIEW))

                            .compose(pageContext.copyOf(content));

            pageActionBuilder

                    .newAction(ActionDefinition.SEB_EXAM_CONFIG_TEMPLATE_ATTR_EDIT)
                    .withParentEntityKey(entityKey)
                    .withSelect(
                            attrTable::getMultiSelection,
                            PageAction::applySingleSelectionAsEntityKey,
                            EMPTY_ATTRIBUTE_SELECTION_TEXT_KEY)
                    .publishIf(() -> modifyGrant, false)

                    .newAction(ActionDefinition.SEB_EXAM_CONFIG_TEMPLATE_ATTR_SET_DEFAULT)
                    .withParentEntityKey(entityKey)
                    .withSelect(
                            attrTable::getMultiSelection,
                            action -> this.resetToDefaults(attributeValueSupplier, action, attrTable),
                            EMPTY_ATTRIBUTE_SELECTION_TEXT_KEY)
                    .noEventPropagation()
                    .publishIf(() -> modifyGrant, false)

                    .newAction(ActionDefinition.SEB_EXAM_CONFIG_TEMPLATE_ATTR_LIST_REMOVE_VIEW)
                    .withParentEntityKey(entityKey)
                    .withSelect(
                            attrTable::getMultiSelection,
                            action -> this.removeFormView(action, attrTable),
                            EMPTY_ATTRIBUTE_SELECTION_TEXT_KEY)
                    .noEventPropagation()
                    .publishIf(() -> modifyGrant, false)

                    .newAction(ActionDefinition.SEB_EXAM_CONFIG_TEMPLATE_ATTR_LIST_ATTACH_DEFAULT_VIEW)
                    .withParentEntityKey(entityKey)
                    .withSelect(
                            attrTable::getMultiSelection,
                            action -> this.attachView(action, attrTable),
                            EMPTY_ATTRIBUTE_SELECTION_TEXT_KEY)
                    .noEventPropagation()
                    .publishIf(() -> modifyGrant, false);
        }

        pageActionBuilder

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_TEMPLATE_NEW)
                .publishIf(() -> writeGrant && isReadonly)

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_TEMPLATE_MODIFY)
                .withEntityKey(entityKey)
                .publishIf(() -> modifyGrant && isReadonly)

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_DELETE)
                .withEntityKey(entityKey)
                .withConfirm(() -> CONFIRM_DELETE)
                .withExec(this::deleteConfiguration)
                .publishIf(() -> writeGrant && isReadonly)

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_TEMPLATE_CREATE_CONFIG)
                .withEntityKey(entityKey)
                .withExec(this.sebxamConfigCreationPopup.configCreationFunction(
                        pageContext
                                .withAttribute(
                                        PageContext.AttributeKeys.CREATE_FROM_TEMPLATE,
                                        Constants.TRUE_STRING)
                                .withAttribute(
                                        PageContext.AttributeKeys.COPY_AS_TEMPLATE,
                                        Constants.FALSE_STRING)))
                .publishIf(() -> modifyGrant && isReadonly)

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_TEMPLATE_SAVE)
                .withEntityKey(entityKey)
                .withExec(formHandle::processFormSave)
                .ignoreMoveAwayFromEdit()
                .publishIf(() -> !isReadonly)

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_TEMPLATE_CANCEL_MODIFY)
                .withEntityKey(entityKey)
                .withExec(this.pageService.backToCurrentFunction())
                .publishIf(() -> !isReadonly);

    }

    private PageAction deleteConfiguration(final PageAction action) {
        final ConfigurationNode configNode = this.restService
                .getBuilder(GetExamConfigNode.class)
                .withURIVariable(API.PARAM_MODEL_ID, action.getEntityKey().modelId)
                .call()
                .getOrThrow();

        final Result<EntityProcessingReport> call = this.restService
                .getBuilder(DeleteExamConfiguration.class)
                .withURIVariable(API.PARAM_MODEL_ID, action.getEntityKey().modelId)
                .call();

        final PageContext pageContext = action.pageContext();

        final EntityProcessingReport report = call.getOrThrow();
        final String configName = configNode.toName().name;
        if (report.getErrors().isEmpty()) {
            pageContext.publishPageMessage(DELETE_CONFIRM_TITLE, new LocTextKey(DELETE_CONFIRM.name, configName));
        } else {
            pageContext.publishPageMessage(
                    DELETE_CONFIRM_TITLE,
                    new LocTextKey(DELETE_ERROR_DEPENDENCY.name, configName,
                            report.getErrors().iterator().next().getErrorMessage().systemMessage));
        }

        return this.pageService.pageActionBuilder(pageContext)
                .newAction(ActionDefinition.SEB_EXAM_CONFIG_TEMPLATE_LIST)
                .create();
    }

    private String getAttributeName(final TemplateAttribute attribute) {

        final String name = this.i18nSupport.getText(
                ExamConfigurationService.ATTRIBUTE_LABEL_LOC_TEXT_PREFIX + attribute.getName(),
                "");
        if (StringUtils.isNotBlank(name)) {
            return attribute.getName() + " (" + name + ")";
        } else {
            return attribute.getName();
        }
    }

    private PageAction resetToDefaults(
            final AttributeValueSupplier attributeValueSupplier,
            final PageAction action,
            final EntityTable<TemplateAttribute> attrTable) {

        final PageAction resetToDefaults = this.examConfigurationService.resetToDefaults(action);
        // reload the list
        attributeValueSupplier.update();
        attrTable.updateCurrentPage();
        return resetToDefaults;
    }

    private PageAction removeFormView(
            final PageAction action,
            final EntityTable<TemplateAttribute> attrTable) {

        final PageAction removeFormView = this.examConfigurationService.removeFromView(action);
        // reload the page
        attrTable.updateCurrentPage();
        return removeFormView;
    }

    private PageAction attachView(
            final PageAction action,
            final EntityTable<TemplateAttribute> attrTable) {

        final PageAction attachView = this.examConfigurationService.attachToDefaultView(action);
        // reload the page
        attrTable.updateCurrentPage();
        return attachView;
    }

    private final class AttributeValueSupplier {

        private final PageService pageService;
        private final String configurationId;
        private final Map<Long, String> attrValueMapping = new HashMap<>();

        public AttributeValueSupplier(
                final PageService pageService,
                final String configurationId) {

            this.pageService = pageService;
            this.configurationId = configurationId;
            update();
        }

        public String getAttributeValue(final Long attributeId) {
            if (this.attrValueMapping.containsKey(attributeId)) {
                return this.attrValueMapping.get(attributeId);
            } else {
                return Constants.EMPTY_NOTE;
            }
        }

        private void update() {
            this.attrValueMapping.clear();

            this.pageService.getRestService()
                    .getBuilder(GetConfigurationValues.class)
                    .withQueryParam(
                            ConfigurationValue.FILTER_ATTR_CONFIGURATION_ID,
                            this.configurationId)
                    .call()
                    .getOrElse(Collections::emptyList)
                    .stream()
                    .forEach(val -> {
                        if (this.attrValueMapping.containsKey(val.attributeId)) {
                            this.attrValueMapping.put(val.attributeId,
                                    this.attrValueMapping.get(val.attributeId) + "," + val.value);
                        } else {
                            this.attrValueMapping.put(val.attributeId, val.value);
                        }
                    });
        }
    }

}
