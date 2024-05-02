/*
 * Copyright (c) 2018 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import org.apache.commons.lang3.BooleanUtils;
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
import ch.ethz.seb.sebserver.gbl.util.Utils;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class Exam implements GrantEntity {

    public static final Exam EMPTY_EXAM = new Exam(
            -1L, -1L, -1L, Constants.EMPTY_NOTE, false, Constants.EMPTY_NOTE,
            null, null, ExamType.UNDEFINED, null, null, ExamStatus.FINISHED,
            null, Boolean.FALSE, null, Boolean.FALSE, null,
            null, null, null);

    public static final String FILTER_ATTR_TYPE = "type";
    public static final String FILTER_ATTR_STATUS = "status";
    public static final String FILTER_CACHED_QUIZZES = "cached-quizzes";
    public static final String FILTER_ATTR_HIDE_MISSING = "show-missing";

    /** This attribute name is used to store the number of quiz recover attempts done by exam update process */
    public static final String ADDITIONAL_ATTR_QUIZ_RECOVER_ATTEMPTS = "QUIZ_RECOVER_ATTEMPTS";
    /** This attribute name is used on exams to store the flag for indicating the signature key check */
    public static final String ADDITIONAL_ATTR_SIGNATURE_KEY_CHECK_ENABLED = "SIGNATURE_KEY_CHECK_ENABLED";
    /** This attribute name is used to store the signature check grant threshold for numerical trust checks */
    public static final String ADDITIONAL_ATTR_NUMERICAL_TRUST_THRESHOLD = "NUMERICAL_TRUST_THRESHOLD";
    /** This attribute name is used to store the signature check encryption certificate is one is used */
    public static final String ADDITIONAL_ATTR_SIGNATURE_KEY_CERT_ALIAS = "SIGNATURE_KEY_CERT_ALIAS";
    /** This attribute name is used to store the per exam generated app-signature-key encryption salt */
    public static final String ADDITIONAL_ATTR_SIGNATURE_KEY_SALT = "SIGNATURE_KEY_SALT";
    /** Comma separated String value that defines allowed SEB version from linked Exam Configuration */
    public static final String ADDITIONAL_ATTR_ALLOWED_SEB_VERSIONS = "ALLOWED_SEB_VERSIONS";

    public static final String ADDITIONAL_ATTR_DEFAULT_CONNECTION_CONFIGURATION = "DEFAULT_CONNECTION_CONFIGURATION";

    public enum ExamStatus {
        UP_COMING,
        RUNNING,
        FINISHED,
        ARCHIVED
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
    public final Long lmsSetupId;

    @JsonProperty(EXAM.ATTR_EXTERNAL_ID)
    @NotNull
    public final String externalId;

    @JsonProperty(EXAM.ATTR_LMS_AVAILABLE)
    public final Boolean lmsAvailable;

    @JsonProperty(EXAM.ATTR_QUIZ_NAME)
    public final String name;

    @JsonProperty(EXAM.ATTR_QUIZ_START_TIME)
    public final DateTime startTime;

    @JsonProperty(EXAM.ATTR_QUIZ_END_TIME)
    public final DateTime endTime;

    @JsonProperty(EXAM.ATTR_TYPE)
    @NotNull
    public final ExamType type;

    @JsonProperty(EXAM.ATTR_OWNER)
    public final String owner;

    @JsonProperty(EXAM.ATTR_SUPPORTER)
    public final Collection<String> supporter;

    @JsonProperty(EXAM.ATTR_STATUS)
    public final ExamStatus status;

    @JsonProperty(EXAM.ATTR_QUIT_PASSWORD)
    public final String quitPassword;

    @JsonProperty(EXAM.ATTR_LMS_SEB_RESTRICTION)
    public final Boolean sebRestriction;

    @JsonProperty(EXAM.ATTR_BROWSER_KEYS)
    public final String browserExamKeys;

    @JsonProperty(EXAM.ATTR_ACTIVE)
    public final Boolean active;

    @JsonProperty(EXAM.ATTR_LASTUPDATE)
    public final String lastUpdate;

    @JsonProperty(EXAM.ATTR_EXAM_TEMPLATE_ID)
    public final Long examTemplateId;

    @JsonProperty(EXAM.ATTR_LAST_MODIFIED)
    public final Long lastModified;

    @JsonProperty(API.PARAM_ADDITIONAL_ATTRIBUTES)
    public final Map<String, String> additionalAttributes;

    @JsonIgnore
    public final boolean checkASK;
    @JsonIgnore
    public final List<AllowedSEBVersion> allowedSEBVersions;

    @JsonCreator
    public Exam(
            @JsonProperty(EXAM.ATTR_ID) final Long id,
            @JsonProperty(EXAM.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(EXAM.ATTR_LMS_SETUP_ID) final Long lmsSetupId,
            @JsonProperty(EXAM.ATTR_EXTERNAL_ID) final String externalId,
            @JsonProperty(EXAM.ATTR_LMS_AVAILABLE) final Boolean lmsAvailable,
            @JsonProperty(EXAM.ATTR_QUIZ_NAME) final String name,
            @JsonProperty(EXAM.ATTR_QUIZ_START_TIME) final DateTime startTime,
            @JsonProperty(EXAM.ATTR_QUIZ_END_TIME) final DateTime endTime,
            @JsonProperty(EXAM.ATTR_TYPE) final ExamType type,
            @JsonProperty(EXAM.ATTR_OWNER) final String owner,
            @JsonProperty(EXAM.ATTR_SUPPORTER) final Collection<String> supporter,
            @JsonProperty(EXAM.ATTR_STATUS) final ExamStatus status,
            @JsonProperty(EXAM.ATTR_QUIT_PASSWORD) final String quitPassword,
            @JsonProperty(EXAM.ATTR_LMS_SEB_RESTRICTION) final Boolean sebRestriction,
            @JsonProperty(EXAM.ATTR_BROWSER_KEYS) final String browserExamKeys,
            @JsonProperty(EXAM.ATTR_ACTIVE) final Boolean active,
            @JsonProperty(EXAM.ATTR_LASTUPDATE) final String lastUpdate,
            @JsonProperty(EXAM.ATTR_EXAM_TEMPLATE_ID) final Long examTemplateId,
            @JsonProperty(EXAM.ATTR_LAST_MODIFIED) final Long lastModified,
            @JsonProperty(API.PARAM_ADDITIONAL_ATTRIBUTES) final Map<String, String> additionalAttributes) {

        this.id = id;
        this.institutionId = institutionId;
        this.lmsSetupId = lmsSetupId;
        this.externalId = externalId;
        this.lmsAvailable = lmsAvailable;
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.type = type;
        this.owner = owner;
        this.status = (status != null) ? status : getStatusFromDate(startTime, endTime);
        this.quitPassword = quitPassword;
        this.sebRestriction = sebRestriction;
        this.browserExamKeys = browserExamKeys;
        this.active = (active != null) ? active : Boolean.TRUE;
        this.lastUpdate = lastUpdate;
        this.examTemplateId = examTemplateId;
        this.lastModified = lastModified;

        this.supporter = (supporter != null)
                ? Collections.unmodifiableCollection(supporter)
                : Collections.emptyList();

        this.additionalAttributes = Utils.immutableMapOf(additionalAttributes);

        this.checkASK = BooleanUtils
                .toBoolean(this.additionalAttributes.get(Exam.ADDITIONAL_ATTR_SIGNATURE_KEY_CHECK_ENABLED));
        this.allowedSEBVersions = initAllowedSEBVersions();
    }

    public Exam(final POSTMapper postMap) {
        this.id = null;
        this.institutionId = postMap.getLong(EXAM.ATTR_INSTITUTION_ID);
        this.lmsSetupId = postMap.getLong(EXAM.ATTR_LMS_SETUP_ID);
        this.externalId = postMap.getString(EXAM.ATTR_EXTERNAL_ID);
        this.lmsAvailable = true;
        this.name = postMap.getString(EXAM.ATTR_QUIZ_NAME);
        this.startTime = postMap.getDateTime(EXAM.ATTR_QUIZ_START_TIME);
        this.endTime = postMap.getDateTime(EXAM.ATTR_QUIZ_END_TIME);
        this.type = postMap.getEnum(EXAM.ATTR_TYPE, ExamType.class, ExamType.UNDEFINED);
        this.owner = postMap.getString(EXAM.ATTR_OWNER);
        this.status = postMap.getEnum(EXAM.ATTR_STATUS, ExamStatus.class, getStatusFromDate(this.startTime, this.endTime));
        this.quitPassword = postMap.getString(EXAM.ATTR_QUIT_PASSWORD);
        this.sebRestriction = null;
        this.browserExamKeys = null;
        this.active = postMap.getBoolean(EXAM.ATTR_ACTIVE);
        this.supporter = postMap.getStringSet(EXAM.ATTR_SUPPORTER);
        this.lastUpdate = null;
        this.examTemplateId = postMap.getLong(EXAM.ATTR_EXAM_TEMPLATE_ID);
        this.lastModified = null;

        final Map<String, String> additionalAttributes = new HashMap<>();
        if (postMap.contains(QuizData.QUIZ_ATTR_DESCRIPTION)) {
            additionalAttributes.put(QuizData.QUIZ_ATTR_DESCRIPTION, postMap.getString(QuizData.QUIZ_ATTR_DESCRIPTION));
        }
        additionalAttributes.put(QuizData.QUIZ_ATTR_START_URL, postMap.getString(QuizData.QUIZ_ATTR_START_URL));
        this.additionalAttributes = Utils.immutableMapOf(additionalAttributes);

        this.checkASK = BooleanUtils
                .toBoolean(this.additionalAttributes.get(Exam.ADDITIONAL_ATTR_SIGNATURE_KEY_CHECK_ENABLED));
        this.allowedSEBVersions = initAllowedSEBVersions();
    }

    public Exam(final QuizData quizData) {
        this(null, quizData, POSTMapper.EMPTY_MAP);
    }
    public Exam(final String modelId, final QuizData quizData, final POSTMapper mapper) {

        final Map<String, String> additionalAttributes = new HashMap<>(quizData.getAdditionalAttributes());
        additionalAttributes.put(QuizData.QUIZ_ATTR_DESCRIPTION, quizData.description);
        additionalAttributes.put(QuizData.QUIZ_ATTR_START_URL, quizData.startURL);

        this.id = (modelId != null) ? Long.parseLong(modelId) : null;
        this.institutionId = quizData.institutionId;
        this.lmsSetupId = quizData.lmsSetupId;
        this.externalId = quizData.id;
        this.lmsAvailable = true;
        this.name = quizData.name;
        this.startTime = quizData.startTime;
        this.endTime = quizData.endTime;
        this.type = mapper.getEnum(EXAM.ATTR_TYPE, ExamType.class, ExamType.UNDEFINED);
        this.owner = mapper.getString(EXAM.ATTR_OWNER);
        this.status = mapper.getEnum(
                EXAM.ATTR_STATUS,
                ExamStatus.class,
                getStatusFromDate(this.startTime, this.endTime));
        this.quitPassword = mapper.getString(EXAM.ATTR_QUIT_PASSWORD);
        this.sebRestriction = null;
        this.browserExamKeys = mapper.getString(EXAM.ATTR_BROWSER_KEYS);
        this.active = mapper.getBoolean(EXAM.ATTR_ACTIVE);
        this.supporter = mapper.getStringSet(EXAM.ATTR_SUPPORTER);
        this.lastUpdate = null;
        this.examTemplateId = mapper.getLong(EXAM.ATTR_EXAM_TEMPLATE_ID);
        this.lastModified = null;
        this.additionalAttributes = Utils.immutableMapOf(additionalAttributes);

        this.checkASK = BooleanUtils
                .toBoolean(this.additionalAttributes.get(Exam.ADDITIONAL_ATTR_SIGNATURE_KEY_CHECK_ENABLED));
        this.allowedSEBVersions = initAllowedSEBVersions();
    }

    private List<AllowedSEBVersion> initAllowedSEBVersions() {
        if (this.additionalAttributes.containsKey(ADDITIONAL_ATTR_ALLOWED_SEB_VERSIONS)) {
            final String asvString = this.additionalAttributes.get(Exam.ADDITIONAL_ATTR_ALLOWED_SEB_VERSIONS);
            final String[] split = StringUtils.split(asvString, Constants.LIST_SEPARATOR);
            final List<AllowedSEBVersion> result = new ArrayList<>();
            for (final String s : split) {
                final AllowedSEBVersion allowedSEBVersion = new AllowedSEBVersion(s);
                if (allowedSEBVersion.isValidFormat) {
                    result.add(allowedSEBVersion);
                }
            }
            return result;
        } else {
            return Collections.emptyList();
        }
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

    public Boolean getLmsAvailable() {
        return this.lmsAvailable;
    }

    @JsonIgnore
    public boolean isLmsAvailable() {
        return BooleanUtils.isTrue(this.lmsAvailable);
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
        return this.getAdditionalAttribute(QuizData.QUIZ_ATTR_DESCRIPTION);
    }

    public DateTime getStartTime() {
        return this.startTime;
    }

    public DateTime getEndTime() {
        return this.endTime;
    }

    public String getStartURL() {
        return this.getAdditionalAttribute(QuizData.QUIZ_ATTR_START_URL);
    }

    public ExamStatus getStatus() {
        return this.status;
    }

    public String getQuitPassword() {
        return quitPassword;
    }

    public String getBrowserExamKeys() {
        return this.browserExamKeys;
    }

    public Boolean getActive() {
        return this.active;
    }

    public Long getExamTemplateId() {
        return this.examTemplateId;
    }

    public Long getLastModified() {
        return this.lastModified;
    }

    public boolean additionalAttributesIncluded() {
        return this.additionalAttributes != null && !this.additionalAttributes.isEmpty();
    }

    public String getAdditionalAttribute(final String attrName) {
        if (this.additionalAttributes == null || !this.additionalAttributes.containsKey(attrName)) {
            return null;
        }

        return this.additionalAttributes.get(attrName);
    }

    @Override
    public Exam printSecureCopy() {
        return new Exam(
        id,
        institutionId,
        lmsSetupId,
        externalId,
        lmsAvailable,
        name,
        startTime,
        endTime,
        type,
        owner,
        supporter,
        status,
        "--",
        sebRestriction,
        "--",
        active,
        lastUpdate,
        examTemplateId,
        lastModified,
        Collections.emptyMap());
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
        builder.append(Utils.truncateText(this.getDescription(), 32));
        builder.append(", startTime=");
        builder.append(this.startTime);
        builder.append(", endTime=");
        builder.append(this.endTime);
        builder.append(", startURL=");
        builder.append(this.getStartURL());
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
