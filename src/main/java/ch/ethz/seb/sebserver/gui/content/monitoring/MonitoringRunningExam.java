/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.monitoring;

import static ch.ethz.seb.sebserver.gbl.model.user.UserFeatures.Feature.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

import ch.ethz.seb.sebserver.gbl.model.exam.AllowedSEBVersion;
import ch.ethz.seb.sebserver.gbl.model.session.ProctoringGroupMonitoringData;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetScreenProctoringSettings;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.async.AsyncRunner;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroup;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringFeature;
import ch.ethz.seb.sebserver.gbl.model.exam.ScreenProctoringSettings;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionIssueStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ScreenProctoringGroup;
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
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.clientgroup.GetClientGroups;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.indicator.GetIndicators;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetScreenProctoringGroups;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.session.ClientConnectionTable;
import ch.ethz.seb.sebserver.gui.service.session.FullPageMonitoringGUIUpdate;
import ch.ethz.seb.sebserver.gui.service.session.FullPageMonitoringUpdate;
import ch.ethz.seb.sebserver.gui.service.session.InstructionProcessor;
import ch.ethz.seb.sebserver.gui.service.session.MonitoringFilter;
import ch.ethz.seb.sebserver.gui.service.session.proctoring.MonitoringProctoringService;
import ch.ethz.seb.sebserver.gui.service.session.proctoring.ProctoringGUIService;

@Lazy
@Component
@GuiProfile
public class MonitoringRunningExam implements TemplateComposer {

    private static final Logger log = LoggerFactory.getLogger(MonitoringRunningExam.class);

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
    private final SEBSendLockPopup sebSendLockPopup;
    private final MonitoringProctoringService monitoringProctoringService;
    private final boolean distributedSetup;
    private final long pollInterval;

    public MonitoringRunningExam(
            final ServerPushService serverPushService,
            final PageService pageService,
            final AsyncRunner asyncRunner,
            final InstructionProcessor instructionProcessor,
            final MonitoringExamSearchPopup monitoringExamSearchPopup,
            final SEBSendLockPopup sebSendLockPopup,
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
        this.sebSendLockPopup = sebSendLockPopup;
    }

    @Override
    public void compose(final PageContext pageContext) {
        final EntityKey entityKey = pageContext.getEntityKey();
        final CurrentUser currentUser = this.resourceService.getCurrentUser();
        final Exam exam = this.restService.getBuilder(GetExam.class)
                .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                .call()
                .getOrThrow();
        final UserInfo user = currentUser.get();

        final boolean detailEnabled = currentUser.isFeatureEnabled(MONITORING_RUNNING_EXAM_DETAIL_VIEW);
        final boolean quitEnabled = currentUser.isFeatureEnabled(MONITORING_RUNNING_EXAM_QUIT);
        final boolean lockscreenEnabled = currentUser.isFeatureEnabled(MONITORING_RUNNING_EXAM_QUIT);
        final boolean cancelEnabled = currentUser.isFeatureEnabled(MONITORING_RUNNING_EXAM_CANCEL_CON);
        final boolean supporting = user.hasAnyRole(UserRole.EXAM_SUPPORTER, UserRole.TEACHER) &&
                exam.supporter.contains(user.uuid);
        final BooleanSupplier isExamSupporter = () -> supporting || user.hasRole(UserRole.EXAM_ADMIN);

        final Collection<Indicator> indicators = this.restService.getBuilder(GetIndicators.class)
                .withQueryParam(Indicator.FILTER_ATTR_EXAM_ID, entityKey.modelId)
                .call()
                .getOrThrow();

        final Collection<ClientGroup> clientGroups = this.restService.getBuilder(GetClientGroups.class)
                .withQueryParam(Indicator.FILTER_ATTR_EXAM_ID, entityKey.modelId)
                .call()
                .getOrThrow();

        final Composite content = this.pageService.getWidgetFactory().defaultPageLayout(
                pageContext.getParent(),
                new LocTextKey(
                        "sebserver.monitoring.exam",
                        StringEscapeUtils.escapeXml11(exam.name)));

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
                clientGroups,
                this.distributedSetup);
        guiUpdates.add(clientTable);

