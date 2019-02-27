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
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.InstitutionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserActivityLogRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserRecordDynamicSqlSupport;

@Lazy
@Service
@WebServiceProfile
public class PaginationService {

    public enum SortOrder {
        ASCENDING,
        DESCENDING;

        public final static String DESCENDING_PREFIX = "-";

        public String encode(final String sort) {
            return (this == DESCENDING) ? DESCENDING_PREFIX + sort : sort;
        }

        public static String decode(final String sort) {
            return (sort != null && sort.startsWith(DESCENDING_PREFIX))
                    ? sort.substring(1)
                    : sort;
        }

        public static SortOrder getSortOrder(final String encoded) {
            return (encoded != null && encoded.startsWith(DESCENDING_PREFIX))
                    ? SortOrder.DESCENDING
                    : SortOrder.ASCENDING;
        }
    }

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

    /** Use this to set a page limitation on SQL level. This checks first if there is
     * already a page-limitation set for the local thread and if not, set the default page-limitation */
    public void setDefaultLimitIfNotSet() {
        if (PageHelper.getLocalPage() != null) {
            return;
        }
        setPagination(1, this.maxPageSize, null, null);
    }

    public void setDefaultLimit() {
        setPagination(1, this.maxPageSize, null, null);
    }

    public void setDefaultLimit(final String sort, final SqlTable table) {
        setPagination(1, this.maxPageSize, sort, table);
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
            final String sort,
            final SqlTable table) {

        final com.github.pagehelper.Page<Object> startPage =
                PageHelper.startPage(getPageNumber(pageNumber), getPageSize(pageSize), true, true, false);

        if (table != null && StringUtils.isNoneBlank(sort)) {
            final SortOrder sortOrder = SortOrder.getSortOrder(sort);
            final String sortColumnName = verifySortColumnName(sort, table);
            if (StringUtils.isNoneBlank(sortColumnName)) {
                switch (sortOrder) {
                    case DESCENDING: {
                        PageHelper.orderBy(sortColumnName + " DESC");
                        break;
                    }
                    default: {
                        PageHelper.orderBy(sortColumnName);
                    }
                }
            }
        }

        return startPage;
    }

    public <T extends Entity> Result<Page<T>> getPage(
            final Integer pageNumber,
            final Integer pageSize,
            final String sort,
            final SqlTable table,
            final Supplier<Result<Collection<T>>> delegate) {

        return Result.tryCatch(() -> {
            final com.github.pagehelper.Page<Object> page =
                    setPagination(pageNumber, pageSize, sort, table);
            final Collection<T> pageList = delegate.get().getOrThrow();
            return new Page<>(page.getPages(), page.getPageNum(), sort, pageList);
        });
    }

    private String verifySortColumnName(final String sort, final SqlTable table) {

        if (StringUtils.isBlank(sort)) {
            return this.defaultSortColumn.get(table.name());
        }

        final Map<String, String> mapping = this.sortColumnMapping.get(table.name());
        if (mapping != null) {
            final String sortColumn = SortOrder.decode(sort);
            if (StringUtils.isBlank(sortColumn)) {
                return this.defaultSortColumn.get(table.name());
            }
            return mapping.get(sortColumn);
        }

        return this.defaultSortColumn.get(table.name());
    }

    // TODO is it possible to generate this within MyBatis generator?
    private void initSortColumnMapping() {

        // Institution Table
        final Map<String, String> institutionTableMap = new HashMap<>();
        institutionTableMap.put(
                Domain.INSTITUTION.ATTR_NAME,
                InstitutionRecordDynamicSqlSupport.name.name());
        institutionTableMap.put(
                Domain.INSTITUTION.ATTR_URL_SUFFIX,
                InstitutionRecordDynamicSqlSupport.urlSuffix.name());
        institutionTableMap.put(
                Domain.INSTITUTION.ATTR_ACTIVE,
                InstitutionRecordDynamicSqlSupport.active.name());
        this.sortColumnMapping.put(
                InstitutionRecordDynamicSqlSupport.institutionRecord.name(),
                institutionTableMap);
        this.defaultSortColumn.put(
                InstitutionRecordDynamicSqlSupport.institutionRecord.name(),
                Domain.INSTITUTION.ATTR_ID);

        // User Table
        final Map<String, String> userTableMap = new HashMap<>();
        userTableMap.put(Domain.USER.ATTR_NAME, UserRecordDynamicSqlSupport.name.name());
        userTableMap.put(Domain.USER.ATTR_USERNAME, UserRecordDynamicSqlSupport.username.name());
        userTableMap.put(Domain.USER.ATTR_EMAIL, UserRecordDynamicSqlSupport.email.name());
        userTableMap.put(Domain.USER.ATTR_LANGUAGE, UserRecordDynamicSqlSupport.language.name());
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
        this.sortColumnMapping.put(
                ExamRecordDynamicSqlSupport.examRecord.name(),
                examTableMap);
        this.defaultSortColumn.put(
                ExamRecordDynamicSqlSupport.examRecord.name(),
                Domain.EXAM.ATTR_ID);

    }

}
