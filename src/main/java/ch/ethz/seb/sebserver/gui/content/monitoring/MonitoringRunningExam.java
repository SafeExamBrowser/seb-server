/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.monitoring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.async.AsyncRunner;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringFeature;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.GuiServiceInfo;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.content.action.ActionPane;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionActivationEvent;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.push.ServerPushService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExam;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetIndicators;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetProctoringSettings;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.session.ClientConnectionTable;
import ch.ethz.seb.sebserver.gui.service.session.FullPageMonitoringGUIUpdate;
import ch.ethz.seb.sebserver.gui.service.session.FullPageMonitoringUpdate;
import ch.ethz.seb.sebserver.gui.service.session.InstructionProcessor;
import ch.ethz.seb.sebserver.gui.service.session.MonitoringStatus;
import ch.ethz.seb.sebserver.gui.service.session.proctoring.MonitoringProctoringService;
import ch.ethz.seb.sebserver.gui.service.session.proctoring.ProctoringGUIService;

@Lazy
@Component
@GuiProfile
public class MonitoringRunningExam implements TemplateComposer {

    //private static final Logger log = LoggerFactory.getLogger(MonitoringRunningExam.class);

    private static final LocTextKey EMPTY_SELECTION_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.exam.connection.emptySelection");
    private static final LocTextKey EMPTY_ACTIVE_SELECTION_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.exam.connection.emptySelection.active");
    private static final LocTextKey CONFIRM_QUIT_SELECTED =
            new LocTextKey("sebserver.monitoring.exam.connection.action.instruction.quit.selected.confirm");
    private static final LocTextKey CONFIRM_QUIT_ALL =
            new LocTextKey("sebserver.monitoring.exam.connection.action.instruction.quit.all.confirm");
    private static final LocTextKey CONFIRM_OPEN_TOWNHALL =
            new LocTextKey("sebserver.monitoring.exam.connection.action.openTownhall.confirm");
    private static final LocTextKey CONFIRM_CLOSE_TOWNHALL =
            new LocTextKey("sebserver.monitoring.exam.connection.action.closeTownhall.confirm");
    private static final LocTextKey CONFIRM_DISABLE_SELECTED =
            new LocTextKey("sebserver.monitoring.exam.connection.action.instruction.disable.selected.confirm");

    private final ServerPushService serverPushService;
    private final PageService pageService;
    private final RestService restService;
    private final ResourceService resourceService;
    private final AsyncRunner asyncRunner;
    private final InstructionProcessor instructionProcessor;
    private final MonitoringExamSearchPopup monitoringExamSearchPopup;
    private final MonitoringProctoringService monitoringProctoringService;
    private final boolean distributedSetup;
    private final long pollInterval;

    protected MonitoringRunningExam(
            final ServerPushService serverPushService,
            final PageService pageService,
            final AsyncRunner asyncRunner,
            final InstructionProcessor instructionProcessor,
            final MonitoringExamSearchPopup monitoringExamSearchPopup,
            final MonitoringProctoringService monitoringProctoringService,
            final GuiServiceInfo guiServiceInfo,
            @Value("${sebserver.gui.webservice.poll-interval:2000}") final long pollInterval) {

        this.serverPushService = serverPushService;
        this.pageService = pageService;
        this.restService = pageService.getRestService();
        this.resourceService = pageService.getResourceService();
        this.asyncRunner = asyncRunner;
        this.instructionProcessor = instructionProcessor;
        this.monitoringProctoringService = monitoringProctoringService;
        this.pollInterval = pollInterval;
        this.distributedSetup = guiServiceInfo.isDistributedSetup();
        this.monitoringExamSearchPopup = monitoringExamSearchPopup;
    }

