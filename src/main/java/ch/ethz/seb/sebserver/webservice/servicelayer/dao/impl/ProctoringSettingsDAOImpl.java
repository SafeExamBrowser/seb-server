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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final Cryptor cryptor;

    public ProctoringSettingsDAOImpl(
            final AdditionalAttributesDAO additionalAttributesDAO,
            final RemoteProctoringRoomDAO remoteProctoringRoomDAO,
            final Cryptor cryptor) {

        this.additionalAttributesDAO = additionalAttributesDAO;
        this.remoteProctoringRoomDAO = remoteProctoringRoomDAO;
        this.cryptor = cryptor;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ProctoringServiceSettings> getProctoringSettings(final EntityKey entityKey) {
        return Result.tryCatch(() -> {
            final Long entityId = Long.parseLong(entityKey.modelId);

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
        });
    }

    @Override
    @Transactional
    public Result<ProctoringServiceSettings> saveProctoringServiceSettings(
            final EntityKey entityKey,
            final ProctoringServiceSettings proctoringServiceSettings) {

        return Result.tryCatch(() -> {
            final Long entityId = Long.parseLong(entityKey.modelId);

            final Map<String, String> attributes = new HashMap<>();
            attributes.put(
                    ProctoringServiceSettings.ATTR_ENABLE_PROCTORING,
                    String.valueOf(proctoringServiceSettings.enableProctoring));
            attributes.put(
                    ProctoringServiceSettings.ATTR_SERVER_TYPE,
                    proctoringServiceSettings.serverType.name());
            attributes.put(
                    ProctoringServiceSettings.ATTR_SERVER_URL,
                    StringUtils.trim(proctoringServiceSettings.serverURL));
            attributes.put(
                    ProctoringServiceSettings.ATTR_COLLECTING_ROOM_SIZE,
                    String.valueOf(proctoringServiceSettings.collectingRoomSize));
            attributes.put(
                    ProctoringServiceSettings.ATTR_APP_KEY,
                    StringUtils.trim(proctoringServiceSettings.appKey));
            attributes.put(
                    ProctoringServiceSettings.ATTR_APP_SECRET,
                    encryptSecret(Utils.trim(proctoringServiceSettings.appSecret)));
            attributes.put(
                    ProctoringServiceSettings.ATTR_ACCOUNT_ID,
                    StringUtils.trim(proctoringServiceSettings.accountId));
            attributes.put(
                    ProctoringServiceSettings.ATTR_ACCOUNT_CLIENT_ID,
                    StringUtils.trim(proctoringServiceSettings.clientId));
            attributes.put(
                    ProctoringServiceSettings.ATTR_ACCOUNT_CLIENT_SECRET,
                    encryptSecret(Utils.trim(proctoringServiceSettings.clientSecret)));
            attributes.put(
                    ProctoringServiceSettings.ATTR_SDK_KEY,
                    StringUtils.trim(proctoringServiceSettings.sdkKey));
            attributes.put(
                    ProctoringServiceSettings.ATTR_SDK_SECRET,
                    encryptSecret(Utils.trim(proctoringServiceSettings.sdkSecret)));
            attributes.put(
                    ProctoringServiceSettings.ATTR_ENABLED_FEATURES,
                    StringUtils.join(proctoringServiceSettings.enabledFeatures, Constants.LIST_SEPARATOR));
            attributes.put(
                    ProctoringServiceSettings.ATTR_USE_ZOOM_APP_CLIENT_COLLECTING_ROOM,
                    String.valueOf(proctoringServiceSettings.useZoomAppClientForCollectingRoom));

            this.additionalAttributesDAO.saveAdditionalAttributes(
                    entityKey.entityType,
                    entityId,
                    attributes,
                    true);

            return proctoringServiceSettings;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ScreenProctoringSettings> getScreenProctoringSettings(final EntityKey entityKey) {
        return Result.tryCatch(() -> {
            final Long entityId = Long.parseLong(entityKey.modelId);

            //checkType(parentEntityKey);

            return this.additionalAttributesDAO
                    .getAdditionalAttributes(entityKey.entityType, entityId)
                    .map(attrs -> attrs.stream()
                            .collect(Collectors.toMap(
                                    AdditionalAttributeRecord::getName,
                                    Function.identity())))
                    .map(mapping -> {
                        return new ScreenProctoringSettings(
                                entityId,
                                getScreenproctoringEnabled(mapping),
                                getString(mapping, ScreenProctoringSettings.ATTR_SPS_SERVICE_URL),
                                getString(mapping, ScreenProctoringSettings.ATTR_SPS_API_KEY),
                                getString(mapping, ScreenProctoringSettings.ATTR_SPS_API_SECRET),
                                getString(mapping, ScreenProctoringSettings.ATTR_SPS_ACCOUNT_ID),
                                getString(mapping, ScreenProctoringSettings.ATTR_SPS_ACCOUNT_PASSWORD),
                                getScreenProctoringCollectingStrategy(mapping),
                                getScreenProctoringCollectingSize(mapping));
                    })
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

            final Map<String, String> attributes = new HashMap<>();
            attributes.put(
                    ScreenProctoringSettings.ATTR_ENABLE_SCREEN_PROCTORING,
                    String.valueOf(screenProctoringSettings.enableScreenProctoring));
            attributes.put(
                    ScreenProctoringSettings.ATTR_SPS_SERVICE_URL,
                    StringUtils.trim(screenProctoringSettings.spsServiceURL));
            attributes.put(
                    ScreenProctoringSettings.ATTR_COLLECTING_STRATEGY,
                    String.valueOf(screenProctoringSettings.collectingStrategy));
            attributes.put(
                    ScreenProctoringSettings.ATTR_COLLECTING_GROUP_SIZE,
                    String.valueOf(screenProctoringSettings.collectingGroupSize));
            attributes.put(
                    ScreenProctoringSettings.ATTR_SPS_API_KEY,
                    StringUtils.trim(screenProctoringSettings.spsAPIKey));
            attributes.put(
                    ScreenProctoringSettings.ATTR_SPS_API_SECRET,
                    encryptSecret(Utils.trim(screenProctoringSettings.spsAPISecret)));
            attributes.put(
                    ScreenProctoringSettings.ATTR_SPS_ACCOUNT_ID,
                    StringUtils.trim(screenProctoringSettings.spsAccountId));
            attributes.put(
                    ScreenProctoringSettings.ATTR_SPS_ACCOUNT_PASSWORD,
                    encryptSecret(Utils.trim(screenProctoringSettings.spsAccountPassword)));

            this.additionalAttributesDAO.saveAdditionalAttributes(
                    entityKey.entityType,
                    entityId,
                    attributes,
                    true);

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

    private Boolean getEnabled(final Map<String, AdditionalAttributeRecord> mapping) {
        if (mapping.containsKey(ProctoringServiceSettings.ATTR_ENABLE_PROCTORING)) {
            return BooleanUtils.toBoolean(mapping.get(ProctoringServiceSettings.ATTR_ENABLE_PROCTORING).getValue());
        } else {
            return false;
        }
    }

    private Boolean getScreenproctoringEnabled(final Map<String, AdditionalAttributeRecord> mapping) {
        if (mapping.containsKey(ScreenProctoringSettings.ATTR_ENABLE_SCREEN_PROCTORING)) {
            return BooleanUtils
                    .toBoolean(mapping.get(ScreenProctoringSettings.ATTR_ENABLE_SCREEN_PROCTORING).getValue());
        } else {
            return false;
        }
    }

    private ProctoringServerType getServerType(final Map<String, AdditionalAttributeRecord> mapping) {
        if (mapping.containsKey(ProctoringServiceSettings.ATTR_SERVER_TYPE)) {
            return ProctoringServerType
                    .valueOf(mapping.get(ProctoringServiceSettings.ATTR_SERVER_TYPE).getValue());
        } else {
            return ProctoringServerType.JITSI_MEET;
        }
    }

    private String getString(final Map<String, AdditionalAttributeRecord> mapping, final String name) {
        if (mapping.containsKey(name)) {
            return mapping.get(name).getValue();
        } else {
            return null;
        }
    }

    private Boolean getBoolean(final Map<String, AdditionalAttributeRecord> mapping, final String name) {
        if (mapping.containsKey(name)) {
            return BooleanUtils.toBooleanObject(mapping.get(name).getValue());
        } else {
            return false;
        }
    }

    private Integer getCollectingRoomSize(final Map<String, AdditionalAttributeRecord> mapping) {
        if (mapping.containsKey(ProctoringServiceSettings.ATTR_COLLECTING_ROOM_SIZE)) {
            return Integer.valueOf(mapping.get(ProctoringServiceSettings.ATTR_COLLECTING_ROOM_SIZE).getValue());
        } else {
            return 20;
        }
    }

    private CollectingStrategy getScreenProctoringCollectingStrategy(
            final Map<String, AdditionalAttributeRecord> mapping) {
        if (mapping.containsKey(ScreenProctoringSettings.ATTR_COLLECTING_STRATEGY)) {
            return CollectingStrategy
                    .valueOf(mapping.get(ScreenProctoringSettings.ATTR_COLLECTING_STRATEGY).getValue());
        } else {
            return CollectingStrategy.FIX_SIZE;
        }
    }

    private Integer getScreenProctoringCollectingSize(final Map<String, AdditionalAttributeRecord> mapping) {
        if (mapping.containsKey(ScreenProctoringSettings.ATTR_COLLECTING_GROUP_SIZE)) {
            return Integer.valueOf(mapping.get(ScreenProctoringSettings.ATTR_COLLECTING_GROUP_SIZE).getValue());
        } else {
            return 0;
        }
    }

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

    private String encryptSecret(final CharSequence secret) {
        if (StringUtils.isBlank(secret)) {
            return null;
        }
        return this.cryptor.encrypt(Utils.trim(secret))
                .getOrThrow()
                .toString();
    }

}
