/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig;
import ch.ethz.seb.sebserver.gbl.util.Tuple3;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Activatable;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityName;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamStatus;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamType;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamConfigurationMap;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.exam.OpenEdxSEBRestriction.PermissionComponent;
import ch.ethz.seb.sebserver.gbl.model.exam.OpenEdxSEBRestriction.WhiteListPath;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationStatus;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.TemplateAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.View;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnectionData;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent;
import ch.ethz.seb.sebserver.gbl.model.session.ClientEvent.EventType;
import ch.ethz.seb.sebserver.gbl.model.user.UserActivityLog;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserLogActivityType;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamNames;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExams;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.GetInstitutionNames;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.GetLmsSetupNames;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetExamConfigNodeNames;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetExamConfigNodes;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetViews;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.GetUserAccountNames;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;

@Lazy
@Service
@GuiProfile
/** Defines functionality to get resources or functions of resources to feed e.g. selection or
 * combo-box content. */
public class ResourceService {

    private static final String MISSING_CLIENT_PING_NAME_KEY = "MISSING";

    public static final Comparator<Tuple<String>> RESOURCE_COMPARATOR = Comparator.comparing(t -> t._2);
    public static final Comparator<Tuple3<String>> RESOURCE_COMPARATOR_TUPLE_3 = Comparator.comparing(t -> t._2);

    public static final EnumSet<EntityType> ENTITY_TYPE_EXCLUDE_MAP = EnumSet.of(
            EntityType.ADDITIONAL_ATTRIBUTES,
            EntityType.CLIENT_CONNECTION,
            EntityType.CLIENT_EVENT,
            EntityType.CONFIGURATION_ATTRIBUTE,
            EntityType.CONFIGURATION_VALUE,
            EntityType.CONFIGURATION,
            EntityType.ORIENTATION,
            EntityType.USER_ACTIVITY_LOG,
            EntityType.USER_ROLE,
            EntityType.WEBSERVICE_SERVER_INFO);

    public static final EnumSet<EventType> CLIENT_EVENT_TYPE_EXCLUDE_MAP = EnumSet.of(
            EventType.LAST_PING);

    public static final String EXAMCONFIG_STATUS_PREFIX = "sebserver.examconfig.status.";
    public static final String EXAM_TYPE_PREFIX = "sebserver.exam.type.";
    public static final String USERACCOUNT_ROLE_PREFIX = "sebserver.useraccount.role.";
    public static final String EXAM_INDICATOR_TYPE_PREFIX = "sebserver.exam.indicator.type.";
    public static final String LMSSETUP_TYPE_PREFIX = "sebserver.lmssetup.type.";
    public static final String CONFIG_ATTRIBUTE_TYPE_PREFIX = "sebserver.configtemplate.attr.type.";
    public static final String SEB_RESTRICTION_WHITE_LIST_PREFIX = "sebserver.exam.form.sebrestriction.whiteListPaths.";
    public static final String SEB_RESTRICTION_PERMISSIONS_PREFIX = "sebserver.exam.form.sebrestriction.permissions.";
    public static final String SEB_CLIENT_CONFIG_PURPOSE_PREFIX = "sebserver.clientconfig.config.purpose.";

    public static final EnumSet<AttributeType> ATTRIBUTE_TYPES_NOT_DISPLAYED = EnumSet.of(
            AttributeType.LABEL,
            AttributeType.COMPOSITE_TABLE,
            AttributeType.INLINE_TABLE);

    public static final String CLIENT_EVENT_TYPE_PREFIX = "sebserver.monitoring.exam.connection.event.type.";
    public static final String USER_ACTIVITY_TYPE_PREFIX = "sebserver.overall.types.activityType.";
    public static final String ENTITY_TYPE_PREFIX = "sebserver.overall.types.entityType.";
    public static final String SEB_CONNECTION_STATUS_KEY_PREFIX = "sebserver.monitoring.exam.connection.status.";
    public static final LocTextKey ACTIVE_TEXT_KEY = new LocTextKey("sebserver.overall.status.active");
    public static final LocTextKey INACTIVE_TEXT_KEY = new LocTextKey("sebserver.overall.status.inactive");

