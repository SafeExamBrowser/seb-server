/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.api;

public final class API {

    public enum BulkActionType {
        HARD_DELETE,
        DEACTIVATE,
        ACTIVATE;
    }

    public static final String PARAM_LOGO_IMAGE = "logoImageBase64";
    public static final String PARAM_INSTITUTION_ID = "institutionId";
    public static final String PARAM_MODEL_ID = "modelId";
    public static final String PARAM_MODEL_ID_LIST = "modelIds";
    public static final String PARAM_PARENT_MODEL_ID = "parentModelId";
    public static final String PARAM_ENTITY_TYPE = "entityType";
    public static final String PARAM_BULK_ACTION_TYPE = "bulkActionType";
    public static final String PARAM_CLIENT_CONFIG_SECRET = "client_config_secret";

    public static final String INSTITUTION_VAR_PATH_SEGMENT = "/{" + PARAM_INSTITUTION_ID + "}";
    public static final String MODEL_ID_VAR_PATH_SEGMENT = "/{" + PARAM_MODEL_ID + "}";

    public static final String OAUTH_TOKEN_ENDPOINT = "/oauth/token"; // TODO to config properties?
    public static final String OAUTH_REVOKE_TOKEN_ENDPOINT = "/oauth/revoke-token"; // TODO to config properties?
    public static final String CURRENT_USER_ENDPOINT = API.USER_ACCOUNT_ENDPOINT + "/me";

    public static final String INFO_ENDPOINT = "/info";

    public static final String LOGO_PATH_SEGMENT = "/logo";
    public static final String INSTITUTIONAL_LOGO_PATH = LOGO_PATH_SEGMENT + "/{urlSuffix}";

    public static final String PRIVILEGES_PATH_SEGMENT = "/privileges";
    public static final String PRIVILEGES_ENDPOINT = INFO_ENDPOINT + PRIVILEGES_PATH_SEGMENT;

    public static final String INSTITUTION_ENDPOINT = "/institution";

    public static final String LMS_SETUP_ENDPOINT = "/lms_setup";
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

    public static final String EXAM_INDICATOR_ENDPOINT = "/indicator";

    public static final String SEB_CLIENT_CONFIG_ENDPOINT = "/client_configuration";
    public static final String SEB_CLIENT_CONFIG_DOWNLOAD_PATH_SEGMENT = "/download";

    public static final String CONFIGURATION_NODE_ENDPOINT = "/configuration_node";
    public static final String CONFIGURATION_FOLLOWUP_PATH_SEGMENT = "/followup";
    public static final String CONFIGURATION_ENDPOINT = "/configuration";
    public static final String CONFIGURATION_SAVE_TO_HISTORY_PATH_SEGMENT = "/save_to_history";
    public static final String CONFIGURATION_RESTORE_FROM_HISTORY_PATH_SEGMENT = "/restore";

    public static final String CONFIGURATION_VALUE_ENDPOINT = "/configuration_value";
    public static final String CONFIGURATION_TABLE_VALUE_PATH_SEGMENT = "/table";
    public static final String CONFIGURATION_TABLE_ROW_VALUE_PATH_SEGMENT =
            CONFIGURATION_TABLE_VALUE_PATH_SEGMENT + "/row";

    public static final String CONFIGURATION_ATTRIBUTE_ENDPOINT = "/configuration_attribute";

    public static final String ORIENTATION_ENDPOINT = "/orientation";

    public static final String VIEW_ENDPOINT = ORIENTATION_ENDPOINT + "/view";

    public static final String EXAM_CONFIGURATION_MAP_ENDPOINT = "/exam_configuration_map";

    public static final String USER_ACTIVITY_LOG_ENDPOINT = "/useractivity";

    public static final String SELF_PATH_SEGMENT = "/self";

    public static final String NAMES_PATH_SEGMENT = "/names";

    public static final String LIST_PATH_SEGMENT = "/list";

    public static final String ACTIVE_PATH_SEGMENT = "/active";

    public static final String INACTIVE_PATH_SEGMENT = "/inactive";

    public static final String DEPENDENCY_PATH_SEGMENT = "/dependency";

    public static final String PASSWORD_PATH_SEGMENT = "/password";

    public static final String PATH_VAR_ACTIVE = MODEL_ID_VAR_PATH_SEGMENT + ACTIVE_PATH_SEGMENT;
    public static final String PATH_VAR_INACTIVE = MODEL_ID_VAR_PATH_SEGMENT + INACTIVE_PATH_SEGMENT;

    // *************************
    // ** Exam API
    // *************************

    public static final String EXAM_API_PARAM_EXAM_ID = "examId";

    public static final String EXAM_API_SEB_CONNECTION_TOKEN = "seb-connection-token";

    public static final String EXAM_API_HANDSHAKE_ENDPOINT = "/handshake";

    public static final String EXAM_API_CONFIGURATION_REQUEST_ENDPOINT = "/examconfig";

    public static final String EXAM_API_PING_ENDPOINT = "/sebping";

    public static final String EXAM_API_EVENT_ENDPOINT = "/sebevent";

}
