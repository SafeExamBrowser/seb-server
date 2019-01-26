/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.model.user.ExamineeAccountDetails;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.PaginationService.SortOrder;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;

public class MockupLmsAPITemplate implements LmsAPITemplate {

    private final LmsSetup setup;

    private final Collection<QuizData> mockups;

    public MockupLmsAPITemplate(final LmsSetup setup) {
        if (!setup.isActive() || setup.lmsType != LmsType.MOCKUP) {
            throw new IllegalArgumentException();
        }

        this.setup = setup;
        this.mockups = new ArrayList<>();
        this.mockups.add(new QuizData(
                "quiz1", "Demo Quiz 1", "Demo Quit Mockup",
                "2020-01-01 09:00:00", "2021-01-01 09:00:00", "http://lms.mockup.com/api/"));
        this.mockups.add(new QuizData(
                "quiz2", "Demo Quiz 2", "Demo Quit Mockup",
                "2020-01-01 09:00:00", "2021-01-01 09:00:00", "http://lms.mockup.com/api/"));
        this.mockups.add(new QuizData(
                "quiz3", "Demo Quiz 3", "Demo Quit Mockup",
                "2018-07-30 09:00:00", "2018-08-01 00:00:00", "http://lms.mockup.com/api/"));
        this.mockups.add(new QuizData(
                "quiz4", "Demo Quiz 4", "Demo Quit Mockup",
                "2018-01-01 00:00:00", "2019-01-01 00:00:00", "http://lms.mockup.com/api/"));
        this.mockups.add(new QuizData(
                "quiz5", "Demo Quiz 5", "Demo Quit Mockup",
                "2018-01-01 09:00:00", "2021-01-01 09:00:00", "http://lms.mockup.com/api/"));
        this.mockups.add(new QuizData(
                "quiz6", "Demo Quiz 6", "Demo Quit Mockup",
                "2018-01-01 09:00:00", "2021-01-01 09:00:00", "http://lms.mockup.com/api/"));
        this.mockups.add(new QuizData(
                "quiz7", "Demo Quiz 7", "Demo Quit Mockup",
                "2018-01-01 09:00:00", "2021-01-01 09:00:00", "http://lms.mockup.com/api/"));
    }

    @Override
    public LmsSetup lmsSetup() {
        return this.setup;
    }

    @Override
    public LmsSetupTestResult testLmsSetup() {
        if (this.setup.lmsApiUrl.equals("mockup") &&
                this.setup.lmsAuthName.equals("mockup") &&
                this.setup.lmsAuthSecret.equals("mockup")) {

            return new LmsSetupTestResult(true);
        } else {
            return new LmsSetupTestResult(false);
        }
    }

    public Collection<QuizData> getQuizzes(
            final String name,
            final Long from,
            final String sort) {

        final int orderFactor = (SortOrder.getSortOrder(sort) == SortOrder.DESCENDING)
                ? -1
                : 1;

        final String _sort = SortOrder.getSortColumn(sort);
        final Comparator<QuizData> comp = (_sort != null)
                ? (_sort.equals(QuizData.FILTER_ATTR_START_TIME))
                        ? (q1, q2) -> q1.startTime.compareTo(q2.startTime) * orderFactor
                        : (q1, q2) -> q1.name.compareTo(q2.name) * orderFactor
                : (q1, q2) -> q1.name.compareTo(q2.name) * orderFactor;

        return this.mockups.stream()
                .filter(mockup -> (name != null)
                        ? mockup.name.contains(name)
                        : true && (from != null)
                                ? mockup.startTime.getMillis() >= from
                                : true)
                .sorted(comp)
                .collect(Collectors.toList());
    }

    @Override
    public Page<QuizData> getQuizzes(
            final String name,
            final Long from,
            final String sort,
            final int pageNumber,
            final int pageSize) {

        final int startIndex = pageNumber * pageSize;
        final int endIndex = startIndex + pageSize;
        int index = 0;
        final Collection<QuizData> quizzes = getQuizzes(name, from, sort);
        final int numberOfPages = quizzes.size() / pageSize;
        final Iterator<QuizData> iterator = quizzes.iterator();
        final List<QuizData> pageContent = new ArrayList<>();
        while (iterator.hasNext() && index < endIndex) {
            final QuizData next = iterator.next();
            if (index >= startIndex) {
                pageContent.add(next);
            }
            index++;
        }

        return new Page<>(numberOfPages, pageNumber, sort, pageContent);
    }

    @Override
    public Collection<Result<QuizData>> getQuizzes(final Set<String> ids) {
        return this.mockups.stream()
                .filter(mockup -> ids.contains(mockup.id))
                .map(mockup -> Result.of(mockup))
                .collect(Collectors.toList());
    }

    @Override
    public Result<ExamineeAccountDetails> getExamineeAccountDetails(final String examineeUserId) {
        return Result.of(new ExamineeAccountDetails(examineeUserId, "mockup", "mockup", "mockup"));
    }

}
