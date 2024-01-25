/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.session;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.async.AsyncRunner;
import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroup;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionIssueStatus;
import ch.ethz.seb.sebserver.gbl.monitoring.MonitoringFullPageData;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.push.ServerPushContext;
import ch.ethz.seb.sebserver.gui.service.push.ServerPushService;
import ch.ethz.seb.sebserver.gui.service.push.UpdateErrorHandler;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.clientgroup.GetClientGroups;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetMonitoringFullPageData;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.DisposedOAuth2RestTemplateException;

/** Encapsulates the update and the current status of all monitoring data needed for a
 * full page monitoring.
 *
 * This handles server push and GUI update and also implements kind of circuit breaker and error handling */
public class FullPageMonitoringUpdate implements MonitoringFilter {

    static final Logger log = LoggerFactory.getLogger(FullPageMonitoringUpdate.class);

    private static final String USER_SESSION_STATUS_FILTER_ATTRIBUTE = "USER_SESSION_STATUS_FILTER";

    private static final String USER_SESSION_ISSUE_FILTER_ATTRIBUTE = "USER_SESSION_ISSUE_FILTER";

    private static final String USER_SESSION_GROUP_FILTER_ATTRIBUTE = "USER_SESSION_GROUP_FILTER";

    private final ServerPushService serverPushService;
    private final PageService pageService;
    private final AsyncRunner asyncRunner;
    private final RestCall<MonitoringFullPageData>.RestCallBuilder restCallBuilder;
    private final Collection<FullPageMonitoringGUIUpdate> guiUpdates;

    private ServerPushContext pushContext;

    private final EnumSet<ConnectionStatus> statusFilter;
    private String statusFilterParam = "";

    private final EnumSet<ConnectionIssueStatus> issueFilter;
    private String issueFilterParam = "";

    private final Set<Long> clientGroupFilter;
    private String clientGroupFilterParam = "";
    private boolean filterChanged = false;

    private boolean updateInProgress = false;
    private MonitoringFullPageData monitoringFullPageData = null;

    public FullPageMonitoringUpdate(
            final Long examId,
            final PageService pageService,
            final ServerPushService serverPushService,
            final AsyncRunner asyncRunner,
            final Collection<FullPageMonitoringGUIUpdate> guiUpdates) {

        this.serverPushService = serverPushService;
        this.pageService = pageService;
        this.asyncRunner = asyncRunner;
        this.restCallBuilder = pageService
                .getRestService()
                .getBuilder(GetMonitoringFullPageData.class)
                .withURIVariable(API.PARAM_PARENT_MODEL_ID, String.valueOf(examId));
        this.guiUpdates = guiUpdates;

        this.statusFilter = EnumSet.noneOf(ConnectionStatus.class);
        loadStatusFilter();

        this.issueFilter = EnumSet.noneOf(ConnectionIssueStatus.class);
        loadIssueFilter();

        final Collection<ClientGroup> clientGroups = pageService.getRestService()
                .getBuilder(GetClientGroups.class)
                .withQueryParam(Indicator.FILTER_ATTR_EXAM_ID, String.valueOf(examId))
                .call()
                .getOr(Collections.emptyList());

        if (clientGroups != null && !clientGroups.isEmpty()) {
            this.clientGroupFilter = new HashSet<>();
        } else {
            this.clientGroupFilter = null;
        }
    }

    public void start(final PageContext pageContext, final Composite anchor, final long pollInterval) {
        try {
            final UpdateErrorHandler updateErrorHandler =
                    new UpdateErrorHandler(this.pageService, pageContext);

            this.pushContext = new ServerPushContext(
                    anchor,
                    Utils.truePredicate(),
                    updateErrorHandler);

            this.serverPushService.runServerPush(
                    this.pushContext,
                    pollInterval,
                    context -> update());
        } catch (final Exception e) {
            log.error("Failed to start FullPageMonitoringUpdate: ", e);
        }
    }

