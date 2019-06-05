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

    void validate(ConfigurationValue value) throws FieldValidationException;

    void validate(ConfigurationTableValues tableValue) throws FieldValidationException;

    void exportPlainXML(OutputStream out, Long institutionId, Long configurationNodeId);

    void exportForExam(OutputStream out, Long configExamMappingId);

    String generateConfigKey(Long configurationNodeId);

}