    private final I18nSupport i18nSupport;
    private final RestService restService;
    private final CurrentUser currentUser;
    private final boolean mock_lms_enabled;

    protected ResourceService(
            final I18nSupport i18nSupport,
            final RestService restService,
            final CurrentUser currentUser,
            @Value("${sebserver.gui.webservice.mock-lms-enabled:true}") final boolean mock_lms_enabled) {

        this.i18nSupport = i18nSupport;
        this.restService = restService;
        this.currentUser = currentUser;
        this.mock_lms_enabled = mock_lms_enabled;
    }

    public I18nSupport getI18nSupport() {
        return this.i18nSupport;
    }

    public RestService getRestService() {
        return this.restService;
    }

    public CurrentUser getCurrentUser() {
        return this.currentUser;
    }

    public Supplier<List<Tuple<String>>> localizedResourceSupplier(final List<Tuple<String>> source) {
        return () -> source.stream()
                .map(tuple -> new Tuple<>(tuple._1, this.i18nSupport.getText(tuple._2)))
                .collect(Collectors.toList());
    }

    public List<Tuple<String>> activityResources() {
        final List<Tuple<String>> result = new ArrayList<>();
        result.add(new Tuple<>(Constants.TRUE_STRING, this.i18nSupport.getText(ACTIVE_TEXT_KEY)));
        result.add(new Tuple<>(Constants.FALSE_STRING, this.i18nSupport.getText(INACTIVE_TEXT_KEY)));
        return result;
    }

    public List<Tuple<String>> lmsTypeResources() {
        return Arrays.stream(LmsType.values())
                .filter(lmsType -> lmsType != LmsType.MOCKUP || this.mock_lms_enabled)
                .map(lmsType -> new Tuple<>(
                        lmsType.name(),
                        this.i18nSupport.getText(LMSSETUP_TYPE_PREFIX + lmsType.name(), lmsType.name())))
                .sorted(RESOURCE_COMPARATOR)
                .collect(Collectors.toList());
    }

    public List<Tuple<String>> clientEventTypeResources() {
        return Arrays.stream(EventType.values())
                .filter(eventType -> !CLIENT_EVENT_TYPE_EXCLUDE_MAP.contains(eventType))
                .map(eventType -> new Tuple<>(
                        eventType.name(),
                        getEventTypeName(eventType)))
                .sorted(RESOURCE_COMPARATOR)
                .collect(Collectors.toList());
    }

    public String getEventTypeName(final ClientEvent event) {
        if (event == null) {
            return getEventTypeName(EventType.UNKNOWN);
        }
        return getEventTypeName(event.eventType);
    }

    public String getEventTypeName(final EventType eventType) {
        if (eventType == null) {
            return Constants.EMPTY_NOTE;
        }
        return this.i18nSupport.getText(CLIENT_EVENT_TYPE_PREFIX + eventType.name(), eventType.name());
    }

    public List<Tuple<String>> indicatorTypeResources() {
        return Arrays.stream(IndicatorType.values())
                .map(type -> new Tuple3<>(
                        type.name(),
                        this.i18nSupport.getText(EXAM_INDICATOR_TYPE_PREFIX + type.name(), type.name()),
                        Utils.formatLineBreaks(this.i18nSupport.getText(
                                EXAM_INDICATOR_TYPE_PREFIX + type.name() + Constants.TOOLTIP_TEXT_KEY_SUFFIX,
                                StringUtils.EMPTY))))
                .sorted(RESOURCE_COMPARATOR_TUPLE_3)
                .collect(Collectors.toList());
    }

    public List<Tuple<String>> examConfigurationSelectionResources() {
        return getExamConfigurationSelection()
                .getOr(Collections.emptyList())
                .stream()
                .map(entityName -> new Tuple<>(entityName.modelId, entityName.name))
                .sorted(RESOURCE_COMPARATOR)
                .collect(Collectors.toList());
    }

