/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.session;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroup;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientNotification;
import ch.ethz.seb.sebserver.gbl.monitoring.IndicatorValue;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.Form;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEvent;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.DisposedOAuth2RestTemplateException;
import ch.ethz.seb.sebserver.gui.service.session.IndicatorData.ThresholdColor;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

public class ClientConnectionDetails implements MonitoringEntry {

    private static final Logger log = LoggerFactory.getLogger(ClientConnectionDetails.class);

    private final static LocTextKey EXAM_NAME_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.form.exam");
    private final static LocTextKey CONNECTION_ID_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.form.id");
    private final static LocTextKey CONNECTION_GROUP_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.form.group");
    private final static LocTextKey CONNECTION_INFO_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.form.info");
    private final static LocTextKey CONNECTION_STATUS_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.form.status");
    private final static LocTextKey WRONG_SEB_CLIENT_TOOLTIP =
            new LocTextKey("sebserver.finished.connection.form.info.wrong.client.tooltip");
    private final static LocTextKey GRANTED_TEXT =
            new LocTextKey("sebserver.monitoring.exam.connection.status.GRANTED");

    private static final int NUMBER_OF_NONE_INDICATOR_ROWS = 3;

    private final PageService pageService;
    private final ResourceService resourceService;
    private final Map<Long, IndicatorData> indicatorMapping;
    private final Map<Long, ClientGroup> clientGroupMapping;
    private final boolean hasClientGroups;
    private final RestCall<ClientConnectionData>.RestCallBuilder restCallBuilder;
    private final FormHandle<?> formHandle;
    private final ColorData colorData;
    private final Function<MonitoringEntry, String> localizedClientConnectionStatusNameFunction;
    public final boolean checkSecurityGrant;

    private ClientConnectionData connectionData = null;
    public boolean grantChecked = false;
    public boolean grantDenied = false;
    private boolean statusChanged = true;
    private boolean missingChanged = true;
    private long startTime = -1;
    private Consumer<ClientConnectionData> statusChangeListener = null;

