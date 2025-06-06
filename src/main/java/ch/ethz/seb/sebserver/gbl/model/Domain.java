package ch.ethz.seb.sebserver.gbl.model;

import javax.annotation.Generated;

/** Defines the global names of the domain model and domain model fields.
* This shall be used as a static overall domain model names reference within SEB Server Web-Service as well as within the integrated GUI
* This file is generated by the org.eth.demo.sebserver.gen.DomainModelNameReferencePlugin and must not be edited manually.**/
@Generated(value="org.mybatis.generator.api.MyBatisGenerator",comments="ch.ethz.seb.sebserver.gen.DomainModelNameReferencePlugin",date="2024-11-04T15:08:40.789+01:00")
public interface Domain {

    interface CONFIGURATION_ATTRIBUTE {
        String TYPE_NAME = "ConfigurationAttribute";
        String REFERENCE_NAME = "configurationAttributes";
        String ATTR_ID = "id";
        String ATTR_NAME = "name";
        String ATTR_TYPE = "type";
        String ATTR_PARENT_ID = "parentId";
        String ATTR_RESOURCES = "resources";
        String ATTR_VALIDATOR = "validator";
        String ATTR_DEPENDENCIES = "dependencies";
        String ATTR_DEFAULT_VALUE = "defaultValue";
    }

    interface CONFIGURATION_VALUE {
        String TYPE_NAME = "ConfigurationValue";
        String REFERENCE_NAME = "configurationValues";
        String ATTR_ID = "id";
        String ATTR_INSTITUTION_ID = "institutionId";
        String ATTR_CONFIGURATION_ID = "configurationId";
        String ATTR_CONFIGURATION_ATTRIBUTE_ID = "configurationAttributeId";
        String ATTR_LIST_INDEX = "listIndex";
        String ATTR_VALUE = "value";
    }

    interface VIEW {
        String TYPE_NAME = "View";
        String REFERENCE_NAME = "views";
        String ATTR_ID = "id";
        String ATTR_NAME = "name";
        String ATTR_COLUMNS = "columns";
        String ATTR_POSITION = "position";
        String ATTR_TEMPLATE_ID = "templateId";
    }

    interface ORIENTATION {
        String TYPE_NAME = "Orientation";
        String REFERENCE_NAME = "orientations";
        String ATTR_ID = "id";
        String ATTR_CONFIG_ATTRIBUTE_ID = "configAttributeId";
        String ATTR_TEMPLATE_ID = "templateId";
        String ATTR_VIEW_ID = "viewId";
        String ATTR_GROUP_ID = "groupId";
        String ATTR_X_POSITION = "xPosition";
        String ATTR_Y_POSITION = "yPosition";
        String ATTR_WIDTH = "width";
        String ATTR_HEIGHT = "height";
        String ATTR_TITLE = "title";
    }

    interface CONFIGURATION {
        String TYPE_NAME = "Configuration";
        String REFERENCE_NAME = "configurations";
        String ATTR_ID = "id";
        String ATTR_INSTITUTION_ID = "institutionId";
        String ATTR_CONFIGURATION_NODE_ID = "configurationNodeId";
        String ATTR_VERSION = "version";
        String ATTR_VERSION_DATE = "versionDate";
        String ATTR_FOLLOWUP = "followup";
    }

    interface CONFIGURATION_NODE {
        String TYPE_NAME = "ConfigurationNode";
        String REFERENCE_NAME = "configurationNodes";
        String ATTR_ID = "id";
        String ATTR_INSTITUTION_ID = "institutionId";
        String ATTR_TEMPLATE_ID = "templateId";
        String ATTR_OWNER = "owner";
        String ATTR_NAME = "name";
        String ATTR_DESCRIPTION = "description";
        String ATTR_TYPE = "type";
        String ATTR_STATUS = "status";
        String ATTR_LAST_UPDATE_TIME = "lastUpdateTime";
        String ATTR_LAST_UPDATE_USER = "lastUpdateUser";
    }