    public List<Tuple<String>> userRoleResources() {
        return UserRole.publicRolesForUser(this.currentUser.get())
                .stream()
                .map(ur -> new Tuple3<>(
                        ur.name(),
                        this.i18nSupport.getText(USERACCOUNT_ROLE_PREFIX + ur.name()),
                        Utils.formatLineBreaks(this.i18nSupport.getText(
                                USERACCOUNT_ROLE_PREFIX + ur.name() + Constants.TOOLTIP_TEXT_KEY_SUFFIX,
                                StringUtils.EMPTY))))
                .sorted(RESOURCE_COMPARATOR)
                .collect(Collectors.toList());
    }

    public List<Tuple<String>> institutionResource() {
        return this.restService.getBuilder(GetInstitutionNames.class)
                .withQueryParam(Entity.FILTER_ATTR_ACTIVE, Constants.TRUE_STRING)
                .call()
                .getOr(Collections.emptyList())
                .stream()
                .map(entityName -> new Tuple<>(entityName.modelId, entityName.name))
                .sorted(RESOURCE_COMPARATOR)
                .collect(Collectors.toList());
    }

    public Function<String, String> getInstitutionNameFunction() {
        final Map<String, String> idNameMap = this.restService.getBuilder(GetInstitutionNames.class)
                .withQueryParam(Entity.FILTER_ATTR_ACTIVE, Constants.TRUE_STRING)
                .call()
                .getOr(Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(e -> e.modelId, e -> e.name));

        return id -> {
            if (!idNameMap.containsKey(id)) {
                return Constants.EMPTY_NOTE;
            }
            return idNameMap.get(id);
        };
    }

    public List<Tuple<String>> lmsSetupResource() {
        final boolean isSEBAdmin = this.currentUser.get().hasRole(UserRole.SEB_SERVER_ADMIN);
        final String institutionId = (isSEBAdmin) ? "" : String.valueOf(this.currentUser.get().institutionId);
        return this.restService.getBuilder(GetLmsSetupNames.class)
                .withQueryParam(Entity.FILTER_ATTR_INSTITUTION, institutionId)
                .withQueryParam(Entity.FILTER_ATTR_ACTIVE, Constants.TRUE_STRING)
                .call()
                .getOr(Collections.emptyList())
                .stream()
                .map(entityName -> new Tuple<>(entityName.modelId, entityName.name))
                .sorted(RESOURCE_COMPARATOR)
                .collect(Collectors.toList());
    }

    public Function<String, String> getLmsSetupNameFunction() {
        final boolean isSEBAdmin = this.currentUser.get().hasRole(UserRole.SEB_SERVER_ADMIN);
        final String institutionId = (isSEBAdmin) ? "" : String.valueOf(this.currentUser.get().institutionId);
        final Map<String, String> idNameMap = this.restService.getBuilder(GetLmsSetupNames.class)
                .withQueryParam(Entity.FILTER_ATTR_INSTITUTION, institutionId)
                .withQueryParam(Entity.FILTER_ATTR_ACTIVE, Constants.TRUE_STRING)
                .call()
                .getOr(Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(e -> e.modelId, e -> e.name));

        return id -> {
            if (!idNameMap.containsKey(id)) {
                return Constants.EMPTY_NOTE;
            }
            return idNameMap.get(id);
        };
    }

    public List<Tuple<String>> entityTypeResources() {
        return Arrays.stream(EntityType.values())
                .filter(type -> !ENTITY_TYPE_EXCLUDE_MAP.contains(type))
                .map(type -> new Tuple<>(type.name(), getEntityTypeName(type)))
                .sorted(RESOURCE_COMPARATOR)
                .collect(Collectors.toList());
    }

    public static LocTextKey getEntityTypeNameKey(final EntityType type) {
        return new LocTextKey(ENTITY_TYPE_PREFIX + type.name());
    }