        clientTable
                .withDefaultActionIf(() -> detailEnabled,
                        actionBuilder
                                .newAction(ActionDefinition.MONITOR_EXAM_CLIENT_CONNECTION)
                                .withParentEntityKey(entityKey)
                                .create(),
                        this.pageService)
                .withSelectionListener(this.getSelectionPublisherClientConnectionTable(
                        pageContext,
                        ActionDefinition.MONITOR_EXAM_CLIENT_CONNECTION,
                        ActionDefinition.MONITOR_EXAM_QUIT_SELECTED,
                        ActionDefinition.MONITOR_EXAM_LOCK_SELECTED,
                        ActionDefinition.MONITOR_EXAM_DISABLE_SELECTED_CONNECTION));

        actionBuilder

                .newAction(ActionDefinition.MONITORING_EXAM_SEARCH_CONNECTIONS)
                .withEntityKey(entityKey)
                .withExec(this::openSearchPopup)
                .noEventPropagation()
                .publishIf(isExamSupporter)

                .newAction(ActionDefinition.MONITOR_EXAM_QUIT_ALL)
                .withEntityKey(entityKey)
                .withConfirm(() -> CONFIRM_QUIT_ALL)
                .withExec(action -> this.quitSEBClients(action, clientTable, true))
                .noEventPropagation()
                .publishIf(() -> isExamSupporter.getAsBoolean() && quitEnabled)

                .newAction(ActionDefinition.MONITOR_EXAM_QUIT_SELECTED)
                .withEntityKey(entityKey)
                .withConfirm(() -> CONFIRM_QUIT_SELECTED)
                .withSelect(
                        () -> this.selectionForInstruction(clientTable),
                        action -> this.quitSEBClients(action, clientTable, false),
                        EMPTY_ACTIVE_SELECTION_TEXT_KEY)
                .noEventPropagation()
                .publishIf(() -> isExamSupporter.getAsBoolean() && quitEnabled, false)

                .newAction(ActionDefinition.MONITOR_EXAM_LOCK_SELECTED)
                .withEntityKey(entityKey)
                .withSelect(
                        () -> this.selectionForInstruction(clientTable),
                        action -> this.showSEBLockActionPopup(action, clientTable),
                        EMPTY_ACTIVE_SELECTION_TEXT_KEY)
                .noEventPropagation()
                .publishIf(() -> isExamSupporter.getAsBoolean() && lockscreenEnabled, false)

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
                .publishIf(() -> isExamSupporter.getAsBoolean() && detailEnabled, false)

                .newAction(ActionDefinition.MONITOR_EXAM_DISABLE_SELECTED_CONNECTION)
                .withEntityKey(entityKey)
                .withConfirm(() -> CONFIRM_DISABLE_SELECTED)
                .withSelect(
                        clientTable::getSelection,
                        action -> this.disableSEBClients(action, clientTable, false),
                        EMPTY_SELECTION_TEXT_KEY)
                .noEventPropagation()
                .publishIf(() -> isExamSupporter.getAsBoolean() && cancelEnabled, false);

        if (isExamSupporter.getAsBoolean()) {
            guiUpdates.add(createFilterActions(
                    clientGroups,
                    fullPageMonitoringUpdate,
                    actionBuilder,
                    clientTable,
                    isExamSupporter,
                    exam.checkASK,
                    exam.allowedSEBVersions));
            
            final ProctoringServiceSettings proctoringSettings = new ProctoringServiceSettings(exam);
            final ScreenProctoringSettings screenProctoringSettings = restService
                    .getBuilder(GetScreenProctoringSettings.class)
                    .withURIVariable(API.PARAM_MODEL_ID, exam.getModelId())
                    .call()
                    .getOrThrow();
            guiUpdates.add(createProctoringActions(
                    proctoringSettings,
                    currentUser.isFeatureEnabled(EXAM_SCREEN_PROCTORING)
                            ? screenProctoringSettings
                            : null,
                    currentUser.getProctoringGUIService(),
                    pageContext,
                    content));

        }