    interface EXAM_CONFIGURATION_MAP {
        String TYPE_NAME = "ExamConfigurationMap";
        String REFERENCE_NAME = "examConfigurationMaps";
        String ATTR_ID = "id";
        String ATTR_INSTITUTION_ID = "institutionId";
        String ATTR_EXAM_ID = "examId";
        String ATTR_CONFIGURATION_NODE_ID = "configurationNodeId";
        String ATTR_ENCRYPT_SECRET = "encryptSecret";
        String ATTR_CLIENT_GROUP_ID = "clientGroupId";
    }

    interface EXAM {
        String TYPE_NAME = "Exam";
        String REFERENCE_NAME = "exams";
        String ATTR_ID = "id";
        String ATTR_INSTITUTION_ID = "institutionId";
        String ATTR_LMS_SETUP_ID = "lmsSetupId";
        String ATTR_EXTERNAL_ID = "externalId";
        String ATTR_OWNER = "owner";
        String ATTR_SUPPORTER = "supporter";
        String ATTR_TYPE = "type";
        String ATTR_QUIT_PASSWORD = "quitPassword";
        String ATTR_BROWSER_KEYS = "browserKeys";
        String ATTR_STATUS = "status";
        String ATTR_LMS_SEB_RESTRICTION = "lmsSebRestriction";
        String ATTR_UPDATING = "updating";
        String ATTR_LASTUPDATE = "lastupdate";
        String ATTR_ACTIVE = "active";
        String ATTR_EXAM_TEMPLATE_ID = "examTemplateId";
        String ATTR_LAST_MODIFIED = "lastModified";
        String ATTR_QUIZ_NAME = "quizName";
        String ATTR_QUIZ_START_TIME = "quizStartTime";
        String ATTR_QUIZ_END_TIME = "quizEndTime";
        String ATTR_LMS_AVAILABLE = "lmsAvailable";
    }

    interface CLIENT_CONNECTION {
        String TYPE_NAME = "ClientConnection";
        String REFERENCE_NAME = "clientConnections";
        String ATTR_ID = "id";
        String ATTR_INSTITUTION_ID = "institutionId";
        String ATTR_EXAM_ID = "examId";
        String ATTR_STATUS = "status";
        String ATTR_CONNECTION_TOKEN = "connectionToken";
        String ATTR_EXAM_USER_SESSION_ID = "examUserSessionId";
        String ATTR_CLIENT_ADDRESS = "clientAddress";
        String ATTR_VIRTUAL_CLIENT_ADDRESS = "virtualClientAddress";
        String ATTR_VDI = "vdi";
        String ATTR_VDI_PAIR_TOKEN = "vdiPairToken";
        String ATTR_CREATION_TIME = "creationTime";
        String ATTR_UPDATE_TIME = "updateTime";
        String ATTR_SCREEN_PROCTORING_GROUP_ID = "screenProctoringGroupId";
        String ATTR_SCREEN_PROCTORING_GROUP_UPDATE = "screenProctoringGroupUpdate";
        String ATTR_REMOTE_PROCTORING_ROOM_ID = "remoteProctoringRoomId";
        String ATTR_REMOTE_PROCTORING_ROOM_UPDATE = "remoteProctoringRoomUpdate";
        String ATTR_CLIENT_MACHINE_NAME = "clientMachineName";
        String ATTR_CLIENT_OS_NAME = "clientOsName";
        String ATTR_CLIENT_VERSION = "clientVersion";
        String ATTR_SECURITY_CHECK_GRANTED = "securityCheckGranted";
        String ATTR_ASK = "ask";
        String ATTR_CLIENT_VERSION_GRANTED = "clientVersionGranted";
    }

    interface REMOTE_PROCTORING_ROOM {
        String TYPE_NAME = "RemoteProctoringRoom";
        String REFERENCE_NAME = "remoteProctoringRooms";
        String ATTR_ID = "id";
        String ATTR_EXAM_ID = "examId";
        String ATTR_NAME = "name";
        String ATTR_SIZE = "size";
        String ATTR_SUBJECT = "subject";
        String ATTR_TOWNHALL_ROOM = "townhallRoom";
        String ATTR_BREAK_OUT_CONNECTIONS = "breakOutConnections";
        String ATTR_JOIN_KEY = "joinKey";
        String ATTR_ROOM_DATA = "roomData";
    }

