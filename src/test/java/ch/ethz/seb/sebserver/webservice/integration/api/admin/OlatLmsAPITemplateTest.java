/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.admin;

import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpEntity;
import org.springframework.test.context.jdbc.Sql;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamType;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ExamConfigurationValueService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.APITemplateDataSupplier;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.olat.OlatLmsAPITemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.olat.OlatLmsData.RestrictionDataPost;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.olat.OlatLmsRestTemplate;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
public class OlatLmsAPITemplateTest extends AdministrationAPIIntegrationTester {

    @Autowired
    private ExamConfigurationValueService examConfigurationValueService;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private OlatLmsRestTemplate olatLmsRestTemplate;

    @Test
    public void testSetRestriction() throws Exception {

        final OlatLmsRestTemplate restTemplateMock = Mockito.mock(OlatLmsRestTemplate.class);
        final APITemplateDataSupplier apiTemplateDataSupplier = Mockito.mock(APITemplateDataSupplier.class);
        Mockito.when(apiTemplateDataSupplier.getLmsSetup()).thenReturn(new LmsSetup(
                1L, 1L, null, null, null, null, null, null, null, null, null, null, false, null));

        final OlatLmsAPITemplate olatLmsAPITemplate = new OlatLmsAPITemplate(
                null,
                null,
                apiTemplateDataSupplier,
                this.examConfigurationValueService,
                this.cacheManager,
                true);

        Mockito.when(restTemplateMock.exchange(Mockito.any(), Mockito.any(), Mockito.any(),
                (Class) Mockito.any(), (Object[]) Mockito.any())).then(new Answer() {

                    @Override
                    public Object answer(final InvocationOnMock invocation) throws Throwable {
                        final HttpEntity<RestrictionDataPost> argument2 = invocation.getArgument(2, HttpEntity.class);
                        assertNotNull(argument2);
                        final RestrictionDataPost body = argument2.getBody();
                        assertNotNull(body);
//                        assertEquals("seb://quitlink.seb", body.quitLink);
//                        assertEquals("123", body.quitSecret);
                        return null;
                    }

                });

        final Field field = OlatLmsAPITemplate.class.getDeclaredField("cachedRestTemplate");
        field.setAccessible(true);
        field.set(olatLmsAPITemplate, restTemplateMock);

        final Exam exam = new Exam(
                2L,
                1L,
                1L,
                Constants.EMPTY_NOTE,
                false,
                Constants.EMPTY_NOTE,
                null,
                null,
                ExamType.UNDEFINED,
                null,
                null,
                ExamStatus.FINISHED,
                Boolean.FALSE,
                null,
                Boolean.FALSE,
                null,
                null,
                null,
                null);

        final SEBRestriction sebRestriction = new SEBRestriction(
                2L,
                Arrays.asList("configKey1", "configKey2"),
                Arrays.asList("browserKey1", "browserKey2"),
                null, null);

        olatLmsAPITemplate.applySEBClientRestriction(exam, sebRestriction);

    }

}
