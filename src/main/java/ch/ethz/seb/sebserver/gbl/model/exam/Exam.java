/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain.EXAM;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class Exam implements GrantEntity {

    public static final Exam EMPTY_EXAM = new Exam(
            -1L,
            -1L,
            -1L,
            Constants.EMPTY_NOTE,
            Constants.EMPTY_NOTE,
            Constants.EMPTY_NOTE,
            null,
            null,
            Constants.EMPTY_NOTE,
            ExamType.UNDEFINED,
            null,
            null,
            ExamStatus.FINISHED,
//            Boolean.FALSE,
            null,
            Boolean.FALSE,
            null);

    public static final String FILTER_ATTR_TYPE = "type";
    public static final String FILTER_ATTR_STATUS = "status";
    public static final String FILTER_CACHED_QUIZZES = "cached-quizzes";

    public enum ExamStatus {
        UP_COMING,
        RUNNING,
        FINISHED
    }

    public enum ExamType {
        UNDEFINED,
        MANAGED,
        BYOD,
        VDI
    }

    @JsonProperty(EXAM.ATTR_ID)
    public final Long id;

    @JsonProperty(EXAM.ATTR_INSTITUTION_ID)
    @NotNull
    public final Long institutionId;

    @JsonProperty(EXAM.ATTR_LMS_SETUP_ID)
    @NotNull
    public final Long lmsSetupId;

    @JsonProperty(EXAM.ATTR_EXTERNAL_ID)
    @NotNull
    public final String externalId;

    @JsonProperty(QuizData.QUIZ_ATTR_NAME)
    public final String name;

    @JsonProperty(QuizData.QUIZ_ATTR_DESCRIPTION)
    public final String description;

    @JsonProperty(QuizData.QUIZ_ATTR_START_TIME)
    public final DateTime startTime;

    @JsonProperty(QuizData.QUIZ_ATTR_END_TIME)
    public final DateTime endTime;

    @JsonProperty(QuizData.QUIZ_ATTR_START_URL)
    public final String startURL;

    @JsonProperty(EXAM.ATTR_TYPE)
    @NotNull
    public final ExamType type;

    @JsonProperty(EXAM.ATTR_OWNER)
    public final String owner;

    @JsonProperty(EXAM.ATTR_SUPPORTER)
    @NotEmpty(message = "exam:supporter:notNull")
    public final Collection<String> supporter;

    @JsonProperty(EXAM.ATTR_STATUS)
    public final ExamStatus status;

    @JsonProperty(EXAM.ATTR_BROWSER_KEYS)
    public final String browserExamKeys;

    @JsonProperty(EXAM.ATTR_ACTIVE)
    public final Boolean active;

    @JsonProperty(EXAM.ATTR_LASTUPDATE)
    public final String lastUpdate;

    @JsonCreator
    public Exam(
            @JsonProperty(EXAM.ATTR_ID) final Long id,
            @JsonProperty(EXAM.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(EXAM.ATTR_LMS_SETUP_ID) final Long lmsSetupId,
            @JsonProperty(EXAM.ATTR_EXTERNAL_ID) final String externalId,
            @JsonProperty(QuizData.QUIZ_ATTR_NAME) final String name,
            @JsonProperty(QuizData.QUIZ_ATTR_DESCRIPTION) final String description,
            @JsonProperty(QuizData.QUIZ_ATTR_START_TIME) final DateTime startTime,
            @JsonProperty(QuizData.QUIZ_ATTR_END_TIME) final DateTime endTime,
            @JsonProperty(QuizData.QUIZ_ATTR_START_URL) final String startURL,
            @JsonProperty(EXAM.ATTR_TYPE) final ExamType type,
            @JsonProperty(EXAM.ATTR_OWNER) final String owner,
            @JsonProperty(EXAM.ATTR_SUPPORTER) final Collection<String> supporter,
            @JsonProperty(EXAM.ATTR_STATUS) final ExamStatus status,
            @JsonProperty(EXAM.ATTR_BROWSER_KEYS) final String browserExamKeys,
            @JsonProperty(EXAM.ATTR_ACTIVE) final Boolean active,
            @JsonProperty(EXAM.ATTR_LASTUPDATE) final String lastUpdate) {

        this.id = id;
        this.institutionId = institutionId;
        this.lmsSetupId = lmsSetupId;
        this.externalId = externalId;
        this.name = name;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.startURL = startURL;
        this.type = type;
        this.owner = owner;
        this.status = (status != null) ? status : getStatusFromDate(startTime, endTime);
        this.browserExamKeys = browserExamKeys;
        this.active = (active != null) ? active : Boolean.TRUE;
        this.lastUpdate = lastUpdate;

        this.supporter = (supporter != null)
                ? Collections.unmodifiableCollection(supporter)
                : Collections.emptyList();
    }

    public Exam(final String modelId, final QuizData quizData, final POSTMapper mapper) {

        this.id = (modelId != null) ? Long.parseLong(modelId) : null;
        this.institutionId = quizData.institutionId;
        this.lmsSetupId = quizData.lmsSetupId;
        this.externalId = quizData.id;
        this.name = quizData.name;
        this.description = quizData.description;
        this.startTime = quizData.startTime;
        this.endTime = quizData.endTime;
        this.startURL = quizData.startURL;
        this.type = mapper.getEnum(EXAM.ATTR_TYPE, ExamType.class, ExamType.UNDEFINED);
        this.owner = mapper.getString(EXAM.ATTR_OWNER);
        this.status = mapper.getEnum(
                EXAM.ATTR_STATUS,
                ExamStatus.class,
                getStatusFromDate(this.startTime, this.endTime));
        this.browserExamKeys = mapper.getString(EXAM.ATTR_BROWSER_KEYS);
        this.active = mapper.getBoolean(EXAM.ATTR_ACTIVE);
        this.supporter = mapper.getStringSet(EXAM.ATTR_SUPPORTER);
        this.lastUpdate = null;
    }

    public Exam(final QuizData quizData) {
        this(null, quizData, POSTMapper.EMPTY_MAP);
    }

    public Exam(final Long id, final ExamStatus status) {
        this.id = id;
        this.institutionId = null;
        this.lmsSetupId = null;
        this.externalId = null;
        this.name = null;
        this.description = null;
        this.startTime = null;
        this.endTime = null;
        this.startURL = null;
        this.type = null;
        this.owner = null;
        this.status = (status != null) ? status : getStatusFromDate(this.startTime, this.endTime);
        this.browserExamKeys = null;
        this.active = null;
        this.supporter = null;
        this.lastUpdate = null;
    }

    @Override
    public EntityType entityType() {
        return EntityType.EXAM;
    }

    public Long getId() {
        return this.id;
    }

    @Override
    @JsonIgnore
    public String getModelId() {
        if (this.id == null) {
            return null;
        }

        return String.valueOf(this.id);
    }

    @JsonIgnore
    @Override
    public Long getInstitutionId() {
        return this.institutionId;
    }

    public boolean isOwner(final String userId) {
        if (StringUtils.isBlank(userId)) {
            return false;
        }

        if (userId.equals(this.owner)) {
            return true;
        }

        return this.supporter.contains(userId);
    }

    @Override
    public String getOwnerId() {
        final ArrayList<String> owners = new ArrayList<>(this.supporter);
        if (!StringUtils.isBlank(this.owner)) {
            owners.add(this.owner);
        }
        return StringUtils.join(owners, Constants.LIST_SEPARATOR);
    }

    public Long getLmsSetupId() {
        return this.lmsSetupId;
    }

    public String getExternalId() {
        return this.externalId;
    }

    public ExamType getType() {
        return this.type;
    }

    public Collection<String> getSupporter() {
        return this.supporter;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public DateTime getStartTime() {
        return this.startTime;
    }

    public DateTime getEndTime() {
        return this.endTime;
    }

    public String getStartURL() {
        return this.startURL;
    }

    public ExamStatus getStatus() {
        return this.status;
    }

    public String getBrowserExamKeys() {
        return this.browserExamKeys;
    }

    public Boolean getActive() {
        return this.active;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Exam [id=");
        builder.append(this.id);
        builder.append(", institutionId=");
        builder.append(this.institutionId);
        builder.append(", lmsSetupId=");
        builder.append(this.lmsSetupId);
        builder.append(", externalId=");
        builder.append(this.externalId);
        builder.append(", name=");
        builder.append(this.name);
        builder.append(", description=");
        builder.append(this.description);
        builder.append(", startTime=");
        builder.append(this.startTime);
        builder.append(", endTime=");
        builder.append(this.endTime);
        builder.append(", startURL=");
        builder.append(this.startURL);
        builder.append(", type=");
        builder.append(this.type);
        builder.append(", owner=");
        builder.append(this.owner);
        builder.append(", supporter=");
        builder.append(this.supporter);
        builder.append(", status=");
        builder.append(this.status);
        builder.append(", browserExamKeys=");
        builder.append(this.browserExamKeys);
        builder.append(", active=");
        builder.append(this.active);
        builder.append(", lastUpdate=");
        builder.append(this.lastUpdate);
        builder.append("]");
        return builder.toString();
    }

    public static ExamStatus getStatusFromDate(final DateTime startTime, final DateTime endTime) {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        if (startTime != null && now.isBefore(startTime)) {
            return ExamStatus.UP_COMING;
        } else if (startTime != null && now.isAfter(startTime) && (endTime == null || now.isBefore(endTime))) {
            return ExamStatus.RUNNING;
        } else if (endTime != null && now.isAfter(endTime)) {
            return ExamStatus.FINISHED;
        } else {
            return null;
        }
    }

}
