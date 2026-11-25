/*
 * Copyright (c) 2023 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.CollectingStrategy;
import ch.ethz.seb.sebserver.gbl.model.exam.ScreenProctoringSettings;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.AdditionalAttributeRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ProctoringSettingsDAO;

@Lazy
@Component
public class ProctoringSettingsDAOImpl implements ProctoringSettingsDAO {

    private static final Logger log = LoggerFactory.getLogger(ProctoringSettingsDAOImpl.class);

    private final AdditionalAttributesDAO additionalAttributesDAO;
    private final WebserviceInfo.ScreenProctoringServiceBundle screenProctoringServiceBundle;
    private final Cryptor cryptor;
    private final JSONMapper jsonMapper;

    public ProctoringSettingsDAOImpl(
            final AdditionalAttributesDAO additionalAttributesDAO,
            final WebserviceInfo webserviceInfo,
            final Cryptor cryptor, 
            final JSONMapper jsonMapper) {

        this.additionalAttributesDAO = additionalAttributesDAO;
        this.screenProctoringServiceBundle = webserviceInfo.getScreenProctoringServiceBundle();
        this.cryptor = cryptor;
        this.jsonMapper = jsonMapper;
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

    private Boolean getScreenproctoringEnabled(final Map<String, AdditionalAttributeRecord> mapping) {
        if (mapping.containsKey(ScreenProctoringSettings.ATTR_ENABLE_SCREEN_PROCTORING)) {
            return BooleanUtils
                    .toBoolean(mapping.get(ScreenProctoringSettings.ATTR_ENABLE_SCREEN_PROCTORING).getValue());
        } else {
            return false;
        }
    }

    private CollectingStrategy getScreenProctoringCollectingStrategy(
            final Map<String, AdditionalAttributeRecord> mapping) {
        if (mapping.containsKey(ScreenProctoringSettings.ATTR_COLLECTING_STRATEGY)) {
            return CollectingStrategy
                    .valueOf(mapping.get(ScreenProctoringSettings.ATTR_COLLECTING_STRATEGY).getValue());
        } else {
            return CollectingStrategy.APPLY_SEB_GROUPS;
        }
    }


    private Integer getScreenProctoringCollectingSize(final Map<String, AdditionalAttributeRecord> mapping) {
        if (mapping.containsKey(ScreenProctoringSettings.ATTR_COLLECTING_GROUP_SIZE)) {
            return Integer.valueOf(mapping.get(ScreenProctoringSettings.ATTR_COLLECTING_GROUP_SIZE).getValue());
        } else {
            return 0;
        }
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
                                getScreenProctoringCollectingStrategy(mapping),
                                null,
                                getScreenProctoringCollectingSize(mapping),
                                null,
                                true,
                                false);
                    } else {
                        return new ScreenProctoringSettings(
                                entityId,
                                getScreenproctoringEnabled(mapping),
                                getScreenProctoringCollectingStrategy(mapping),
                                null,
                                getScreenProctoringCollectingSize(mapping),
                                null,
                                false,
                                false);
                    }
                })
                .getOrThrow();
    }

}
