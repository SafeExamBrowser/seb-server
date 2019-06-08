/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig;

import java.io.OutputStream;

import ch.ethz.seb.sebserver.gbl.api.APIMessage.FieldValidationException;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;

/** The base interface and service for all SEB Exam Configuration related functionality. */
public interface SebExamConfigService {

    /** Validates a given ConfigurationValue by using registered ConfigurationValueValodator
     * beans to find a proper validator for the specified ConfigurationValue
     *
     * @param value The ConfigurationValue to validate
     * @throws FieldValidationException on validation exception */
    void validate(ConfigurationValue value) throws FieldValidationException;

    /** Validates a ConfigurationTableValues container by extracting each value and
     * validate each, collecting the error if there are some.
     *
     * @param tableValue The ConfigurationTableValues container
     * @throws FieldValidationException on validation exception */
    void validate(ConfigurationTableValues tableValue) throws FieldValidationException;

    /** Used to export a specified SEB Exam Configuration as plain XML
     * This exports the values of the follow-up configuration defined by a given
     * ConfigurationNode (configurationNodeId)
     *
     * @param out The output stream to write the plain XML text to.
     * @param institutionId The identifier of the institution of the requesting user
     * @param configurationNodeId the identifier of the ConfigurationNode to export */
    void exportPlainXML(OutputStream out, Long institutionId, Long configurationNodeId);

    /** Used to export a SEB Exam Configuration within its defined Configuration Exam Mapping.
     * either with encryption if defined or as plain text within the SEB Configuration format
     * as described here: https://www.safeexambrowser.org/developer/seb-file-format.html
     *
     * @param out The output stream to write the export data to
     * @param configExamMappingId The identifier of the Exam Configuration Mapping */
    void exportForExam(OutputStream out, Long configExamMappingId);

    /** TODO */
    String generateConfigKey(Long configurationNodeId);

}
