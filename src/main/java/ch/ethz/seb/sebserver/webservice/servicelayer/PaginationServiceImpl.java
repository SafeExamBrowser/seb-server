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
import ch.ethz.seb.sebserver.gbl.model.PageSortOrder;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ConfigurationNodeRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.ExamRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.InstitutionRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.LmsSetupRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.SebClientConfigRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserActivityLogRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.UserRecordDynamicSqlSupport;

@Lazy
@Service
@WebServiceProfile
public class PaginationServiceImpl implements PaginationService {

    private final int defaultPageSize;
    private final int maxPageSize;

    private final Map<String, Map<String, String>> sortColumnMapping;
    private final Map<String, String> defaultSortColumn;

    public PaginationServiceImpl(
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
    @Override
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
    @Override
    public void setDefaultLimitIfNotSet() {
        if (PageHelper.getLocalPage() != null) {
            return;
        }
        setPagination(1, this.maxPageSize, null, null);
    }

    @Override
    public void setDefaultLimit() {
        setPagination(1, this.maxPageSize, null, null);
    }

    @Override
    public void setDefaultLimit(final String sort, final SqlTable table) {
        setPagination(1, this.maxPageSize, sort, table);
    }

    @Override
    public int getPageNumber(final Integer pageNumber) {
        return (pageNumber == null)
                ? 1
                : pageNumber;
    }

    /** Get the given pageSize as int type if it is not null and in the range of one to the defined maximum page size.
     * If the given pageSize null or less then one, this returns the defined default page size.
     * If the given pageSize is greater then the defined maximum page size this returns the the defined maximum page
     * size
     *
     * @param pageSize the page size Integer value to convert
     * @return the given pageSize as int type if it is not null and in the range of one to the defined maximum page
     *         size, */
    @Override
    public int getPageSize(final Integer pageSize) {
        return (pageSize == null || pageSize < 1)
                ? this.defaultPageSize
                : (pageSize > this.maxPageSize)
                        ? this.maxPageSize
                        : pageSize;
    }

    @Override
    public <T extends Entity> Result<Page<T>> getPage(
            final Integer pageNumber,
            final Integer pageSize,
            final String sort,
            final String tableName,
            final Supplier<Result<Collection<T>>> delegate) {

        return Result.tryCatch(() -> {
            final SqlTable table = SqlTable.of(tableName);
            final com.github.pagehelper.Page<Object> page =
                    setPagination(pageNumber, pageSize, sort, table);

            final Collection<T> list = delegate.get().getOrThrow();

            return new Page<>(
                    page.getPages(),
                    page.getPageNum(),
                    sort,
                    list);
        });
    }

    private String verifySortColumnName(final String sort, final SqlTable table) {

        if (StringUtils.isBlank(sort)) {
            return this.defaultSortColumn.get(table.name());
        }

        final Map<String, String> mapping = this.sortColumnMapping.get(table.name());
        if (mapping != null) {
            final String sortColumn = PageSortOrder.decode(sort);
            if (StringUtils.isBlank(sortColumn)) {
                return this.defaultSortColumn.get(table.name());
            }
            return mapping.get(sortColumn);
        }

        return this.defaultSortColumn.get(table.name());
    }

    private com.github.pagehelper.Page<Object> setPagination(
            final Integer pageNumber,
            final Integer pageSize,
            final String sort,
            final SqlTable table) {

        final com.github.pagehelper.Page<Object> startPage =
                PageHelper.startPage(getPageNumber(pageNumber), getPageSize(pageSize), true, true, false);

        if (table != null && StringUtils.isNotBlank(sort)) {
            final PageSortOrder sortOrder = PageSortOrder.getSortOrder(sort);
            final String sortColumnName = verifySortColumnName(sort, table);
            if (StringUtils.isNotBlank(sortColumnName)) {
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

    private void initSortColumnMapping() {

        // define and initialize sort column mapping for...

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

        // LMS Setup Table
        final Map<String, String> lmsSetupTableMap = new HashMap<>();
        lmsSetupTableMap.put(Domain.LMS_SETUP.ATTR_NAME, LmsSetupRecordDynamicSqlSupport.name.name());
        lmsSetupTableMap.put(Domain.LMS_SETUP.ATTR_LMS_TYPE, LmsSetupRecordDynamicSqlSupport.lmsType.name());
        this.sortColumnMapping.put(LmsSetupRecordDynamicSqlSupport.lmsSetupRecord.name(), lmsSetupTableMap);
        this.defaultSortColumn.put(LmsSetupRecordDynamicSqlSupport.lmsSetupRecord.name(), Domain.LMS_SETUP.ATTR_ID);

        // Exam Table
        final Map<String, String> examTableMap = new HashMap<>();
        examTableMap.put(
                Domain.EXAM.ATTR_TYPE,
                ExamRecordDynamicSqlSupport.type.name());
        this.sortColumnMapping.put(
                ExamRecordDynamicSqlSupport.examRecord.name(),
                examTableMap);
        this.defaultSortColumn.put(
                ExamRecordDynamicSqlSupport.examRecord.name(),
                Domain.EXAM.ATTR_ID);

        // SEB Client Configuration Table
        final Map<String, String> sebClientConfigTableMap = new HashMap<>();
        sebClientConfigTableMap.put(
                Domain.SEB_CLIENT_CONFIGURATION.ATTR_INSTITUTION_ID,
                SebClientConfigRecordDynamicSqlSupport.institutionId.name());
        sebClientConfigTableMap.put(
                Domain.SEB_CLIENT_CONFIGURATION.ATTR_NAME,
                SebClientConfigRecordDynamicSqlSupport.name.name());
        sebClientConfigTableMap.put(
                Domain.SEB_CLIENT_CONFIGURATION.ATTR_DATE,
                SebClientConfigRecordDynamicSqlSupport.date.name());
        this.sortColumnMapping.put(
                SebClientConfigRecordDynamicSqlSupport.sebClientConfigRecord.name(),
                sebClientConfigTableMap);
        this.defaultSortColumn.put(
                SebClientConfigRecordDynamicSqlSupport.sebClientConfigRecord.name(),
                Domain.SEB_CLIENT_CONFIGURATION.ATTR_ID);

        // ConfigurationNode
        final Map<String, String> configurationNodeTableMap = new HashMap<>();
        configurationNodeTableMap.put(
                Domain.CONFIGURATION_NODE.ATTR_INSTITUTION_ID,
                ConfigurationNodeRecordDynamicSqlSupport.institutionId.name());
        configurationNodeTableMap.put(
                Domain.CONFIGURATION_NODE.ATTR_NAME,
                ConfigurationNodeRecordDynamicSqlSupport.name.name());
        configurationNodeTableMap.put(
                Domain.CONFIGURATION_NODE.ATTR_DESCRIPTION,
                ConfigurationNodeRecordDynamicSqlSupport.description.name());
        this.sortColumnMapping.put(
                ConfigurationNodeRecordDynamicSqlSupport.configurationNodeRecord.name(),
                configurationNodeTableMap);
        this.defaultSortColumn.put(
                ConfigurationNodeRecordDynamicSqlSupport.configurationNodeRecord.name(),
                Domain.CONFIGURATION_NODE.ATTR_ID);

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

    }

}
