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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeZone;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityName;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam.ExamType;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamConfigurationMap;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationStatus;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationNode.ConfigurationType;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamConfigMappingNames;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.GetInstitutionNames;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.GetLmsSetupNames;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.GetUserAccountNames;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;

@Lazy
@Service
@GuiProfile
/** Defines functionality to get resources or functions of resources to feed e.g. selection or
 * combo-box content. */
public class ResourceService {

    public static final String EXAMCONFIG_STATUS_PREFIX = "sebserver.examconfig.status.";
    public static final String EXAM_TYPE_PREFIX = "sebserver.exam.type.";
    public static final String USERACCOUNT_ROLE_PREFIX = "sebserver.useraccount.role.";
    public static final String EXAM_INDICATOR_TYPE_PREFIX = "sebserver.exam.indicator.type.";
    public static final String LMSSETUP_TYPE_PREFIX = "sebserver.lmssetup.type.";
    public static final LocTextKey ACTIVE_TEXT_KEY = new LocTextKey("sebserver.overall.status.active");
    public static final LocTextKey INACTIVE_TEXT_KEY = new LocTextKey("sebserver.overall.status.inactive");

    private final I18nSupport i18nSupport;
    private final RestService restService;
    private final CurrentUser currentUser;

    protected ResourceService(
            final I18nSupport i18nSupport,
            final RestService restService,
            final CurrentUser currentUser) {

        this.i18nSupport = i18nSupport;
        this.restService = restService;
        this.currentUser = currentUser;
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
        result.add(new Tuple<>("true", this.i18nSupport.getText("sebserver.overall.status.active")));
        result.add(new Tuple<>("false", this.i18nSupport.getText("sebserver.overall.status.inactive")));
        return result;
    }

    public List<Tuple<String>> lmsTypeResources() {
        return Arrays.asList(LmsType.values())
                .stream()
                .map(lmsType -> new Tuple<>(
                        lmsType.name(),
                        this.i18nSupport.getText(LMSSETUP_TYPE_PREFIX + lmsType.name())))
                .collect(Collectors.toList());
    }

    public List<Tuple<String>> indicatorTypeResources() {
        return Arrays.asList(IndicatorType.values())
                .stream()
                .map(type -> new Tuple<>(
                        type.name(),
                        this.i18nSupport.getText(EXAM_INDICATOR_TYPE_PREFIX + type.name())))
                .collect(Collectors.toList());
    }

    public List<Tuple<String>> examConfigurationSelectionResources() {
        return getExamConfigurationSelection()
                .getOr(Collections.emptyList())
                .stream()
                .map(entityName -> new Tuple<>(entityName.modelId, entityName.name))
                .collect(Collectors.toList());
    }

    public List<Tuple<String>> userRoleResources() {
        return UserRole.publicRolesForUser(this.currentUser.get())
                .stream()
                .map(ur -> new Tuple<>(
                        ur.name(),
                        this.i18nSupport.getText(USERACCOUNT_ROLE_PREFIX + ur.name())))
                .collect(Collectors.toList());
    }

    public List<Tuple<String>> institutionResource() {
        return this.restService.getBuilder(GetInstitutionNames.class)
                .withQueryParam(Entity.FILTER_ATTR_ACTIVE, Constants.TRUE_STRING)
                .call()
                .getOr(Collections.emptyList())
                .stream()
                .map(entityName -> new Tuple<>(entityName.modelId, entityName.name))
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

    /** Get a list of language key/name tuples for all supported languages in the
     * language of the current users locale.
     *
     * @param i18nSupport I18nSupport to get the actual current users locale
     * @return list of language key/name tuples for all supported languages in the language of the current users
     *         locale */
    public List<Tuple<String>> languageResources() {
        final Locale currentLocale = this.i18nSupport.getCurrentLocale();
        return this.i18nSupport.supportedLanguages()
                .stream()
                .map(locale -> new Tuple<>(locale.toLanguageTag(), locale.getDisplayLanguage(currentLocale)))
                .filter(tuple -> StringUtils.isNotBlank(tuple._2))
                .sorted((t1, t2) -> t1._2.compareTo(t2._2))
                .collect(Collectors.toList());
    }

    public List<Tuple<String>> timeZoneResources() {
        final Locale currentLocale = this.i18nSupport.getCurrentLocale();
        return DateTimeZone
                .getAvailableIDs()
                .stream()
                .map(id -> new Tuple<>(id, DateTimeZone.forID(id).getName(0, currentLocale) + " (" + id + ")"))
                .sorted((t1, t2) -> t1._2.compareTo(t2._2))
                .collect(Collectors.toList());
    }

    public List<Tuple<String>> examTypeResources() {
        return Arrays.asList(ExamType.values())
                .stream()
                .filter(type -> type != ExamType.UNDEFINED)
                .map(type -> new Tuple<>(
                        type.name(),
                        this.i18nSupport.getText(EXAM_TYPE_PREFIX + type.name())))
                .collect(Collectors.toList());
    }

    public List<Tuple<String>> examConfigStatusResources() {
        return Arrays.asList(ConfigurationStatus.values())
                .stream()
                .map(type -> new Tuple<>(
                        type.name(),
                        this.i18nSupport.getText(EXAMCONFIG_STATUS_PREFIX + type.name())))
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
                .collect(Collectors.toList());
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

    private Result<List<EntityName>> getExamConfigurationSelection() {
        return this.restService.getBuilder(GetExamConfigMappingNames.class)
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

    public String examTypeName(final Exam exam) {
        if (exam.type == null) {
            return Constants.EMPTY_NOTE;
        }

        return this.i18nSupport
                .getText(ResourceService.EXAM_TYPE_PREFIX + exam.type.name());
    }

}
