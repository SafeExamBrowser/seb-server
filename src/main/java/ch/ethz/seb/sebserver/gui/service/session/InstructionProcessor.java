/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.session;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction.InstructionType;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCallError;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.DisableClientConnection;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.PropagateInstruction;

@Lazy
@Service
@GuiProfile
public class InstructionProcessor {

    private static final Logger log = LoggerFactory.getLogger(InstructionProcessor.class);

    private final RestService restService;
    private final JSONMapper jsonMapper;

    protected InstructionProcessor(final PageService pageService) {
        this.restService = pageService.getRestService();
        this.jsonMapper = pageService.getJSONMapper();
    }

    public void propagateSEBQuitInstruction(
            final Long examId,
            final String connectionToken,
            final PageContext pageContext) {

        propagateSEBQuitInstruction(
                examId,
                p -> Stream.of(connectionToken).collect(Collectors.toSet()),
                pageContext);

    }

    public void propagateSEBQuitInstruction(
            final Long examId,
            final Function<Predicate<ClientConnection>, Set<String>> selectionFunction,
            final PageContext pageContext) {

        final Set<String> connectionTokens = selectionFunction
                .apply(ClientConnection.getStatusPredicate(ConnectionStatus.ACTIVE));

        if (connectionTokens.isEmpty()) {
            log.warn("Empty selection");
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Propagate SEB quit instruction for exam: {} and connections: {}",
                    examId,
                    connectionTokens);
        }

        final ClientInstruction clientInstruction = new ClientInstruction(
                null,
                examId,
                InstructionType.SEB_QUIT,
                StringUtils.join(connectionTokens, Constants.LIST_SEPARATOR),
                null);

        processInstruction(() -> this.restService.getBuilder(PropagateInstruction.class)
                .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(examId))
                .withBody(clientInstruction)
                .call()
                .getOrThrow(),
                pageContext);

    }

    public void disableConnection(
            final Long examId,
            final Function<Predicate<ClientConnection>, Set<String>> selectionFunction,
            final PageContext pageContext) {

        final Set<String> connectionTokens = selectionFunction
                .apply(ClientConnection.getStatusPredicate(
                        ConnectionStatus.CONNECTION_REQUESTED,
                        ConnectionStatus.UNDEFINED,
                        ConnectionStatus.CLOSED,
                        ConnectionStatus.AUTHENTICATED));

        if (connectionTokens.isEmpty()) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Disable SEB client connections for exam: {} and connections: {}",
                    examId,
                    connectionTokens);
        }

        processInstruction(() -> this.restService.getBuilder(DisableClientConnection.class)
                .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(examId))
                .withFormParam(
                        Domain.CLIENT_CONNECTION.ATTR_CONNECTION_TOKEN,
                        StringUtils.join(connectionTokens, Constants.LIST_SEPARATOR))
                .call()
                .getOrThrow(),
                pageContext);

    }

    private void processInstruction(final Supplier<String> apiCall, final PageContext pageContext) {
        try {
            final String response = apiCall.get();

            if (StringUtils.isNotBlank(response)) {
                try {
                    final Collection<APIMessage> errorMessage = this.jsonMapper.readValue(
                            response,
                            Constants.TYPE_REFERENCE_API_MESSAGE);

                    pageContext.notifyUnexpectedError(new RestCallError(
                            "Failed to propagate SEB client instruction: ",
                            errorMessage));

                } catch (final Exception e) {
                    log.error("Failed to parse error response: {}", response);
                }
            }
        } catch (final Exception e) {
            log.error("Failed to propagate SEB client instruction: ", e);
        }
    }

}