    public String getEntityTypeName(final EntityType type) {
        if (type == null) {
            return Constants.EMPTY_NOTE;
        }
        return this.i18nSupport.getText(getEntityTypeNameKey(type));
    }

    public String getEntityTypeName(final UserActivityLog userLog) {
        return getEntityTypeName(userLog.entityType);
    }

    public List<Tuple<String>> userActivityTypeResources() {
        return Arrays.stream(UserLogActivityType.values())
                .map(type -> new Tuple<>(type.name(), getUserActivityTypeName(type)))
                .sorted(RESOURCE_COMPARATOR)
                .collect(Collectors.toList());
    }

    public String getUserActivityTypeName(final UserLogActivityType type) {
        if (type == null) {
            return Constants.EMPTY_NOTE;
        }
        return this.i18nSupport.getText(USER_ACTIVITY_TYPE_PREFIX + type.name());
    }

    public String getUserActivityTypeName(final UserActivityLog userLog) {
        return getUserActivityTypeName(userLog.activityType);
    }

    /** Get a list of language key/name tuples for all supported languages in the
     * language of the current users locale.
     *
     * @return list of language key/name tuples for all supported languages in the language of the current users
     *         locale */
    public List<Tuple<String>> languageResources() {
        final Locale currentLocale = this.i18nSupport.getUsersLanguageLocale();
        return this.i18nSupport.supportedLanguages()
                .stream()
                .map(locale -> new Tuple<>(locale.toLanguageTag(), locale.getDisplayLanguage(currentLocale)))
                .filter(tuple -> StringUtils.isNotBlank(tuple._2))
                .sorted(RESOURCE_COMPARATOR)
                .collect(Collectors.toList());
    }

    public List<Tuple<String>> timeZoneResources() {
        final Locale currentLocale = this.i18nSupport.getUsersLanguageLocale();
        return DateTimeZone
                .getAvailableIDs()
                .stream()
                .map(id -> new Tuple<>(id, id + " (" + DateTimeZone.forID(id).getName(0, currentLocale) + ")"))
                .sorted(RESOURCE_COMPARATOR)
                .collect(Collectors.toList());
    }

    public List<Tuple<String>> examTypeResources() {
        return Arrays.stream(ExamType.values())
                .map(type -> new Tuple3<>(
                        type.name(),
                        this.i18nSupport.getText(EXAM_TYPE_PREFIX + type.name()),
                        Utils.formatLineBreaks(this.i18nSupport.getText(
                                EXAM_INDICATOR_TYPE_PREFIX + type.name() + Constants.TOOLTIP_TEXT_KEY_SUFFIX,
                                StringUtils.EMPTY))))
                .sorted(RESOURCE_COMPARATOR)
                .collect(Collectors.toList());
    }

    public List<Tuple<String>> examConfigStatusResources() {
        return examConfigStatusResources(false);
    }

    public List<Tuple<String>> examConfigStatusResources(final boolean isAttachedToExam) {
        return Arrays.stream(ConfigurationStatus.values())
                .filter(status -> {
                    if (isAttachedToExam) {
                        return status != ConfigurationStatus.READY_TO_USE;
                    } else {
                        return status != ConfigurationStatus.IN_USE;
                    }
                })
                .map(type -> new Tuple3<>(
                        type.name(),
                        this.i18nSupport.getText(EXAMCONFIG_STATUS_PREFIX + type.name()),
                        Utils.formatLineBreaks(this.i18nSupport.getText(
                                this.i18nSupport.getText(EXAMCONFIG_STATUS_PREFIX + type.name()) + Constants.TOOLTIP_TEXT_KEY_SUFFIX,
                                StringUtils.EMPTY))
                        ))
                .sorted(RESOURCE_COMPARATOR)
                .collect(Collectors.toList());
    }

