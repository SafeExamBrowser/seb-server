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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.github.pagehelper.PageHelper;

import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserActivityLogRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserRecordDynamicSqlSupport;

@Lazy
@Service
@WebServiceProfile
public class PaginationService {

    private final int defaultPageSize;
    private final int maxPageSize;

    private final Map<String, Map<String, String>> sortColumnMapping;
    private final Map<String, String> defaultSortColumn;

    public PaginationService(
            @Value("${sebserver.webservice.api.pagination.defaultPageSize:10}") final int defaultPageSize,
            @Value("${sebserver.webservice.api.pagination.maxPageSize:500}") final int maxPageSize) {

        this.defaultPageSize = defaultPageSize;
        this.maxPageSize = maxPageSize;
        this.sortColumnMapping = new HashMap<>();
        this.defaultSortColumn = new HashMap<>();
        initSortColumnMapping();
    }

    /** Use this to verify whether native sorting (on SQL level) is supported for a given orderBy column
     * and a given SqlTable or not.
     *
     * @param table SqlTable the SQL table (MyBatis)
     * @param orderBy the orderBy columnName
     * @return true if there is native sorting support for the given attributes */
    public boolean isNativeSortingSupported(final SqlTable table, final String orderBy) {
        if (StringUtils.isBlank(orderBy)) {
            return false;
        }

        final Map<String, String> tableMap = this.sortColumnMapping.get(table.name());
        if (tableMap == null) {
            return false;
        }

        return tableMap.containsKey(orderBy);
    }

    public void setDefaultLimitOfNotSet(final SqlTable table) {
        if (PageHelper.getLocalPage() != null) {
            return;
        }
        setPagination(1, this.maxPageSize, null, Page.SortOrder.ASCENDING, table);
    }

    public void setDefaultLimit(final SqlTable table) {
        setPagination(1, this.maxPageSize, null, Page.SortOrder.ASCENDING, table);
    }

    public void setDefaultLimit(final String sortBy, final SqlTable table) {
        setPagination(1, this.maxPageSize, sortBy, Page.SortOrder.ASCENDING, table);
    }

    public void setDefaultLimit(final String sortBy, final Page.SortOrder sortOrder, final SqlTable table) {
        setPagination(1, this.maxPageSize, sortBy, sortOrder, table);
    }

    public int getPageNumber(final Integer pageNumber) {
        return (pageNumber == null)
                ? 1
                : pageNumber;
    }

    public int getPageSize(final Integer pageSize) {
        return (pageSize == null || pageSize < 0)
                ? this.defaultPageSize
                : (pageSize > this.maxPageSize)
                        ? this.maxPageSize
                        : pageSize;
    }

    public com.github.pagehelper.Page<Object> setPagination(
            final Integer pageNumber,
            final Integer pageSize,
            final String sortBy,
            final Page.SortOrder sortOrder,
            final SqlTable table) {

        final com.github.pagehelper.Page<Object> startPage =
                PageHelper.startPage(getPageNumber(pageNumber), getPageSize(pageSize), true, true, false);

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
        return new Page<>(page.getPages(), page.getPageNum(), sortBy, sortOrder, pageList);
    }

    private String verifySortColumnName(final String sortBy, final SqlTable table) {

        if (StringUtils.isBlank(sortBy)) {
            return this.defaultSortColumn.get(table.name());
        }

        final Map<String, String> mapping = this.sortColumnMapping.get(table.name());
        if (mapping != null) {
            return mapping.get(sortBy);
        }

        return this.defaultSortColumn.get(table.name());
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
        this.defaultSortColumn.put(UserRecordDynamicSqlSupport.userRecord.name(), Domain.USER.ATTR_ID);

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
        this.defaultSortColumn.put(
                UserActivityLogRecordDynamicSqlSupport.userActivityLogRecord.name(),
                Domain.USER_ACTIVITY_LOG.ATTR_ID);

        // Exam Table
        final Map<String, String> examTableMap = new HashMap<>();
        examTableMap.put(
                Domain.EXAM.ATTR_INSTITUTION_ID,
                ExamRecordDynamicSqlSupport.institutionId.name());
        examTableMap.put(
                Domain.EXAM.ATTR_LMS_SETUP_ID,
                ExamRecordDynamicSqlSupport.lmsSetupId.name());
        examTableMap.put(
                Domain.EXAM.ATTR_TYPE,
                ExamRecordDynamicSqlSupport.type.name());
        examTableMap.put(
                Domain.EXAM.ATTR_STATUS,
                ExamRecordDynamicSqlSupport.status.name());

    }

}
