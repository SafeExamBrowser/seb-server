/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.session;

import java.util.Collection;
import java.util.EnumMap;

import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.IndicatorValue;
import ch.ethz.seb.sebserver.gui.form.Form;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.session.IndicatorData.ThresholdColor;

public class ClientConnectionDetails {

    private static final Logger log = LoggerFactory.getLogger(ClientConnectionDetails.class);

    private final static LocTextKey EXAM_NAME_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.form.exam");
    private final static LocTextKey CONNECTION_ID_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.form.id");
    private final static LocTextKey CONNECTION_ADDRESS_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.form.address");
    private final static LocTextKey CONNECTION_STATUS_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.form.status");

    private static final int NUMBER_OF_NONE_INDICATOR_ROWS = 3;

    private final ResourceService resourceService;
    private final EnumMap<IndicatorType, IndicatorData> indicatorMapping;
    private final RestCall<ClientConnectionData>.RestCallBuilder restCallBuilder;
    private final FormHandle<?> formHandle;
    private final ColorData colorData;

    private ClientConnectionData connectionData = null;
    private boolean statusChanged = true;

    public ClientConnectionDetails(
            final PageService pageService,
            final PageContext pageContext,
            final Exam exam,
            final RestCall<ClientConnectionData>.RestCallBuilder restCallBuilder,
            final Collection<Indicator> indicators) {

        final Display display = pageContext.getRoot().getDisplay();

        this.resourceService = pageService.getResourceService();
        this.restCallBuilder = restCallBuilder;
        this.colorData = new ColorData(display);
        this.indicatorMapping = IndicatorData.createFormIndicators(
                indicators,
                display,
                this.colorData,
                NUMBER_OF_NONE_INDICATOR_ROWS);

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
                .addField(FormBuilder.text(
                        Domain.CLIENT_CONNECTION.ATTR_CLIENT_ADDRESS,
                        CONNECTION_ADDRESS_TEXT_KEY,
                        Constants.EMPTY_NOTE))
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
    }

    public void updateData() {
        final ClientConnectionData connectionData = this.restCallBuilder
                .call()
                .get(error -> {
                    log.error("Unexpected error while trying to get current client connection data: ", error);
                    return null;
                });

        if (this.connectionData != null && connectionData != null) {
            this.statusChanged =
                    this.connectionData.clientConnection.status != connectionData.clientConnection.status ||
                            BooleanUtils.toBoolean(this.connectionData.missingPing) !=
                                    BooleanUtils.toBoolean(connectionData.missingPing);
        }
        this.connectionData = connectionData;
    }

    public void updateGUI() {
        if (this.connectionData == null) {
            return;
        }

        final Form form = this.formHandle.getForm();
        form.setFieldValue(
                Domain.CLIENT_CONNECTION.ATTR_EXAM_USER_SESSION_ID,
                this.connectionData.clientConnection.userSessionId);

        form.setFieldValue(
                Domain.CLIENT_CONNECTION.ATTR_CLIENT_ADDRESS,
                this.connectionData.clientConnection.clientAddress);

        if (this.statusChanged) {
            // update status
            form.setFieldValue(
                    Domain.CLIENT_CONNECTION.ATTR_STATUS,
                    this.resourceService.localizedClientConnectionStatusName(this.connectionData));
            final Color statusColor = this.colorData.getStatusColor(this.connectionData);
            final Color statusTextColor = this.colorData.getStatusTextColor(statusColor);
            form.setFieldColor(Domain.CLIENT_CONNECTION.ATTR_STATUS, statusColor);
            form.setFieldTextColor(Domain.CLIENT_CONNECTION.ATTR_STATUS, statusTextColor);
        }

        // update indicators
        this.connectionData.getIndicatorValues()
                .forEach(indValue -> {
                    final IndicatorData indData = this.indicatorMapping.get(indValue.getType());
                    final double value = indValue.getValue();
                    final String displayValue = IndicatorValue.getDisplayValue(indValue);

                    if (!this.connectionData.clientConnection.status.establishedStatus) {

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
    }

}
