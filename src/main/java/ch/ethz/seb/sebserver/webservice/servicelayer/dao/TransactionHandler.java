/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import org.springframework.transaction.interceptor.TransactionInterceptor;

/** Defines some static Spring based transaction handling functionality for rollback handling */
public interface TransactionHandler {

    /** Use this to mark the current transaction within the calling thread as "to rollback".
     * This uses Springs; TransactionInterceptor.currentTransactionStatus().setRollbackOnly()
     *
     * Set the transaction rollback-only. This instructs the transaction manager
     * that the only possible outcome of the transaction may be a rollback, as
     * alternative to throwing an exception which would in turn trigger a rollback.
     * <p>
     * This is mainly intended for transactions managed by
     * {@link org.springframework.transaction.support.TransactionTemplate} or
     * {@link org.springframework.transaction.interceptor.TransactionInterceptor},
     * where the actual commit/rollback decision is made by the container.
     *
     * @see org.springframework.transaction.support.TransactionCallback#doInTransaction
     * @see org.springframework.transaction.interceptor.TransactionAttribute#rollbackOn */
    static void rollback() {
        TransactionInterceptor.currentTransactionStatus().setRollbackOnly();
    }

    /** Use this to mark the current transaction within the calling thread as "to rollback".
     * This uses Springs; TransactionInterceptor.currentTransactionStatus().setRollbackOnly()
     *
     * Set the transaction rollback-only. This instructs the transaction manager
     * that the only possible outcome of the transaction may be a rollback, as
     * alternative to throwing an exception which would in turn trigger a rollback.
     * <p>
     * This is mainly intended for transactions managed by
     * {@link org.springframework.transaction.support.TransactionTemplate} or
     * {@link org.springframework.transaction.interceptor.TransactionInterceptor},
     * where the actual commit/rollback decision is made by the container.
     *
     * @see org.springframework.transaction.support.TransactionCallback#doInTransaction
     * @see org.springframework.transaction.interceptor.TransactionAttribute#rollbackOn */
    static void rollback(final Throwable t) {
        TransactionInterceptor.currentTransactionStatus().setRollbackOnly();
    }

}
