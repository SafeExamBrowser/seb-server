/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import org.joda.time.DateTime;
import org.springframework.util.MultiValueMap;

import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.JodaTimeTypeResolver;

public class FilterMap extends POSTMapper {

    public FilterMap(final MultiValueMap<String, String> params) {
        super(params);
    }

    public Integer getActiveAsInt() {
        return getBooleanAsInteger(UserInfo.FILTER_ATTR_ACTIVE);
    }

    public Long getInstitutionId() {
        return getLong(UserInfo.FILTER_ATTR_INSTITUTION);
    }

    public String getName() {
        return getSQLWildcard(UserInfo.FILTER_ATTR_NAME);
    }

    public String getUserUsername() {
        return getSQLWildcard(UserInfo.FILTER_ATTR_USER_NAME);
    }

    public String getUserEmail() {
        return getSQLWildcard(UserInfo.FILTER_ATTR_EMAIL);
    }

    public String getUserLocale() {
        return getString(UserInfo.FILTER_ATTR_LOCALE);
    }

    public String getLmsSetupName() {
        return getSQLWildcard(LmsSetup.FILTER_ATTR_NAME);
    }

    public String getLmsSetupType() {
        return getString(LmsSetup.FILTER_ATTR_LMS_TYPE);
    }

    public DateTime getExamFromTime() {
        return JodaTimeTypeResolver.getDateTime(getString(Exam.FILTER_ATTR_FROM));
    }

    public String getExamQuizId() {
        return getString(Exam.FILTER_ATTR_QUIZ_ID);
    }

    public Long getExamLmsSetupId() {
        return getLong(Exam.FILTER_ATTR_LMS_SETUP);
    }

    public String getExamStatus() {
        return getString(Exam.FILTER_ATTR_STATUS);
    }

    public String getExamType() {
        return getString(Exam.FILTER_ATTR_TYPE);
    }

    public String getExamOwner() {
        return getString(Exam.FILTER_ATTR_OWNER);
    }

    public Long getIndicatorExamId() {
        return getLong(Indicator.FILTER_ATTR_EXAM);
    }

    public String getIndicatorName() {
        return getSQLWildcard(Indicator.FILTER_ATTR_NAME);
    }

    public String getSQLWildcard(final String name) {
        return toSQLWildcard(this.params.getFirst(name));
    }

    public static String toSQLWildcard(final String text) {
        return (text == null) ? null : "%" + text + "%";
    }

}
