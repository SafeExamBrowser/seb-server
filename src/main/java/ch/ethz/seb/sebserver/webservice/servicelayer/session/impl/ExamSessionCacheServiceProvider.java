/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;


import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Lazy
@Service
@WebServiceProfile
public class ExamSessionCacheServiceProvider {

    private final ApplicationContext applicationContext;
    private ExamSessionCacheService examSessionCacheService = null;

    public ExamSessionCacheServiceProvider(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public ExamSessionCacheService getExamSessionCacheService() {
        if (examSessionCacheService == null) {
            examSessionCacheService = applicationContext.getBean(ExamSessionCacheService.class);
        }
        return examSessionCacheService;
    }
}