    public List<Tuple<String>> examSupporterResources() {
        final UserInfo userInfo = this.currentUser.get();
        final List<EntityName> selection = this.restService.getBuilder(GetUserAccountNames.class)
                .withQueryParam(Entity.FILTER_ATTR_INSTITUTION, String.valueOf(userInfo.institutionId))
                .withQueryParam(Entity.FILTER_ATTR_ACTIVE, Constants.TRUE_STRING)
                .withQueryParam(UserInfo.FILTER_ATTR_ROLE, UserRole.EXAM_SUPPORTER.name())
                .call()
                .getOr(Collections.emptyList());
        return selection
                .stream()
                .map(entityName -> new Tuple<>(entityName.modelId, entityName.name))
                .sorted(RESOURCE_COMPARATOR)
                .collect(Collectors.toList());
    }

    public List<Tuple<String>> userResources() {
        final UserInfo userInfo = this.currentUser.get();
        return this.restService.getBuilder(GetUserAccountNames.class)
                .withQueryParam(Entity.FILTER_ATTR_INSTITUTION, String.valueOf(userInfo.institutionId))
                .withQueryParam(Entity.FILTER_ATTR_ACTIVE, Constants.TRUE_STRING)
                .call()
                .getOr(Collections.emptyList())
                .stream()
                .map(entityName -> new Tuple<>(entityName.modelId, entityName.name))
                .sorted(RESOURCE_COMPARATOR)
                .collect(Collectors.toList());
    }

    public <T extends Activatable> Function<T, String> localizedActivityFunction() {
        final Function<Boolean, String> localizedActivityResource = localizedActivityResource();
        return activatable -> localizedActivityResource.apply(activatable.isActive());
    }

    public Function<Boolean, String> localizedActivityResource() {
        return activity -> activity
                ? this.i18nSupport.getText(ACTIVE_TEXT_KEY)
                : this.i18nSupport.getText(INACTIVE_TEXT_KEY);
    }

    public String localizedExamConfigInstitutionName(final ConfigurationNode config) {
        return getInstitutionNameFunction()
                .apply(String.valueOf(config.institutionId));
    }

    public String localizedExamConfigStatusName(final ConfigurationNode config) {
        if (config.status == null) {
            return Constants.EMPTY_NOTE;
        }

        return this.i18nSupport
                .getText(ResourceService.EXAMCONFIG_STATUS_PREFIX + config.status.name());
    }

    public String localizedExamConfigStatusName(final ExamConfigurationMap config) {
        if (config.configStatus == null) {
            return Constants.EMPTY_NOTE;
        }

        return this.i18nSupport
                .getText(ResourceService.EXAMCONFIG_STATUS_PREFIX + config.configStatus.name());
    }

    public String localizedClientConnectionStatusName(final ClientConnectionData connectionData) {
        if (connectionData == null) {
            final String name = ConnectionStatus.UNDEFINED.name();
            return this.i18nSupport.getText(
                    SEB_CONNECTION_STATUS_KEY_PREFIX + name,
                    name);
        }
        if (connectionData.missingPing) {
            return this.i18nSupport.getText(
                    SEB_CONNECTION_STATUS_KEY_PREFIX + MISSING_CLIENT_PING_NAME_KEY,
                    MISSING_CLIENT_PING_NAME_KEY);
        } else {
            return localizedClientConnectionStatusName((connectionData.clientConnection != null)
                    ? connectionData.clientConnection.status
                    : ConnectionStatus.UNDEFINED);
        }
    }

    public String localizedClientConnectionStatusName(final ConnectionStatus status) {
        String name;
        if (status != null) {
            name = status.name();
        } else {
            name = ConnectionStatus.UNDEFINED.name();
        }
        return this.i18nSupport
                .getText(SEB_CONNECTION_STATUS_KEY_PREFIX + name, name);
    }

    public String localizedExamTypeName(final ExamConfigurationMap examMap) {
        if (examMap.examType == null) {
            return Constants.EMPTY_NOTE;
        }

        return this.i18nSupport
                .getText(ResourceService.EXAM_TYPE_PREFIX + examMap.examType.name());
    }

