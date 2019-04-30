/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.api;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.util.Result;

public abstract class PageToListCallAdapter<T> extends RestCall<List<T>> {

    private final static int PAGE_SIZE_OF_PAGER = 100;

    private final Class<? extends RestCall<Page<T>>> pageCallerType;

    protected PageToListCallAdapter(
            final Class<? extends RestCall<Page<T>>> pageCallerType,
            final EntityType entityType,
            final TypeReference<List<T>> typeRef,
            final String path) {

        super(new TypeKey<>(
                CallType.GET_LIST,
                entityType,
                typeRef),
                HttpMethod.GET,
                MediaType.APPLICATION_FORM_URLENCODED,
                path);

        this.pageCallerType = pageCallerType;
    }

    @Override
    protected Result<List<T>> exchange(final RestCall<List<T>>.RestCallBuilder builder) {
        return Result.tryCatch(() -> {
            final RestCall<Page<T>> pageCall = this.restService.getRestCall(this.pageCallerType);
            final List<T> collector = new ArrayList<>();
            this.collectPage(collector, pageCall, builder, 1);
            return collector;
        });
    }

    private void collectPage(
            final List<T> collector,
            final RestCall<Page<T>> pageCall,
            final RestCall<List<T>>.RestCallBuilder builder,
            final int pageNumber) {

        final RestCall<Page<T>>.RestCallBuilder newBuilder = pageCall.newBuilder(builder)
                .withPaging(pageNumber, PAGE_SIZE_OF_PAGER);
        final Page<T> page = pageCall
                .exchange(newBuilder)
                .getOrThrow();

        collector.addAll(page.content);

        if (page.getPageNumber() < page.getNumberOfPages()) {
            collectPage(collector, pageCall, builder, page.getPageNumber() + 1);
        }
    }

}
