/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam.impl;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringFeature;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.AdditionalAttributeRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.RemoteProctoringRoomDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.exam.ProctoringAdminService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamProctoringService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring.ExamProctoringServiceFactory;

@Lazy
@Service
@WebServiceProfile
public class ProctoringAdminServiceImpl implements ProctoringAdminService {

    private static final Logger log = LoggerFactory.getLogger(ProctoringAdminServiceImpl.class);

    private final AdditionalAttributesDAO additionalAttributesDAO;
    private final RemoteProctoringRoomDAO remoteProctoringRoomDAO;
    private final ExamProctoringServiceFactory examProctoringServiceFactory;
    private final Cryptor cryptor;

    public ProctoringAdminServiceImpl(
            final AdditionalAttributesDAO additionalAttributesDAO,
            final RemoteProctoringRoomDAO remoteProctoringRoomDAO,
            final ExamProctoringServiceFactory examProctoringServiceFactory,
            final Cryptor cryptor) {

        this.additionalAttributesDAO = additionalAttributesDAO;
        this.remoteProctoringRoomDAO = remoteProctoringRoomDAO;
        this.examProctoringServiceFactory = examProctoringServiceFactory;
        this.cryptor = cryptor;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<ProctoringServiceSettings> getProctoringSettings(final EntityKey parentEntityKey) {

        return Result.tryCatch(() -> {
            final Long entityId = Long.parseLong(parentEntityKey.modelId);
            checkType(parentEntityKey);

            return this.additionalAttributesDAO
                    .getAdditionalAttributes(parentEntityKey.entityType, entityId)
                    .map(attrs -> attrs.stream()
                            .collect(Collectors.toMap(
                                    attr -> attr.getName(),
                                    Function.identity())))
                    .map(mapping -> {
                        return new ProctoringServiceSettings(
                                entityId,
                                getEnabled(mapping),
                                getServerType(mapping),
                                getString(mapping, ProctoringServiceSettings.ATTR_SERVER_URL),
                                getCollectingRoomSize(mapping),
                                getEnabledFeatures(mapping),
                                parentEntityKey.entityType == EntityType.EXAM
                                        ? this.remoteProctoringRoomDAO.isServiceInUse(entityId).getOr(true)
                                        : false,
                                getString(mapping, ProctoringServiceSettings.ATTR_APP_KEY),
                                getString(mapping, ProctoringServiceSettings.ATTR_APP_SECRET),
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
            final EntityKey parentEntityKey,
            final ProctoringServiceSettings proctoringServiceSettings) {

        return Result.tryCatch(() -> {
            final Long entityId = Long.parseLong(parentEntityKey.modelId);
            checkType(parentEntityKey);

            if (StringUtils.isNotBlank(proctoringServiceSettings.serverURL)) {
                testExamProctoring(proctoringServiceSettings).getOrThrow();
            }

            this.additionalAttributesDAO.saveAdditionalAttribute(
                    parentEntityKey.entityType,
                    entityId,
                    ProctoringServiceSettings.ATTR_ENABLE_PROCTORING,
                    String.valueOf(proctoringServiceSettings.enableProctoring));

            this.additionalAttributesDAO.saveAdditionalAttribute(
                    parentEntityKey.entityType,
                    entityId,
                    ProctoringServiceSettings.ATTR_SERVER_TYPE,
                    proctoringServiceSettings.serverType.name());

            this.additionalAttributesDAO.saveAdditionalAttribute(
                    parentEntityKey.entityType,
                    entityId,
                    ProctoringServiceSettings.ATTR_SERVER_URL,
                    StringUtils.trim(proctoringServiceSettings.serverURL));

            this.additionalAttributesDAO.saveAdditionalAttribute(
                    parentEntityKey.entityType,
                    entityId,
                    ProctoringServiceSettings.ATTR_COLLECTING_ROOM_SIZE,
                    String.valueOf(proctoringServiceSettings.collectingRoomSize));

            this.additionalAttributesDAO.saveAdditionalAttribute(
                    parentEntityKey.entityType,
                    entityId,
                    ProctoringServiceSettings.ATTR_APP_KEY,
                    StringUtils.trim(proctoringServiceSettings.appKey));

            this.additionalAttributesDAO.saveAdditionalAttribute(
                    parentEntityKey.entityType,
                    entityId,
                    ProctoringServiceSettings.ATTR_APP_SECRET,
                    this.cryptor.encrypt(Utils.trim(proctoringServiceSettings.appSecret))
                            .getOrThrow()
                            .toString());

            if (StringUtils.isNotBlank(proctoringServiceSettings.sdkKey)) {
                this.additionalAttributesDAO.saveAdditionalAttribute(
                        parentEntityKey.entityType,
                        entityId,
                        ProctoringServiceSettings.ATTR_SDK_KEY,
                        StringUtils.trim(proctoringServiceSettings.sdkKey));

                this.additionalAttributesDAO.saveAdditionalAttribute(
                        parentEntityKey.entityType,
                        entityId,
                        ProctoringServiceSettings.ATTR_SDK_SECRET,
                        this.cryptor.encrypt(Utils.trim(proctoringServiceSettings.sdkSecret))
                                .getOrThrow()
                                .toString());
            }

            this.additionalAttributesDAO.saveAdditionalAttribute(
                    parentEntityKey.entityType,
                    entityId,
                    ProctoringServiceSettings.ATTR_ENABLED_FEATURES,
                    StringUtils.join(proctoringServiceSettings.enabledFeatures, Constants.LIST_SEPARATOR));

            this.additionalAttributesDAO.saveAdditionalAttribute(
                    parentEntityKey.entityType,
                    entityId,
                    ProctoringServiceSettings.ATTR_USE_ZOOM_APP_CLIENT_COLLECTING_ROOM,
                    String.valueOf(proctoringServiceSettings.useZoomAppClientForCollectingRoom));

            return proctoringServiceSettings;
        });
    }

    @Override
    public Result<ExamProctoringService> getExamProctoringService(final ProctoringServerType type) {
        return this.examProctoringServiceFactory
                .getExamProctoringService(type);
    }

    private void checkType(final EntityKey parentEntityKey) {
        if (!SUPPORTED_PARENT_ENTITES.contains(parentEntityKey.entityType)) {
            throw new UnsupportedOperationException(
                    "No proctoring service settings supported for entity: " + parentEntityKey);
        }
    }

    private Boolean getEnabled(final Map<String, AdditionalAttributeRecord> mapping) {
        if (mapping.containsKey(ProctoringServiceSettings.ATTR_ENABLE_PROCTORING)) {
            return BooleanUtils.toBoolean(mapping.get(ProctoringServiceSettings.ATTR_ENABLE_PROCTORING).getValue());
        } else {
            return false;
        }
    }

    private ProctoringServerType getServerType(final Map<String, AdditionalAttributeRecord> mapping) {
        if (mapping.containsKey(ProctoringServiceSettings.ATTR_SERVER_TYPE)) {
            return ProctoringServerType.valueOf(mapping.get(ProctoringServiceSettings.ATTR_SERVER_TYPE).getValue());
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

    private EnumSet<ProctoringFeature> getEnabledFeatures(final Map<String, AdditionalAttributeRecord> mapping) {
        if (mapping.containsKey(ProctoringServiceSettings.ATTR_ENABLED_FEATURES)) {
            try {
                final String value = mapping.get(ProctoringServiceSettings.ATTR_ENABLED_FEATURES).getValue();
                return StringUtils.isNotBlank(value)
                        ? EnumSet.copyOf(Arrays.asList(StringUtils.split(value, Constants.LIST_SEPARATOR))
                                .stream()
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

}