    interface SCREEN_PROCTORING_GROUP {
        String TYPE_NAME = "ScreenProctoringGroup";
        String REFERENCE_NAME = "screenProctoringGroups";
        String ATTR_ID = "id";
        String ATTR_EXAM_ID = "examId";
        String ATTR_UUID = "uuid";
        String ATTR_NAME = "name";
        String ATTR_SIZE = "size";
        String ATTR_DATA = "data";
        String ATTR_IS_FALLBACK = "isFallback";
        String ATTR_SEB_GROUP_ID = "sebGroupId";
    }

    interface CLIENT_EVENT {
        String TYPE_NAME = "ClientEvent";
        String REFERENCE_NAME = "clientEvents";
        String ATTR_ID = "id";
        String ATTR_CLIENT_CONNECTION_ID = "clientConnectionId";
        String ATTR_TYPE = "type";
        String ATTR_CLIENT_TIME = "clientTime";
        String ATTR_SERVER_TIME = "serverTime";
        String ATTR_NUMERIC_VALUE = "numericValue";
        String ATTR_TEXT = "text";
    }

    interface CLIENT_INSTRUCTION {
        String TYPE_NAME = "ClientInstruction";
        String REFERENCE_NAME = "clientInstructions";
        String ATTR_ID = "id";
        String ATTR_EXAM_ID = "examId";
        String ATTR_CONNECTION_TOKEN = "connectionToken";
        String ATTR_TYPE = "type";
        String ATTR_ATTRIBUTES = "attributes";
        String ATTR_NEEDS_CONFIRMATION = "needsConfirmation";
        String ATTR_TIMESTAMP = "timestamp";
    }

    interface INDICATOR {
        String TYPE_NAME = "Indicator";
        String REFERENCE_NAME = "indicators";
        String ATTR_ID = "id";
        String ATTR_EXAM_ID = "examId";
        String ATTR_TYPE = "type";
        String ATTR_NAME = "name";
        String ATTR_COLOR = "color";
        String ATTR_ICON = "icon";
        String ATTR_TAGS = "tags";
    }

    interface THRESHOLD {
        String TYPE_NAME = "Threshold";
        String REFERENCE_NAME = "thresholds";
        String ATTR_ID = "id";
        String ATTR_INDICATOR_ID = "indicatorId";
        String ATTR_VALUE = "value";
        String ATTR_COLOR = "color";
        String ATTR_ICON = "icon";
    }

    interface INSTITUTION {
        String TYPE_NAME = "Institution";
        String REFERENCE_NAME = "institutions";
        String ATTR_ID = "id";
        String ATTR_NAME = "name";
        String ATTR_URL_SUFFIX = "urlSuffix";
        String ATTR_THEME_NAME = "themeName";
        String ATTR_ACTIVE = "active";
        String ATTR_LOGO_IMAGE = "logoImage";
    }

    interface SEB_CLIENT_CONFIGURATION {
        String TYPE_NAME = "SebClientConfiguration";
        String REFERENCE_NAME = "sebClientConfigurations";
        String ATTR_ID = "id";
        String ATTR_INSTITUTION_ID = "institutionId";
        String ATTR_NAME = "name";
        String ATTR_DATE = "date";
        String ATTR_CLIENT_NAME = "clientName";
        String ATTR_CLIENT_SECRET = "clientSecret";
        String ATTR_ENCRYPT_SECRET = "encryptSecret";
        String ATTR_ACTIVE = "active";
        String ATTR_LAST_UPDATE_TIME = "lastUpdateTime";
        String ATTR_LAST_UPDATE_USER = "lastUpdateUser";
    }

