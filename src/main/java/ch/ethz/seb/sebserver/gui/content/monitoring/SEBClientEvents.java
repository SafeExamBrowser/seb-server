/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.monitoring;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.tomcat.util.buf.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.client.service.UrlLauncher;
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
import ch.ethz.seb.sebserver.gbl.api.authorization.PrivilegeType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityName;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.ExportType;
import ch.ethz.seb.sebserver.gbl.model.session.ExtendedClientEvent;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.download.DownloadService;
import ch.ethz.seb.sebserver.gui.service.remote.download.SEBClientLogExport;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.logs.GetClientEventNames;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.logs.GetExtendedClientEventPage;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition.TableFilterAttribute;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.table.TableFilter.CriteriaType;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class SEBClientEvents implements TemplateComposer {

    private static final Logger log = LoggerFactory.getLogger(SEBClientEvents.class);

    private static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.list.title");
    private static final LocTextKey EMPTY_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.list.empty");

    private static final LocTextKey EXAM_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.list.column.exam");
    private static final LocTextKey CLIENT_SESSION_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.list.column.client-session");
    private static final LocTextKey TYPE_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.list.column.type");
    private static final LocTextKey TIME_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.list.column.time");
    private static final LocTextKey VALUE_TEXT_KEY =
            new LocTextKey("sebserver.seblogs.list.column.value");
    private final static LocTextKey EMPTY_SELECTION_TEXT =
            new LocTextKey("sebserver.seblogs.info.pleaseSelect");

    private final TableFilterAttribute examFilter;
    private final TableFilterAttribute clientSessionFilter;
    private final TableFilterAttribute eventTypeFilter;

    private final PageService pageService;
    private final ResourceService resourceService;
    private final RestService restService;
    private final I18nSupport i18nSupport;
    private final DownloadService downloadService;
    private final SEBClientEventDetailsPopup sebClientEventDetailsPopup;
    private final SEBClientEventDeletePopup sebClientEventDeletePopup;
    private final int pageSize;
    private final String exportFileName;

    public SEBClientEvents(
            final PageService pageService,
            final DownloadService downloadService,
            final SEBClientEventDetailsPopup sebClientEventDetailsPopup,
            final SEBClientEventDeletePopup sebClientEventDeletePopup,
            @Value("${sebserver.gui.seb.client.logs.export.filename:SEBClientLogs}") final String exportFileName,
            @Value("${sebserver.gui.list.page.size:20}") final Integer pageSize) {

        this.pageService = pageService;
        this.downloadService = downloadService;
        this.resourceService = pageService.getResourceService();
        this.restService = this.resourceService.getRestService();
        this.i18nSupport = this.resourceService.getI18nSupport();
        this.sebClientEventDetailsPopup = sebClientEventDetailsPopup;
        this.sebClientEventDeletePopup = sebClientEventDeletePopup;
        this.pageSize = pageSize;
        this.exportFileName = exportFileName;

        this.examFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                ExtendedClientEvent.FILTER_ATTRIBUTE_EXAM,
                this.resourceService::getExamLogSelectionResources);

        this.clientSessionFilter = new TableFilterAttribute(
                CriteriaType.TEXT,
                ClientConnection.FILTER_ATTR_SESSION_ID);

        this.eventTypeFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                ClientEvent.FILTER_ATTR_TYPE,
                this.resourceService::clientEventTypeResources);
    }

    @Override
    public void compose(final PageContext pageContext) {
        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();
        // content page layout with title
        final Composite content = widgetFactory.defaultPageLayout(
                pageContext.getParent(),
                TITLE_TEXT_KEY);

        final PageActionBuilder actionBuilder = this.pageService.pageActionBuilder(
                pageContext
                        .clearEntityKeys()
                        .clearAttributes());

        final boolean writeGrant = this.pageService.getCurrentUser()
                .hasInstitutionalPrivilege(PrivilegeType.WRITE, EntityType.CLIENT_EVENT);

        final Consumer<Boolean> deleteActionActivation = this.pageService.getActionActiviationPublisher(
                pageContext,
                ActionDefinition.LOGS_SEB_CLIENT_DELETE_ALL);
        final Consumer<Boolean> detailsActionActivation = this.pageService.getActionActiviationPublisher(
                pageContext,
                ActionDefinition.LOGS_SEB_CLIENT_SHOW_DETAILS);
        final Consumer<Integer> contentChangeListener = contentSize -> {
            deleteActionActivation.accept(contentSize > 0);
            detailsActionActivation.accept(contentSize > 0);
        };

        // table
        final EntityTable<ExtendedClientEvent> table = this.pageService.entityTableBuilder(
                this.restService.getRestCall(GetExtendedClientEventPage.class))
                .withEmptyMessage(EMPTY_TEXT_KEY)
                .withPaging(this.pageSize)
                .withDefaultSort(Domain.CLIENT_CONNECTION.ATTR_EXAM_USER_SESSION_ID)

                .withColumn(new ColumnDefinition<>(
                        Domain.CLIENT_CONNECTION.ATTR_EXAM_ID,
                        EXAM_TEXT_KEY,
                        examNameFunction())
                                .withFilter(this.examFilter)
                                .widthProportion(2))

                .withColumn(new ColumnDefinition<>(
                        Domain.CLIENT_CONNECTION.ATTR_EXAM_USER_SESSION_ID,
                        CLIENT_SESSION_TEXT_KEY,
                        ExtendedClientEvent::getUserSessionId)
                                .withFilter(this.clientSessionFilter)
                                .sortable()
                                .widthProportion(2))

                .withColumn(new ColumnDefinition<ExtendedClientEvent>(
                        Domain.CLIENT_EVENT.ATTR_TYPE,
                        TYPE_TEXT_KEY,
                        this.resourceService::getEventTypeName)
                                .withFilter(this.eventTypeFilter)
                                .sortable()
                                .widthProportion(1))

                .withColumn(new ColumnDefinition<>(
                        Domain.CLIENT_EVENT.ATTR_SERVER_TIME,
                        new LocTextKey(
                                TIME_TEXT_KEY.name,
                                this.i18nSupport.getUsersTimeZoneTitleSuffix()),
                        this::getEventTime)
                                .withFilter(new TableFilterAttribute(
                                        CriteriaType.DATE_TIME_RANGE,
                                        ClientEvent.FILTER_ATTR_SERVER_TIME_FROM_TO,
                                        Utils.toDateTimeUTC(Utils.getMillisecondsNow())
                                                .minusYears(1)
                                                .toString()))
                                .sortable()
                                .widthProportion(2))

                .withColumn(new ColumnDefinition<ExtendedClientEvent>(
                        Domain.CLIENT_EVENT.ATTR_NUMERIC_VALUE,
                        VALUE_TEXT_KEY,
                        clientEvent -> (clientEvent.numValue != null)
                                ? String.valueOf(clientEvent.numValue)
                                : Constants.EMPTY_NOTE)
                                        .widthProportion(1))

                .withDefaultAction(t -> actionBuilder
                        .newAction(ActionDefinition.LOGS_SEB_CLIENT_SHOW_DETAILS)
                        .withExec(action -> this.sebClientEventDetailsPopup.showDetails(action,
                                t.getSingleSelectedROWData()))
                        .noEventPropagation()
                        .create())

                .withSelectionListener(this.pageService.getSelectionPublisher(
                        pageContext,
                        ActionDefinition.LOGS_SEB_CLIENT_SHOW_DETAILS))
                .withContentChangeListener(contentChangeListener)
                .compose(pageContext.copyOf(content));

        actionBuilder
                .newAction(ActionDefinition.LOGS_SEB_CLIENT_SHOW_DETAILS)
                .withSelect(
                        table::getMultiSelection,
                        action -> this.sebClientEventDetailsPopup.showDetails(action, table.getSingleSelectedROWData()),
                        EMPTY_SELECTION_TEXT)
                .noEventPropagation()
                .publish(false)

                .newAction(ActionDefinition.LOGS_SEB_CLIENT_EXPORT_CSV)
                .withExec(action -> this.exportLogs(action, ExportType.CSV, table))
                .noEventPropagation()
                .publishIf(() -> writeGrant, table.hasAnyContent())

                .newAction(ActionDefinition.LOGS_SEB_CLIENT_DELETE_ALL)
                .withExec(action -> this.getOpenDelete(action, table.getFilterCriteria()))
                .noEventPropagation()
                .publishIf(() -> writeGrant, table.hasAnyContent());
    }

    private PageAction exportLogs(
            final PageAction action,
            final ExportType type,
            final EntityTable<ExtendedClientEvent> table) {

        try {

            final UrlLauncher urlLauncher = RWT.getClient().getService(UrlLauncher.class);
            final String fileName = this.exportFileName
                    + Constants.UNDERLINE
                    + this.i18nSupport.formatDisplayDate(Utils.getMillisecondsNow())
                            .replace(" ", "_")
                            .replace(".", "_")
                    + Constants.FILE_EXT_CSV;
            final Map<String, String> queryAttrs = new HashMap<>();

            queryAttrs.put(API.SEB_CLIENT_EVENT_EXPORT_TYPE, type.name());
            final String sortAttr = table.getSortOrder().encode(table.getSortColumn());
            queryAttrs.put(Page.ATTR_SORT, sortAttr);
            table.getFilterCriteria().forEach((name, value) -> queryAttrs.put(name, value.get(0)));

            final String downloadURL = this.downloadService
                    .createDownloadURL(
                            SEBClientLogExport.class,
                            fileName,
                            queryAttrs);

            urlLauncher.openURL(downloadURL);
        } catch (final Exception e) {
            log.error("Failed open export log download: ", e);
        }

        return action;
    }

    private PageAction getOpenDelete(
            final PageAction pageAction,
            final MultiValueMap<String, String> filterCriteria) {

        try {

            final List<String> ids = this.restService
                    .getBuilder(GetClientEventNames.class)
                    .withQueryParams(filterCriteria)
                    .call()
                    .getOrThrow()
                    .stream()
                    .map(EntityName::getModelId)
                    .collect(Collectors.toList());

            final PageAction deleteAction = pageAction
                    .withAttribute(
                            PageContext.AttributeKeys.ENTITY_ID_LIST,
                            StringUtils.join(ids, Constants.COMMA))
                    .withAttribute(
                            PageContext.AttributeKeys.ENTITY_LIST_TYPE,
                            EntityType.CLIENT_EVENT.name());

            return this.sebClientEventDeletePopup.deleteWizardFunction(deleteAction.pageContext())
                    .apply(deleteAction);
        } catch (final Exception e) {
            log.error("Unexpected error while try to open SEB client log delete popup", e);
            return pageAction;
        }
    }

    private Function<ExtendedClientEvent, String> examNameFunction() {
        final Map<Long, String> examNameMapping = this.resourceService.getExamNameMapping();
        return event -> examNameMapping.get(event.examId);
    }

    private String getEventTime(final ExtendedClientEvent event) {
        if (event == null || event.serverTime == null) {
            return Constants.EMPTY_NOTE;
        }

        return this.i18nSupport
                .formatDisplayDateTime(Utils.toDateTimeUTC(event.serverTime));
    }

}