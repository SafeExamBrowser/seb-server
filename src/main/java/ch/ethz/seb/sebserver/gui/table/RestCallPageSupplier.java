/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.table;

import java.util.function.Function;

import org.springframework.util.MultiValueMap;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.PageSortOrder;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;

public class RestCallPageSupplier<T> implements PageSupplier<T> {

    private final RestCall<Page<T>> restCall;

    protected RestCallPageSupplier(final RestCall<Page<T>> restCall) {
        this.restCall = restCall;
    }

    @Override
    public EntityType getEntityType() {
        return this.restCall.getEntityType();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Builder<T> newBuilder() {
        return new RestCallBuilderAdapter(this.restCall.newBuilder());
    }

    public static final class RestCallBuilderAdapter<T> implements Builder<T> {

        final RestCall<Page<T>>.RestCallBuilder restCallBuilder;

        private RestCallBuilderAdapter(final RestCall<Page<T>>.RestCallBuilder restCallBuilder) {
            this.restCallBuilder = restCallBuilder;
        }

        @Override
        public Builder<T> withPaging(final int pageNumber, final int pageSize) {
            this.restCallBuilder.withPaging(pageNumber, pageSize);
            return this;
        }

        @Override
        public Builder<T> withSorting(final String column, final PageSortOrder order) {
            this.restCallBuilder.withSorting(column, order);
            return this;
        }

        @Override
        public Builder<T> withQueryParams(final MultiValueMap<String, String> params) {
            this.restCallBuilder.withQueryParams(params);
            return this;
        }

        @Override
        public Builder<T> withQueryParam(final String name, final String value) {
            this.restCallBuilder.withQueryParam(name, value);
            return this;
        }

        @Override
        public Builder<T> withURIVariable(final String name, final String id) {
            this.restCallBuilder.withURIVariable(name, id);
            return this;
        }

        @Override
        public Builder<T> apply(final Function<Builder<T>, Builder<T>> f) {
            return f.apply(this);
        }

        @Override
        public Result<Page<T>> getPage() {
            return this.restCallBuilder.call();
        }
    }

}
