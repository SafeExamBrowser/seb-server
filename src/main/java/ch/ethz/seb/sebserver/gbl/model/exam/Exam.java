/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import java.util.Collection;
import java.util.Collections;

import javax.validation.constraints.NotNull;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Activatable;
import ch.ethz.seb.sebserver.gbl.model.Domain.EXAM;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class Exam implements GrantEntity, Activatable {

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
            null,
            null,
            false);

    // TODO make this a configurable exam attribute
    /** The number of hours to add at the start- and end-time of the exam
     * To add a expanded time-frame in which the exam state is running on SEB-Server side */
    public static final int EXAM_RUN_TIME_EXPAND_HOURS = 2;

    public static final String ATTR_STATUS = "examStatus";
    public static final String FILTER_ATTR_TYPE = "type";

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

    @JsonProperty(EXAM.ATTR_QUIT_PASSWORD)
    public final String quitPassword;

    @JsonProperty(EXAM.ATTR_BROWSER_KEYS)
    public final String browserExamKeys;

    @JsonProperty(EXAM.ATTR_OWNER)
    public final String owner;

    @JsonProperty(EXAM.ATTR_SUPPORTER)
    public final Collection<String> supporter;

    /** Indicates whether this Exam is active or not */
    @JsonProperty(EXAM.ATTR_ACTIVE)
    public final Boolean active;

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
            @JsonProperty(EXAM.ATTR_QUIT_PASSWORD) final String quitPassword,
            @JsonProperty(EXAM.ATTR_BROWSER_KEYS) final String browserExamKeys,
            @JsonProperty(EXAM.ATTR_OWNER) final String owner,
            @JsonProperty(EXAM.ATTR_SUPPORTER) final Collection<String> supporter,
            @JsonProperty(EXAM.ATTR_ACTIVE) final Boolean active) {

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
        this.quitPassword = quitPassword;
        this.browserExamKeys = browserExamKeys;
        this.owner = owner;
        this.active = (active != null) ? active : Boolean.FALSE;

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
        this.quitPassword = mapper.getString(EXAM.ATTR_QUIT_PASSWORD);
        this.browserExamKeys = mapper.getString(EXAM.ATTR_BROWSER_KEYS);
        this.owner = mapper.getString(EXAM.ATTR_OWNER);
        this.active = mapper.getBoolean(EXAM.ATTR_ACTIVE);
        this.supporter = mapper.getStringSet(EXAM.ATTR_SUPPORTER);
    }

    public Exam(final QuizData quizzData) {
        this(null, quizzData, POSTMapper.EMPTY_MAP);
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

    @Override
    public String getOwnerId() {
        return this.owner;
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

    public String getQuitPassword() {
        return this.quitPassword;
    }

    public String getBrowserExamKeys() {
        return this.browserExamKeys;
    }

    @JsonIgnore
    public ExamStatus getStatus() {
        if (this.startTime == null) {
            return ExamStatus.UP_COMING;
        }

        // expand time frame
        final DateTime expStartTime = this.startTime.minusHours(EXAM_RUN_TIME_EXPAND_HOURS);
        final DateTime expEndTime = (this.endTime != null)
                ? this.endTime.plusHours(EXAM_RUN_TIME_EXPAND_HOURS) : null;

        final DateTime now = DateTime.now(DateTimeZone.UTC);
        if (expStartTime.isBefore(now)) {
            if (expEndTime == null || expEndTime.isAfter(now)) {
                return ExamStatus.RUNNING;
            } else {
                return ExamStatus.FINISHED;
            }
        }

        return ExamStatus.UP_COMING;
    }

    @Override
    public boolean isActive() {
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
        builder.append(", quitPassword=");
        builder.append(this.quitPassword);
        builder.append(", browserExamKeys=");
        builder.append(this.browserExamKeys);
        builder.append(", owner=");
        builder.append(this.owner);
        builder.append(", supporter=");
        builder.append(this.supporter);
        builder.append(", active=");
        builder.append(this.active);
        builder.append("]");
        return builder.toString();
    }

}
