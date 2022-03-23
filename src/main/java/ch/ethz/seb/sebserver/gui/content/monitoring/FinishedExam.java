/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.monitoring;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
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
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetClientConnectionPage;
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

        final TableBuilder<ClientConnection> tableBuilder =
                this.pageService.entityTableBuilder(restService.getRestCall(GetClientConnectionPage.class))
                        .withEmptyMessage(EMPTY_LIST_TEXT_KEY)
                        .withPaging(10)
                        .withStaticFilter(ClientConnection.FILTER_ATTR_EXAM_ID, examKey.modelId)

                        .withColumn(new ColumnDefinition<>(
                                Domain.CLIENT_CONNECTION.ATTR_EXAM_USER_SESSION_ID,
                                TABLE_COLUMN_NAME,
                                ClientConnection::getUserSessionId)
                                        .withFilter(this.nameFilter))

                        .withColumn(new ColumnDefinition<>(
                                ClientConnection.ATTR_INFO,
                                TABLE_COLUMN_INFO,
                                ClientConnection::getInfo)
                                        .withFilter(this.infoFilter))

                        .withColumn(new ColumnDefinition<ClientConnection>(
                                Domain.CLIENT_CONNECTION.ATTR_STATUS,
                                TABLE_COLUMN_STATUS,
                                row -> this.pageService.getResourceService()
                                        .localizedClientConnectionStatusName(row.getStatus()))
                                                .withFilter(this.statusFilter))

                        .withDefaultAction(t -> actionBuilder
                                .newAction(ActionDefinition.MONITOR_EXAM_CLIENT_CONNECTION)
                                .withParentEntityKey(examKey)
                                .create());

        tableBuilder.compose(pageContext);
    }

}
