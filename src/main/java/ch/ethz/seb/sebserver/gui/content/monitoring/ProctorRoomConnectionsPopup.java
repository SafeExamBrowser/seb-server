/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.monitoring;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputDialog;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetCollectingRoomConnections;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

@Lazy
@Component
@GuiProfile
public class ProctorRoomConnectionsPopup {

    private static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.exam.proctoring.room.connections.title");
    private static final LocTextKey JOIN_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.exam.proctoring.room.connections.joinurl");

    private static final LocTextKey EMPTY_LIST_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.search.list.empty");
    private static final LocTextKey TABLE_COLUMN_NAME =
            new LocTextKey("sebserver.monitoring.search.list.name");

    public static final String PAGE_ATTR_JOIN_LINK = "PAGE_ATTR_JOIN_LINK";

    private final PageService pageService;

    protected ProctorRoomConnectionsPopup(final PageService pageService) {
        this.pageService = pageService;
    }

    public void show(final PageContext pageContext, final String roomSubject) {
        final ModalInputDialog<Void> dialog = new ModalInputDialog<>(
                pageContext.getParent().getShell(),
                this.pageService.getWidgetFactory());
        dialog.setLargeDialogWidth();
        dialog.setDialogHeight(380);
        dialog.open(
                new LocTextKey(TITLE_TEXT_KEY.name, roomSubject),
                pageContext,
                c -> this.compose(c, dialog));
    }

    private void compose(final PageContext pageContext, final ModalInputDialog<Void> dialog) {
        final EntityKey entityKey = pageContext.getEntityKey();
        final EntityKey parentEntityKey = pageContext.getParentEntityKey();
        final String joinLink = pageContext.getAttribute(PAGE_ATTR_JOIN_LINK);

        if (StringUtils.isNotBlank(joinLink)) {
            final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();

            final String ariaLabel = widgetFactory.getI18nSupport().getText(JOIN_TEXT_KEY) + joinLink;
            final String testKey = JOIN_TEXT_KEY.name;

            final Composite titleComp = widgetFactory.voidComposite(pageContext.getParent());
            final GridLayout layout = (GridLayout) titleComp.getLayout();
            layout.numColumns = 2;
            layout.makeColumnsEqualWidth = false;

            final Label label = widgetFactory.labelLocalized(titleComp, JOIN_TEXT_KEY);
            label.setLayoutData(new GridData());
            label.setData(RWT.CUSTOM_VARIANT, CustomVariant.TITLE_LABEL.key);

            final Text textInput = widgetFactory.textInput(titleComp, testKey, ariaLabel);
            final GridData gridData = new GridData(SWT.LEFT, SWT.TOP, false, false);
            textInput.setLayoutData(gridData);
            textInput.setText(joinLink);
            textInput.setEditable(false);

            titleComp.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        }

        final PageActionBuilder actionBuilder = this.pageService
                .pageActionBuilder(pageContext.clearEntityKeys());

        final List<ClientConnection> connections = new ArrayList<>(this.pageService.getRestService()
                .getBuilder(GetCollectingRoomConnections.class)
                .withURIVariable(API.PARAM_MODEL_ID, parentEntityKey.modelId)
                .withQueryParam(Domain.REMOTE_PROCTORING_ROOM.ATTR_ID, entityKey.modelId)
                .call()
                .getOrThrow());

        this.pageService.staticListTableBuilder(connections, EntityType.CLIENT_CONNECTION)

                .withEmptyMessage(EMPTY_LIST_TEXT_KEY)
                .withPaging(10)

                .withColumn(new ColumnDefinition<>(
                        Domain.CLIENT_CONNECTION.ATTR_EXAM_USER_SESSION_ID,
                        TABLE_COLUMN_NAME,
                        ClientConnection::getUserSessionId))

                .withDefaultAction(t -> actionBuilder
                        .newAction(ActionDefinition.MONITOR_EXAM_CLIENT_CONNECTION)
                        .withParentEntityKey(parentEntityKey)
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
