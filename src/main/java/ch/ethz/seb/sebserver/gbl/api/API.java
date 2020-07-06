/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.api;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;

public final class API {

    public enum BulkActionType {
        HARD_DELETE,
        DEACTIVATE,
        ACTIVATE
    }

    public static final String SEB_FILE_EXTENSION = "seb";

    public static final String PARAM_LOGO_IMAGE = "logoImageBase64";
    public static final String PARAM_INSTITUTION_ID = "institutionId";
    public static final String PARAM_MODEL_ID = "modelId";
    public static final String PARAM_MODEL_ID_LIST = "modelIds";
    public static final String PARAM_PARENT_MODEL_ID = "parentModelId";
    public static final String PARAM_ENTITY_TYPE = "entityType";
    public static final String PARAM_BULK_ACTION_TYPE = "bulkActionType";
    public static final String PARAM_VIEW_ID = "viewId";
    public static final String PARAM_INSTRUCTION_TYPE = "instructionType";
    public static final String PARAM_INSTRUCTION_ATTRIBUTES = "instructionAttributes";

    public static final String DEFAULT_CONFIG_TEMPLATE_ID = String.valueOf(ConfigurationNode.DEFAULT_TEMPLATE_ID);

    public static final String INSTITUTION_VAR_PATH_SEGMENT = "/{" + PARAM_INSTITUTION_ID + "}";
    public static final String MODEL_ID_VAR_PATH_SEGMENT = "/{" + PARAM_MODEL_ID + "}";
    public static final String PARENT_MODEL_ID_VAR_PATH_SEGMENT = "/{" + PARAM_PARENT_MODEL_ID + "}";

    public static final String OAUTH_ENDPOINT = "/oauth";
    public static final String OAUTH_TOKEN_ENDPOINT = OAUTH_ENDPOINT + "/token";
    public static final String OAUTH_REVOKE_TOKEN_ENDPOINT = OAUTH_ENDPOINT + "/revoke-token";

    public static final String CURRENT_USER_PATH_SEGMENT = "/me";
    public static final String CURRENT_USER_ENDPOINT = API.USER_ACCOUNT_ENDPOINT + CURRENT_USER_PATH_SEGMENT;
    public static final String SELF_PATH_SEGMENT = "/self";
    public static final String LOGIN_PATH_SEGMENT = "/loglogin";
    public static final String LOGOUT_PATH_SEGMENT = "/loglogout";

    public static final String INFO_ENDPOINT = "/info";
    public static final String INFO_PARAM_INST_SUFFIX = "urlSuffix";
    public static final String INFO_INST_PATH_SEGMENT = "/institution";
    public static final String INFO_INST_ENDPOINT = INFO_INST_PATH_SEGMENT + "/{" + INFO_PARAM_INST_SUFFIX + "}";
    public static final String LOGO_PATH_SEGMENT = "/logo";

    public static final String INSTITUTIONAL_LOGO_PATH = LOGO_PATH_SEGMENT + "/{" + INFO_PARAM_INST_SUFFIX + "}";
    public static final String REGISTER_ENDPOINT = "/register";

    public static final String NAMES_PATH_SEGMENT = "/names";

    public static final String LIST_PATH_SEGMENT = "/list";

    public static final String ACTIVE_PATH_SEGMENT = "/active";

    public static final String INACTIVE_PATH_SEGMENT = "/inactive";

    public static final String DEPENDENCY_PATH_SEGMENT = "/dependency";

    public static final String PASSWORD_PATH_SEGMENT = "/password";

    public static final String PATH_VAR_ACTIVE = MODEL_ID_VAR_PATH_SEGMENT + ACTIVE_PATH_SEGMENT;
    public static final String PATH_VAR_INACTIVE = MODEL_ID_VAR_PATH_SEGMENT + INACTIVE_PATH_SEGMENT;

    public static final String PRIVILEGES_PATH_SEGMENT = "/privileges";
    public static final String PRIVILEGES_ENDPOINT = INFO_ENDPOINT + PRIVILEGES_PATH_SEGMENT;

    // *************************
    // ** SEB Client API
    // *************************

    public static final String EXAM_API_PARAM_EXAM_ID = "examId";

    public static final String EXAM_API_SEB_CONNECTION_TOKEN = "SEBConnectionToken";

    public static final String EXAM_API_USER_SESSION_ID = "seb_user_session_id";

    public static final String EXAM_API_HANDSHAKE_ENDPOINT = "/handshake";

    public static final String EXAM_API_CONFIGURATION_REQUEST_ENDPOINT = "/examconfig";

    public static final String EXAM_API_PING_ENDPOINT = "/sebping";

    public static final String EXAM_API_PING_TIMESTAMP = "timestamp";

    public static final String EXAM_API_PING_NUMBER = "ping-number";

    public static final String EXAM_API_EVENT_ENDPOINT = "/seblog";

    // *************************
    // ** Domain Object API
    // *************************

    public static final String INSTITUTION_ENDPOINT = "/institution";

