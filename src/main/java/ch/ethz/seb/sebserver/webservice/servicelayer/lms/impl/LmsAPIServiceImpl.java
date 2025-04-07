/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl;

import java.util.Collection;

import ch.ethz.seb.sebserver.webservice.servicelayer.lms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.LmsSetupDAO;

@Lazy
@Service
@WebServiceProfile
public class LmsAPIServiceImpl implements LmsAPIService {

    private static final Logger log = LoggerFactory.getLogger(LmsAPIServiceImpl.class);

    private final LmsSetupDAO lmsSetupDAO;
    private final QuizLookupService quizLookupService;
    private final LmsAPITemplateCacheService lmsAPITemplateCacheService;

    public LmsAPIServiceImpl(
            final LmsSetupDAO lmsSetupDAO,
            final QuizLookupService quizLookupService,
            final LmsAPITemplateCacheService lmsAPITemplateCacheService,
            final Collection<LmsAPITemplateFactory> lmsAPITemplateFactories
    ) {

        this.lmsSetupDAO = lmsSetupDAO;
        this.quizLookupService = quizLookupService;
        this.lmsAPITemplateCacheService = lmsAPITemplateCacheService;
    }

    /** Listen to LmsSetupChangeEvent to release an affected LmsAPITemplate from cache
     *
     * @param event the event holding the changed LmsSetup */
    @EventListener
    public void notifyLmsSetupChange(final LmsSetupChangeEvent event) {
        final LmsSetup lmsSetup = event.getLmsSetup();
        if (lmsSetup == null) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("LmsSetup changed. Update cache by removing eventually used references");
        }

        lmsAPITemplateCacheService.clearCache(lmsSetup.getModelId());
        this.quizLookupService.clear(lmsSetup.institutionId);
    }

    @Override
    public void cleanup() {
        lmsAPITemplateCacheService.cleanup();
    }

    @Override
    public void cleanupSetup(final Long id) {
        if (id == null) {
            cleanup();
            return;
        }
        lmsAPITemplateCacheService.clearCache(String.valueOf(id));
    }

    @Override
    public Result<LmsSetup> getLmsSetup(final Long id) {
        return this.lmsSetupDAO.byPK(id);
    }

    @Override
    public Result<Page<QuizData>> requestQuizDataPage(
            final int pageNumber,
            final int pageSize,
            final String sort,
            final FilterMap filterMap) {

        return this.quizLookupService.requestQuizDataPage(
                pageNumber,
                pageSize,
                sort,
                filterMap,
                this::getLmsAPITemplate);
    }

    @Override
    public Result<LmsAPITemplate> getLmsAPITemplate(final String lmsSetupId) {
        return lmsAPITemplateCacheService.getLmsAPITemplate(lmsSetupId);
    }
}
