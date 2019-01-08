/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer;

import java.util.Collection;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.pagehelper.PageHelper;

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserRecordDynamicSqlSupport.UserRecord;

@Service
public class PaginationService {

    private final int defaultPageSize;
    private final int maxPageSize;

    public PaginationService(
            @Value("${sebserver.webservice.api.pagination.defaultPageSize:10}") final int defaultPageSize,
            @Value("${sebserver.webservice.api.pagination.maxPageSize:500}") final int maxPageSize) {

        this.defaultPageSize = defaultPageSize;
        this.maxPageSize = maxPageSize;
    }

    public void setOnePageLimit(final SqlTable table) {
        setPagination(1, this.maxPageSize, null, Page.SortOrder.ASCENDING, table);
    }

    public void setOnePageLimit(final String sortBy, final SqlTable table) {
        setPagination(1, this.maxPageSize, sortBy, Page.SortOrder.ASCENDING, table);
    }

    public void setOnePageLimit(final String sortBy, final Page.SortOrder sortOrder, final SqlTable table) {
        setPagination(1, this.maxPageSize, sortBy, sortOrder, table);
    }

    public com.github.pagehelper.Page<Object> setPagination(
            final Integer pageNumber,
            final Integer pageSize,
            final String sortBy,
            final Page.SortOrder sortOrder,
            final SqlTable table) {

        final int _pageNumber = (pageNumber == null)
                ? 1
                : pageNumber;

        final int _pageSize = (pageSize == null || pageSize < 0)
                ? this.defaultPageSize
                : (pageSize > this.maxPageSize)
                        ? this.maxPageSize
                        : pageSize;

        final com.github.pagehelper.Page<Object> startPage =
                PageHelper.startPage(_pageNumber, _pageSize, true, true, false);

        final SqlColumn<?> sortColumn = verifySortColumn(sortBy, table);
        if (sortColumn != null) {
            if (sortOrder == Page.SortOrder.DESCENDING) {
                PageHelper.orderBy(sortColumn.name() + " DESC");
            } else {
                PageHelper.orderBy(sortColumn.name());
            }
        }

        return startPage;
    }

    public <T extends Entity> Page<T> getPage(
            final Integer pageNumber,
            final Integer pageSize,
            final String sortBy,
            final Page.SortOrder sortOrder,
            final SqlTable table,
            final Supplier<Collection<T>> delegate) {

        final com.github.pagehelper.Page<Object> page = setPagination(pageNumber, pageSize, sortBy, sortOrder, table);
        final Collection<T> pageList = delegate.get();
        return new Page<>(page.getPages(), pageNumber, sortBy, sortOrder, pageList);
    }

    private SqlColumn<?> verifySortColumn(final String sortBy, final SqlTable table) {
        if (table == UserRecordDynamicSqlSupport.userRecord) {
            return verifySortColumn(sortBy, UserRecordDynamicSqlSupport.userRecord);
        }

        return null;
    }

    private SqlColumn<?> verifySortColumn(final String sortBy, final UserRecord table) {

        if (StringUtils.isBlank(sortBy)) {
            return UserRecordDynamicSqlSupport.id;
        }

        if (sortBy.equals(UserRecordDynamicSqlSupport.name.name())) {
            return UserRecordDynamicSqlSupport.name;
        }

        if (sortBy.equals(UserRecordDynamicSqlSupport.username.name())) {
            return UserRecordDynamicSqlSupport.username;
        }

        if (sortBy.equals(UserRecordDynamicSqlSupport.email.name())) {
            return UserRecordDynamicSqlSupport.email;
        }

        return null;
    }

}
