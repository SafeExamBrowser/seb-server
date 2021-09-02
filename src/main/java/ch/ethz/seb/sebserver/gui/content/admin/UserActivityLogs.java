/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.admin;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.tomcat.util.buf.StringUtils;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityName;
import ch.ethz.seb.sebserver.gbl.model.user.UserActivityLog;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputDialog;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.logs.GetUserLogNames;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.logs.GetUserLogPage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.GetUserAccount;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition.TableFilterAttribute;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.table.TableFilter.CriteriaType;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class UserActivityLogs implements TemplateComposer {

    private static final Logger log = LoggerFactory.getLogger(UserActivityLogs.class);

    private static final LocTextKey DETAILS_TITLE_TEXT_KEY =
            new LocTextKey("sebserver.userlogs.details.title");
    private static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.userlogs.list.title");
    private static final LocTextKey EMPTY_TEXT_KEY =
            new LocTextKey("sebserver.userlogs.list.empty");
    private static final LocTextKey INSTITUTION_TEXT_KEY =
            new LocTextKey("sebserver.userlogs.list.column.institution");
    private static final LocTextKey USER_TEXT_KEY =
            new LocTextKey("sebserver.userlogs.list.column.user");
    private static final LocTextKey DATE_TEXT_KEY =
            new LocTextKey("sebserver.userlogs.list.column.dateTime");
    private static final LocTextKey ACTIVITY_TEXT_KEY =
            new LocTextKey("sebserver.userlogs.list.column.activityType");
    private static final LocTextKey ENTITY_TYPE_TEXT_KEY =
            new LocTextKey("sebserver.userlogs.list.column.entityType");
    private final static LocTextKey EMPTY_SELECTION_TEXT =
            new LocTextKey("sebserver.userlogs.info.pleaseSelect");

    private static final LocTextKey FORM_USER_TEXT_KEY =
            new LocTextKey("sebserver.userlogs.form.user");
    private static final LocTextKey FORM_DATE_TEXT_KEY =
            new LocTextKey("sebserver.userlogs.form.dateTime");
    private static final LocTextKey FORM_ACTIVITY_TEXT_KEY =
            new LocTextKey("sebserver.userlogs.form.activityType");
    private static final LocTextKey FORM_ENTITY_TYPE_TEXT_KEY =
            new LocTextKey("sebserver.userlogs.form.entityType");
    private static final LocTextKey FORM_ENTITY_ID_TEXT_KEY =
            new LocTextKey("sebserver.userlogs.form.entityId");
    private static final LocTextKey FORM_MESSAGE_TEXT_KEY =
            new LocTextKey("sebserver.userlogs.form.message");

    private final TableFilterAttribute institutionFilter;
    private final TableFilterAttribute userNameFilter =
            new TableFilterAttribute(CriteriaType.TEXT, UserActivityLog.FILTER_ATTR_USER_NAME);
    private final TableFilterAttribute activityFilter;
    private final TableFilterAttribute entityFilter;

    private final PageService pageService;
    private final ResourceService resourceService;
    private final I18nSupport i18nSupport;
    private final WidgetFactory widgetFactory;
    private final UserActivityLogsDeletePopup userActivityLogsDeletePopup;
    private final int pageSize;

    public UserActivityLogs(
            final PageService pageService,
            final UserActivityLogsDeletePopup userActivityLogsDeletePopup,
            @Value("${sebserver.gui.list.page.size:20}") final Integer pageSize) {

        this.pageService = pageService;
        this.resourceService = pageService.getResourceService();
        this.i18nSupport = this.resourceService.getI18nSupport();
        this.widgetFactory = pageService.getWidgetFactory();
        this.userActivityLogsDeletePopup = userActivityLogsDeletePopup;
        this.pageSize = pageSize;

        this.institutionFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                Entity.FILTER_ATTR_INSTITUTION,
                this.resourceService::institutionResource);

        this.activityFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                UserActivityLog.FILTER_ATTR_ACTIVITY_TYPES,
                this.resourceService::userActivityTypeResources);

        this.entityFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                UserActivityLog.FILTER_ATTR_ENTITY_TYPES,
                this.resourceService::entityTypeResources);
    }

    @Override
    public void compose(final PageContext pageContext) {
        final CurrentUser currentUser = this.resourceService.getCurrentUser();
        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();
        final RestService restService = this.resourceService.getRestService();
        // content page layout with title
        final Composite content = widgetFactory.defaultPageLayout(
                pageContext.getParent(),
                TITLE_TEXT_KEY);

        final PageActionBuilder actionBuilder = this.pageService.pageActionBuilder(
                pageContext
                        .clearEntityKeys()
                        .clearAttributes());

        final BooleanSupplier isSEBAdmin =
                () -> currentUser.get().hasRole(UserRole.SEB_SERVER_ADMIN);

        final Function<UserActivityLog, String> institutionNameFunction =
                this.resourceService.getInstitutionNameFunction()
                        .compose(log -> {
                            try {
                                final UserInfo user = restService.getBuilder(GetUserAccount.class)
                                        .withURIVariable(API.PARAM_MODEL_ID, log.getUserUuid())
                                        .call().getOrThrow();
                                return String.valueOf(user.getInstitutionId());
                            } catch (final Exception e) {
                                return Constants.EMPTY_NOTE;
                            }
                        });

        final Consumer<Boolean> deleteActionActivation = this.pageService.getActionActiviationPublisher(
                pageContext,
                ActionDefinition.LOGS_USER_ACTIVITY_DELETE_ALL);
        final Consumer<Boolean> detailsActionActivation = this.pageService.getActionActiviationPublisher(
                pageContext,
                ActionDefinition.LOGS_USER_ACTIVITY_SHOW_DETAILS);
        final Consumer<Integer> contentChangeListener = contentSize -> {
            deleteActionActivation.accept(contentSize > 0);
            detailsActionActivation.accept(contentSize > 0);
        };

        // table
        final EntityTable<UserActivityLog> table = this.pageService.entityTableBuilder(
                restService.getRestCall(GetUserLogPage.class))
                .withEmptyMessage(EMPTY_TEXT_KEY)
                .withPaging(this.pageSize)
                .withDefaultSort(isSEBAdmin.getAsBoolean()
                        ? UserActivityLog.FILTER_ATTR_INSTITUTION
                        : UserActivityLog.ATTR_USER_NAME)
                .withColumnIf(
                        isSEBAdmin,
                        () -> new ColumnDefinition<>(
                                UserActivityLog.FILTER_ATTR_INSTITUTION,
                                INSTITUTION_TEXT_KEY,
                                institutionNameFunction)
                                        .withFilter(this.institutionFilter)
                                        .sortable())

                .withColumn(new ColumnDefinition<>(
                        UserActivityLog.ATTR_USER_NAME,
                        USER_TEXT_KEY,
                        UserActivityLog::getUsername)
                                .sortable()
                                .withFilter(this.userNameFilter))

                .withColumn(new ColumnDefinition<UserActivityLog>(
                        Domain.USER_ACTIVITY_LOG.ATTR_ACTIVITY_TYPE,
                        ACTIVITY_TEXT_KEY,
                        this.resourceService::getUserActivityTypeName)
                                .withFilter(this.activityFilter)
                                .sortable())

                .withColumn(new ColumnDefinition<UserActivityLog>(
                        Domain.USER_ACTIVITY_LOG.ATTR_ENTITY_ID,
                        ENTITY_TYPE_TEXT_KEY,
                        this.resourceService::getEntityTypeName)
                                .withFilter(this.entityFilter)
                                .sortable())

                .withColumn(new ColumnDefinition<>(
                        Domain.USER_ACTIVITY_LOG.ATTR_TIMESTAMP,
                        new LocTextKey(DATE_TEXT_KEY.name, this.i18nSupport.getUsersTimeZoneTitleSuffix()),
                        this::getLogTime)
                                .withFilter(new TableFilterAttribute(
                                        CriteriaType.DATE_RANGE,
                                        UserActivityLog.FILTER_ATTR_FROM_TO,
                                        Utils.toDateTimeUTC(Utils.getMillisecondsNow())
                                                .minusYears(1)
                                                .toString()))
                                .sortable())

                .withDefaultAction(t -> actionBuilder
                        .newAction(ActionDefinition.LOGS_USER_ACTIVITY_SHOW_DETAILS)
                        .withExec(action -> this.showDetails(action, t.getSingleSelectedROWData()))
                        .noEventPropagation()
                        .create())

                .withSelectionListener(this.pageService.getSelectionPublisher(
                        pageContext,
                        ActionDefinition.LOGS_USER_ACTIVITY_SHOW_DETAILS))

                .withContentChangeListener(contentChangeListener)
                .compose(pageContext.copyOf(content));

        actionBuilder
                .newAction(ActionDefinition.LOGS_USER_ACTIVITY_SHOW_DETAILS)
                .withSelect(
                        table::getSelection,
                        action -> this.showDetails(action, table.getSingleSelectedROWData()),
                        EMPTY_SELECTION_TEXT)
                .noEventPropagation()
                .publish(false)

                .newAction(ActionDefinition.LOGS_USER_ACTIVITY_DELETE_ALL)
                .withExec(action -> this.getOpenDelete(action, table.getFilterCriteria()))
                .noEventPropagation()
                .publishIf(isSEBAdmin, table.hasAnyContent());
    }

    private PageAction getOpenDelete(final PageAction pageAction, final MultiValueMap<String, String> filterCriteria) {
        try {
            final List<String> ids = this.pageService
                    .getRestService()
                    .getBuilder(GetUserLogNames.class)
                    .withQueryParams(filterCriteria)
                    .call()
                    .getOrThrow()
                    .stream()
                    .map(EntityName::getModelId)
                    .collect(Collectors.toList());

            final PageAction deleteAction = pageAction.withAttribute(
                    PageContext.AttributeKeys.ENTITY_ID_LIST,
                    StringUtils.join(ids, Constants.COMMA))
                    .withAttribute(
                            PageContext.AttributeKeys.ENTITY_LIST_TYPE,
                            EntityType.CLIENT_EVENT.name());

            return this.userActivityLogsDeletePopup
                    .deleteWizardFunction(deleteAction.pageContext())
                    .apply(deleteAction);
        } catch (final Exception e) {
            log.error("Unexpected error while try to open user activity log delete popup", e);
            return pageAction;
        }
    }

    private String getLogTime(final UserActivityLog log) {
        if (log == null || log.timestamp == null) {
            return Constants.EMPTY_NOTE;
        }

        return this.i18nSupport
                .formatDisplayDateTime(Utils.toDateTimeUTC(log.timestamp));
    }

    private PageAction showDetails(final PageAction action, final UserActivityLog userActivityLog) {
        action.getSingleSelection();

        final ModalInputDialog<Void> dialog = new ModalInputDialog<>(
                action.pageContext().getParent().getShell(),
                this.widgetFactory);
        dialog.setLargeDialogWidth();
        dialog.open(
                DETAILS_TITLE_TEXT_KEY,
                action.pageContext(),
                pc -> createDetailsForm(userActivityLog, pc));

        return action;
    }

    private void createDetailsForm(final UserActivityLog userActivityLog, final PageContext pc) {

        final Composite parent = pc.getParent();
        final Composite grid = this.widgetFactory.createPopupScrollComposite(parent);

        this.pageService.formBuilder(pc.copyOf(grid))
                .withDefaultSpanInput(6)
                .withEmptyCellSeparation(false)
                .readonly(true)
                .addField(FormBuilder.text(
                        UserActivityLog.ATTR_USER_NAME,
                        FORM_USER_TEXT_KEY,
                        String.valueOf(userActivityLog.getUsername())))
                .addField(FormBuilder.text(
                        Domain.USER_ACTIVITY_LOG.ATTR_ACTIVITY_TYPE,
                        FORM_ACTIVITY_TEXT_KEY,
                        this.resourceService.getUserActivityTypeName(userActivityLog)))
                .addField(FormBuilder.text(
                        Domain.USER_ACTIVITY_LOG.ATTR_ENTITY_TYPE,
                        FORM_ENTITY_TYPE_TEXT_KEY,
                        this.resourceService.getEntityTypeName(userActivityLog)))
                .addField(FormBuilder.text(
                        Domain.USER_ACTIVITY_LOG.ATTR_ENTITY_ID,
                        FORM_ENTITY_ID_TEXT_KEY,
                        userActivityLog.entityId))
                .addField(FormBuilder.text(
                        Domain.USER_ACTIVITY_LOG.ATTR_TIMESTAMP,
                        FORM_DATE_TEXT_KEY,
                        this.widgetFactory.getI18nSupport()
                                .formatDisplayDateTime(Utils.toDateTimeUTC(userActivityLog.timestamp)) + " " +
                                this.i18nSupport.getUsersTimeZoneTitleSuffix()))
                .addField(FormBuilder.text(
                        Domain.USER_ACTIVITY_LOG.ATTR_MESSAGE,
                        FORM_MESSAGE_TEXT_KEY,
                        String.valueOf(userActivityLog.message))
                        .asArea())
                .build();
    }

}