    @Override
    public EnumSet<ConnectionStatus> getStatusFilter() {
        return this.statusFilter;
    }

    @Override
    public String getStatusFilterParam() {
        return this.statusFilterParam;
    }

    @Override
    public EnumSet<ConnectionIssueStatus> getIssueFilter() {
        return this.issueFilter;
    }

    @Override
    public String getIssueFilterParam() {
        return this.issueFilterParam;
    }

    @Override
    public boolean filterChanged() {
        return this.filterChanged;
    }

    @Override
    public void resetFilterChanged() {
        this.filterChanged = false;
    }

    @Override
    public boolean isStatusHidden(final ConnectionStatus status) {
        return this.statusFilter.contains(status);
    }

    @Override
    public void hideStatus(final ConnectionStatus status) {
        this.statusFilter.add(status);
        saveFilter();
    }

    @Override
    public void showStatus(final ConnectionStatus status) {
        this.statusFilter.remove(status);
        saveFilter();
    }

    @Override
    public boolean hasClientGroupFilter() {
        return this.clientGroupFilter != null;
    }

    @Override
    public boolean isClientGroupHidden(final Long clientGroupId) {
        return this.clientGroupFilter != null && this.clientGroupFilter.contains(clientGroupId);
    }

    @Override
    public void hideClientGroup(final Long clientGroupId) {
        if (this.clientGroupFilter == null) {
            return;
        }

        this.clientGroupFilter.add(clientGroupId);
        saveFilter();
    }

    @Override
    public void showClientGroup(final Long clientGroupId) {
        if (this.clientGroupFilter == null) {
            return;
        }

        this.clientGroupFilter.remove(clientGroupId);
        saveFilter();
    }

    @Override
    public void hideIssue(final ConnectionIssueStatus connectionIssueStatus) {
        this.issueFilter.add(connectionIssueStatus);
        saveFilter();
    }

    @Override
    public void showIssue(final ConnectionIssueStatus connectionIssueStatus){
        this.issueFilter.remove(connectionIssueStatus);
        saveFilter();
    }

    @Override
    public MonitoringFullPageData getMonitoringFullPageData() {
        return this.monitoringFullPageData;
    }

    private void update() {
        if (this.updateInProgress) {
            return;
        }

        this.updateInProgress = true;

        this.asyncRunner.runAsync(() -> {

            try {
                updateBusinessData();

            } catch (final Exception e) {
                log.error("Failed to update full page monitoring: ", e);
            } finally {
                this.updateInProgress = false;
            }
        });

        if (this.monitoringFullPageData != null) {
            callGUIUpdates();
        }
    }

    private void updateBusinessData() {
        RestCall<MonitoringFullPageData>.RestCallBuilder restCallBuilder = this.restCallBuilder
                .withHeader(API.EXAM_MONITORING_STATE_FILTER, this.statusFilterParam)
                .withHeader(API.EXAM_MONITORING_ISSUE_FILTER, this.issueFilterParam);

        if (hasClientGroupFilter()) {
            restCallBuilder = restCallBuilder
                    .withHeader(API.EXAM_MONITORING_CLIENT_GROUP_FILTER, this.clientGroupFilterParam);
        }

        this.monitoringFullPageData = restCallBuilder
                .call()
                .get(error -> {
                    this.pushContext.reportError(error);
                    recoverFromDisposedRestTemplate(error);
                    return this.monitoringFullPageData;
                });
    }

    private void callGUIUpdates() {
        this.guiUpdates.forEach(updater -> {
            try {
                updater.update(this);
            } catch (final Exception e) {
                log.error("Failed to update monitoring GUI element: ", e);
                this.pushContext.reportError(e);
            }
        });
    }

