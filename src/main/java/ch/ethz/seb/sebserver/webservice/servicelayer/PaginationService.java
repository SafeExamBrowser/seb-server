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

import org.mybatis.dynamic.sql.SqlTable;

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.util.Result;

/** A service to apply pagination functionality within collection results form data access layer.
 * The default implementation uses Mybatis-PageHelper to apply the pagination on SQL level where possible:
 * https://github.com/pagehelper/Mybatis-PageHelper */

public interface PaginationService {

    /** Use this to verify whether native sorting (on SQL level) is supported for a given orderBy column
     * and a given SqlTable or not.
     *
     * @param table SqlTable the SQL table (MyBatis)
     * @param orderBy the orderBy columnName
     * @return true if there is native sorting support for the given attributes */
    boolean isNativeSortingSupported(final SqlTable table, final String orderBy);

    /** Use this to set a page limitation on SQL level. This checks first if there is
     * already a page-limitation set for the local thread and if not, set the default page-limitation */
    void setDefaultLimitIfNotSet();

    void setDefaultLimit();

    void setDefaultLimit(final String sort, final String tableName);

    int getPageNumber(final Integer pageNumber);

    /** Get the given pageSize as int type if it is not null and in the range of one to the defined maximum page size.
     * If the given pageSize null or less then one, this returns the defined default page size.
     * If the given pageSize is greater then the defined maximum page size this returns the the defined maximum page
     * size
     *
     * @param pageSize the page size Integer value to convert
     * @return the given pageSize as int type if it is not null and in the range of one to the defined maximum page
     *         size, */
    int getPageSize(final Integer pageSize);

    /** Get a Page of specified domain models from given pagination attributes within collection supplier delegate.
     *
     *
     * @param pageNumber the current page number
     * @param pageSize the (full) size of the page
     * @param sort the name of the sort column with a leading '-' for descending sort order
     * @param tableName the name of the SQL table on which the pagination is applying to
     * @param delegate a collection supplier the does the underling SQL query with specified pagination attributes
     * @return Result refers to a Page of specified type of model models or to an exception on error case */
    <T extends Entity> Result<Page<T>> getPage(
            final Integer pageNumber,
            final Integer pageSize,
            final String sort,
            final String tableName,
            final Supplier<Result<Collection<T>>> delegate);

}