    @Override
    public void compose(final PageContext pageContext) {
        final RestService restService = this.resourceService.getRestService();
        final EntityKey entityKey = pageContext.getEntityKey();
        final CurrentUser currentUser = this.resourceService.getCurrentUser();
        final Exam exam = restService.getBuilder(GetExam.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .call()
                .getOrThrow();
        final UserInfo user = currentUser.get();
        final boolean supporting = user.hasRole(UserRole.EXAM_SUPPORTER) &&
                exam.supporter.contains(user.uuid);
        final BooleanSupplier isExamSupporter = () -> supporting || user.hasRole(UserRole.EXAM_ADMIN);

        final Collection<Indicator> indicators = restService.getBuilder(GetIndicators.class)
                .withQueryParam(Indicator.FILTER_ATTR_EXAM_ID, entityKey.modelId)
                .call()
                .getOrThrow();

        final Composite content = this.pageService.getWidgetFactory().defaultPageLayout(
                pageContext.getParent(),
                new LocTextKey("sebserver.monitoring.exam", exam.name));

        final Composite tablePane = new Composite(content, SWT.NONE);
        tablePane.setLayout(new GridLayout());
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.heightHint = 100;
        tablePane.setLayoutData(gridData);

        final PageActionBuilder actionBuilder = this.pageService
                .pageActionBuilder(pageContext.clearEntityKeys());

        final Collection<FullPageMonitoringGUIUpdate> guiUpdates = new ArrayList<>();
        final FullPageMonitoringUpdate fullPageMonitoringUpdate = new FullPageMonitoringUpdate(
                exam.id,
                this.pageService,
                this.serverPushService,
                this.asyncRunner,
                guiUpdates);

        final ClientConnectionTable clientTable = new ClientConnectionTable(
                this.pageService,
                tablePane,
                exam,
                indicators,
                this.distributedSetup);
        guiUpdates.add(clientTable);

        clientTable
                .withDefaultAction(
                        actionBuilder
                                .newAction(ActionDefinition.MONITOR_EXAM_CLIENT_CONNECTION)
                                .withParentEntityKey(entityKey)
                                .create(),
                        this.pageService)
                .withSelectionListener(this.pageService.getSelectionPublisher(
                        pageContext,
                        ActionDefinition.MONITOR_EXAM_CLIENT_CONNECTION,
                        ActionDefinition.MONITOR_EXAM_QUIT_SELECTED,
                        ActionDefinition.MONITOR_EXAM_DISABLE_SELECTED_CONNECTION,
                        ActionDefinition.MONITOR_EXAM_NEW_PROCTOR_ROOM));

        actionBuilder

                .newAction(ActionDefinition.MONITOR_EXAM_CLIENT_CONNECTION)
                .withParentEntityKey(entityKey)
                .withExec(pageAction -> {
                    final Tuple<String> singleSelection = clientTable.getSingleSelection();
                    if (singleSelection == null) {
                        throw new PageMessageException(EMPTY_SELECTION_TEXT_KEY);
                    }

                    final PageAction copyOfPageAction = PageAction.copyOf(pageAction);
                    copyOfPageAction.withEntityKey(new EntityKey(
                            singleSelection._1,
                            EntityType.CLIENT_CONNECTION));
                    copyOfPageAction.withAttribute(
                            Domain.CLIENT_CONNECTION.ATTR_CONNECTION_TOKEN,
                            singleSelection._2);

                    return copyOfPageAction;
                })
                .publishIf(isExamSupporter, false)

                .newAction(ActionDefinition.MONITOR_EXAM_QUIT_ALL)
                .withEntityKey(entityKey)
                .withConfirm(() -> CONFIRM_QUIT_ALL)
                .withExec(action -> this.quitSEBClients(action, clientTable, true))
                .noEventPropagation()
                .publishIf(isExamSupporter)

                .newAction(ActionDefinition.MONITORING_EXAM_SEARCH_CONNECTIONS)
                .withEntityKey(entityKey)
                .withExec(this::openSearchPopup)
                .noEventPropagation()
                .publishIf(isExamSupporter)

                .newAction(ActionDefinition.MONITOR_EXAM_QUIT_SELECTED)
                .withEntityKey(entityKey)
                .withConfirm(() -> CONFIRM_QUIT_SELECTED)
                .withSelect(
                        () -> this.selectionForQuitInstruction(clientTable),
                        action -> this.quitSEBClients(action, clientTable, false),
                        EMPTY_ACTIVE_SELECTION_TEXT_KEY)
                .noEventPropagation()
                .publishIf(isExamSupporter, false)

                .newAction(ActionDefinition.MONITOR_EXAM_DISABLE_SELECTED_CONNECTION)
                .withEntityKey(entityKey)
                .withConfirm(() -> CONFIRM_DISABLE_SELECTED)
                .withSelect(
                        clientTable::getSelection,
                        action -> this.disableSEBClients(action, clientTable, false),
                        EMPTY_SELECTION_TEXT_KEY)
                .noEventPropagation()
                .publishIf(isExamSupporter, false);

        if (isExamSupporter.getAsBoolean()) {
            guiUpdates.add(createFilterActions(
                    fullPageMonitoringUpdate,
                    actionBuilder,
                    clientTable,
                    isExamSupporter));

            final ProctoringServiceSettings proctoringSettings = this.restService
                    .getBuilder(GetProctoringSettings.class)
                    .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                    .call()
                    .getOr(null);

            if (proctoringSettings != null && proctoringSettings.enableProctoring) {
                guiUpdates.add(createProctoringActions(
                        proctoringSettings,
                        currentUser.getProctoringGUIService(),
                        pageContext,
                        content,
                        actionBuilder));
            }
        }

        // finally start the page update (server push)
        fullPageMonitoringUpdate.start(pageContext, content, this.pollInterval);
    }

    private FullPageMonitoringGUIUpdate createProctoringActions(
            final ProctoringServiceSettings proctoringSettings,
            final ProctoringGUIService proctoringGUIService,
            final PageContext pageContext,
            final Composite parent,
            final PageActionBuilder actionBuilder) {

        if (proctoringSettings.enabledFeatures.contains(ProctoringFeature.TOWN_HALL)) {
            final EntityKey entityKey = pageContext.getEntityKey();
            actionBuilder.newAction(ActionDefinition.MONITOR_EXAM_OPEN_TOWNHALL_PROCTOR_ROOM)
                    .withEntityKey(entityKey)
                    .withConfirm(action -> {
                        if (!this.monitoringProctoringService.isTownhallRoomActive(action.getEntityKey().modelId)) {
                            return CONFIRM_OPEN_TOWNHALL;
                        } else {
                            return CONFIRM_CLOSE_TOWNHALL;
                        }
                    })
                    .withExec(action -> this.monitoringProctoringService.toggleTownhallRoom(
                            proctoringGUIService,
                            proctoringSettings,
                            action))
                    .noEventPropagation()
                    .publish();

            if (this.monitoringProctoringService.isTownhallRoomActive(entityKey.modelId)) {
                this.pageService.firePageEvent(
                        new ActionActivationEvent(
                                true,
                                new Tuple<>(
                                        ActionDefinition.MONITOR_EXAM_OPEN_TOWNHALL_PROCTOR_ROOM,
                                        ActionDefinition.MONITOR_EXAM_CLOSE_TOWNHALL_PROCTOR_ROOM)),
                        pageContext);
            }
        }

        this.monitoringProctoringService.initCollectingRoomActions(
                pageContext,
                actionBuilder,
                proctoringSettings,
                proctoringGUIService);

        return monitoringStatus -> this.monitoringProctoringService.updateCollectingRoomActions(
                monitoringStatus.proctoringData(),
                pageContext,
                actionBuilder,
                proctoringSettings,
                proctoringGUIService);
    }

    private FullPageMonitoringGUIUpdate createFilterActions(
            final MonitoringStatus monitoringStatus,
            final PageActionBuilder actionBuilder,
            final ClientConnectionTable clientTable,
            final BooleanSupplier isExamSupporter) {

        final StatusFilterGUIUpdate statusFilterGUIUpdate =
                new StatusFilterGUIUpdate(this.pageService.getPolyglotPageService());

        addFilterAction(
                monitoringStatus,
                statusFilterGUIUpdate,
                actionBuilder,
                clientTable,
                ConnectionStatus.CONNECTION_REQUESTED,
                ActionDefinition.MONITOR_EXAM_SHOW_REQUESTED_CONNECTION,
                ActionDefinition.MONITOR_EXAM_HIDE_REQUESTED_CONNECTION);
        addFilterAction(
                monitoringStatus,
                statusFilterGUIUpdate,
                actionBuilder,
                clientTable,
                ConnectionStatus.ACTIVE,
                ActionDefinition.MONITOR_EXAM_SHOW_ACTIVE_CONNECTION,
                ActionDefinition.MONITOR_EXAM_HIDE_ACTIVE_CONNECTION);
        addFilterAction(
                monitoringStatus,
                statusFilterGUIUpdate,
                actionBuilder,
                clientTable,
                ConnectionStatus.CLOSED,
                ActionDefinition.MONITOR_EXAM_SHOW_CLOSED_CONNECTION,
                ActionDefinition.MONITOR_EXAM_HIDE_CLOSED_CONNECTION);
        addFilterAction(
                monitoringStatus,
                statusFilterGUIUpdate,
                actionBuilder,
                clientTable,
                ConnectionStatus.DISABLED,
                ActionDefinition.MONITOR_EXAM_SHOW_DISABLED_CONNECTION,
                ActionDefinition.MONITOR_EXAM_HIDE_DISABLED_CONNECTION);

        return statusFilterGUIUpdate;
    }

    private void addFilterAction(
            final MonitoringStatus monitoringStatus,
            final StatusFilterGUIUpdate statusFilterGUIUpdate,
            final PageActionBuilder actionBuilder,
            final ClientConnectionTable clientTable,
            final ConnectionStatus status,
            final ActionDefinition showActionDef,
            final ActionDefinition hideActionDef) {

        final int numOfConnections = monitoringStatus.getNumOfConnections(status);
        if (monitoringStatus.isStatusHidden(status)) {
            final PageAction showAction = actionBuilder.newAction(showActionDef)
                    .withExec(showStateViewAction(monitoringStatus, clientTable, status))
                    .noEventPropagation()
                    .withSwitchAction(
                            actionBuilder.newAction(hideActionDef)
                                    .withExec(
                                            hideStateViewAction(monitoringStatus, clientTable, status))
                                    .noEventPropagation()
                                    .withNameAttributes(numOfConnections)
                                    .create())
                    .withNameAttributes(numOfConnections)
                    .create();
            this.pageService.publishAction(
                    showAction,
                    treeItem -> statusFilterGUIUpdate.register(treeItem, status));
        } else {
            final PageAction hideAction = actionBuilder.newAction(hideActionDef)
                    .withExec(hideStateViewAction(monitoringStatus, clientTable, status))
                    .noEventPropagation()
                    .withSwitchAction(
                            actionBuilder.newAction(showActionDef)
                                    .withExec(
                                            showStateViewAction(monitoringStatus, clientTable, status))
                                    .noEventPropagation()
                                    .withNameAttributes(numOfConnections)
                                    .create())
                    .withNameAttributes(numOfConnections)
                    .create();
            this.pageService.publishAction(
                    hideAction,
                    treeItem -> statusFilterGUIUpdate.register(treeItem, status));
        }
    }

    /** This holds the filter action items and implements the specific GUI update for it */
    private class StatusFilterGUIUpdate implements FullPageMonitoringGUIUpdate {

        private final PolyglotPageService polyglotPageService;
        private final TreeItem[] actionItemPerStateFilter = new TreeItem[ConnectionStatus.values().length];

        public StatusFilterGUIUpdate(final PolyglotPageService polyglotPageService) {
            this.polyglotPageService = polyglotPageService;
        }

        void register(final TreeItem item, final ConnectionStatus status) {
            this.actionItemPerStateFilter[status.code] = item;
        }

        @Override
        public void update(final MonitoringStatus monitoringStatus) {
            final ConnectionStatus[] states = ConnectionStatus.values();
            for (int i = 0; i < states.length; i++) {
                final ConnectionStatus state = states[i];
                final int numOfConnections = monitoringStatus.getNumOfConnections(state);
                if (numOfConnections >= 0 && this.actionItemPerStateFilter[state.code] != null) {
                    final TreeItem treeItem = this.actionItemPerStateFilter[state.code];
                    final PageAction action = (PageAction) treeItem.getData(ActionPane.ACTION_EVENT_CALL_KEY);
                    action.setTitleArgument(0, numOfConnections);
                    this.polyglotPageService.injectI18n(treeItem, action.getTitle());
                }
            }
        }
    }

    private PageAction openSearchPopup(final PageAction action) {
        this.monitoringExamSearchPopup.show(action.pageContext());
        return action;
    }

    private static Function<PageAction, PageAction> showStateViewAction(
            final MonitoringStatus monitoringStatus,
            final ClientConnectionTable clientTable,
            final ConnectionStatus status) {

        return action -> {
            monitoringStatus.showStatus(status);
            clientTable.removeSelection();
            return action;
        };
    }

    private static Function<PageAction, PageAction> hideStateViewAction(
            final MonitoringStatus monitoringStatus,
            final ClientConnectionTable clientTable,
            final ConnectionStatus status) {

        return action -> {
            monitoringStatus.hideStatus(status);
            clientTable.removeSelection();
            return action;
        };
    }

    private Set<EntityKey> selectionForQuitInstruction(final ClientConnectionTable clientTable) {
        final Set<String> connectionTokens = clientTable.getConnectionTokens(
                cc -> cc.status.clientActiveStatus,
                true);
        if (connectionTokens == null || connectionTokens.isEmpty()) {
            return Collections.emptySet();
        }

        return clientTable.getSelection();
    }

    private PageAction quitSEBClients(
            final PageAction action,
            final ClientConnectionTable clientTable,
            final boolean all) {

        this.instructionProcessor.propagateSEBQuitInstruction(
                clientTable.getExam().id,
                statesPredicate -> clientTable.getConnectionTokens(
                        statesPredicate,
                        !all),
                action.pageContext());

        clientTable.removeSelection();
        clientTable.forceUpdateAll();
        return action;
    }

    private PageAction disableSEBClients(
            final PageAction action,
            final ClientConnectionTable clientTable,
            final boolean all) {

        this.instructionProcessor.disableConnection(
                clientTable.getExam().id,
                statesPredicate -> clientTable.getConnectionTokens(
                        statesPredicate,
                        !all),
                action.pageContext());

        clientTable.removeSelection();
        clientTable.forceUpdateAll();
        return action;
    }

}
