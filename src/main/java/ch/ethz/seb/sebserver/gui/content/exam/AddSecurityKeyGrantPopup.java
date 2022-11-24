/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.exam;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.widgets.Composite;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.institution.AppSignatureKeyInfo;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.ModalInputDialogComposer;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputDialog;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetClientConnections;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GrantClientConnectionSecurityKey;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class AddSecurityKeyGrantPopup {

    private static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.exam.signaturekey.seb.add.title");
    private static final LocTextKey TITLE_TEXT_INFO =
            new LocTextKey("sebserver.exam.signaturekey.seb.add.info");

    private static final LocTextKey TITLE_TEXT_FORM_SIGNATURE =
            new LocTextKey("sebserver.exam.signaturekey.seb.add.signature");
    private static final LocTextKey TITLE_TEXT_FORM_TAG =
            new LocTextKey("sebserver.exam.signaturekey.seb.add.tag");

    private static final LocTextKey TABLE_COLUMN_NAME =
            new LocTextKey("sebserver.exam.signaturekey.list.name");
    private static final LocTextKey TABLE_COLUMN_INFO =
            new LocTextKey("sebserver.exam.signaturekey.list.info");
    private static final LocTextKey TABLE_COLUMN_STATUS =
            new LocTextKey("sebserver.exam.signaturekey.list.status");

    private final PageService pageService;

    protected AddSecurityKeyGrantPopup(final PageService pageService) {
        this.pageService = pageService;
    }

    public PageAction showGrantPopup(final PageAction action, final AppSignatureKeyInfo appSignatureKeyInfo) {
        final PageContext pageContext = action.pageContext();
        final PopupComposer popupComposer = new PopupComposer(this.pageService, pageContext, appSignatureKeyInfo);
        try {
            final ModalInputDialog<FormHandle<?>> dialog =
                    new ModalInputDialog<>(
                            action.pageContext().getParent().getShell(),
                            this.pageService.getWidgetFactory());
            dialog.setDialogWidth(800);

            final Predicate<FormHandle<?>> applyGrant = formHandle -> applyGrant(
                    pageContext,
                    formHandle,
                    appSignatureKeyInfo);

            dialog.open(
                    TITLE_TEXT_KEY,
                    applyGrant,
                    Utils.EMPTY_EXECUTION,
                    popupComposer);

        } catch (final Exception e) {
            action.pageContext().notifyUnexpectedError(e);
        }
        return action;
    }

    private final class PopupComposer implements ModalInputDialogComposer<FormHandle<?>> {

        private final PageService pageService;
        private final PageContext pageContext;
        private final AppSignatureKeyInfo appSignatureKeyInfo;

        protected PopupComposer(
                final PageService pageService,
                final PageContext pageContext,
                final AppSignatureKeyInfo appSignatureKeyInfo) {

            this.pageService = pageService;
            this.pageContext = pageContext;
            this.appSignatureKeyInfo = appSignatureKeyInfo;
        }

        @Override
        public Supplier<FormHandle<?>> compose(final Composite parent) {
            final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();
            widgetFactory.addFormSubContextHeader(parent, TITLE_TEXT_INFO, null);

            final PageContext formContext = this.pageContext.copyOf(parent);
            final FormHandle<?> form = this.pageService.formBuilder(formContext)

                    .addField(FormBuilder.text(
                            Domain.SEB_SECURITY_KEY_REGISTRY.ATTR_KEY_VALUE,
                            TITLE_TEXT_FORM_SIGNATURE,
                            String.valueOf(this.appSignatureKeyInfo.key))
                            .readonly(true))

                    .addField(FormBuilder.text(
                            Domain.SEB_SECURITY_KEY_REGISTRY.ATTR_TAG,
                            TITLE_TEXT_FORM_TAG)
                            .mandatory())

                    .build();

            final String clientConnectionIds = StringUtils.join(
                    this.appSignatureKeyInfo.connectionIds
                            .keySet()
                            .stream()
                            .map(String::valueOf)
                            .collect(Collectors.toList()),
                    Constants.LIST_SEPARATOR_CHAR);

            this.pageService.getRestService().getBuilder(GetClientConnections.class)
                    .withQueryParam(API.PARAM_MODEL_ID_LIST, clientConnectionIds)
                    .call()
                    .onSuccess(connections -> {
                        final List<ClientConnection> list = new ArrayList<>();
                        this.pageService.staticListTableBuilder(list, EntityType.CLIENT_CONNECTION)
                                .withPaging(10)

                                .withColumn(new ColumnDefinition<>(
                                        Domain.CLIENT_CONNECTION.ATTR_EXAM_USER_SESSION_ID,
                                        TABLE_COLUMN_NAME,
                                        ClientConnection::getUserSessionId)
                                                .widthProportion(2))

                                .withColumn(new ColumnDefinition<>(
                                        ClientConnection.ATTR_INFO,
                                        TABLE_COLUMN_INFO,
                                        ClientConnection::getInfo)
                                                .widthProportion(3))

                                .withColumn(new ColumnDefinition<ClientConnection>(
                                        Domain.CLIENT_CONNECTION.ATTR_STATUS,
                                        TABLE_COLUMN_STATUS,
                                        row -> this.pageService.getResourceService()
                                                .localizedClientConnectionStatusName(row.getStatus()))
                                                        .widthProportion(1));

                    });

            return () -> form;
        }
    }

    private boolean applyGrant(
            final PageContext pageContext,
            final FormHandle<?> formHandle,
            final AppSignatureKeyInfo appSignatureKeyInfo) {

        if (appSignatureKeyInfo.connectionIds.isEmpty()) {
            return true;
        }

        final Long connectioId = appSignatureKeyInfo.connectionIds.keySet().iterator().next();

        return this.pageService
                .getRestService()
                .getBuilder(GrantClientConnectionSecurityKey.class)
                .withURIVariable(API.PARAM_PARENT_MODEL_ID, String.valueOf(appSignatureKeyInfo.examId))
                .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(connectioId))
                .withFormBinding(formHandle.getFormBinding())
                .call()
                .onError(formHandle::handleError)
                .hasValue();
    }

}