    public ClientConnectionDetails(
            final PageService pageService,
            final PageContext pageContext,
            final Exam exam,
            final RestCall<ClientConnectionData>.RestCallBuilder restCallBuilder,
            final Collection<Indicator> indicators,
            final Collection<ClientGroup> clientGroups) {

        final Display display = pageContext.getRoot().getDisplay();
        this.hasClientGroups = clientGroups != null && !clientGroups.isEmpty();
        this.pageService = pageService;
        this.resourceService = pageService.getResourceService();
        this.restCallBuilder = restCallBuilder;
        this.colorData = new ColorData(display);
        this.checkSecurityGrant = BooleanUtils.toBoolean(
                exam.additionalAttributes.get(Exam.ADDITIONAL_ATTR_SIGNATURE_KEY_CHECK_ENABLED));

        this.indicatorMapping = IndicatorData.createFormIndicators(
                indicators,
                display,
                this.colorData,
                NUMBER_OF_NONE_INDICATOR_ROWS);

        this.clientGroupMapping = clientGroups == null || clientGroups.isEmpty()
                ? null
                : clientGroups
                        .stream()
                        .collect(Collectors.toMap(cg -> cg.id, Function.identity()));

        final FormBuilder formBuilder = pageService.formBuilder(pageContext)
                .readonly(true)
                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_NAME,
                        EXAM_NAME_TEXT_KEY,
                        exam.getName()))
                .addField(FormBuilder.text(
                        Domain.CLIENT_CONNECTION.ATTR_EXAM_USER_SESSION_ID,
                        CONNECTION_ID_TEXT_KEY,
                        Constants.EMPTY_NOTE))
                .addFieldIf(() -> this.hasClientGroups,
                        () -> FormBuilder.text(
                                ClientConnectionData.ATTR_CLIENT_GROUPS,
                                CONNECTION_GROUP_TEXT_KEY,
                                Constants.EMPTY_NOTE)
                                .asMarkupLabel())
                .withDefaultSpanInput(3)

                .addField(FormBuilder.text(
                        ClientConnection.ATTR_INFO,
                        CONNECTION_INFO_TEXT_KEY,
                        Constants.EMPTY_NOTE)
                        .asArea(50)
                        .asColorBox())
                .withDefaultSpanEmptyCell(2)
                .withDefaultSpanInput(3)
                .addField(FormBuilder.text(
                        Domain.CLIENT_CONNECTION.ATTR_STATUS,
                        CONNECTION_STATUS_TEXT_KEY,
                        Constants.EMPTY_NOTE)
                        .asColorBox())
                .addEmptyCell();

        this.indicatorMapping
                .values()
                .forEach(indData -> formBuilder.addField(FormBuilder.text(
                        indData.indicator.name,
                        new LocTextKey(indData.indicator.name),
                        Constants.EMPTY_NOTE)
                        .asColorBox()
                        .withDefaultLabel(indData.indicator.name))
                        .addEmptyCell());

        this.formHandle = formBuilder.build();
        this.localizedClientConnectionStatusNameFunction =
                this.resourceService.localizedClientMonitoringStatusNameFunction();
    }

    @Override
    public ConnectionStatus getStatus() {
        if (this.connectionData == null) {
            return ConnectionStatus.UNDEFINED;
        }
        return this.connectionData.clientConnection.status;
    }

    public boolean isActive() {
        if (this.connectionData == null) {
            return true;
        }
        return this.connectionData.clientConnection.status != null
                && this.connectionData.clientConnection.status.clientActiveStatus;
    }

    @Override
    public boolean hasMissingPing() {
        return (this.connectionData != null) ? this.connectionData.missingPing : false;
    }

    @Override
    public boolean grantChecked() {
        return !this.checkSecurityGrant || this.grantChecked;
    }

    @Override
    public boolean grantDenied() {
        return this.checkSecurityGrant && this.grantDenied;
    }

    @Override
    public int incidentFlag() {
        return -1;
    }

    @Override
    public boolean sebVersionDenied() {
        if (this.connectionData == null) {
            return false;
        }
        return this.connectionData.clientConnection.clientVersionGranted != null &&
                !this.connectionData.clientConnection.clientVersionGranted;
    }

    @Override
    public boolean showNoGrantCheckApplied() {
        return this.checkSecurityGrant;
    }

    public void setStatusChangeListener(final Consumer<ClientConnectionData> statusChangeListener) {
        this.statusChangeListener = statusChangeListener;
    }

    public void updateData() {
        final ClientConnectionData connectionData = this.restCallBuilder
                .call()
                .get(error -> {
                    log.error("Unexpected error while trying to get current client connection data: {}",
                            error.getMessage());
                    recoverFromDisposedRestTemplate(error);
                    return null;
                });

        if (this.connectionData != null && connectionData != null) {
            this.statusChanged =
                    this.connectionData.clientConnection.status != connectionData.clientConnection.status ||
                            this.connectionData.clientConnection.securityCheckGranted != connectionData.clientConnection.securityCheckGranted;
            this.missingChanged = BooleanUtils.toBoolean(this.connectionData.missingPing) != BooleanUtils
                    .toBoolean(connectionData.missingPing);
        }
        this.connectionData = connectionData;
        if (this.connectionData == null || this.connectionData.clientConnection.securityCheckGranted == null) {
            this.grantChecked = false;
            this.grantDenied = false;
        } else {
            this.grantChecked = true;
            this.grantDenied = !this.connectionData.clientConnection.securityCheckGranted;
        }
        if (this.startTime < 0) {
            this.startTime = System.currentTimeMillis();
        }

    }

    public void updateGUI(
            final Supplier<EntityTable<ClientNotification>> notificationTableSupplier,
            final PageContext pageContext) {

        if (this.connectionData == null) {
            return;
        }

        // Note: This is to update the whole page (by reload) only when the status has changed
        //       while this page was open. This prevent constant page reloads.
        if (this.statusChanged && System.currentTimeMillis() - this.startTime > Constants.SECOND_IN_MILLIS) {
            reloadPage(pageContext);
            return;
        }

        final Form form = this.formHandle.getForm();
        form.setFieldValue(
                Domain.CLIENT_CONNECTION.ATTR_EXAM_USER_SESSION_ID,
                this.connectionData.clientConnection.userSessionId);

        form.setFieldValue(
                ClientConnection.ATTR_INFO,
                getConnectionInfo(this.connectionData.clientConnection));

        if (this.hasClientGroups
                && Constants.EMPTY_NOTE.equals(form.getFieldValue(ClientConnectionData.ATTR_CLIENT_GROUPS))) {
            form.setFieldValue(
                    ClientConnectionData.ATTR_CLIENT_GROUPS,
                    getGroupInfo());
        }

        if (this.missingChanged || this.statusChanged) {
            // update status
            String stateName = this.localizedClientConnectionStatusNameFunction.apply(this);
            if (stateName != null && getStatus().clientActiveStatus && this.grantChecked && !this.grantDenied) {
                stateName = stateName + pageContext.getI18nSupport().getText(GRANTED_TEXT);
            }
            if (stateName != null) {
                stateName = stateName.replace("&nbsp;", " ");
            }
            form.setFieldValue(Domain.CLIENT_CONNECTION.ATTR_STATUS, stateName);
            final Color statusColor = this.colorData.getStatusColor(this);
            final Color statusTextColor = this.colorData.getStatusTextColor(statusColor);
            form.setFieldColor(Domain.CLIENT_CONNECTION.ATTR_STATUS, statusColor);
            form.setFieldTextColor(Domain.CLIENT_CONNECTION.ATTR_STATUS, statusTextColor);

            if (this.statusChangeListener != null) {
                this.statusChangeListener.accept(this.connectionData);
            }
        }

        // update indicators
        this.connectionData.getIndicatorValues()
                .forEach(indValue -> {
                    final IndicatorData indData = this.indicatorMapping.get(indValue.getIndicatorId());
                    if (indData == null) {
                        return;
                    }
                    final double value = indValue.getValue();
                    final String displayValue = IndicatorValue.getDisplayValue(indValue, indData.indicator.type);

                    if (!this.connectionData.clientConnection.status.clientActiveStatus) {

                        form.setFieldValue(
                                indData.indicator.name,
                                (indData.indicator.type.showOnlyInActiveState)
                                        ? Constants.EMPTY_NOTE
                                        : displayValue);
                        form.setFieldColor(indData.indicator.name, indData.defaultColor);
                        form.setFieldTextColor(indData.indicator.name, indData.defaultTextColor);
                    } else {
                        form.setFieldValue(indData.indicator.name, displayValue);
                        final int weight = IndicatorData.getWeight(indData, value);
                        if (weight >= 0 && weight < indData.thresholdColor.length) {
                            final ThresholdColor thresholdColor = indData.thresholdColor[weight];
                            form.setFieldColor(indData.indicator.name, thresholdColor.color);
                            form.setFieldTextColor(indData.indicator.name, thresholdColor.textColor);
                        } else {
                            form.setFieldColor(indData.indicator.name, indData.defaultColor);
                            form.setFieldTextColor(indData.indicator.name, indData.defaultTextColor);
                        }
                    }
                });

        // update notifications
        final EntityTable<ClientNotification> notificationTable = notificationTableSupplier.get();
        if (notificationTable != null) {
            notificationTable.refreshPageSize();
        }
        if (this.connectionData.clientConnection.clientVersionGranted != null &&
                !this.connectionData.clientConnection.clientVersionGranted) {
            form.setFieldColor(ClientConnection.ATTR_INFO, this.colorData.color2);
            form.getFieldInput(ClientConnection.ATTR_INFO)
                    .setToolTipText(this.pageService.getI18nSupport().getText(WRONG_SEB_CLIENT_TOOLTIP));
        }
    }

    private String getConnectionInfo(final ClientConnection clientConnection) {
        return Utils.formatLineBreaks(this.pageService.getI18nSupport().getText(
                "sebserver.monitoring.exam.connection.info", "--",
                clientConnection.getSebVersion(),
                clientConnection.getSebOSName(),
                clientConnection.clientAddress));
    }

    private void reloadPage(final PageContext pageContext) {
        final PageAction pageReloadAction = this.pageService.pageActionBuilder(pageContext)
                .newAction(ActionDefinition.MONITOR_EXAM_CLIENT_CONNECTION)
                .create();
        this.pageService.firePageEvent(
                new ActionEvent(pageReloadAction),
                pageContext);
    }

    public void recoverFromDisposedRestTemplate(final Exception error) {
        if (log.isDebugEnabled()) {
            log.debug("Try to recover from disposed OAuth2 rest template...");
        }
        if (error instanceof DisposedOAuth2RestTemplateException) {
            this.pageService.getRestService().injectCurrentRestTemplate(this.restCallBuilder);
        }
    }

    private String getGroupInfo() {
        final StringBuilder sb = new StringBuilder();
        this.clientGroupMapping.keySet().stream().forEach(key -> {
            if (this.connectionData.groups.contains(key)) {
                final ClientGroup clientGroup = this.clientGroupMapping.get(key);
                sb.append(WidgetFactory.getTextWithBackgroundHTML(clientGroup.name, clientGroup.color));
            }
        });

        if (sb.length() <= 0) {
            return Constants.EMPTY_NOTE;
        } else {
            return sb.toString();
        }
    }

}
