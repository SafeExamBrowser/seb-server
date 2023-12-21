/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.api.APIMessage.FieldValidationException;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.util.Result;

/** The base interface and service for all SEB Exam Configuration related functionality. */
public interface ExamConfigService {

    /** Validates a given ConfigurationValue by using registered ConfigurationValueValidator
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

    /** Get the follow-up configuration identifier for a given configuration node identifier.
     *
     * @param examConfigNodeId the exam configuration node identifier
     * @return Result refer to the follow-up configuration identifier of the given config node or to an error when
     *         happened */
    Result<Long> getFollowupConfigurationId(final Long examConfigNodeId);

    /** Used to export a specified SEB Exam Configuration as plain XML
     * This exports the values of the last published changes of a given
     * ConfigurationNode (configurationNodeId)
     *
     * @param out The output stream to write the plain XML text to.
     * @param institutionId The identifier of the institution of the requesting user
     * @param configurationNodeId the identifier of the ConfigurationNode to export
     * @param followup indicates if the follow-up configuration entry shall be used or otherwise the last stable
     *            to export SEB Settings */
    void exportPlainXML(
            OutputStream out,
            Long institutionId,
            Long configurationNodeId,
            boolean followup);

    /** Used to export the default SEB Exam Configuration for a given exam identifier.
     * either with encryption if defined or as plain text within the SEB Configuration format
     * as described here: https://www.safeexambrowser.org/developer/seb-file-format.html
     *
     * @param out The output stream to write the export data to
     * @param institutionId The identifier of the institution of the requesting user
     * @param examId the exam identifier
     * @return The configuration node identifier (PK) */
    default Long exportForExam(final OutputStream out, final Long institutionId, final Long examId) {
        return exportForExam(out, institutionId, examId, (String) null);
    }

    /** Used to export the default SEB Exam Configuration for a given exam identifier.
     * either with encryption if defined or as plain text within the SEB Configuration format
     * as described here: https://www.safeexambrowser.org/developer/seb-file-format.html
     *
     * @param out The output stream to write the export data to
     * @param institutionId The identifier of the institution of the requesting user
     * @param examId the exam identifier
     * @param clientGroupId the client group id if there is a separated exam config for defined group
     * @return The configuration node identifier (PK) */
    Long exportForExam(OutputStream out, Long institutionId, Long examId, String clientGroupId);

    /** Used to export the default SEB Exam Configuration for a given exam identifier.
     * either with encryption if defined or as plain text within the SEB Configuration format
     * as described here: https://www.safeexambrowser.org/developer/seb-file-format.html
     *
     * @param out The output stream to write the export data to
     * @param institutionId The identifier of the institution of the requesting user
     * @param examId the exam identifier that defines the mapping
     * @param configurationNodeId the configurationNodeId that defines the mapping
     * @return The configuration node identifier (PK) */
    Long exportForExam(OutputStream out, Long institutionId, Long examId, Long configurationNodeId);

    /** Generates a Config-Key from the SEB exam configuration defined by configurationNodeId.
     * See https://www.safeexambrowser.org/developer/seb-config-key.html for more information about the Config-Key
     *
     * @param institutionId the institutional id
     * @param configurationNodeId the configurationNodeId
     * @param followup indicates if the follow-up configuration entry shall be used or otherwise the last stable
     *            to calculate the config key
     * @return Result refer to the generated Config-Key or to an error if happened. */
    Result<String> generateConfigKey(
            Long institutionId,
            Long configurationNodeId,
            boolean followup);

    /** Generates a list of Config-Key from a given Exam by collecting all the SEB Exam Configurations that are attached
     * to this Exam
     * See https://www.safeexambrowser.org/developer/seb-config-key.html for more information about the Config-Key
     *
     * @param institutionId the institutional id
     * @param examId the Exam identifier
     * @return Result refer to a list of generated Config-Key for all configurations of the exam or to an error if
     *         happened. */
    Result<Collection<String>> generateConfigKeys(Long institutionId, Long examId);

    /** Imports a SEB Exam Configuration from a SEB File of the format:
     * https://www.safeexambrowser.org/developer/seb-file-format.html
     *
     * First tries to read the file from the given input stream and detect the file format. A password
     * is needed if the file is in an encrypted format.
     *
     * Then loads the ConfigurationNode on which the import should take place and performs a "save in history"
     * action first to allow to make an easy rollback or even later an undo by the user.
     *
     * Then parses the XML and adds each attribute to the new Configuration.
     *
     * @param config The Configuration to import the attribute values to
     * @param input The InputStream to get the SEB config file as byte-stream
     * @param password A password is only needed if the file is in an encrypted format
     * @return The newly created Configuration instance */
    Result<Configuration> importFromSEBFile(Configuration config, InputStream input, CharSequence password);

    /** Use this to check whether a specified ConfigurationNode has unpublished changes within the settings.
     *
     * This uses the Config Key of the actual and the follow-up settings to verify if there are changes made that
     * are not published yet.
     *
     * @param institutionId the institutional id
     * @param configurationNodeId the id if the ConfigurationNode
     * @return true if there are unpublished changed in the SEB setting of the follow-up for the specified
     *         ConfigurationNode */
    Result<Boolean> hasUnpublishedChanged(Long institutionId, Long configurationNodeId);

    /** Used to reset the settings of a given configuration to the settings of its origin template.
     * If the given configuration has no origin template, an error will be reported.
     *
     * NOTE: This do not publish the changes (applied template settings).
     *
     * @param configurationNode The ConfigurationNode
     * @return Result refer to the configuration with reseted settings or to an error when happened */
    Result<ConfigurationNode> resetToTemplateSettings(ConfigurationNode configurationNode);

    /** Checks if given configuration is ready to save.
     *
     * @param configurationNode the ConfigurationNode instance
     * @return Result refer to the given ConfigurationNode or to an error if the check has failed */
    Result<ConfigurationNode> checkSaveConsistency(ConfigurationNode configurationNode);

    /** Sets or resets an imported (hashed) quiz password
     *
     * @param node the ConfigurationNode
     * @param quitPassword the quit password to reset (if null or empty, no quit password shall be set)
     * @return Result refer to the origin ConfigurationNode or to an error when happened*/
    Result<ConfigurationNode> setQuitPassword(ConfigurationNode node, String quitPassword);
}