    interface LMS_SETUP {
        String TYPE_NAME = "LmsSetup";
        String REFERENCE_NAME = "lmsSetups";
        String ATTR_ID = "id";
        String ATTR_INSTITUTION_ID = "institutionId";
        String ATTR_NAME = "name";
        String ATTR_LMS_TYPE = "lmsType";
        String ATTR_LMS_URL = "lmsUrl";
        String ATTR_LMS_CLIENTNAME = "lmsClientname";
        String ATTR_LMS_CLIENTSECRET = "lmsClientsecret";
        String ATTR_LMS_REST_API_TOKEN = "lmsRestApiToken";
        String ATTR_LMS_PROXY_HOST = "lmsProxyHost";
        String ATTR_LMS_PROXY_PORT = "lmsProxyPort";
        String ATTR_LMS_PROXY_AUTH_USERNAME = "lmsProxyAuthUsername";
        String ATTR_LMS_PROXY_AUTH_SECRET = "lmsProxyAuthSecret";
        String ATTR_UPDATE_TIME = "updateTime";
        String ATTR_ACTIVE = "active";
        String ATTR_CONNECTION_ID = "connectionId";
        String ATTR_INTEGRATION_ACTIVE = "integrationActive";
    }

    interface USER {
        String TYPE_NAME = "User";
        String REFERENCE_NAME = "users";
        String ATTR_ID = "id";
        String ATTR_INSTITUTION_ID = "institutionId";
        String ATTR_UUID = "uuid";
        String ATTR_CREATION_DATE = "creationDate";
        String ATTR_NAME = "name";
        String ATTR_SURNAME = "surname";
        String ATTR_USERNAME = "username";
        String ATTR_PASSWORD = "password";
        String ATTR_EMAIL = "email";
        String ATTR_LANGUAGE = "language";
        String ATTR_TIMEZONE = "timezone";
        String ATTR_ACTIVE = "active";
        String ATTR_DIRECT_LOGIN = "directLogin";
        String ATTR_LOCAL_ACCOUNT = "localAccount";
    }

    interface USER_ROLE {
        String TYPE_NAME = "UserRole";
        String REFERENCE_NAME = "userRoles";
        String ATTR_ID = "id";
        String ATTR_USER_ID = "userId";
        String ATTR_ROLE_NAME = "roleName";
    }

    interface USER_ACTIVITY_LOG {
        String TYPE_NAME = "UserActivityLog";
        String REFERENCE_NAME = "userActivityLogs";
        String ATTR_ID = "id";
        String ATTR_USER_UUID = "userUuid";
        String ATTR_TIMESTAMP = "timestamp";
        String ATTR_ACTIVITY_TYPE = "activityType";
        String ATTR_ENTITY_TYPE = "entityType";
        String ATTR_ENTITY_ID = "entityId";
        String ATTR_MESSAGE = "message";
    }

    interface ADDITIONAL_ATTRIBUTES {
        String TYPE_NAME = "AdditionalAttributes";
        String REFERENCE_NAME = "additionalAttributess";
        String ATTR_ID = "id";
        String ATTR_ENTITY_TYPE = "entityType";
        String ATTR_ENTITY_ID = "entityId";
        String ATTR_NAME = "name";
        String ATTR_VALUE = "value";
    }

    interface WEBSERVICE_SERVER_INFO {
        String TYPE_NAME = "WebserviceServerInfo";
        String REFERENCE_NAME = "webserviceServerInfos";
        String ATTR_ID = "id";
        String ATTR_UUID = "uuid";
        String ATTR_SERVICE_ADDRESS = "serviceAddress";
        String ATTR_MASTER = "master";
        String ATTR_UPDATE_TIME = "updateTime";
    }

    interface CERTIFICATE {
        String TYPE_NAME = "Certificate";
        String REFERENCE_NAME = "certificates";
        String ATTR_ID = "id";
        String ATTR_INSTITUTION_ID = "institutionId";
        String ATTR_ALIASES = "aliases";
        String ATTR_CERT_STORE = "certStore";
    }

