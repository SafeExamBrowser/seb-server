/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.util.Utils;

public final class Page<T> {

    public enum SortOrder {
        ASCENDING,
        DESCENDING
    }

    public static final String ATTR_NUMBER_OF_PAGES = "number_of_pages";
    public static final String ATTR_PAGE_NUMBER = "page_number";
    public static final String ATTR_PAGE_SIZE = "page_size";
    public static final String ATTR_SORT_BY = "sort_by";
    public static final String ATTR_SORT_ORDER = "sort_order";
    public static final String ATTR_CONTENT = "content";

    @JsonProperty(ATTR_NUMBER_OF_PAGES)
    public final Integer numberOfPages;
    @JsonProperty(ATTR_PAGE_NUMBER)
    public final Integer pageNumber;
    @JsonProperty(ATTR_PAGE_SIZE)
    public final Integer pageSize;
    @JsonProperty(ATTR_SORT_BY)
    public final String sortBy;
    @JsonProperty(ATTR_SORT_ORDER)
    public final SortOrder sortOrder;

    @JsonProperty(ATTR_CONTENT)
    public final Collection<T> content;

    @JsonCreator
    public Page(
            @JsonProperty(ATTR_NUMBER_OF_PAGES) final Integer numberOfPages,
            @JsonProperty(ATTR_PAGE_NUMBER) final Integer pageNumber,
            @JsonProperty(ATTR_SORT_BY) final String sortBy,
            @JsonProperty(ATTR_SORT_ORDER) final SortOrder sortOrder,
            @JsonProperty(ATTR_CONTENT) final Collection<T> content) {

        this.numberOfPages = numberOfPages;
        this.pageNumber = pageNumber;
        this.content = Utils.immutableCollectionOf(content);
        this.pageSize = content.size();
        this.sortBy = sortBy;
        this.sortOrder = sortOrder;
    }

    public Integer getNumberOfPages() {
        return this.numberOfPages;
    }

    public Integer getPageNumber() {
        return this.pageNumber;
    }

    public Integer getPageSize() {
        return this.pageSize;
    }

    public String getSortBy() {
        return this.sortBy;
    }

    public SortOrder getSortOrder() {
        return this.sortOrder;
    }

    public Collection<T> getContent() {
        return this.content;
    }

}
