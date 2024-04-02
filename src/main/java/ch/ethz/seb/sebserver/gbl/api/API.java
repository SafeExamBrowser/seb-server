/*
 * Copyright (c) 2018 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.api;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;

public final class API {

    public static final String ERROR_PATH = "/sebserver/error";
    public static final String CHECK_PATH = "/sebserver/check";

    public enum BulkActionType {
        HARD_DELETE,
        DEACTIVATE,
        ACTIVATE
    }

    public enum BatchActionType {
        EXAM_CONFIG_STATE_CHANGE(EntityType.CONFIGURATION_NODE),
        EXAM_CONFIG_REST_TEMPLATE_SETTINGS(EntityType.CONFIGURATION_NODE),
        EXAM_CONFIG_DELETE(EntityType.CONFIGURATION_NODE),
        ARCHIVE_EXAM(EntityType.EXAM),
        DELETE_EXAM(EntityType.EXAM);

        public final EntityType entityType;

        private BatchActionType(final EntityType entityType) {
            this.entityType = entityType;
        }
    }

    public static final String SEB_FILE_EXTENSION = "seb";

    public static final String PARAM_LOGO_IMAGE = "logoImageBase64";
    public static final String PARAM_INSTITUTION_ID = "institutionId";
    public static final String PARAM_MODEL_ID = "modelId";
    public static final String PARAM_MODEL_ID_LIST = "modelIds";
    public static final String PARAM_PARENT_MODEL_ID = "parentModelId";
    public static final String PARAM_ENTITY_TYPE = "entityType";

    public static final String PARAM_BULK_ACTION_TYPE = "bulkActionType";
    public static final String PARAM_BULK_ACTION_ADD_INCLUDES = "bulkActionAddIncludes";
    public static final String PARAM_BULK_ACTION_INCLUDES = "bulkActionIncludes";
    public static final String PARAM_VIEW_ID = "viewId";
    public static final String PARAM_INSTRUCTION_TYPE = "instructionType";
    public static final String PARAM_INSTRUCTION_ATTRIBUTES = "instructionAttributes";
    public static final String PARAM_ADDITIONAL_ATTRIBUTES = "additionalAttributes";

    public static final String DEFAULT_CONFIG_TEMPLATE_ID = String.valueOf(ConfigurationNode.DEFAULT_TEMPLATE_ID);

    public static final String INSTITUTION_VAR_PATH_SEGMENT = "/{" + PARAM_INSTITUTION_ID + "}";
    public static final String MODEL_ID_VAR_PATH_SEGMENT = "/{" + PARAM_MODEL_ID + "}";
    public static final String PARENT_MODEL_ID_VAR_PATH_SEGMENT = "/{" + PARAM_PARENT_MODEL_ID + "}";

    public static final String OAUTH_ENDPOINT = "/oauth";
    public static final String OAUTH_TOKEN_ENDPOINT = OAUTH_ENDPOINT + "/token";
    public static final String OAUTH_JWTTOKEN_ENDPOINT = OAUTH_ENDPOINT + "/jwttoken";
    public static final String OAUTH_REVOKE_TOKEN_ENDPOINT = OAUTH_ENDPOINT + "/revoke-token";

    public static final String CURRENT_USER_PATH_SEGMENT = "/me";
    public static final String CURRENT_USER_ENDPOINT = API.USER_ACCOUNT_ENDPOINT + CURRENT_USER_PATH_SEGMENT;
    public static final String SELF_PATH_SEGMENT = "/self";
    public static final String LOGIN_PATH_SEGMENT = "/loglogin";
    public static final String LOGOUT_PATH_SEGMENT = "/loglogout";

    public static final String FEATURES_PATH_SEGMENT = "/features";

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
    public static final String TOGGLE_ACTIVITY_PATH_SEGMENT = "/toggle-activity";
    public static final String INACTIVE_PATH_SEGMENT = "/inactive";

    public static final String FORCE_PATH_SEGMENT = "/force";

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

    public static final String EXAM_API_PARAM_CLIENT_ID = "client_id";

    public static final String EXAM_API_PARAM_SEB_OS_NAME = "seb_os_name";

    public static final String EXAM_API_PARAM_SEB_MACHINE_NAME = "seb_machine_name";

    public static final String EXAM_API_PARAM_SEB_VERSION = "seb_version";

    public static final String EXAM_API_PARAM_SIGNATURE_KEY = "seb_signature_key";

    public static final String EXAM_API_SEB_CONNECTION_TOKEN = "SEBConnectionToken";

    public static final String EXAM_API_EXAM_SIGNATURE_SALT_HEADER = "SEBExamSalt";

    public static final String EXAM_API_EXAM_ALT_BEK = "SEBServerBEK";

    public static final String EXAM_API_USER_SESSION_ID = "seb_user_session_id";

    public static final String EXAM_API_HANDSHAKE_ENDPOINT = "/handshake";

    public static final String EXAM_API_CONFIGURATION_REQUEST_ENDPOINT = "/examconfig";

    public static final String EXAM_API_CONFIGURATION_LIGHT_ENDPOINT = "/light-config";

    public static final String EXAM_API_PING_ENDPOINT = "/sebping";

    public static final String EXAM_API_PING_TIMESTAMP = "timestamp";

    public static final String EXAM_API_PING_NUMBER = "ping-number";

    public static final String EXAM_API_PING_INSTRUCTION_CONFIRM = "instruction-confirm";

    public static final String EXAM_API_EVENT_ENDPOINT = "/seblog";

    public static final String LOG_EVENT_TAG_BATTERY_STATUS = "<battery>";
    public static final String LOG_EVENT_TAG_WLAN_STATUS = "<wlan>";

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
    public static final String EXAM_ADMINISTRATION_ARCHIVE_PATH_SEGMENT = "/archive";
    public static final String EXAM_ADMINISTRATION_CONSISTENCY_CHECK_PATH_SEGMENT = "/check-consistency";
    public static final String EXAM_ADMINISTRATION_CONSISTENCY_CHECK_INCLUDE_RESTRICTION = "include-restriction";
    public static final String EXAM_ADMINISTRATION_SEB_RESTRICTION_PATH_SEGMENT = "/seb-restriction";
    public static final String EXAM_ADMINISTRATION_CHECK_RESTRICTION_PATH_SEGMENT = "/check-seb-restriction";
    public static final String EXAM_ADMINISTRATION_CHECK_IMPORTED_PATH_SEGMENT = "/check-imported";
    public static final String EXAM_ADMINISTRATION_SEB_RESTRICTION_CHAPTERS_PATH_SEGMENT = "/chapters";
    public static final String EXAM_ADMINISTRATION_PROCTORING_PATH_SEGMENT = "/proctoring";
    public static final String EXAM_ADMINISTRATION_SCREEN_PROCTORING_PATH_SEGMENT = "/screen-proctoring";
    public static final String EXAM_ADMINISTRATION_PROCTORING_RESET_PATH_SEGMENT = "reset";
    public static final String EXAM_ADMINISTRATION_SEB_SECURITY_KEY_GRANTS_PATH_SEGMENT = "/grant";
    public static final String EXAM_ADMINISTRATION_SEB_SECURITY_KEY_INFO_PATH_SEGMENT = "/sebkeyinfo";

    public static final String EXAM_INDICATOR_ENDPOINT = "/indicator";
    public static final String EXAM_CLIENT_GROUP_ENDPOINT = "/client-group";

    public static final String SEB_CLIENT_CONFIG_ENDPOINT = "/client_configuration";
    public static final String SEB_CLIENT_CONFIG_CREDENTIALS_PATH_SEGMENT = "/credentials";
    public static final String SEB_CLIENT_CONFIG_DOWNLOAD_PATH_SEGMENT = "/download";

    public static final String CONFIGURATION_NODE_ENDPOINT = "/configuration-node";
    public static final String CONFIGURATION_FOLLOWUP_PATH_SEGMENT = "/followup";
    public static final String PARAM_FOLLOWUP = "followup";
    public static final String CONFIGURATION_CONFIG_KEY_PATH_SEGMENT = "/configkey";
    public static final String CONFIGURATION_SETTINGS_PUBLISHED_PATH_SEGMENT = "/settings_published";
    public static final String CONFIGURATION_ENDPOINT = "/configuration";
    public static final String CONFIGURATION_SAVE_TO_HISTORY_PATH_SEGMENT = "/save-to-history";
    public static final String CONFIGURATION_UNDO_PATH_SEGMENT = "/undo";
    public static final String CONFIGURATION_COPY_PATH_SEGMENT = "/copy";
    public static final String CONFIGURATION_RESTORE_FROM_HISTORY_PATH_SEGMENT = "/restore";
    public static final String CONFIGURATION_RESET_TO_TEMPLATE_PATH_SEGMENT = "/reset-to-template";
    public static final String CONFIGURATION_VALUE_ENDPOINT = "/configuration_value";
    public static final String CONFIGURATION_TABLE_VALUE_PATH_SEGMENT = "/table";
    public static final String CONFIGURATION_ATTRIBUTE_ENDPOINT = "/configuration_attribute";
    public static final String CONFIGURATION_SEB_SETTINGS_DOWNLOAD_PATH_SEGMENT = "/downloadSettings";
    public static final String CONFIGURATION_IMPORT_PATH_SEGMENT = "/import";
    public static final String IMPORT_PASSWORD_ATTR_NAME = "importFilePassword";
    public static final String QUIT_PASSWORD_ATTR_NAME = "quitPassword";
    public static final String IMPORT_FILE_ATTR_NAME = "importFile";
    public static final String CONFIGURATION_SET_QUIT_PWD_PATH_SEGMENT = "/quitpwd";

    public static final String TEMPLATE_ATTRIBUTE_ENDPOINT = "/template-attribute";
    public static final String TEMPLATE_ATTRIBUTE_RESET_VALUES = "/reset";
    public static final String TEMPLATE_ATTRIBUTE_REMOVE_ORIENTATION = "/remove-orientation";
    public static final String TEMPLATE_ATTRIBUTE_ATTACH_DEFAULT_ORIENTATION = "/attach-default-orientation";

    public static final String ORIENTATION_ENDPOINT = "/orientation";
    public static final String VIEW_ENDPOINT = ORIENTATION_ENDPOINT + "/view";

    public static final String EXAM_CONFIGURATION_MAP_ENDPOINT = "/exam-configuration-map";

    public static final String USER_ACTIVITY_LOG_ENDPOINT = "/useractivity";

    public static final String EXAM_MONITORING_ENDPOINT = "/monitoring";
    public static final String EXAM_MONITORING_FULLPAGE = "/fullpage";
    public static final String EXAM_MONITORING_STATIC_CLIENT_DATA = "/static-client-data";
    public static final String EXAM_MONITORING_INSTRUCTION_ENDPOINT = "/instruction";
    public static final String EXAM_MONITORING_NOTIFICATION_ENDPOINT = "/notification";
    public static final String EXAM_MONITORING_DISABLE_CONNECTION_ENDPOINT = "/disable-connection";
    public static final String EXAM_MONITORING_SIGNATURE_KEY_ENDPOINT = "/signature";
    public static final String EXAM_MONITORING_STATE_FILTER = "hidden-states";
    public static final String EXAM_MONITORING_CLIENT_GROUP_FILTER = "hidden-client-group";
    public static final String EXAM_MONITORING_ISSUE_FILTER = "hidden-issues";

    public static final String EXAM_MONITORING_FINISHED_ENDPOINT = "/finishedexams";
    public static final String EXAM_MONITORING_SEB_CONNECTION_TOKEN_PATH_SEGMENT =
            "/{" + EXAM_API_SEB_CONNECTION_TOKEN + "}";

    public static final String EXAM_PROCTORING_ENDPOINT = EXAM_MONITORING_ENDPOINT + "/proctoring";
    public static final String EXAM_PROCTORING_COLLECTING_ROOMS_SEGMENT = "/collecting-rooms";
    public static final String EXAM_SCREEN_PROCTORING_GROUPS_SEGMENT = "/screenproctoring-groups";
    public static final String EXAM_PROCTORING_OPEN_BREAK_OUT_ROOM_SEGMENT = "/open";
    public static final String EXAM_PROCTORING_CLOSE_ROOM_SEGMENT = "/close";
    public static final String EXAM_PROCTORING_NOTIFY_OPEN_ROOM_SEGMENT = "/notify-open-room";
    public static final String EXAM_PROCTORING_RECONFIGURATION_ATTRIBUTES = "/reconfiguration-attributes";
    public static final String EXAM_PROCTORING_ROOM_CONNECTIONS_PATH_SEGMENT = "/room-connections";
    public static final String EXAM_PROCTORING_ACTIVATE_TOWNHALL_ROOM = "activate-towhall-room";
    public static final String EXAM_PROCTORING_TOWNHALL_ROOM_DATA = "towhall-room-data";
    public static final String EXAM_PROCTORING_TOWNHALL_ROOM_AVAILABLE = "towhall-available";

    public static final String EXAM_PROCTORING_ATTR_RECEIVE_AUDIO = "receive_audio";
    public static final String EXAM_PROCTORING_ATTR_RECEIVE_VIDEO = "receive_video";
    public static final String EXAM_PROCTORING_ATTR_ALLOW_CHAT = "allow_chat";

    public static final String SEB_CLIENT_CONNECTION_ENDPOINT = "/seb-client-connection";
    public static final String SEB_CLIENT_CONNECTION_DATA_ENDPOINT = "/data";

    public static final String SEB_CLIENT_EVENT_ENDPOINT = "/seb-client-event";
    public static final String SEB_CLIENT_EVENT_SEARCH_PATH_SEGMENT = "/search";
    public static final String SEB_CLIENT_EVENT_EXPORT_PATH_SEGMENT = "/export";
    public static final String SEB_CLIENT_EVENT_EXPORT_TYPE = "exportType";
    public static final String SEB_CLIENT_EVENT_EXPORT_INCLUDE_CONNECTIONS = "includeConnectionDetails";
    public static final String SEB_CLIENT_EVENT_EXPORT_INCLUDE_EXAMS = "includeExamDetails";
    public static final String SEB_CLIENT_EVENT_EXTENDED_PAGE_ENDPOINT = SEB_CLIENT_EVENT_ENDPOINT
            + SEB_CLIENT_EVENT_SEARCH_PATH_SEGMENT;

    public static final String CERTIFICATE_ENDPOINT = "/certificate";
    public static final String CERTIFICATE_ALIAS = "alias";
    public static final String CERTIFICATE_ALIAS_VAR_PATH_SEGMENT = "/{" + CERTIFICATE_ALIAS + "}";

    public static final String EXAM_TEMPLATE_ENDPOINT = "/exam-template";
    public static final String EXAM_TEMPLATE_INDICATOR_PATH_SEGMENT = "/indicator";
    public static final String EXAM_TEMPLATE_CLIENT_GROUP_PATH_SEGMENT = "/client-group";
    public static final String EXAM_TEMPLATE_DEFAULT_PATH_SEGMENT = "/default";

    public static final String BATCH_ACTION_ENDPOINT = "/batch-action";

}
