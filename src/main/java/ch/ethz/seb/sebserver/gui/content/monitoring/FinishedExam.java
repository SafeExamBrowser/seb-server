/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.monitoring;

import java.util.Collection;
import java.util.function.Function;

import org.eclipse.swt.widgets.Composite;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.IndicatorValue;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.push.ServerPushService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetIndicators;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetFinishedExamClientConnectionPage;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition.TableFilterAttribute;
import ch.ethz.seb.sebserver.gui.table.TableBuilder;
import ch.ethz.seb.sebserver.gui.table.TableFilter.CriteriaType;

@Lazy
@Component
@GuiProfile
public class FinishedExam implements TemplateComposer {

    private static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.finished.exam.connections.title");
    private static final LocTextKey EMPTY_LIST_TEXT_KEY =
            new LocTextKey("sebserver.finished.exam.connections.empty");
    private static final LocTextKey TABLE_COLUMN_NAME =
            new LocTextKey("sebserver.finished.exam.connections.name");
    private static final LocTextKey TABLE_COLUMN_INFO =
            new LocTextKey("sebserver.finished.exam.connections.info");
    private static final LocTextKey TABLE_COLUMN_STATUS =
            new LocTextKey("sebserver.finished.exam.connections.status");

    private final TableFilterAttribute nameFilter =
            new TableFilterAttribute(CriteriaType.TEXT, ClientConnection.FILTER_ATTR_SESSION_ID);
    private final TableFilterAttribute infoFilter =
            new TableFilterAttribute(CriteriaType.TEXT, ClientConnection.ATTR_INFO);
    private final TableFilterAttribute statusFilter;

    private final PageService pageService;
    private final RestService restService;
    private final ResourceService resourceService;
    private final int pageSize;

    public FinishedExam(
            final ServerPushService serverPushService,
            final PageService pageService,
            @Value("${sebserver.gui.list.page.size:20}") final Integer pageSize) {

        this.pageService = pageService;
        this.restService = pageService.getRestService();
        this.resourceService = pageService.getResourceService();
        this.pageSize = pageSize;

        this.statusFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                ClientConnection.FILTER_ATTR_STATUS,
                pageService.getResourceService()::localizedClientConnectionStatusResources);
    }

    @Override
    public void compose(final PageContext pageContext) {
        final EntityKey examKey = pageContext.getEntityKey();

        final RestService restService = this.pageService.getRestService();
        final PageActionBuilder actionBuilder = this.pageService
                .pageActionBuilder(pageContext.clearEntityKeys());

        final Collection<Indicator> indicators = restService.getBuilder(GetIndicators.class)
                .withQueryParam(Indicator.FILTER_ATTR_EXAM_ID, examKey.modelId)
                .call()
                .getOrThrow();

        final Composite content = this.pageService.getWidgetFactory().defaultPageLayout(
                pageContext.getParent(),
                TITLE_TEXT_KEY);

        final TableBuilder<ClientConnectionData> tableBuilder =
                this.pageService.entityTableBuilder(restService.getRestCall(GetFinishedExamClientConnectionPage.class))
                        .withEmptyMessage(EMPTY_LIST_TEXT_KEY)
                        .withPaging(this.pageSize)
                        .withStaticFilter(ClientConnection.FILTER_ATTR_EXAM_ID, examKey.modelId)

                        .withColumn(new ColumnDefinition<ClientConnectionData>(
                                Domain.CLIENT_CONNECTION.ATTR_EXAM_USER_SESSION_ID,
                                TABLE_COLUMN_NAME,
                                c -> c.clientConnection.getUserSessionId())
                                        .withFilter(this.nameFilter))

                        .withColumn(new ColumnDefinition<ClientConnectionData>(
                                ClientConnection.ATTR_INFO,
                                TABLE_COLUMN_INFO,
                                c -> c.clientConnection.getInfo())
                                        .withFilter(this.infoFilter))

                        .withColumn(new ColumnDefinition<ClientConnectionData>(
                                Domain.CLIENT_CONNECTION.ATTR_STATUS,
                                TABLE_COLUMN_STATUS,
                                row -> this.pageService.getResourceService()
                                        .localizedClientConnectionStatusName(row.clientConnection.getStatus()))
                                                .withFilter(this.statusFilter))

                        .withDefaultAction(t -> actionBuilder
                                .newAction(ActionDefinition.MONITOR_EXAM_CLIENT_CONNECTION)
                                .withParentEntityKey(examKey)
                                .create());

        indicators.stream().forEach(indicator -> {
            tableBuilder.withColumn(new ColumnDefinition<>(
                    indicator.name,
                    new LocTextKey(indicator.name),
                    indicatorValueFunction(indicator)));
        });

        tableBuilder.compose(pageContext.copyOf(content));
    }

    private Function<ClientConnectionData, String> indicatorValueFunction(final Indicator indicator) {
        return clientConnectionData -> {
            return clientConnectionData.indicatorValues
                    .stream()
                    .filter(indicatorValue -> indicatorValue.getIndicatorId().equals(indicator.id))
                    .findFirst()
                    .map(iv -> IndicatorValue.getDisplayValue(iv, indicator.type))
                    .orElse(Constants.EMPTY_NOTE);
        };
    }

}
