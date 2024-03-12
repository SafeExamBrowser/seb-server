/*
 * Copyright (c) 2020 ETH Zürich, IT Services
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

public interface PageSupplier<T> {

    EntityType getEntityType();

    Builder<T> newBuilder();

    public interface Builder<T> {
        public Builder<T> withPaging(final int pageNumber, final int pageSize);

        public Builder<T> withSorting(final String column, final PageSortOrder order);

        public Builder<T> withQueryParams(final MultiValueMap<String, String> params);

        public Builder<T> withQueryParam(String name, String value);

        public Builder<T> withURIVariable(String name, String id);

        public Builder<T> apply(final Function<Builder<T>, Builder<T>> f);

        public Result<Page<T>> getPage();

    }

}
