/*
 * Copyright (c) 2024 ETH Zürich, IT Services / Informatikdienste (ID)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.async.AsyncServiceSpringConfig;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.SEBClientConfigDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientConnectionService;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.exam.endpoint.discovery}")
@ConditionalOnExpression("'${sebserver.webservice.light.setup}'.equals('true')")
public class LightController {

    private final SEBClientConnectionService sebClientConnectionService;
    private final SEBClientConfigDAO sebClientConfigDAO;
    private final AdditionalAttributesDAO additionalAttributesDAO;
    private final Executor executor;

    protected LightController(
            final SEBClientConnectionService sebClientConnectionService,
            final SEBClientConfigDAO sebClientConfigDAO,
            final AdditionalAttributesDAO additionalAttributesDAO,
            @Qualifier(AsyncServiceSpringConfig.EXAM_API_EXECUTOR_BEAN_NAME) final Executor executor) {

        this.sebClientConnectionService = sebClientConnectionService;
        this.sebClientConfigDAO = sebClientConfigDAO;
        this.additionalAttributesDAO = additionalAttributesDAO;
        this.executor = executor;
    }

    //this.examAPI_V1_Endpoint + API.EXAM_API_CONFIGURATION_LIGHT_ENDPOINT
    //http://localhost:8080/exam-api/discovery/light-config
    @RequestMapping(
            path =  API.EXAM_API_CONFIGURATION_LIGHT_ENDPOINT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public CompletableFuture<Void> getLightConfig(
            final HttpServletRequest request,
            final HttpServletResponse response){

        //temp solution: get first active seb client config we can get -->
        //in a light setup there should be only one setup so this step is not necessary and we can just use the first and only item in the db
        String modelId = getSebClientConfigId();

        return CompletableFuture.runAsync(
                () -> {
                    try {
                        this.sebClientConnectionService.streamLightExamConfig(modelId, response);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                this.executor
        );

    }

    @RequestMapping(
            path =  API.EXAM_API_CONFIGURATION_LIGHT_ENDPOINT + API.PASSWORD_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public UsernamePasswordView getInitialAdminPassword(
            final HttpServletRequest request,
            final HttpServletResponse response){

        try {
            final String username = this.additionalAttributesDAO.getAdditionalAttribute(EntityType.USER, 2L, Domain.USER.ATTR_USERNAME)
                    .getOrThrow()
                    .getValue();

            final String password = this.additionalAttributesDAO.getAdditionalAttribute(EntityType.USER, 2L, Domain.USER.ATTR_PASSWORD)
                    .getOrThrow()
                    .getValue();

            return new UsernamePasswordView(
                    username,
                    password
            );

        } catch (final Exception e) {
            return new UsernamePasswordView(
                    "initial username missing or changed",
                    "inital password missing or changed"
            );
        }
    }

    private String getSebClientConfigId() {
        return this.sebClientConfigDAO
                .allMatching(
                        new FilterMap().putIfAbsent(
                                "active",
                                String.valueOf(true)
                        ),
                        Utils.truePredicate()
                )
                .getOrThrow()
                .stream()
                .toList()
                .get(0)
                .getModelId();
    }

}

record UsernamePasswordView(String username, String password) {
    @JsonCreator
    UsernamePasswordView {
    }
}