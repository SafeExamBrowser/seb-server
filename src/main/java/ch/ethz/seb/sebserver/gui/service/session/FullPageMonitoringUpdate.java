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
import java.util.EnumSet;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.async.AsyncRunner;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.MonitoringFullPageData;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.push.ServerPushContext;
import ch.ethz.seb.sebserver.gui.service.push.ServerPushService;
import ch.ethz.seb.sebserver.gui.service.push.UpdateErrorHandler;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GetMonitoringFullPageData;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.DisposedOAuth2RestTemplateException;

/** Encapsulates the update and the current status of all monitoring data needed for a
 * full page monitoring.
 *
 * This handles server push and GUI update and also implements kind of circuit breaker and error handling */
public class FullPageMonitoringUpdate implements MonitoringStatus {

    static final Logger log = LoggerFactory.getLogger(FullPageMonitoringUpdate.class);

    private static final String USER_SESSION_STATUS_FILTER_ATTRIBUTE = "USER_SESSION_STATUS_FILTER_ATTRIBUTE";

    private final ServerPushService serverPushService;
    private final PageService pageService;
    private final AsyncRunner asyncRunner;
    private final RestCall<MonitoringFullPageData>.RestCallBuilder restCallBuilder;
    private final Collection<FullPageMonitoringGUIUpdate> guiUpdates;

    private ServerPushContext pushContext;
    private final EnumSet<ConnectionStatus> statusFilter;
    private String statusFilterParam = "";
    private boolean statusFilterChanged = false;
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
    public boolean statusFilterChanged() {
        return this.statusFilterChanged;
    }

    @Override
    public void resetStatusFilterChanged() {
        this.statusFilterChanged = false;
    }

    @Override
    public boolean isStatusHidden(final ConnectionStatus status) {
        return this.statusFilter.contains(status);
    }

    @Override
    public void hideStatus(final ConnectionStatus status) {
        this.statusFilter.add(status);
        saveStatusFilter();
    }

    @Override
    public void showStatus(final ConnectionStatus status) {
        this.statusFilter.remove(status);
        saveStatusFilter();
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
        this.monitoringFullPageData = this.restCallBuilder
                .withHeader(API.EXAM_MONITORING_STATE_FILTER, this.statusFilterParam)
                .call()
                .get(error -> {
                    recoverFromDisposedRestTemplate(error);
                    this.pushContext.reportError(error);
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

    private void saveStatusFilter() {
        try {
            this.pageService
                    .getCurrentUser()
                    .putAttribute(
                            USER_SESSION_STATUS_FILTER_ATTRIBUTE,
                            StringUtils.join(this.statusFilter, Constants.LIST_SEPARATOR));
        } catch (final Exception e) {
            log.warn("Failed to save status filter to user session");
        } finally {
            this.statusFilterParam = StringUtils.join(this.statusFilter, Constants.LIST_SEPARATOR);
            this.statusFilterChanged = true;
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
        } catch (final Exception e) {
            log.warn("Failed to load status filter to user session");
            this.statusFilter.clear();
            this.statusFilter.add(ConnectionStatus.DISABLED);
        } finally {
            this.statusFilterParam = StringUtils.join(this.statusFilter, Constants.LIST_SEPARATOR);
            this.statusFilterChanged = true;
        }
    }

    public void recoverFromDisposedRestTemplate(final Exception error) {
        if (log.isDebugEnabled()) {
            log.debug("Try to recover from disposed OAuth2 rest template...");
        }
        if (error instanceof DisposedOAuth2RestTemplateException) {
            this.pageService.getRestService().injectCurrentRestTemplate(this.restCallBuilder);
        }
    }
}
