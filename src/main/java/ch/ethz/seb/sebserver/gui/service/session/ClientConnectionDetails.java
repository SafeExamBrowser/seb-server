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

import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gui.form.Form;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.push.ServerPushContext;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;

public class ClientConnectionDetails {

    private static final Logger log = LoggerFactory.getLogger(ClientConnectionDetails.class);

    private final static LocTextKey EXAM_NAME_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.list.column.examname");
    private final static LocTextKey CONNECTION_ID_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.list.column.id");
    private final static LocTextKey CONNECTION_ADDRESS_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.list.column.address");
    private final static LocTextKey CONNECTION_STATUS_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.connection.list.column.status");

    private static final int NUMBER_OF_NONE_INDICATOR_ROWS = 3;

    private final PageService pageService;
    private final Exam exam;
    private final EnumMap<IndicatorType, IndicatorData> indicatorMapping;
    private final RestCall<ClientConnectionData>.RestCallBuilder restCallBuilder;
    private final FormHandle<?> formhandle;
    private final StatusData statusColor;

    private ClientConnectionData connectionData = null;

    public ClientConnectionDetails(
            final PageService pageService,
            final PageContext pageContext,
            final Exam exam,
            final RestCall<ClientConnectionData>.RestCallBuilder restCallBuilder,
            final Collection<Indicator> indicators) {

        final Display display = pageContext.getRoot().getDisplay();
        this.pageService = pageService;
        this.exam = exam;
        this.restCallBuilder = restCallBuilder;
        this.statusColor = new StatusData(display);
        this.indicatorMapping = IndicatorData.createFormIndicators(
                indicators,
                display,
                NUMBER_OF_NONE_INDICATOR_ROWS);

        final FormBuilder formBuilder = this.pageService.formBuilder(pageContext, 4)
                .readonly(true)
                .addField(FormBuilder.text(
                        QuizData.QUIZ_ATTR_NAME,
                        EXAM_NAME_TEXT_KEY,
                        this.exam.getName()))
                .addField(FormBuilder.text(
                        Domain.CLIENT_CONNECTION.ATTR_EXAM_USER_SESSION_ID,
                        CONNECTION_ID_TEXT_KEY,
                        Constants.EMPTY_NOTE))
                .addField(FormBuilder.text(
                        Domain.CLIENT_CONNECTION.ATTR_CLIENT_ADDRESS,
                        CONNECTION_ADDRESS_TEXT_KEY,
                        Constants.EMPTY_NOTE))
                .withDefaultSpanInput(1)
                .addField(FormBuilder.text(
                        Domain.CLIENT_CONNECTION.ATTR_STATUS,
                        CONNECTION_STATUS_TEXT_KEY,
                        Constants.EMPTY_NOTE))
                .addEmptyCell();

        this.indicatorMapping
                .values()
                .stream()
                .forEach(indData -> {
                    formBuilder.addField(FormBuilder.text(
                            indData.indicator.name,
                            new LocTextKey(indData.indicator.name),
                            Constants.EMPTY_NOTE)
                            .withDefaultLabel(indData.indicator.name))
                            .addEmptyCell();
                });

        this.formhandle = formBuilder.build();

    }

    public void updateData(final ServerPushContext context) {
        this.connectionData = this.restCallBuilder
                .call()
                .get(error -> {
                    log.error("Unexpected error while trying to get current client connection data: ", error);
                    return null;
                });
    }

    public void updateGUI(final ServerPushContext context) {
        if (this.connectionData == null) {
            return;
        }

        final Form form = this.formhandle.getForm();
        form.setFieldValue(
                Domain.CLIENT_CONNECTION.ATTR_EXAM_USER_SESSION_ID,
                this.connectionData.clientConnection.userSessionId);

        form.setFieldValue(
                Domain.CLIENT_CONNECTION.ATTR_CLIENT_ADDRESS,
                this.connectionData.clientConnection.clientAddress);

        // update status
        form.setFieldValue(
                Domain.CLIENT_CONNECTION.ATTR_STATUS,
                getStatusName());
        form.setFieldColor(
                Domain.CLIENT_CONNECTION.ATTR_STATUS,
                this.statusColor.getStatusColor(this.connectionData));

        // update indicators
        this.connectionData.getIndicatorValues()
                .stream()
                .forEach(indValue -> {
                    final IndicatorData indData = this.indicatorMapping.get(indValue.getType());
                    final double value = indValue.getValue();
                    final int colorIndex = IndicatorData.getColorIndex(indData, value);
                    if (this.connectionData.clientConnection.status != ConnectionStatus.ESTABLISHED) {
                        form.setFieldValue(indData.indicator.name, Constants.EMPTY_NOTE);
                        form.setFieldColor(indData.indicator.name, indData.defaultColor);
                    } else {
                        form.setFieldValue(indData.indicator.name, String.valueOf(value));
                        form.setFieldColor(indData.indicator.name, indData.thresholdColor[colorIndex].color);
                    }
                });
    }

    String getStatusName() {
        return this.pageService.getResourceService().localizedClientConnectionStatusName(
                (this.connectionData != null && this.connectionData.clientConnection != null)
                        ? this.connectionData.clientConnection.status
                        : ConnectionStatus.UNDEFINED);
    }

}