    interface EXAM_TEMPLATE {
        String TYPE_NAME = "ExamTemplate";
        String REFERENCE_NAME = "examTemplates";
        String ATTR_ID = "id";
        String ATTR_INSTITUTION_ID = "institutionId";
        String ATTR_CONFIGURATION_TEMPLATE_ID = "configurationTemplateId";
        String ATTR_NAME = "name";
        String ATTR_DESCRIPTION = "description";
        String ATTR_EXAM_TYPE = "examType";
        String ATTR_SUPPORTER = "supporter";
        String ATTR_INDICATOR_TEMPLATES = "indicatorTemplates";
        String ATTR_INSTITUTIONAL_DEFAULT = "institutionalDefault";
        String ATTR_LMS_INTEGRATION = "lmsIntegration";
        String ATTR_CLIENT_CONFIGURATION_ID = "clientConfigurationId";
    }

    interface BATCH_ACTION {
        String TYPE_NAME = "BatchAction";
        String REFERENCE_NAME = "batchActions";
        String ATTR_ID = "id";
        String ATTR_INSTITUTION_ID = "institutionId";
        String ATTR_OWNER = "owner";
        String ATTR_ACTION_TYPE = "actionType";
        String ATTR_ATTRIBUTES = "attributes";
        String ATTR_SOURCE_IDS = "sourceIds";
        String ATTR_SUCCESSFUL = "successful";
        String ATTR_LAST_UPDATE = "lastUpdate";
        String ATTR_PROCESSOR_ID = "processorId";
    }

    interface CLIENT_INDICATOR {
        String TYPE_NAME = "ClientIndicator";
        String REFERENCE_NAME = "clientIndicators";
        String ATTR_ID = "id";
        String ATTR_CLIENT_CONNECTION_ID = "clientConnectionId";
        String ATTR_TYPE = "type";
        String ATTR_VALUE = "value";
    }

    interface CLIENT_NOTIFICATION {
        String TYPE_NAME = "ClientNotification";
        String REFERENCE_NAME = "clientNotifications";
        String ATTR_ID = "id";
        String ATTR_CLIENT_CONNECTION_ID = "clientConnectionId";
        String ATTR_EVENT_TYPE = "eventType";
        String ATTR_NOTIFICATION_TYPE = "notificationType";
        String ATTR_VALUE = "value";
        String ATTR_TEXT = "text";
    }

    interface CLIENT_GROUP {
        String TYPE_NAME = "ClientGroup";
        String REFERENCE_NAME = "clientGroups";
        String ATTR_ID = "id";
        String ATTR_EXAM_ID = "examId";
        String ATTR_NAME = "name";
        String ATTR_TYPE = "type";
        String ATTR_COLOR = "color";
        String ATTR_ICON = "icon";
        String ATTR_DATA = "data";
    }

    interface SEB_SECURITY_KEY_REGISTRY {
        String TYPE_NAME = "SebSecurityKeyRegistry";
        String REFERENCE_NAME = "sebSecurityKeyRegistrys";
        String ATTR_ID = "id";
        String ATTR_INSTITUTION_ID = "institutionId";
        String ATTR_KEY_TYPE = "keyType";
        String ATTR_KEY_VALUE = "keyValue";
        String ATTR_TAG = "tag";
        String ATTR_EXAM_ID = "examId";
        String ATTR_EXAM_TEMPLATE_ID = "examTemplateId";
    }

    interface ENTITY_PRIVILEGE {
        String TYPE_NAME = "EntityPrivilege";
        String REFERENCE_NAME = "entityPrivileges";
        String ATTR_ID = "id";
        String ATTR_ENTITY_TYPE = "entityType";
        String ATTR_ENTITY_ID = "entityId";
        String ATTR_USER_UUID = "userUuid";
        String ATTR_PRIVILEGE_TYPE = "privilegeType";
    }

    interface FEATURE_PRIVILEGE {
        String TYPE_NAME = "FeaturePrivilege";
        String REFERENCE_NAME = "featurePrivileges";
        String ATTR_ID = "id";
        String ATTR_FEATURE_ID = "featureId";
        String ATTR_USER_UUID = "userUuid";
    }
}