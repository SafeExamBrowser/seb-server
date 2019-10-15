/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationStatus;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.TemplateAttribute;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
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

    private static final Logger log = LoggerFactory.getLogger(ConfigTemplateForm.class);

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
    private static final LocTextKey ATTRIBUTES_LIST_NAME_TEXT_KEY =
            new LocTextKey("sebserver.configtemplate.attrs.list.name");
    private static final LocTextKey ATTRIBUTES_LIST_VIEW_TEXT_KEY =
            new LocTextKey("sebserver.configtemplate.attrs.list.view");
    private static final LocTextKey ATTRIBUTES_LIST_GROUP_TEXT_KEY =
            new LocTextKey("sebserver.configtemplate.attrs.list.group");

    private final PageService pageService;
    private final RestService restService;
    private final CurrentUser currentUser;
    private final ResourceService resourceService;

    private final TableFilterAttribute nameFilter =
            new TableFilterAttribute(CriteriaType.TEXT, TemplateAttribute.FILTER_ATTR_NAME);
    private final TableFilterAttribute groupFilter =
            new TableFilterAttribute(CriteriaType.TEXT, TemplateAttribute.FILTER_ATTR_GROUP);

    protected ConfigTemplateForm(
            final PageService pageService,
            final RestService restService,
            final CurrentUser currentUser) {

        this.pageService = pageService;
        this.restService = restService;
        this.currentUser = currentUser;
        this.resourceService = pageService.getResourceService();

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
                        .get(pageContext::notifyError);

        if (examConfig == null) {
            log.error("Failed to get ConfigurationNode for Template. "
                    + "Error was notified to the User. "
                    + "See previous logs for more infomation");
            return;
        }

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

        // The SebClientConfig form
        final FormHandle<ConfigurationNode> formHandle = this.pageService.formBuilder(
                formContext.copyOf(content), 4)
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
                        examConfig.name))
                .addField(FormBuilder.text(
                        Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION,
                        FORM_DESCRIPTION_TEXT_KEY,
                        examConfig.description)
                        .asArea())
                .buildFor((isNew)
                        ? this.restService.getRestCall(NewExamConfig.class)
                        : this.restService.getRestCall(SaveExamConfig.class));

        if (isReadonly) {

            widgetFactory.label(content, "");
            widgetFactory.labelLocalizedTitle(
                    content,
                    ATTRIBUTES_LIST_TITLE_TEXT_KEY);

            final TableFilterAttribute viewFilter = new TableFilterAttribute(
                    CriteriaType.SINGLE_SELECTION,
                    TemplateAttribute.FILTER_ATTR_VIEW,
                    () -> this.resourceService.getViewResources(entityKey.modelId));

            final EntityTable<TemplateAttribute> attrTable =
                    this.pageService.entityTableBuilder(this.restService.getRestCall(GetTemplateAttributePage.class))
                            .withRestCallAdapter(restCall -> restCall.withURIVariable(
                                    API.PARAM_MODEL_ID,
                                    entityKey.modelId))
                            .withPaging(15)
                            .withColumn(new ColumnDefinition<>(
                                    Domain.CONFIGURATION_ATTRIBUTE.ATTR_NAME,
                                    ATTRIBUTES_LIST_NAME_TEXT_KEY,
                                    TemplateAttribute::getName)
                                            .withFilter(this.nameFilter)
                                            .sortable())
                            .withColumn(new ColumnDefinition<>(
                                    Domain.ORIENTATION.ATTR_VIEW_ID,
                                    ATTRIBUTES_LIST_VIEW_TEXT_KEY,
                                    getViewNameFunction(entityKey))
                                            .withFilter(viewFilter)
                                            .sortable())
                            .withColumn(new ColumnDefinition<>(
                                    Domain.ORIENTATION.ATTR_GROUP_ID,
                                    ATTRIBUTES_LIST_GROUP_TEXT_KEY,
                                    TemplateAttribute::getGroupId)
                                            .withFilter(this.groupFilter)
                                            .sortable())
//                            .withDefaultAction(pageActionBuilder
//                                    .newAction(ActionDefinition.SEB_EXAM_CONFIG_TEMPLATE_VIEW_FROM_LIST)
//                                    .create())
                            .compose(pageContext.copyOf(content));

            // TODO list of all attributes with filter
        }

        this.pageService.pageActionBuilder(formContext.clearEntityKeys())

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_TEMPLATE_NEW)
                .publishIf(() -> writeGrant && isReadonly)

                .newAction(ActionDefinition.SEB_EXAM_CONFIG_TEMPLATE_MODIFY)
                .withEntityKey(entityKey)
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

    private final Function<TemplateAttribute, String> getViewNameFunction(final EntityKey templateId) {
        final Map<String, String> mapping = this.resourceService.getViewResources(templateId.modelId)
                .stream()
                .collect(Collectors.toMap(tuple -> tuple._1, tuple -> tuple._2));

        return attr -> mapping.get(attr.getViewModelId());
    }

}
