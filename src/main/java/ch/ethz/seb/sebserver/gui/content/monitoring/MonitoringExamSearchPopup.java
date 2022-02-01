/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.monitoring;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputDialog;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetClientConnectionPage;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition.TableFilterAttribute;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.table.TableFilter.CriteriaType;

@Lazy
@Component
@GuiProfile
public class MonitoringExamSearchPopup {

    private static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.search.title");
    private static final LocTextKey EMPTY_LIST_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.search.list.empty");
    private static final LocTextKey TABLE_COLUMN_NAME =
            new LocTextKey("sebserver.monitoring.search.list.name");
    private static final LocTextKey TABLE_COLUMN_INFO =
            new LocTextKey("sebserver.monitoring.search.list.info");
    private static final LocTextKey TABLE_COLUMN_STATUS =
            new LocTextKey("sebserver.monitoring.search.list.status");

    private final PageService pageService;

    private final TableFilterAttribute nameFilter =
            new TableFilterAttribute(CriteriaType.TEXT, ClientConnection.FILTER_ATTR_SESSION_ID);
    private final TableFilterAttribute infoFilter =
            new TableFilterAttribute(CriteriaType.TEXT, ClientConnection.ATTR_INFO);
    private final TableFilterAttribute statusFilter;

    protected MonitoringExamSearchPopup(final PageService pageService) {
        this.pageService = pageService;

        this.statusFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                ClientConnection.FILTER_ATTR_STATUS,
                ConnectionStatus.ACTIVE.name(),
                pageService.getResourceService()::localizedClientConnectionStatusResources);
    }

    public void show(final PageContext pageContext) {
        final ModalInputDialog<Void> dialog = new ModalInputDialog<>(
                pageContext.getParent().getShell(),
                this.pageService.getWidgetFactory());
        dialog.setLargeDialogWidth();
        dialog.setDialogHeight(380);
        dialog.open(
                TITLE_TEXT_KEY,
                pageContext,
                pc -> this.compose(pc, dialog));
    }

    private void compose(final PageContext pageContext, final ModalInputDialog<Void> dialog) {
        final EntityKey examKey = pageContext.getEntityKey();

        final RestService restService = this.pageService.getRestService();
        final PageActionBuilder actionBuilder = this.pageService
                .pageActionBuilder(pageContext.clearEntityKeys());

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
                        .withExec(action -> showClientConnection(action, dialog, t))
                        .create())

                .compose(pageContext);
    }

    private PageAction showClientConnection(
            final PageAction pageAction,
            final ModalInputDialog<Void> dialog,
            final EntityTable<ClientConnection> table) {

        final ClientConnection singleSelectedROWData = table.getSingleSelectedROWData();
        dialog.close();
        return pageAction
                .withEntityKey(new EntityKey(
                        singleSelectedROWData.id,
                        EntityType.CLIENT_CONNECTION))
                .withAttribute(
                        Domain.CLIENT_CONNECTION.ATTR_CONNECTION_TOKEN,
                        singleSelectedROWData.getConnectionToken());
    }

}
