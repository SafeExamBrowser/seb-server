/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputDialog;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetCollectingRoomConnections;

@Lazy
@Component
@GuiProfile
public class ProctorRoomConnectionsPopup {

    private static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.exam.proctoring.room.connections.title");

    private final PageService pageService;

    protected ProctorRoomConnectionsPopup(final PageService pageService) {
        this.pageService = pageService;
    }

    public void show(final PageContext pageContext, final String roomSubject) {
        final ModalInputDialog<Void> dialog = new ModalInputDialog<>(
                pageContext.getParent().getShell(),
                this.pageService.getWidgetFactory());
        dialog.setLargeDialogWidth();
        dialog.open(
                new LocTextKey(TITLE_TEXT_KEY.name, roomSubject),
                pageContext,
                this::compose);
    }

    private void compose(final PageContext pageContext) {
        final Composite parent = pageContext.getParent();
        final Composite grid = this.pageService.getWidgetFactory().createPopupScrollComposite(parent);
        final EntityKey entityKey = pageContext.getEntityKey();
        final EntityKey parentEntityKey = pageContext.getParentEntityKey();

        this.pageService.getRestService()
                .getBuilder(GetCollectingRoomConnections.class)
                .withURIVariable(API.PARAM_MODEL_ID, parentEntityKey.modelId)
                .withQueryParam(Domain.REMOTE_PROCTORING_ROOM.ATTR_ID, entityKey.modelId)
                .call()
                .getOrThrow()
                .stream()
                .forEach(connection -> {
                    final Label label = new Label(grid, SWT.NONE);
                    label.setText(connection.userSessionId);
                });
    }

}