    private void saveFilter() {
        try {
            this.pageService
                    .getCurrentUser()
                    .putAttribute(
                            USER_SESSION_STATUS_FILTER_ATTRIBUTE,
                            StringUtils.join(this.statusFilter, Constants.LIST_SEPARATOR));
            this.pageService
                    .getCurrentUser()
                    .putAttribute(
                            USER_SESSION_ISSUE_FILTER_ATTRIBUTE,
                            StringUtils.join(this.issueFilter, Constants.LIST_SEPARATOR));
            if (hasClientGroupFilter()) {
                this.pageService
                        .getCurrentUser()
                        .putAttribute(
                                USER_SESSION_GROUP_FILTER_ATTRIBUTE,
                                StringUtils.join(this.clientGroupFilter, Constants.LIST_SEPARATOR));
            }
        } catch (final Exception e) {
            log.warn("Failed to save status filter to user session");
        } finally {
            this.statusFilterParam = StringUtils.join(this.statusFilter, Constants.LIST_SEPARATOR);
            this.issueFilterParam = StringUtils.join(this.issueFilter, Constants.LIST_SEPARATOR);
            if (hasClientGroupFilter()) {
                this.clientGroupFilterParam = StringUtils.join(this.clientGroupFilter, Constants.LIST_SEPARATOR);
            }
            this.filterChanged = true;
        }
    }

    private void loadStatusFilter() {
        try {
            final String attribute = this.pageService
                    .getCurrentUser()
                    .getAttribute(USER_SESSION_STATUS_FILTER_ATTRIBUTE);
            this.statusFilter.clear();
            if (attribute != null) {
                Arrays.asList(StringUtils.split(attribute, Constants.LIST_SEPARATOR))
                        .forEach(name -> this.statusFilter.add(ConnectionStatus.valueOf(name)));
            } else {
                this.statusFilter.add(ConnectionStatus.DISABLED);
            }

            if (hasClientGroupFilter()) {
                final String groups = this.pageService
                        .getCurrentUser()
                        .getAttribute(USER_SESSION_GROUP_FILTER_ATTRIBUTE);
                this.statusFilter.clear();
                if (groups != null) {
                    Arrays.asList(StringUtils.split(groups, Constants.LIST_SEPARATOR))
                            .forEach(id -> this.clientGroupFilter.add(Long.parseLong(id)));
                }
            }
        } catch (final Exception e) {
            log.warn("Failed to load status filter to user session");
            this.statusFilter.clear();
            this.statusFilter.add(ConnectionStatus.DISABLED);
            if (hasClientGroupFilter()) {
                this.clientGroupFilter.clear();
            }
        } finally {
            this.statusFilterParam = StringUtils.join(this.statusFilter, Constants.LIST_SEPARATOR);
            if (hasClientGroupFilter()) {
                this.clientGroupFilterParam = StringUtils.join(this.clientGroupFilter, Constants.LIST_SEPARATOR);
            }
            this.filterChanged = true;
        }
    }

    private void loadIssueFilter() {
        try {
            final String attribute = this.pageService
                    .getCurrentUser()
                    .getAttribute(USER_SESSION_ISSUE_FILTER_ATTRIBUTE);
            this.issueFilter.clear();
            if (attribute != null) {
                Arrays.asList(StringUtils.split(attribute, Constants.LIST_SEPARATOR))
                        .forEach(name -> this.issueFilter.add(ConnectionIssueStatus.valueOf(name)));
            }

        } catch (final Exception e) {
            log.warn("Failed to load status filter to user session");
            this.issueFilter.clear();
        } finally {
            this.issueFilterParam = StringUtils.join(this.issueFilter, Constants.LIST_SEPARATOR);
            this.filterChanged = true;
        }
    }

    public void recoverFromDisposedRestTemplate(final Exception error) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Try to recover from disposed OAuth2 rest template...");
            }
            if (error instanceof DisposedOAuth2RestTemplateException) {
                this.pageService.getRestService().injectCurrentRestTemplate(this.restCallBuilder);
            }
        } catch (final Exception e) {
            log.error("Failed to recover from disposed rest template: ", e);
        }
    }
}
