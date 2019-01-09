/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.SqlTable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.pagehelper.PageHelper;

import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserActivityLogRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserRecordDynamicSqlSupport;

@Service
public class PaginationService {

    private final int defaultPageSize;
    private final int maxPageSize;

    private final Map<String, Map<String, String>> sortColumnMapping;

    public PaginationService(
            @Value("${sebserver.webservice.api.pagination.defaultPageSize:10}") final int defaultPageSize,
            @Value("${sebserver.webservice.api.pagination.maxPageSize:500}") final int maxPageSize) {

        this.defaultPageSize = defaultPageSize;
        this.maxPageSize = maxPageSize;
        this.sortColumnMapping = new HashMap<>();
        initSortColumnMapping();
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

        final String sortColumnName = verifySortColumnName(sortBy, table);
        if (StringUtils.isNoneBlank(sortColumnName)) {
            if (sortOrder == Page.SortOrder.DESCENDING) {
                PageHelper.orderBy(sortColumnName + " DESC");
            } else {
                PageHelper.orderBy(sortColumnName);
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

    private String verifySortColumnName(final String sortBy, final SqlTable table) {

        if (StringUtils.isBlank(sortBy)) {
            return null;
        }

        final Map<String, String> mapping = this.sortColumnMapping.get(table.name());
        if (mapping != null) {
            return mapping.get(sortBy);
        }

        return null;
    }

    // TODO is it possible to generate this within MyBatis generator?
    private void initSortColumnMapping() {
        // User Table
        final Map<String, String> userTableMap = new HashMap<>();
        userTableMap.put(Domain.USER.ATTR_NAME, UserRecordDynamicSqlSupport.name.name());
        userTableMap.put(Domain.USER.ATTR_USERNAME, UserRecordDynamicSqlSupport.username.name());
        userTableMap.put(Domain.USER.ATTR_EMAIL, UserRecordDynamicSqlSupport.email.name());
        userTableMap.put(Domain.USER.ATTR_LOCALE, UserRecordDynamicSqlSupport.locale.name());
        this.sortColumnMapping.put(UserRecordDynamicSqlSupport.userRecord.name(), userTableMap);

        // User Activity Log Table
        final Map<String, String> userActivityLogTableMap = new HashMap<>();
        userActivityLogTableMap.put(
                Domain.USER_ACTIVITY_LOG.ATTR_USER_UUID,
                UserActivityLogRecordDynamicSqlSupport.userUuid.name());
        userActivityLogTableMap.put(
                Domain.USER_ACTIVITY_LOG.ATTR_ACTIVITY_TYPE,
                UserActivityLogRecordDynamicSqlSupport.activityType.name());
        userActivityLogTableMap.put(
                Domain.USER_ACTIVITY_LOG.ATTR_ENTITY_ID,
                UserActivityLogRecordDynamicSqlSupport.entityId.name());
        userActivityLogTableMap.put(
                Domain.USER_ACTIVITY_LOG.ATTR_ENTITY_TYPE,
                UserActivityLogRecordDynamicSqlSupport.entityType.name());
        userActivityLogTableMap.put(
                Domain.USER_ACTIVITY_LOG.ATTR_TIMESTAMP,
                UserActivityLogRecordDynamicSqlSupport.timestamp.name());
        this.sortColumnMapping.put(
                UserActivityLogRecordDynamicSqlSupport.userActivityLogRecord.name(),
                userActivityLogTableMap);

    }

}
