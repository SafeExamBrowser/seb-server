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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction.InstructionType;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCallError;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
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

    public void propagateSebQuitInstruction(
            final Long examId,
            final String connectionToken,
            final PageContext pageContext) {

        propagateSebQuitInstruction(examId, Utils.immutableSetOf(connectionToken), pageContext);
    }

    public void propagateSebQuitInstruction(
            final Long examId,
            final Set<String> connectionTokens,
            final PageContext pageContext) {

        if (examId == null || connectionTokens == null || connectionTokens.isEmpty()) {
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
                StringUtils.join(connectionTokens, Constants.COMMA),
                null);

        try {
            final String response = this.restService.getBuilder(PropagateInstruction.class)
                    .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(examId))
                    .withBody(clientInstruction)
                    .call()
                    .getOrThrow();

            if (StringUtils.isNotBlank(response)) {
                try {
                    final Collection<APIMessage> errorMessage = this.jsonMapper.readValue(
                            response,
                            new TypeReference<Collection<APIMessage>>() {
                            });

                    pageContext.notifyUnexpectedError(new RestCallError(
                            "Failed to propagate SEB quit instruction: ",
                            errorMessage));

                } catch (final Exception e) {
                    log.error("Failed to parse error response: {}", response);
                }
            }
        } catch (final Exception e) {
            log.error("Failed to propagate SEB quit instruction: ", e);
        }
    }

}
