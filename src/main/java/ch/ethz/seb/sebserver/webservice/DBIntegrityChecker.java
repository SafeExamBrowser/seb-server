/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.SEBServerInit;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;

@Lazy
@Component
@WebServiceProfile
public class DBIntegrityChecker {

    private static final Logger log = LoggerFactory.getLogger(DBIntegrityChecker.class);

    private final Collection<DBIntegrityCheck> checkers;
    private final boolean runIntegrityChecks;
    private final boolean tryFix;

    public DBIntegrityChecker(
            final Collection<DBIntegrityCheck> checkers,
            @Value("${sebserver.init.database.integrity.checks:true}") final boolean runIntegrityChecks,
            @Value("${sebserver.init.database.integrity.try-fix:true}") final boolean tryFix) {

        this.checkers = checkers;
        this.runIntegrityChecks = runIntegrityChecks;
        this.tryFix = tryFix;
    }

    public void checkIntegrity() {
        if (this.runIntegrityChecks && !this.checkers.isEmpty()) {

            SEBServerInit.INIT_LOGGER.info("---->");
            SEBServerInit.INIT_LOGGER.info("----> **** Run data-base integrity checks ****");
            SEBServerInit.INIT_LOGGER.info("---->");

            this.checkers.stream().forEach(this::doCheck);
        }
    }

    private void doCheck(final DBIntegrityCheck dbIntegrityCheck) {
        try {

            SEBServerInit.INIT_LOGGER.info("------> Apply check: {} / {}", dbIntegrityCheck.name(),
                    dbIntegrityCheck.description());

            final Result<String> applyCheck = dbIntegrityCheck.applyCheck(this.tryFix);
            if (applyCheck.hasError()) {
                if (applyCheck.getError() instanceof  WebserviceInitException) {
                    throw applyCheck.getError();
                }
                SEBServerInit.INIT_LOGGER.info("--------> Unexpected Error: {}", applyCheck.getError().getMessage());
            } else {
                SEBServerInit.INIT_LOGGER.info("--------> Result: {}", applyCheck.get());
            }

        } catch (final WebserviceInitException initE) {
            throw initE;
        } catch (final Exception e) {
            log.error("Unexpected error while trying to apply data base integrity check: {}", dbIntegrityCheck);
        }
    }

}
