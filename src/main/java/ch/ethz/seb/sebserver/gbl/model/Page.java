/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model;

import java.util.Collection;
import java.util.List;

import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.util.Utils;
import io.swagger.v3.oas.annotations.media.Schema;

/** Data class that defines a Page that corresponds with the SEB Server API page JSON object
 *
 * @param <T> The type of a page entry entity */
public final class Page<T> {

    public static final String ATTR_NAMES_ONLY = "names_only";
    public static final String ATTR_NUMBER_OF_PAGES = "number_of_pages";
    public static final String ATTR_PAGE_NUMBER = "page_number";
    public static final String ATTR_PAGE_SIZE = "page_size";
    public static final String ATTR_SORT = "sort";
    public static final String ATTR_COMPLETE = "complete";
    public static final String ATTR_CONTENT = "content";

    @Schema(description = "The number of available pages for the specified page size.")
    @JsonProperty(ATTR_NUMBER_OF_PAGES)
    public final int numberOfPages;

    @Schema(description = "The actual page number. Starting with 1.")
    @Size(min = 1)
    @JsonProperty(ATTR_PAGE_NUMBER)
    public final int pageNumber;

    @Schema(description = "The the actual size of a page")
    @Size(min = 1)
    @JsonProperty(ATTR_PAGE_SIZE)
    public final int pageSize;

    @Schema(description = "The page sort column name", nullable = true)
    @JsonProperty(ATTR_SORT)
    public final String sort;

    @Schema(description = "The actual content objects of the page. Might be empty.", nullable = false)
    @JsonProperty(ATTR_CONTENT)
    public final List<T> content;

    @JsonProperty(ATTR_COMPLETE)
    public final boolean complete;

    @JsonCreator
    public Page(
            @JsonProperty(value = ATTR_NUMBER_OF_PAGES, required = true) final int numberOfPages,
            @JsonProperty(value = ATTR_PAGE_NUMBER, required = true) final int pageNumber,
            @JsonProperty(value = ATTR_PAGE_SIZE, required = true) final int pageSize,
            @JsonProperty(ATTR_SORT) final String sort,
            @JsonProperty(ATTR_CONTENT) final Collection<T> content,
            @JsonProperty(ATTR_COMPLETE) final boolean complet) {

        this.numberOfPages = numberOfPages;
        this.pageNumber = pageNumber;
        this.content = Utils.immutableListOf(content);
        this.pageSize = pageSize;
        this.sort = sort;
        this.complete = complet;
    }

    public Page(
            final int numberOfPages,
            final int pageNumber,
            final int pageSize,
            final String sort,
            final Collection<T> content) {

        this.numberOfPages = numberOfPages;
        this.pageNumber = pageNumber;
        this.content = Utils.immutableListOf(content);
        this.pageSize = pageSize;
        this.sort = sort;
        this.complete = true;
    }

    public int getNumberOfPages() {
        return this.numberOfPages;
    }

    public int getPageNumber() {
        return this.pageNumber;
    }

    public int getPageSize() {
        return this.pageSize;
    }

    public boolean isComplete() {
        return this.complete;
    }

    public Collection<T> getContent() {
        return this.content;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return this.content == null || this.content.isEmpty();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Page [numberOfPages=");
        builder.append(this.numberOfPages);
        builder.append(", pageNumber=");
        builder.append(this.pageNumber);
        builder.append(", pageSize=");
        builder.append(this.pageSize);
        builder.append(", sort=");
        builder.append(this.sort);
        builder.append(", content=");
        builder.append(this.content);
        builder.append("]");
        return builder.toString();
    }

}
