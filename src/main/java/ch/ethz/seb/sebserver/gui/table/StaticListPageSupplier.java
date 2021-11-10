/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.table;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.springframework.util.MultiValueMap;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.PageSortOrder;
import ch.ethz.seb.sebserver.gbl.util.Result;

/** This implements a page supplier within a static list.
 * Currently ordering and filtering is not possible and must be implemented.
 *
 * @param <T> the type of the list/page elements */
public class StaticListPageSupplier<T> implements PageSupplier<T> {

    private final EntityType entityType;
    private final List<T> list;

    public StaticListPageSupplier(final List<T> list, final EntityType entityType) {
        this.list = list;
        this.entityType = entityType;
    }

    @Override
    public EntityType getEntityType() {
        return this.entityType;
    }

    @Override
    public Builder<T> newBuilder() {
        return new StaticListTableBuilderAdapter<>(this.list);
    }

    public static final class StaticListTableBuilderAdapter<T> implements Builder<T> {

        private final List<T> list;
        private int pageNumber;
        private int pageSize;
        private String column;
        @SuppressWarnings("unused")
        private PageSortOrder order;

        private StaticListTableBuilderAdapter(final List<T> list) {
            this.list = new ArrayList<>(list);
        }

        @Override
        public Builder<T> withPaging(final int pageNumber, final int pageSize) {
            this.pageNumber = pageNumber;
            this.pageSize = pageSize;
            return this;
        }

        @Override
        public Builder<T> withSorting(final String column, final PageSortOrder order) {
            this.column = column;
            this.order = order;
            return this;
        }

        @Override
        public Builder<T> withQueryParams(final MultiValueMap<String, String> params) {
            return this;
        }

        @Override
        public Builder<T> withQueryParam(final String name, final String value) {
            return this;
        }

        @Override
        public Builder<T> withURIVariable(final String name, final String id) {
            return this;
        }

        @Override
        public Builder<T> apply(final Function<Builder<T>, Builder<T>> f) {
            return f.apply(this);
        }

        @Override
        public Result<Page<T>> getPage() {
            return Result.tryCatch(() -> {
                if (this.list.isEmpty()) {
                    return new Page<>(0, this.pageNumber, this.column, this.list);
                }

                if (this.pageSize <= 0) {
                    return new Page<>(1, 1, this.column, this.list);
                }

                final int numOfPages = this.list.size() / this.pageSize;

                if (numOfPages <= 0) {
                    return new Page<>(1, 1, this.column, this.list);
                }

                int from = (this.pageNumber - 1) * this.pageSize;
                if (from < 0) {
                    from = 0;
                }
                int to = (this.pageNumber - 1) * this.pageSize + this.pageSize;
                if (to >= this.list.size()) {
                    to = this.list.size();
                }

                final List<T> subList = this.list.subList(from, to);
                return new Page<>(numOfPages, this.pageNumber, this.column, subList);
            });
        }
    }

}
