/*
 * Copyright (c) 2023 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.CollectingStrategy;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringFeature;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.model.exam.ScreenProctoringSettings;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.AdditionalAttributeRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ProctoringSettingsDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.RemoteProctoringRoomDAO;

@Lazy
@Component
@WebServiceProfile
public class ProctoringSettingsDAOImpl implements ProctoringSettingsDAO {

    private static final Logger log = LoggerFactory.getLogger(ProctoringSettingsDAOImpl.class);

    private final AdditionalAttributesDAO additionalAttributesDAO;
    private final RemoteProctoringRoomDAO remoteProctoringRoomDAO;
    private final WebserviceInfo.ScreenProctoringServiceBundle screenProctoringServiceBundle;
    private final Cryptor cryptor;
    private final JSONMapper jsonMapper;

    public ProctoringSettingsDAOImpl(
            final AdditionalAttributesDAO additionalAttributesDAO,
            final RemoteProctoringRoomDAO remoteProctoringRoomDAO,
            final WebserviceInfo webserviceInfo,
            final Cryptor cryptor, 
            final JSONMapper jsonMapper) {

        this.additionalAttributesDAO = additionalAttributesDAO;
        this.remoteProctoringRoomDAO = remoteProctoringRoomDAO;
        this.screenProctoringServiceBundle = webserviceInfo.getScreenProctoringServiceBundle();
        this.cryptor = cryptor;
        this.jsonMapper = jsonMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ProctoringServiceSettings> getProctoringSettings(final EntityKey entityKey) {
        return Result.tryCatch(() -> {
            final Long entityId = Long.parseLong(entityKey.modelId);

            return additionalAttributesDAO
                    .getAdditionalAttribute(entityKey.entityType, entityId, ProctoringServiceSettings.ATTR_ADDITIONAL_ATTRIBUTE_STORE_NAME)
                    .map(rec -> {
                        final String encryptedSetting = rec.getValue();
                        final String jsonSettings = this.cryptor.decrypt(encryptedSetting).getOrThrow().toString();
                        try {
                            return jsonMapper.readValue(jsonSettings, ProctoringServiceSettings.class);
                        } catch (final JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .onErrorDo( error -> getLegacyProctoringServiceSettings(entityKey, entityId))
                    .getOrThrow();
        });
    }

    @Override
    @Transactional
    public Result<ProctoringServiceSettings> saveProctoringServiceSettings(
            final EntityKey entityKey,
            final ProctoringServiceSettings proctoringServiceSettings) {

        return Result.tryCatch(() -> {
            final Long entityId = Long.parseLong(entityKey.modelId);
            final String json = jsonMapper.writeValueAsString(proctoringServiceSettings);
            final String encryptedSettings = this.cryptor.encrypt(Utils.trim(json))
                    .getOrThrow()
                    .toString();

            this.additionalAttributesDAO.saveAdditionalAttribute(
                    entityKey.entityType,
                    entityId,
                    ProctoringServiceSettings.ATTR_ADDITIONAL_ATTRIBUTE_STORE_NAME,
                    encryptedSettings);

            this.additionalAttributesDAO.saveAdditionalAttribute(
                    entityKey.entityType,
                    entityId,
                    ProctoringServiceSettings.ATTR_ENABLE_PROCTORING,
                    String.valueOf(proctoringServiceSettings.enableProctoring));
            
            return proctoringServiceSettings;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ScreenProctoringSettings> getScreenProctoringSettings(final EntityKey entityKey) {
        return Result.tryCatch(() -> {
            final Long entityId = Long.parseLong(entityKey.modelId);

            return additionalAttributesDAO
                    .getAdditionalAttribute(entityKey.entityType, entityId, ScreenProctoringSettings.ATTR_ADDITIONAL_ATTRIBUTE_STORE_NAME)
                    .map(rec -> {
                        final String encryptedSetting = rec.getValue();
                        final String jsonSettings = this.cryptor.decrypt(encryptedSetting).getOrThrow().toString();
                        try {
                            return jsonMapper.readValue(jsonSettings, ScreenProctoringSettings.class);
                        } catch (final JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .onErrorDo( error -> getLegacyScreenProctoringSettings(entityKey, entityId))
                    .getOrThrow();
        });
    }

    @Override
    @Transactional
    public Result<ScreenProctoringSettings> storeScreenProctoringSettings(
            final EntityKey entityKey,
            final ScreenProctoringSettings screenProctoringSettings) {

        return Result.tryCatch(() -> {

            final Long entityId = Long.parseLong(entityKey.modelId);
            final String json = jsonMapper.writeValueAsString(screenProctoringSettings);
            final String encryptedSettings = this.cryptor.encrypt(Utils.trim(json))
                    .getOrThrow()
                    .toString();

            this.additionalAttributesDAO.saveAdditionalAttribute(
                    entityKey.entityType,
                    entityId,
                    ScreenProctoringSettings.ATTR_ADDITIONAL_ATTRIBUTE_STORE_NAME,
                    encryptedSettings);

            this.additionalAttributesDAO.saveAdditionalAttribute(
                    entityKey.entityType,
                    entityId,
                    ScreenProctoringSettings.ATTR_ENABLE_SCREEN_PROCTORING,
                    String.valueOf(screenProctoringSettings.enableScreenProctoring));
            
            return screenProctoringSettings;
        });
    }

    @Override
    @Transactional
    public void disableScreenProctoring(final Long examId) {
        this.additionalAttributesDAO.saveAdditionalAttribute(
                EntityType.EXAM,
                examId,
                ScreenProctoringSettings.ATTR_ENABLE_SCREEN_PROCTORING,
                Constants.FALSE_STRING)
                .onError(error -> log.warn(
                        "Failed to disable screen proctoring for exam: {} error: {}",
                        examId,
                        error.getMessage()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isScreenProctoringEnabled(final Long examId) {
        return this.additionalAttributesDAO.getAdditionalAttribute(EntityType.EXAM,
                examId,
                ScreenProctoringSettings.ATTR_ENABLE_SCREEN_PROCTORING)
                .map(attrRec -> BooleanUtils.toBoolean(attrRec.getValue()))
                .getOr(false);
    }

    @Deprecated
    private Boolean getEnabled(final Map<String, AdditionalAttributeRecord> mapping) {
        if (mapping.containsKey(ProctoringServiceSettings.ATTR_ENABLE_PROCTORING)) {
            return BooleanUtils.toBoolean(mapping.get(ProctoringServiceSettings.ATTR_ENABLE_PROCTORING).getValue());
        } else {
            return false;
        }
    }

    @Deprecated
    private Boolean getScreenproctoringEnabled(final Map<String, AdditionalAttributeRecord> mapping) {
        if (mapping.containsKey(ScreenProctoringSettings.ATTR_ENABLE_SCREEN_PROCTORING)) {
            return BooleanUtils
                    .toBoolean(mapping.get(ScreenProctoringSettings.ATTR_ENABLE_SCREEN_PROCTORING).getValue());
        } else {
            return false;
        }
    }

    @Deprecated
    private ProctoringServerType getServerType(final Map<String, AdditionalAttributeRecord> mapping) {
        if (mapping.containsKey(ProctoringServiceSettings.ATTR_SERVER_TYPE)) {
            return ProctoringServerType
                    .valueOf(mapping.get(ProctoringServiceSettings.ATTR_SERVER_TYPE).getValue());
        } else {
            return ProctoringServerType.JITSI_MEET;
        }
    }

    @Deprecated
    private String getString(final Map<String, AdditionalAttributeRecord> mapping, final String name) {
        if (mapping.containsKey(name)) {
            return mapping.get(name).getValue();
        } else {
            return null;
        }
    }

    @Deprecated
    private Boolean getBoolean(final Map<String, AdditionalAttributeRecord> mapping, final String name) {
        if (mapping.containsKey(name)) {
            return BooleanUtils.toBooleanObject(mapping.get(name).getValue());
        } else {
            return false;
        }
    }

    @Deprecated
    private Integer getCollectingRoomSize(final Map<String, AdditionalAttributeRecord> mapping) {
        if (mapping.containsKey(ProctoringServiceSettings.ATTR_COLLECTING_ROOM_SIZE)) {
            return Integer.valueOf(mapping.get(ProctoringServiceSettings.ATTR_COLLECTING_ROOM_SIZE).getValue());
        } else {
            return 20;
        }
    }

    @Deprecated
    private CollectingStrategy getScreenProctoringCollectingStrategy(
            final Map<String, AdditionalAttributeRecord> mapping) {
        if (mapping.containsKey(ScreenProctoringSettings.ATTR_COLLECTING_STRATEGY)) {
            return CollectingStrategy
                    .valueOf(mapping.get(ScreenProctoringSettings.ATTR_COLLECTING_STRATEGY).getValue());
        } else {
            return CollectingStrategy.EXAM;
        }
    }

    @Deprecated
    private Integer getScreenProctoringCollectingSize(final Map<String, AdditionalAttributeRecord> mapping) {
        if (mapping.containsKey(ScreenProctoringSettings.ATTR_COLLECTING_GROUP_SIZE)) {
            return Integer.valueOf(mapping.get(ScreenProctoringSettings.ATTR_COLLECTING_GROUP_SIZE).getValue());
        } else {
            return 0;
        }
    }

    @Deprecated
    private EnumSet<ProctoringFeature> getEnabledFeatures(final Map<String, AdditionalAttributeRecord> mapping) {
        if (mapping.containsKey(ProctoringServiceSettings.ATTR_ENABLED_FEATURES)) {
            try {
                final String value = mapping.get(ProctoringServiceSettings.ATTR_ENABLED_FEATURES).getValue();
                return StringUtils.isNotBlank(value)
                        ? EnumSet.copyOf(Arrays.stream(StringUtils.split(value, Constants.LIST_SEPARATOR))
                                .map(str -> {
                                    try {
                                        return ProctoringFeature.valueOf(str);
                                    } catch (final Exception e) {
                                        log.error(
                                                "Failed to enabled single features for proctoring settings. Skipping. {}",
                                                e.getMessage());
                                        return null;
                                    }
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet()))
                        : EnumSet.noneOf(ProctoringFeature.class);
            } catch (final Exception e) {
                log.error("Failed to get enabled features for proctoring settings. Enable all. {}", e.getMessage());
                return EnumSet.allOf(ProctoringFeature.class);
            }
        } else {
            return EnumSet.allOf(ProctoringFeature.class);
        }
    }

    @Deprecated
    private String encryptSecret(final CharSequence secret) {
        if (StringUtils.isBlank(secret)) {
            return null;
        }
        return this.cryptor.encrypt(Utils.trim(secret))
                .getOrThrow()
                .toString();
    }

    private ScreenProctoringSettings getLegacyScreenProctoringSettings(final EntityKey entityKey, final Long entityId) {
        return this.additionalAttributesDAO
                .getAdditionalAttributes(entityKey.entityType, entityId)
                .map(attrs -> attrs.stream()
                        .collect(Collectors.toMap(
                                AdditionalAttributeRecord::getName,
                                Function.identity())))
                .map(mapping -> {
                    if (screenProctoringServiceBundle.bundled) {
                        return new ScreenProctoringSettings(
                                entityId,
                                getScreenproctoringEnabled(mapping),
                                screenProctoringServiceBundle.serviceURL,
                                screenProctoringServiceBundle.clientId,
                                screenProctoringServiceBundle.clientSecret.toString(),
                                screenProctoringServiceBundle.apiAccountName,
                                screenProctoringServiceBundle.apiAccountPassword.toString(),
                                null,
                                getScreenProctoringCollectingStrategy(mapping),
                                null,
                                getScreenProctoringCollectingSize(mapping),
                                null,
                                true);
                    } else {
                        return new ScreenProctoringSettings(
                                entityId,
                                getScreenproctoringEnabled(mapping),
                                getString(mapping, ScreenProctoringSettings.ATTR_SPS_SERVICE_URL),
                                getString(mapping, ScreenProctoringSettings.ATTR_SPS_API_KEY),
                                getString(mapping, ScreenProctoringSettings.ATTR_SPS_API_SECRET),
                                getString(mapping, ScreenProctoringSettings.ATTR_SPS_ACCOUNT_ID),
                                getString(mapping, ScreenProctoringSettings.ATTR_SPS_ACCOUNT_PASSWORD),
                                null,
                                getScreenProctoringCollectingStrategy(mapping),
                                null,
                                getScreenProctoringCollectingSize(mapping),
                                null,
                                false);
                    }
                })
                .getOrThrow();
    }

    private ProctoringServiceSettings getLegacyProctoringServiceSettings(final EntityKey entityKey, final Long entityId) {
        return this.additionalAttributesDAO
                .getAdditionalAttributes(entityKey.entityType, entityId)
                .map(attrs -> attrs.stream()
                        .collect(Collectors.toMap(
                                AdditionalAttributeRecord::getName,
                                Function.identity())))
                .map(mapping -> {
                    return new ProctoringServiceSettings(
                            entityId,
                            getEnabled(mapping),
                            getServerType(mapping),
                            getString(mapping, ProctoringServiceSettings.ATTR_SERVER_URL),
                            getCollectingRoomSize(mapping),
                            getEnabledFeatures(mapping),
                            entityKey.entityType == EntityType.EXAM
                                    ? this.remoteProctoringRoomDAO.isServiceInUse(entityId).getOr(true)
                                    : false,
                            getString(mapping, ProctoringServiceSettings.ATTR_APP_KEY),
                            getString(mapping, ProctoringServiceSettings.ATTR_APP_SECRET),
                            getString(mapping, ProctoringServiceSettings.ATTR_ACCOUNT_ID),
                            getString(mapping, ProctoringServiceSettings.ATTR_ACCOUNT_CLIENT_ID),
                            getString(mapping, ProctoringServiceSettings.ATTR_ACCOUNT_CLIENT_SECRET),
                            getString(mapping, ProctoringServiceSettings.ATTR_SDK_KEY),
                            getString(mapping, ProctoringServiceSettings.ATTR_SDK_SECRET),
                            getBoolean(mapping,
                                    ProctoringServiceSettings.ATTR_USE_ZOOM_APP_CLIENT_COLLECTING_ROOM));
                })
                .getOrThrow();
    }

}