        // finally start the page update (server push)
        fullPageMonitoringUpdate.start(pageContext, content, this.pollInterval);
    }

    private PageAction showSEBLockActionPopup(
            final PageAction action,
            final ClientConnectionTable clientTable) {

        this.sebSendLockPopup.show(
                action,
                statesPredicate -> clientTable.getConnectionTokens(
                        statesPredicate,
                        true));
        clientTable.removeSelection();
        return action;
    }

    private FullPageMonitoringGUIUpdate createProctoringActions(
            final ProctoringServiceSettings proctoringSettings,
            final ScreenProctoringSettings screenProctoringSettings,
            final ProctoringGUIService proctoringGUIService,
            final PageContext pageContext,
            final Composite parent) {

        final CurrentUser currentUser = pageService.getCurrentUser();
        final PageActionBuilder actionBuilder = this.pageService
                .pageActionBuilder(pageContext.clearEntityKeys());

        final boolean proctoringEnabled = proctoringSettings != null
                && currentUser.isFeatureEnabled(MONITORING_RUNNING_EXAM_LIVE_PROCTORING)
                && BooleanUtils.toBoolean(proctoringSettings.enableProctoring);
        final boolean screenProctoringEnabled = screenProctoringSettings != null
                && currentUser.isFeatureEnabled(MONITORING_RUNNING_EXAM_SCREEN_PROCTORING)
                && BooleanUtils.toBoolean(screenProctoringSettings.enableScreenProctoring);

        if (!proctoringEnabled && !screenProctoringEnabled) {
            return monitoringStatus -> {
            };
        }

        if (proctoringEnabled && proctoringSettings.enabledFeatures.contains(ProctoringFeature.TOWN_HALL)) {
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

        proctoringGUIService.clearActionState();
        final EntityKey entityKey = pageContext.getEntityKey();
        final Collection<ScreenProctoringGroup> screenProctoringGroups = ((screenProctoringEnabled)
                ? this.pageService
                        .getRestService()
                        .getBuilder(GetScreenProctoringGroups.class)
                        .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                        .call()
                        .onError(error -> log.error("\"Failed to get screen proctoring group data:", error))
                        .getOr(Collections.emptyList())
                : Collections.emptyList());
        final List<ProctoringGroupMonitoringData> spMonitoringData = screenProctoringGroups
                .stream()
                .map(g -> new ProctoringGroupMonitoringData(g.uuid, g.name, g.size))
                .toList();


        this.monitoringProctoringService.updateCollectingRoomActions(
                spMonitoringData,
                pageContext,
                proctoringSettings,
                proctoringGUIService,
                screenProctoringSettings);

        return monitoringStatus -> this.monitoringProctoringService
                .updateCollectingRoomActions(
                        monitoringStatus.screenProctoringData(),
                        pageContext,
                        proctoringSettings,
                        proctoringGUIService,
                        screenProctoringSettings);
    }

    private FullPageMonitoringGUIUpdate createFilterActions(
            final Collection<ClientGroup> clientGroups,
            final MonitoringFilter monitoringStatus,
            final PageActionBuilder actionBuilder,
            final ClientConnectionTable clientTable,
            final BooleanSupplier isExamSupporter,
            final boolean isAskCheckEnabled,
            final List<AllowedSEBVersion> allowedSEBVersions) {

        final CurrentUser currentUser = pageService.getCurrentUser();
        final boolean stateFilterEnabled = currentUser.isFeatureEnabled(MONITORING_RUNNING_EXAM_STATE_FILTER);
        final boolean issueFilterEnabled = currentUser.isFeatureEnabled(MONITORING_RUNNING_EXAM_ISSUE_FILTER);
        final boolean clientFilterEnabled = currentUser.isFeatureEnabled(MONITORING_RUNNING_EXAM_CLIENT_FILTER);

        final FilterGUIUpdate statusFilterGUIUpdate =
                new FilterGUIUpdate(this.pageService.getPolyglotPageService());

        if (stateFilterEnabled) {
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
                    ConnectionStatus.READY,
                    ActionDefinition.MONITOR_EXAM_SHOW_READY_CONNECTION,
                    ActionDefinition.MONITOR_EXAM_HIDE_READY_CONNECTION);
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
        }

        if(issueFilterEnabled && isAskCheckEnabled) {
            addIssueFilterAction(
                    monitoringStatus,
                    statusFilterGUIUpdate,
                    actionBuilder,
                    clientTable,
                    ConnectionIssueStatus.ASK_GRANTED,
                    ActionDefinition.MONITOR_EXAM_SHOW_ASK_GRANTED,
                    ActionDefinition.MONITOR_EXAM_HIDE_ASK_GRANTED);
        }

        if(issueFilterEnabled && allowedSEBVersions != null) {
            addIssueFilterAction(
                    monitoringStatus,
                    statusFilterGUIUpdate,
                    actionBuilder,
                    clientTable,
                    ConnectionIssueStatus.SEB_VERSION_GRANTED,
                    ActionDefinition.MONITOR_EXAM_SHOW_SEB_VERSION_GRANTED,
                    ActionDefinition.MONITOR_EXAM_HIDE_SEB_VERSION_GRANTED);
        }

        if (clientFilterEnabled && clientGroups != null && !clientGroups.isEmpty()) {
            clientGroups.forEach(clientGroup -> {

                addClientGroupFilterAction(
                        monitoringStatus,
                        statusFilterGUIUpdate,
                        actionBuilder,
                        clientTable,
                        clientGroup,
                        ActionDefinition.MONITOR_EXAM_SHOW_CLIENT_GROUP_CONNECTION,
                        ActionDefinition.MONITOR_EXAM_HIDE_CLIENT_GROUP_CONNECTION);

            });
        }

        return statusFilterGUIUpdate;
    }

    private void addIssueFilterAction(
            final MonitoringFilter filter,
            final FilterGUIUpdate filterGUIUpdate,
            final PageActionBuilder actionBuilder,
            final ClientConnectionTable clientTable,
            final ConnectionIssueStatus connectionIssueStatus,
            final ActionDefinition showActionDef,
            final ActionDefinition hideActionDef) {

        final int numOfConnections = filter.getNumOfConnections(connectionIssueStatus);
        final PageAction action = actionBuilder.newAction(filter.isIssueHidden(connectionIssueStatus)
                        ? showActionDef
                        : hideActionDef)
                .withExec(filter.isIssueHidden(connectionIssueStatus)
                        ? showIssueViewAction(filter, clientTable, connectionIssueStatus)
                        : hideIssueViewAction(filter, clientTable, connectionIssueStatus))
                .noEventPropagation()
                .withSwitchAction(
                        actionBuilder.newAction(filter.isIssueHidden(connectionIssueStatus)
                                        ? hideActionDef
                                        : showActionDef)
                                .withExec(filter.isIssueHidden(connectionIssueStatus)
                                        ? hideIssueViewAction(filter, clientTable, connectionIssueStatus)
                                        : showIssueViewAction(filter, clientTable, connectionIssueStatus))
                                .noEventPropagation()
                                .withNameAttributes(numOfConnections)
                                .create())
                .withNameAttributes(numOfConnections)
                .create();

        this.pageService.publishAction(
                action,
                treeItem -> filterGUIUpdate.register(treeItem, connectionIssueStatus));
    }

    private void addFilterAction(
            final MonitoringFilter filter,
            final FilterGUIUpdate filterGUIUpdate,
            final PageActionBuilder actionBuilder,
            final ClientConnectionTable clientTable,
            final ConnectionStatus status,
            final ActionDefinition showActionDef,
            final ActionDefinition hideActionDef) {

        final int numOfConnections = filter.getNumOfConnections(status);
        final PageAction action;
        if (filter.isStatusHidden(status)) {
            action = actionBuilder.newAction(showActionDef)
                    .withExec(showStateViewAction(filter, clientTable, status))
                    .noEventPropagation()
                    .withSwitchAction(
                            actionBuilder.newAction(hideActionDef)
                                    .withExec(hideStateViewAction(filter, clientTable, status))
                                    .noEventPropagation()
                                    .withNameAttributes(numOfConnections)
                                    .create())
                    .withNameAttributes(numOfConnections)
                    .create();
        } else {
            action = actionBuilder.newAction(hideActionDef)
                    .withExec(hideStateViewAction(filter, clientTable, status))
                    .noEventPropagation()
                    .withSwitchAction(
                            actionBuilder.newAction(showActionDef)
                                    .withExec(showStateViewAction(filter, clientTable, status))
                                    .noEventPropagation()
                                    .withNameAttributes(numOfConnections)
                                    .create())
                    .withNameAttributes(numOfConnections)
                    .create();
        }
        this.pageService.publishAction(
                action,
                treeItem -> filterGUIUpdate.register(treeItem, status));
    }

    private void addClientGroupFilterAction(
            final MonitoringFilter filter,
            final FilterGUIUpdate filterGUIUpdate,
            final PageActionBuilder actionBuilder,
            final ClientConnectionTable clientTable,
            final ClientGroup clientGroup,
            final ActionDefinition showActionDef,
            final ActionDefinition hideActionDef) {

        final int numOfConnections = filter.getNumOfConnections(clientGroup.id);
        final PageAction action;
        if (filter.isClientGroupHidden(clientGroup.id)) {
            action = actionBuilder.newAction(showActionDef)
                    .withExec(showClientGroupAction(filter, clientTable, clientGroup.id))
                    .noEventPropagation()
                    .withSwitchAction(
                            actionBuilder.newAction(hideActionDef)
                                    .withExec(
                                            hideClientGroupViewAction(filter, clientTable, clientGroup.id))
                                    .noEventPropagation()
                                    .withNameAttributes(clientGroup.name, numOfConnections)
                                    .create())
                    .withNameAttributes(clientGroup.name, numOfConnections)
                    .create();
        } else {
            action = actionBuilder.newAction(hideActionDef)
                    .withExec(hideClientGroupViewAction(filter, clientTable, clientGroup.id))
                    .noEventPropagation()
                    .withSwitchAction(
                            actionBuilder.newAction(showActionDef)
                                    .withExec(
                                            showClientGroupAction(filter, clientTable, clientGroup.id))
                                    .noEventPropagation()
                                    .withNameAttributes(clientGroup.name, numOfConnections)
                                    .create())
                    .withNameAttributes(clientGroup.name, numOfConnections)
                    .create();
        }
        this.pageService.publishAction(
                action,
                treeItem -> filterGUIUpdate.register(treeItem, clientGroup.id));
    }

    /** This holds the filter action items and implements the specific GUI update for it */
    private static class FilterGUIUpdate implements FullPageMonitoringGUIUpdate {

        private final PolyglotPageService polyglotPageService;
        private final TreeItem[] actionItemPerStateFilter = new TreeItem[ConnectionStatus.values().length];
        private final TreeItem[] actionItemPerIssueFilter = new TreeItem[ConnectionIssueStatus.values().length];
        private final Map<Long, TreeItem> actionItemPerClientGroup = new HashMap<>();

        public FilterGUIUpdate(final PolyglotPageService polyglotPageService) {
            this.polyglotPageService = polyglotPageService;
        }

        void register(final TreeItem item, final ConnectionStatus status) {
            this.actionItemPerStateFilter[status.code] = item;
        }

        void register(final TreeItem item, final Long clientGroupId) {
            this.actionItemPerClientGroup.put(clientGroupId, item);
        }

        void register(final TreeItem item, final ConnectionIssueStatus status) {
            this.actionItemPerIssueFilter[status.code] = item;
        }

        @Override
        public void update(final MonitoringFilter monitoringStatus) {
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

            final ConnectionIssueStatus[] connectionIssueStates = ConnectionIssueStatus.values();
            for (int i = 0; i < connectionIssueStates.length; i++) {
                final ConnectionIssueStatus state = connectionIssueStates[i];
                final int numOfConnections = monitoringStatus.getNumOfConnections(state);
                if (numOfConnections >= 0 && this.actionItemPerIssueFilter[state.code] != null) {
                    final TreeItem treeItem = this.actionItemPerIssueFilter[state.code];
                    final PageAction action = (PageAction) treeItem.getData(ActionPane.ACTION_EVENT_CALL_KEY);
                    action.setTitleArgument(0, numOfConnections);
                    this.polyglotPageService.injectI18n(treeItem, action.getTitle());
                }
            }


            if (!this.actionItemPerClientGroup.isEmpty()) {
                this.actionItemPerClientGroup.entrySet().forEach(entry -> {
                    final int numOfConnections = monitoringStatus.getNumOfConnections(entry.getKey());
                    if (numOfConnections >= 0) {
                        final TreeItem treeItem = entry.getValue();
                        final PageAction action = (PageAction) treeItem.getData(ActionPane.ACTION_EVENT_CALL_KEY);
                        action.setTitleArgument(1, numOfConnections);
                        this.polyglotPageService.injectI18n(treeItem, action.getTitle());
                    }
                });
            }
        }
    }

    private PageAction openSearchPopup(final PageAction action) {
        this.monitoringExamSearchPopup.show(action.pageContext());
        return action;
    }

    private static Function<PageAction, PageAction> showStateViewAction(
            final MonitoringFilter monitoringStatus,
            final ClientConnectionTable clientTable,
            final ConnectionStatus status) {

        return action -> {
            monitoringStatus.showStatus(status);
            clientTable.removeSelection();
            return action;
        };
    }

    private static Function<PageAction, PageAction> hideStateViewAction(
            final MonitoringFilter monitoringStatus,
            final ClientConnectionTable clientTable,
            final ConnectionStatus status) {

        return action -> {
            monitoringStatus.hideStatus(status);
            clientTable.removeSelection();
            return action;
        };
    }

    private static Function<PageAction, PageAction> showClientGroupAction(
            final MonitoringFilter monitoringStatus,
            final ClientConnectionTable clientTable,
            final Long clientGroupId) {

        return action -> {
            monitoringStatus.showClientGroup(clientGroupId);
            clientTable.removeSelection();
            return action;
        };
    }

    private static Function<PageAction, PageAction> hideClientGroupViewAction(
            final MonitoringFilter monitoringStatus,
            final ClientConnectionTable clientTable,
            final Long clientGroupId) {

        return action -> {
            monitoringStatus.hideClientGroup(clientGroupId);
            clientTable.removeSelection();
            return action;
        };
    }

    private static Function<PageAction, PageAction> showIssueViewAction(
            final MonitoringFilter monitoringStatus,
            final ClientConnectionTable clientTable,
            final ConnectionIssueStatus connectionIssueStatus) {

        return action -> {
            monitoringStatus.showIssue(connectionIssueStatus);
            clientTable.removeSelection();
            return action;
        };

    }

    private static Function<PageAction, PageAction> hideIssueViewAction(
            final MonitoringFilter monitoringStatus,
            final ClientConnectionTable clientTable,
            final ConnectionIssueStatus connectionIssueStatus) {

        return action -> {
            monitoringStatus.hideIssue(connectionIssueStatus);
            clientTable.removeSelection();
            return action;
        };

    }

    private Set<EntityKey> selectionForInstruction(final ClientConnectionTable clientTable) {
        final Set<String> connectionTokens = clientTable.getConnectionTokens(
                cc -> cc.getStatus().clientActiveStatus,
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
                clientTable.getExam().getModelId(),
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

    private Consumer<ClientConnectionTable> getSelectionPublisherClientConnectionTable(
            final PageContext pageContext,
            final ActionDefinition... actionDefinitions) {

        return table -> this.pageService.firePageEvent(
                new ActionActivationEvent(table.getSingleSelection() != null, actionDefinitions),
                pageContext);
    }

}
