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

import org.eclipse.swt.widgets.Composite;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
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
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.logs.GetExtendedClientEventPage;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition.TableFilterAttribute;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.table.TableFilter.CriteriaType;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class SEBClientLogs implements TemplateComposer {

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
    private final SEBClientLogDetailsPopup sebClientLogDetailsPopup;
    private final int pageSize;

    public SEBClientLogs(
            final PageService pageService,
            final SEBClientLogDetailsPopup sebClientLogDetailsPopup,
            @Value("${sebserver.gui.list.page.size:20}") final Integer pageSize) {

        this.pageService = pageService;
        this.resourceService = pageService.getResourceService();
        this.restService = this.resourceService.getRestService();
        this.i18nSupport = this.resourceService.getI18nSupport();
        this.sebClientLogDetailsPopup = sebClientLogDetailsPopup;
        this.pageSize = pageSize;

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

        // table
        final EntityTable<ExtendedClientEvent> table = this.pageService.entityTableBuilder(
                this.restService.getRestCall(GetExtendedClientEventPage.class))
                .withEmptyMessage(EMPTY_TEXT_KEY)
                .withPaging(this.pageSize)

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
                        .withExec(action -> this.sebClientLogDetailsPopup.showDetails(action,
                                t.getSingleSelectedROWData()))
                        .noEventPropagation()
                        .create())

                .withSelectionListener(this.pageService.getSelectionPublisher(
                        pageContext,
                        ActionDefinition.LOGS_SEB_CLIENT_SHOW_DETAILS))

                .compose(pageContext.copyOf(content));

        actionBuilder
                .newAction(ActionDefinition.LOGS_SEB_CLIENT_SHOW_DETAILS)
                .withSelect(
                        table::getSelection,
                        action -> this.sebClientLogDetailsPopup.showDetails(action, table.getSingleSelectedROWData()),
                        EMPTY_SELECTION_TEXT)
                .noEventPropagation()
                .publishIf(table::hasAnyContent, false);
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
