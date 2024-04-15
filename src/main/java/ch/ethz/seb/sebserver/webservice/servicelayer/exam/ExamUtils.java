/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.exam.*;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.FieldError;

public final class ExamUtils {

    public static void newExamFieldValidation(final POSTMapper postParams) {
        noLMSFieldValidation(new Exam(postParams));
    }

    public static Exam noLMSFieldValidation(final Exam exam) {

        // This only applies to exams that has no LMS
        if (exam.lmsSetupId != null) {
            return exam;
        }

        final Collection<APIMessage> validationErrors = new ArrayList<>();
        if (StringUtils.isBlank(exam.name)) {
            validationErrors.add(APIMessage.fieldValidationError(
                    Domain.EXAM.ATTR_QUIZ_NAME,
                    "exam:quizName:notNull"));
        } else {
            final int length = exam.name.length();
            if (length < 3 || length > 255) {
                validationErrors.add(APIMessage.fieldValidationError(
                        Domain.EXAM.ATTR_QUIZ_NAME,
                        "exam:quizName:size:3:255:" + length));
            }
        }

        if (StringUtils.isBlank(exam.getStartURL())) {
            validationErrors.add(APIMessage.fieldValidationError(
                    QuizData.QUIZ_ATTR_START_URL,
                    "exam:quiz_start_url:notNull"));
        } else {
            try {
                new URL(exam.getStartURL()).toURI();
            } catch (final Exception e) {
                validationErrors.add(APIMessage.fieldValidationError(
                        QuizData.QUIZ_ATTR_START_URL,
                        "exam:quiz_start_url:invalidURL"));
            }
        }

        if (exam.startTime == null) {
            validationErrors.add(APIMessage.fieldValidationError(
                    Domain.EXAM.ATTR_QUIZ_START_TIME,
                    "exam:quizStartTime:notNull"));
        } else if (exam.endTime != null) {
            if (exam.startTime
                    .isAfter(exam.endTime)) {
                validationErrors.add(APIMessage.fieldValidationError(
                        Domain.EXAM.ATTR_QUIZ_END_TIME,
                        "exam:quizEndTime:endBeforeStart"));
            }
        }

        if (!validationErrors.isEmpty()) {
            throw new APIMessage.APIMessageException(validationErrors);
        }

        return exam;
    }

    /** Used to check threshold consistency for a given list of thresholds.
     * Checks if all values are present (none null value)
     * Checks if there are duplicates
     * <p>
     * If a check fails, the methods throws a APIMessageException with a FieldError to notify the caller
     *
     * @param thresholds List of Threshold */
    public static void checkThresholdConsistency(final List<Indicator.Threshold> thresholds) {
        if (thresholds != null) {
            final List<Indicator.Threshold> emptyThresholds = thresholds.stream()
                    .filter(t -> t.getValue() == null || t.getColor() == null)
                    .toList();

            if (!emptyThresholds.isEmpty()) {
                throw new APIMessage.APIMessageException(APIMessage.fieldValidationError(
                        new FieldError(
                                Domain.EXAM.TYPE_NAME,
                                Domain.EXAM.ATTR_SUPPORTER,
                                "indicator:thresholds:thresholdEmpty")));
            }

            final Set<Double> values = thresholds.stream()
                    .map(Indicator.Threshold::getValue)
                    .collect(Collectors.toSet());

            if (values.size() != thresholds.size()) {
                throw new APIMessage.APIMessageException(APIMessage.fieldValidationError(
                        new FieldError(
                                Domain.EXAM.TYPE_NAME,
                                Domain.EXAM.ATTR_SUPPORTER,
                                "indicator:thresholds:thresholdDuplicate")));
            }
        }
    }

    /** Used to check client group consistency for a given ClientGroup.
     * Checks if correct entries for specific type
     * <p>
     * If a check fails, the methods throws a APIMessageException with a FieldError to notify the caller
     *
     * @param clientGroup ClientGroup instance to check */
    public static <T extends ClientGroupData> T checkClientGroupConsistency(final T clientGroup) {
        final ClientGroupData.ClientGroupType type = clientGroup.getType();
        if (type == null || type == ClientGroupData.ClientGroupType.NONE) {
            throw new APIMessage.APIMessageException(APIMessage.fieldValidationError(
                    new FieldError(
                            Domain.CLIENT_GROUP.TYPE_NAME,
                            Domain.CLIENT_GROUP.ATTR_TYPE,
                            "clientGroup:type:notNull")));
        }

        switch (type) {
            case IP_V4_RANGE: {
                checkIPRange(clientGroup.getIpRangeStart(), clientGroup.getIpRangeEnd());
                break;
            }
            case CLIENT_OS: {
                checkClientOS(clientGroup.getClientOS());
                break;
            }
            default: {
                throw new APIMessage.APIMessageException(APIMessage.fieldValidationError(
                        new FieldError(
                                Domain.CLIENT_GROUP.TYPE_NAME,
                                Domain.CLIENT_GROUP.ATTR_TYPE,
                                "clientGroup:type:typeInvalid")));
            }
        }

        return clientGroup;
    }

    static void checkIPRange(final String ipRangeStart, final String ipRangeEnd) {
        final long startIP = Utils.ipToLong(ipRangeStart);
        if (StringUtils.isBlank(ipRangeStart) || startIP < 0) {
            throw new APIMessage.APIMessageException(APIMessage.fieldValidationError(
                    new FieldError(
                            Domain.CLIENT_GROUP.TYPE_NAME,
                            ClientGroup.ATTR_IP_RANGE_START,
                            "clientGroup:ipRangeStart:invalidIP")));
        }
        final long endIP = Utils.ipToLong(ipRangeEnd);
        if (StringUtils.isBlank(ipRangeEnd) || endIP < 0) {
            throw new APIMessage.APIMessageException(APIMessage.fieldValidationError(
                    new FieldError(
                            Domain.CLIENT_GROUP.TYPE_NAME,
                            ClientGroup.ATTR_IP_RANGE_END,
                            "clientGroup:ipRangeEnd:invalidIP")));
        }

        if (endIP <= startIP) {
            throw new APIMessage.APIMessageException(APIMessage.fieldValidationError(
                    new FieldError(
                            Domain.CLIENT_GROUP.TYPE_NAME,
                            ClientGroup.ATTR_IP_RANGE_START,
                            "clientGroup:ipRangeStart:invalidIPRange")),
                    APIMessage.fieldValidationError(
                            new FieldError(
                                    Domain.CLIENT_GROUP.TYPE_NAME,
                                    ClientGroup.ATTR_IP_RANGE_END,
                                    "clientGroup:ipRangeEnd:invalidIPRange")));
        }

    }

    public static void checkClientOS(final ClientGroupData.ClientOS clientOS) {
        if (clientOS == null) {
            throw new APIMessage.APIMessageException(APIMessage.fieldValidationError(
                    new FieldError(
                            Domain.CLIENT_GROUP.TYPE_NAME,
                            ClientGroupData.ATTR_CLIENT_OS,
                            "clientGroup:clientOS:notNull")));
        }
    }

}
