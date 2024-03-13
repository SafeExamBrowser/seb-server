/*
 * Copyright (c) 2020 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.springframework.util.MultiValueMap;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.PageSortOrder;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;

public class RemoteListPageSupplier<T> implements PageSupplier<T> {

    private final EntityType entityType;
    private final RestCall<Collection<T>> restCall;

    public RemoteListPageSupplier(final RestCall<Collection<T>> restCall, final EntityType entityType) {
        this.restCall = restCall;
        this.entityType = entityType;
    }

    @Override
    public EntityType getEntityType() {
        return this.entityType;
    }

    @Override
    public Builder<T> newBuilder() {
        return new StaticListTableBuilderAdapter<>(this.restCall);
    }

    public static final class StaticListTableBuilderAdapter<T> implements Builder<T> {

        private final RestCall<Collection<T>>.RestCallBuilder restCallBuilder;
        private int pageNumber;
        private int pageSize;
        private String column;

        private StaticListTableBuilderAdapter(final RestCall<Collection<T>> restCall) {
            this.restCallBuilder = restCall.newBuilder();
        }

        @Override
        public Builder<T> withPaging(final int pageNumber, final int pageSize) {
            this.pageNumber = pageNumber;
            this.pageSize = pageSize;
            return this;
        }

        @Override
        public Builder<T> withSorting(final String column, final PageSortOrder order) {
            this.restCallBuilder.withSorting(column, order);
            this.column = column;
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
            return Result.tryCatch(() -> {

                final Collection<T> collection = this.restCallBuilder.call().getOrThrow();
                final List<T> list = (collection == null || collection.isEmpty())
                        ? Collections.emptyList()
                        : new ArrayList<>(collection);

                if (list.isEmpty()) {
                    return new Page<>(0, this.pageNumber, this.pageSize, this.column, list);
                }

                if (this.pageSize <= 0) {
                    return new Page<>(1, 1, list.size(), this.column, list);
                }

                final int numOfPages = list.size() / this.pageSize;
                if (numOfPages <= 0) {
                    return new Page<>(1, 1, this.pageSize, this.column, list);
                }

                final List<T> subList = list.subList(this.pageNumber * this.pageSize,
                        this.pageNumber * this.pageSize + this.pageSize);
                return new Page<>(numOfPages, this.pageNumber, this.pageSize, this.column, subList);
            });
        }
    }

}