    public String localizedExamTypeName(final Exam exam) {
        if (exam.type == null) {
            return Constants.EMPTY_NOTE;
        }

        return this.i18nSupport
                .getText(ResourceService.EXAM_TYPE_PREFIX + exam.type.name());
    }

    public List<Tuple<String>> getExamLogSelectionResources() {
        final UserInfo userInfo = this.currentUser.get();
        return this.restService.getBuilder(GetExams.class)
                .withQueryParam(Entity.FILTER_ATTR_INSTITUTION, String.valueOf(userInfo.getInstitutionId()))
                .withQueryParam(Exam.FILTER_CACHED_QUIZZES, Constants.TRUE_STRING)
                .call()
                .getOr(Collections.emptyList())
                .stream()
                .filter(exam -> exam != null
                        && (exam.getStatus() == ExamStatus.RUNNING || exam.getStatus() == ExamStatus.FINISHED))
                .map(exam -> new Tuple<>(exam.getModelId(), exam.name))
                .sorted(RESOURCE_COMPARATOR)
                .collect(Collectors.toList());
    }

    public List<Tuple<String>> getExamResources() {
        final UserInfo userInfo = this.currentUser.get();
        return this.restService.getBuilder(GetExamNames.class)
                .withQueryParam(Entity.FILTER_ATTR_INSTITUTION, String.valueOf(userInfo.getInstitutionId()))
                .withQueryParam(Exam.FILTER_CACHED_QUIZZES, Constants.TRUE_STRING)
                .call()
                .getOr(Collections.emptyList())
                .stream()
                .map(entityName -> new Tuple<>(entityName.modelId, entityName.name))
                .sorted(RESOURCE_COMPARATOR)
                .collect(Collectors.toList());
    }

    public Map<Long, String> getExamNameMapping() {
        final UserInfo userInfo = this.currentUser.get();
        return this.restService.getBuilder(GetExamNames.class)
                .withQueryParam(Entity.FILTER_ATTR_INSTITUTION, String.valueOf(userInfo.getInstitutionId()))
                .withQueryParam(Exam.FILTER_CACHED_QUIZZES, Constants.TRUE_STRING)
                .call()
                .getOr(Collections.emptyList())
                .stream()
                .filter(k -> StringUtils.isNotBlank(k.modelId))
                .collect(Collectors.toMap(
                        k -> Long.valueOf(k.modelId),
                        k -> k.name));
    }

    public List<Tuple<String>> getViewResources() {
        return getViewResources(API.DEFAULT_CONFIG_TEMPLATE_ID);
    }

    public List<Tuple<String>> getViewResources(final String templateId) {
        return this.restService.getBuilder(GetViews.class)
                .withQueryParam(
                        View.FILTER_ATTR_TEMPLATE,
                        templateId)
                .call()
                .getOr(Collections.emptyList())
                .stream()
                .map(view -> new Tuple<>(view.getModelId(), view.name))
                .sorted(RESOURCE_COMPARATOR)
                .collect(Collectors.toList());
    }

    public final Function<TemplateAttribute, String> getViewNameFunction(final String templateId) {
        final Map<String, String> mapping = this.getViewResources(templateId)
                .stream()
                .collect(Collectors.toMap(tuple -> tuple._1, tuple -> tuple._2));

        return attr -> mapping.get(attr.getViewModelId());
    }

    public List<Tuple<String>> getAttributeTypeResources() {
        return Arrays.stream(AttributeType.values())
                .filter(type -> !ATTRIBUTE_TYPES_NOT_DISPLAYED.contains(type))
                .map(type -> new Tuple<>(getAttributeTypeFilterName(type), getAttributeTypeName(type)))
                .sorted(RESOURCE_COMPARATOR)
                .collect(Collectors.toList());
    }

    public String getAttributeTypeName(final TemplateAttribute attribute) {
        if (attribute != null && attribute.getConfigAttribute() != null) {
            return getAttributeTypeName(attribute.getConfigAttribute().type);
        }

        return Constants.EMPTY_NOTE;
    }

