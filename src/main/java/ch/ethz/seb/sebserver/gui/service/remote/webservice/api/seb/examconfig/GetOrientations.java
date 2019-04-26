/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;

@Lazy
@Component
@GuiProfile
public class GetOrientations extends RestCall<List<Orientation>> {

    private final GetOrientationPage getOrientationPage;

    public GetOrientations() {
        super(new TypeKey<>(
                CallType.GET_LIST,
                EntityType.ORIENTATION,
                new TypeReference<List<Orientation>>() {
                }),
                HttpMethod.GET,
                MediaType.APPLICATION_FORM_URLENCODED,
                API.ORIENTATION_ENDPOINT);

        this.getOrientationPage = new GetOrientationPage();
    }

    @Override
    protected RestCall<List<Orientation>> init(final RestService restService, final JSONMapper jsonMapper) {
        this.getOrientationPage.init(restService, jsonMapper);
        return super.init(restService, jsonMapper);
    }

    @Override
    protected Result<List<Orientation>> exchange(final RestCall<List<Orientation>>.RestCallBuilder builder) {
        return Result.tryCatch(() -> {
            final List<Orientation> collector = new ArrayList<>();
            collectPage(collector, this.getOrientationPage.newBuilder(builder), 1);
            return collector;
        });
    }

    private void collectPage(
            final List<Orientation> collector,
            final RestCall<Page<Orientation>>.RestCallBuilder builder,
            final int pageNumber) {

        final RestCall<Page<Orientation>>.RestCallBuilder builderWithPaging = builder.withPaging(pageNumber, 100);
        final Page<Orientation> page = this.getOrientationPage
                .exchange(builderWithPaging)
                .getOrThrow();

        collector.addAll(page.content);

        if (page.getPageNumber() < page.getNumberOfPages()) {
            collectPage(collector, builder, page.getPageNumber() + 1);
        }
    }

    private final class GetOrientationPage extends RestCall<Page<Orientation>> {
        public GetOrientationPage() {
            super(new TypeKey<>(
                    CallType.GET_PAGE,
                    EntityType.ORIENTATION,
                    new TypeReference<Page<Orientation>>() {
                    }),
                    HttpMethod.GET,
                    MediaType.APPLICATION_FORM_URLENCODED,
                    API.ORIENTATION_ENDPOINT);
        }

        public RestCall<Page<Orientation>>.RestCallBuilder newBuilder(
                final RestCall<List<Orientation>>.RestCallBuilder builder) {
            return new RestCallBuilder(builder);
        }

        @Override
        protected RestCall<Page<Orientation>> init(final RestService restService, final JSONMapper jsonMapper) {
            return super.init(restService, jsonMapper);
        }

        @Override
        protected Result<Page<Orientation>> exchange(final RestCall<Page<Orientation>>.RestCallBuilder builder) {
            return super.exchange(builder);
        }
    }

}