    public static final String LMS_SETUP_ENDPOINT = "/lms-setup";
    public static final String LMS_SETUP_TEST_PATH_SEGMENT = "/test";
    public static final String LMS_SETUP_TEST_ENDPOINT = LMS_SETUP_ENDPOINT
            + LMS_SETUP_TEST_PATH_SEGMENT
            + MODEL_ID_VAR_PATH_SEGMENT;
    public static final String LMS_SETUP_TEST_AD_HOC_PATH_SEGMENT = "/adhoc";
    public static final String LMS_SETUP_TEST_AD_HOC_ENDPOINT = LMS_SETUP_ENDPOINT
            + LMS_SETUP_TEST_PATH_SEGMENT
            + LMS_SETUP_TEST_AD_HOC_PATH_SEGMENT;

    public static final String USER_ACCOUNT_ENDPOINT = "/useraccount";

    public static final String QUIZ_DISCOVERY_ENDPOINT = "/quiz";

    public static final String EXAM_ADMINISTRATION_ENDPOINT = "/exam";
    public static final String EXAM_ADMINISTRATION_DOWNLOAD_CONFIG_PATH_SEGMENT = "/download-config";
    public static final String EXAM_ADMINISTRATION_CONSISTENCY_CHECK_PATH_SEGMENT = "/check-consistency";
    public static final String EXAM_ADMINISTRATION_SEB_RESTRICTION_PATH_SEGMENT = "/seb-restriction";
    public static final String EXAM_ADMINISTRATION_CHECK_RESTRICTION_PATH_SEGMENT = "/check-seb-restriction";
    public static final String EXAM_ADMINISTRATION_CHECK_IMPORTED_PATH_SEGMENT = "/check-imported";
    public static final String EXAM_ADMINISTRATION_SEB_RESTRICTION_CHAPTERS_PATH_SEGMENT = "/chapters";

    public static final String EXAM_INDICATOR_ENDPOINT = "/indicator";

    public static final String SEB_CLIENT_CONFIG_ENDPOINT = "/client_configuration";
    public static final String SEB_CLIENT_CONFIG_DOWNLOAD_PATH_SEGMENT = "/download";

    public static final String CONFIGURATION_NODE_ENDPOINT = "/configuration-node";
    public static final String CONFIGURATION_FOLLOWUP_PATH_SEGMENT = "/followup";
    public static final String CONFIGURATION_CONFIG_KEY_PATH_SEGMENT = "/configkey";
    public static final String CONFIGURATION_ENDPOINT = "/configuration";
    public static final String CONFIGURATION_SAVE_TO_HISTORY_PATH_SEGMENT = "/save-to-history";
    public static final String CONFIGURATION_UNDO_PATH_SEGMENT = "/undo";
    public static final String CONFIGURATION_COPY_PATH_SEGMENT = "/copy";
    public static final String CONFIGURATION_RESTORE_FROM_HISTORY_PATH_SEGMENT = "/restore";
    public static final String CONFIGURATION_VALUE_ENDPOINT = "/configuration_value";
    public static final String CONFIGURATION_TABLE_VALUE_PATH_SEGMENT = "/table";
    public static final String CONFIGURATION_ATTRIBUTE_ENDPOINT = "/configuration_attribute";
    public static final String CONFIGURATION_PLAIN_XML_DOWNLOAD_PATH_SEGMENT = "/downloadxml";
    public static final String CONFIGURATION_IMPORT_PATH_SEGMENT = "/import";
    public static final String IMPORT_PASSWORD_ATTR_NAME = "importFilePassword";
    public static final String IMPORT_FILE_ATTR_NAME = "importFile";

    public static final String TEMPLATE_ATTRIBUTE_ENDPOINT = "/template-attribute";
    public static final String TEMPLATE_ATTRIBUTE_RESET_VALUES = "/reset";
    public static final String TEMPLATE_ATTRIBUTE_REMOVE_ORIENTATION = "/remove-orientation";
    public static final String TEMPLATE_ATTRIBUTE_ATTACH_DEFAULT_ORIENTATION = "/attach-default-orientation";

    public static final String ORIENTATION_ENDPOINT = "/orientation";
    public static final String VIEW_ENDPOINT = ORIENTATION_ENDPOINT + "/view";

    public static final String EXAM_CONFIGURATION_MAP_ENDPOINT = "/exam-configuration-map";

    public static final String USER_ACTIVITY_LOG_ENDPOINT = "/useractivity";

    public static final String EXAM_MONITORING_ENDPOINT = "/monitoring";
    public static final String EXAM_MONITORING_INSTRUCTION_ENDPOINT = "/instruction";
    public static final String EXAM_MONITORING_DISABLE_CONNECTION_ENDPOINT = "/disable-connection";
    public static final String EXAM_MONITORING_SEB_CONNECTION_TOKEN_PATH_SEGMENT =
            "/{" + EXAM_API_SEB_CONNECTION_TOKEN + "}";
    public static final String EXAM_MONITORING_STATE_FILTER = "hidden-states";

    public static final String SEB_CLIENT_CONNECTION_ENDPOINT = "/seb-client-connection";

    public static final String SEB_CLIENT_EVENT_ENDPOINT = "/seb-client-event";
    public static final String SEB_CLIENT_EVENT_SEARCH_PATH_SEGMENT = "/search";
    public static final String SEB_CLIENT_EVENT_EXTENDED_PAGE_ENDPOINT = SEB_CLIENT_EVENT_ENDPOINT
            + SEB_CLIENT_EVENT_SEARCH_PATH_SEGMENT;

}