    private String getAttributeTypeFilterName(final AttributeType type) {
        if (type == AttributeType.TABLE) {
            return type.name()
                    + Constants.LIST_SEPARATOR
                    + AttributeType.COMPOSITE_TABLE.name()
                    + Constants.LIST_SEPARATOR
                    + AttributeType.INLINE_TABLE.name();
        } else {
            return type.name();
        }
    }

    public String getAttributeTypeName(final AttributeType type) {
        if (type == null) {
            return Constants.EMPTY_NOTE;
        }
        return this.i18nSupport
                .getText(CONFIG_ATTRIBUTE_TYPE_PREFIX + type.name());
    }

    public List<Tuple<String>> getExamConfigTemplateResources() {
        final UserInfo userInfo = this.currentUser.get();
        return this.restService.getBuilder(GetExamConfigNodes.class)
                .withQueryParam(Entity.FILTER_ATTR_INSTITUTION, String.valueOf(userInfo.getInstitutionId()))
                .withQueryParam(ConfigurationNode.FILTER_ATTR_TYPE, ConfigurationType.TEMPLATE.name())
                .call()
                .getOr(Collections.emptyList())
                .stream()
                .map(node -> new Tuple<>(node.getModelId(), node.name))
                .sorted(RESOURCE_COMPARATOR)
                .collect(Collectors.toList());
    }

    public List<Tuple<String>> sebRestrictionWhiteListResources() {
        return Arrays.stream(WhiteListPath.values())
                .map(type -> new Tuple3<>(
                        type.key,
                        this.i18nSupport.getText(SEB_RESTRICTION_WHITE_LIST_PREFIX + type.name(), type.key),
                        Utils.formatLineBreaks(this.i18nSupport.getText(
                                SEB_RESTRICTION_WHITE_LIST_PREFIX + type.name() + Constants.TOOLTIP_TEXT_KEY_SUFFIX,
                                StringUtils.EMPTY))))
                .sorted(RESOURCE_COMPARATOR)
                .collect(Collectors.toList());
    }

    public List<Tuple<String>> sebRestrictionPermissionResources() {
        return Arrays.stream(PermissionComponent.values())
                .map(type -> new Tuple3<>(
                        type.key,
                        this.i18nSupport.getText(SEB_RESTRICTION_PERMISSIONS_PREFIX + type.name(), type.key),
                        Utils.formatLineBreaks(this.i18nSupport.getText(
                                SEB_RESTRICTION_PERMISSIONS_PREFIX + type.name() + Constants.TOOLTIP_TEXT_KEY_SUFFIX,
                                StringUtils.EMPTY))))
                .sorted(RESOURCE_COMPARATOR)
                .collect(Collectors.toList());
    }

    private Result<List<EntityName>> getExamConfigurationSelection() {
        return this.restService.getBuilder(GetExamConfigNodeNames.class)
                .withQueryParam(
                        Entity.FILTER_ATTR_INSTITUTION,
                        String.valueOf(this.currentUser.get().institutionId))
                .withQueryParam(
                        ConfigurationNode.FILTER_ATTR_TYPE,
                        ConfigurationType.EXAM_CONFIG.name())
                .withQueryParam(
                        ConfigurationNode.FILTER_ATTR_STATUS,
                        ConfigurationStatus.READY_TO_USE.name())
                .call();
    }

    public List<Tuple<String>> sebClientConfigPurposeResources() {
        return Arrays.stream(SEBClientConfig.ConfigPurpose.values())
                .map(type -> new Tuple3<>(
                        type.name(),
                        this.i18nSupport.getText(SEB_CLIENT_CONFIG_PURPOSE_PREFIX + type.name()),
                        Utils.formatLineBreaks(this.i18nSupport.getText(
                                SEB_CLIENT_CONFIG_PURPOSE_PREFIX + type.name() + Constants.TOOLTIP_TEXT_KEY_SUFFIX,
                                StringUtils.EMPTY))))
                .sorted(RESOURCE_COMPARATOR)
                .collect(Collectors.toList());
    }

}
