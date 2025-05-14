/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.monitoring;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.tomcat.util.buf.StringUtils;
import org.eclipse.swt.widgets.Composite;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction;
import ch.ethz.seb.sebserver.gbl.model.session.ClientMonitoringDataView;
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
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetClientConnectionPage;
import ch.ethz.seb.sebserver.gui.service.session.InstructionProcessor;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

@Lazy
@Component
@GuiProfile
public class SEBSendLockPopup {

    private static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.lock.title");
    private static final LocTextKey FORM_INFO_TITLE =
            new LocTextKey("sebserver.monitoring.lock.form.info.title");
    private static final LocTextKey FORM_INFO =
            new LocTextKey("sebserver.monitoring.lock.form.info");
    private static final LocTextKey FORM_MESSAGE =
            new LocTextKey("sebserver.monitoring.lock.form.message");

    private static final LocTextKey TABLE_TITLE =
            new LocTextKey("sebserver.monitoring.lock.list.title");
    private static final LocTextKey TABLE_COLUMN_NAME =
            new LocTextKey("sebserver.monitoring.lock.list.name");
    private static final LocTextKey TABLE_COLUMN_INFO =
            new LocTextKey("sebserver.monitoring.lock.list.info");

    private final PageService pageService;
    private final InstructionProcessor instructionProcessor;

    protected SEBSendLockPopup(
            final PageService pageService,
            final InstructionProcessor instructionProcessor) {

        this.pageService = pageService;
        this.instructionProcessor = instructionProcessor;
    }

    public PageAction show(
            final PageAction action,
            final Function<Predicate<ClientMonitoringDataView>, Set<String>> selectionFunction) {

        try {

            final PageContext pageContext = action.pageContext();
            final Set<String> selection = selectionFunction.apply(ClientMonitoringDataView.getStatusPredicate(
                    ConnectionStatus.CONNECTION_REQUESTED,
                    ConnectionStatus.READY,
                    ConnectionStatus.ACTIVE));

            if (selection == null || selection.isEmpty()) {
                action
                        .pageContext()
                        .publishInfo(new LocTextKey("sebserver.monitoring.lock.noselection"));
                return action;
            }

            final String connectionTokens = StringUtils.join(selection, Constants.LIST_SEPARATOR_CHAR);
            final boolean showList = true; //selection.size() > 1  SEBSERV-151;
            final PopupComposer popupComposer = new PopupComposer(
                    this.pageService,
                    pageContext,
                    connectionTokens,
                    showList);

            final ModalInputDialog<FormHandle<?>> dialog =
                    new ModalInputDialog<FormHandle<?>>(
                            action.pageContext().getParent().getShell(),
                            this.pageService.getWidgetFactory())
                                    .setDialogWidth(800)
                                    .setDialogHeight(showList ? 550 : 200);

            final Predicate<FormHandle<?>> doLock = formHandle -> propagateLockInstruction(
                    connectionTokens,
                    pageContext,
                    formHandle);

            dialog.open(
                    TITLE_TEXT_KEY,
                    doLock,
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
        private final String connectionTokens;
        private final boolean showList;

        protected PopupComposer(
                final PageService pageService,
                final PageContext pageContext,
                final String connectionTokens,
                final boolean showList) {

            this.pageService = pageService;
            this.pageContext = pageContext;
            this.connectionTokens = connectionTokens;
            this.showList = showList;
        }

        @Override
        public Supplier<FormHandle<?>> compose(final Composite parent) {
            final EntityKey examKey = this.pageContext.getEntityKey();
            final RestService restService = this.pageService.getRestService();

            final PageContext formContext = this.pageContext.copyOf(parent);
            final FormHandle<?> form = this.pageService.formBuilder(formContext)
                    .addField(FormBuilder.text(
                            "Info",
                            FORM_INFO_TITLE,
                            this.pageService.getI18nSupport().getText(FORM_INFO))
                            .asArea(50)
                            .asHTML()
                            .readonly(true))
                    .addField(FormBuilder.text(
                            ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_FORCE_LOCK_SCREEN.MESSAGE,
                            FORM_MESSAGE)
                            .asArea(50)
                            .asHTML())
                    .build();

            if (this.showList) {
                this.pageService
                        .getWidgetFactory()
                        .labelLocalized(parent, CustomVariant.TEXT_H3, TABLE_TITLE);

                // table of selected SEB connections
                this.pageService
                        .entityTableBuilder(restService.getRestCall(GetClientConnectionPage.class))
                        .withStaticFilter(
                                ClientConnection.FILTER_ATTR_TOKEN_LIST,
                                this.connectionTokens)
                        .withStaticFilter(
                                ClientConnection.FILTER_ATTR_EXAM_ID,
                                examKey.modelId)
                        .withPaging(10)

                        .withColumn(new ColumnDefinition<>(
                                Domain.CLIENT_CONNECTION.ATTR_EXAM_USER_SESSION_ID,
                                TABLE_COLUMN_NAME,
                                ClientConnection::getUserSessionId)
                                        .sortable())

                        .withColumn(new ColumnDefinition<>(
                                ClientConnection.ATTR_INFO,
                                TABLE_COLUMN_INFO,
                                ClientConnection::getInfo)
                                        .sortable())

                        .compose(formContext);
            }

            return () -> form;
        }
    }

    private boolean propagateLockInstruction(
            final String connectionTokens,
            final PageContext pageContext,
            final FormHandle<?> formHandle) {

        try {

            final EntityKey examKey = pageContext.getEntityKey();
            final String lockMessage = formHandle
                    .getForm()
                    .getFieldValue(ClientInstruction.SEB_INSTRUCTION_ATTRIBUTES.SEB_FORCE_LOCK_SCREEN.MESSAGE);

            this.instructionProcessor.propagateSEBLockInstruction(
                    examKey.modelId,
                    lockMessage,
                    null,
                    connectionTokens,
                    pageContext);

        } catch (final Exception e) {
            pageContext.notifyUnexpectedError(e);
        }

        return true;
    }

}
