/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.*;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.*;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SEBSettingsService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamConfigUpdateService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Lazy
@Service
@WebServiceProfile
public class SEBSettingsServiceImpl implements SEBSettingsService {

    private static final Logger log = LoggerFactory.getLogger(SEBSettingsServiceImpl.class);

    private final ConfigurationDAO configurationDAO;
    private final ConfigurationAttributeDAO configurationAttributeDAO;
    private final OrientationDAO orientationDAO;
    private final ConfigurationValueDAO configurationValueDAO;
    private final ExamConfigurationMapDAO examConfigurationMapDAO;
    private final ExamConfigUpdateService examConfigUpdateService;
    private final ExamSessionService examSessionService;

    public SEBSettingsServiceImpl(
            final ConfigurationDAO configurationDAO,
            final ConfigurationAttributeDAO configurationAttributeDAO,
            final OrientationDAO orientationDAO,
            final ConfigurationValueDAO configurationValueDAO,
            final ExamConfigurationMapDAO examConfigurationMapDAO,
            final ExamConfigUpdateService examConfigUpdateService, 
            final ExamSessionService examSessionService) {
        
        this.configurationDAO = configurationDAO;
        this.configurationAttributeDAO = configurationAttributeDAO;
        this.orientationDAO = orientationDAO;
        this.configurationValueDAO = configurationValueDAO;
        this.examConfigurationMapDAO = examConfigurationMapDAO;
        this.examConfigUpdateService = examConfigUpdateService;
        this.examSessionService = examSessionService;
    }

    @Override
    public Set<Long> getAttributeIdsForView(final SEBSettingsView.ViewType viewType) {
        return orientationDAO
                .getConfigAttributeIdsOfView(viewType)
                .getOr(VIEW_ATTRIBUTE_MAPPINGS.get(viewType));
    }


    @Override
    public Result<SEBSettingsView> getSEBSettingsOfTemplate(
            final Long templateId, 
            final SEBSettingsView.ViewType viewType) {
        
        return Result.tryCatch(() -> getSEBSettings(templateId, viewType));
    }

    @Override
    public Result<SEBSettingsView> getSEBSettingsOfExam(
            final Long examId, 
            final SEBSettingsView.ViewType viewType) {
        
        return  examConfigurationMapDAO
                .getDefaultConfigurationNode(examId)
                .map( configNodeId -> getSEBSettings(configNodeId, viewType));

    }

    @Override
    public Result<List<SEBSettingsView.TableRowValues>> getTableValuesOfTemplate(
            final Long templateId, 
            final String attributeName) {
        
        return Result.tryCatch(() -> getTableValues(templateId, attributeName));
    }

    @Override
    public Result<List<SEBSettingsView.TableRowValues>> getTableValuesOfExam(
            final Long examId, 
            final String attributeName) {

        return  examConfigurationMapDAO
                .getDefaultConfigurationNode(examId)
                .map( configNodeId -> getTableValues(configNodeId, attributeName));
    }

    @Override
    public Result<SEBSettingsView.Value> saveSingleValueForTemplate(
            final Long templateId, 
            final Long valueId, 
            final String value) {

        return Result.tryCatch(() -> saveSingleValue(templateId, valueId, value));
    }

    @Override
    public Result<SEBSettingsView.Value> saveSingleValueForExam(
            final Long examId, 
            final Long valueId, 
            final String value) {

        return  examConfigurationMapDAO
                .getDefaultConfigurationNode(examId)
                .map( configNodeId -> saveSingleValue(configNodeId, valueId, value));
    }

    @Override
    public Result<SEBSettingsView.TableRowValues> saveTableRowValuesForTemplate(
            final Long templateId, 
            final SEBSettingsView.TableRowValues values) {

        return Result.tryCatch(() -> saveTableRowValues(templateId, values));
    }

    @Override
    public Result<SEBSettingsView.TableRowValues> saveTableRowValuesForExam(
            final Long examId, 
            final SEBSettingsView.TableRowValues values) {

        return  examConfigurationMapDAO
                .getDefaultConfigurationNode(examId)
                .map( configNodeId -> saveTableRowValues(configNodeId, values));
    }

    @Override
    public Result<SEBSettingsView.TableRowValues> addNewTableRowForTemplate(
            final Long templateId, 
            final String attributeName) {

        return Result.tryCatch(() -> addNewTableRow(templateId, attributeName));
    }

    @Override
    public Result<SEBSettingsView.TableRowValues> addNewTableRowForExam(
            final Long examId, 
            final String attributeName) {

        return  examConfigurationMapDAO
                .getDefaultConfigurationNode(examId)
                .map( configNodeId -> addNewTableRow(configNodeId, attributeName));
    }
    

    @Override
    public Result<List<SEBSettingsView.TableRowValues>> deleteTableRowForTemplate(
            final Long templateId, 
            final String attributeName, 
            final int index) {

        return Result.tryCatch(() -> deleteTableRow(templateId, attributeName, index));
    }

    @Override
    public Result<List<SEBSettingsView.TableRowValues>> deleteTableRowForExam(
            final Long examId, 
            final String attributeName, 
            final int index) {

        return  examConfigurationMapDAO
                .getDefaultConfigurationNode(examId)
                .map( configNodeId -> deleteTableRow(configNodeId, attributeName, index));
    }

    @Override
    public Result<Integer> getActiveSEBClientsForExam(final Long examId) {
        return examSessionService
                .getActiveConnectionTokens(examId)
                .map(Collection::size);
    }

    @Override
    public Result<Long> applySettingsForTemplate(final Long templateId) {

        // create new history entry and clear history
        return configurationDAO
                .saveToHistory(templateId)
                .flatMap(config -> configurationDAO.clearHistory(templateId))
                .map(Configuration::getId);
    }

    @Override
    public Result<Long> applySettingsForExam(final Long examId) {
        // copy values to new followup as usual but then clear the history
        return examConfigurationMapDAO
                .getDefaultConfigurationNode(examId)
                .map(this::processExamConfigurationChange)
                .flatMap(configurationDAO::clearHistory)
                .map( all -> examId);
    }

    @Override
    public Result<Long> undoSettingsForTemplate(final Long templateId) {
        // copy the values from first rev to followup
        return configurationDAO
                .getConfigurationLastStableVersion(templateId)
                .flatMap(backup -> configurationDAO.restoreToVersion(templateId, backup.id))
                .map( config -> templateId);
    }

    @Override
    public Result<Long> undoSettingsForExam(final Long examId) {
        return examConfigurationMapDAO
                .getDefaultConfigurationNode(examId)
                .flatMap(configurationDAO::undo)
                .map( config -> examId);
    }


    private SEBSettingsView getSEBSettings(
            final Long configurationNodeId,
            final SEBSettingsView.ViewType viewType) {
        
        final Configuration followUpConfig = configurationDAO
                .getFollowupConfiguration(configurationNodeId)
                .getOrThrow();

        // get attributes and value mappings
        final Map<String, ConfigurationAttribute> attributes = getAttributesForView(viewType);
        final Map<Long, ConfigurationAttribute> attrIdMapping = attributes.values().stream()
                .collect(Collectors.toMap(attr -> attr.id, Function.identity()));
        final Map<String, SEBSettingsView.Value>  singleValues = getSingleValues(followUpConfig.id, attributes);
        final Map<String, List<SEBSettingsView.TableRowValues>> tableValues = getTableRowMapping(followUpConfig, attributes, attrIdMapping);

        return new SEBSettingsView(
                viewType,
                configurationNodeId,
                followUpConfig.id,
                attributes,
                singleValues,
                tableValues);
    }

    private List<SEBSettingsView.TableRowValues> getTableValues(
            final Long configurationNodeId,
            final String attributeName) {

        final Configuration followUpConfig = configurationDAO
                .getFollowupConfiguration(configurationNodeId)
                .getOrThrow();

        final ConfigurationAttribute tableAttribute = configurationAttributeDAO
                .getAttributeIdByName(attributeName)
                .flatMap(configurationAttributeDAO::byPK)
                .getOrThrow();

        if (tableAttribute.type != AttributeType.TABLE && tableAttribute.type != AttributeType.COMPOSITE_TABLE) {
            throw new APIMessage.APIMessageException(APIMessage.ErrorMessage.BAD_REQUEST.of());
        }

        return getTableValues(followUpConfig, tableAttribute)
                .getOrThrow();
    }

    private SEBSettingsView.Value saveSingleValue(
            final Long configurationNodeId,
            final Long valueId,
            final String value) {
        
        // check if config exists
        configurationDAO
                .getFollowupConfiguration(configurationNodeId)
                .getOrThrow();

        final ConfigurationValue cValue = configurationValueDAO.byPK(valueId).getOrThrow();
        final ConfigurationValue newCValue = configurationValueDAO.save(new ConfigurationValue(
                cValue.id,
                cValue.institutionId,
                cValue.configurationId,
                cValue.attributeId,
                cValue.listIndex,
                value)).getOrThrow();

        return new SEBSettingsView.Value(cValue.id, newCValue.value);
    }

    private SEBSettingsView.TableRowValues saveTableRowValues(
            final Long configurationNodeId,
            final SEBSettingsView.TableRowValues values) {
        
        final Configuration followUpConfig = configurationDAO.
                getFollowupConfiguration(configurationNodeId)
                .getOrThrow();
        
        values.rowValues().forEach((key, value) -> {
            try {
                final Long vid = value.valueId();
                if (vid == null || vid < 0) {
                    final Long attrId = configurationAttributeDAO
                            .getAttributeIdByName(key)
                            .getOrThrow();
                    final ConfigurationValue newCValue = configurationValueDAO.save(new ConfigurationValue(
                                    null,
                                    followUpConfig.institutionId,
                                    followUpConfig.id,
                                    attrId,
                                    values.index(),
                                    value.value()))
                            .getOrThrow();
                } else {
                    final ConfigurationValue cValue = configurationValueDAO.byPK(vid).getOrThrow();
                    configurationValueDAO.save(new ConfigurationValue(
                                    cValue.id,
                                    cValue.institutionId,
                                    cValue.configurationId,
                                    cValue.attributeId,
                                    cValue.listIndex,
                                    value.value()))
                            .getOrThrow();
                }
            } catch (final Exception e) {
                log.warn("Failed to save SEB Settings table row value: {}, {} cause: {}", key, value, e.getMessage());
            }
        });

        return values;
    }

    private SEBSettingsView.TableRowValues addNewTableRow(
            final Long configurationNodeId,
            final String attributeName) {

        final Configuration followUpConfig = configurationDAO
                .getFollowupConfiguration(configurationNodeId)
                .getOrThrow();

        final ConfigurationAttribute tableAttribute = configurationAttributeDAO
                .getAttributeIdByName(attributeName)
                .flatMap(configurationAttributeDAO::byPK)
                .getOrThrow();

        if (tableAttribute.type != AttributeType.TABLE) {
            throw new APIMessage.APIMessageException(APIMessage.ErrorMessage.BAD_REQUEST.of());
        }

        final Collection<ConfigurationAttribute> columns = configurationAttributeDAO
                .allChildAttributes(tableAttribute.id)
                .getOrThrow();

        final int index = configurationValueDAO.getTableSize(followUpConfig.id, tableAttribute.id);
        columns.forEach( column -> {
            final ConfigurationValue newValue = configurationValueDAO.createNew(new ConfigurationValue(
                            null,
                            followUpConfig.institutionId,
                            followUpConfig.id,
                            column.id,
                            index,
                            column.defaultValue))
                    .getOrThrow();
        });

        return getTableValues(followUpConfig, tableAttribute)
                .map(table -> table.get(index))
                .getOrThrow();
    }

    private List<SEBSettingsView.TableRowValues> deleteTableRow(
            final Long configurationNodeId,
            final String attributeName,
            final int index) {

        final Configuration followUpConfig = configurationDAO
                .getFollowupConfiguration(configurationNodeId)
                .getOrThrow();

        final ConfigurationAttribute tableAttribute = configurationAttributeDAO
                .getAttributeIdByName(attributeName)
                .flatMap(configurationAttributeDAO::byPK)
                .getOrThrow();

        if (tableAttribute.type != AttributeType.TABLE) {
            throw new APIMessage.APIMessageException(APIMessage.ErrorMessage.BAD_REQUEST.of("Attribute is not of type TABLE"));
        }

        return configurationValueDAO
                .getTableValues(followUpConfig.institutionId, followUpConfig.id, tableAttribute.id)
                .map( v -> deleteRow(v, index))
                .flatMap( configurationValueDAO::saveTableValues)
                .map( v -> getTableValues(configurationNodeId, attributeName))
                .getOrThrow();
    }
    

    private ConfigurationTableValues deleteRow(
            final ConfigurationTableValues values,
            final int index) {

        // delete the selected row and update indices
        final List<ConfigurationTableValues.TableValue> list = new ArrayList<>();
        values.values.forEach(v -> {
            if (v.listIndex != index) {
                if (v.listIndex > index) {
                    list.add(new ConfigurationTableValues.TableValue(v.attributeId, v.listIndex - 1, v.value));
                } else {
                    list.add(new ConfigurationTableValues.TableValue(v.attributeId, v.listIndex, v.value));
                }
            }
        });

        return new ConfigurationTableValues(
                values.institutionId,
                values.configurationId,
                values.attributeId,
                list);
    }

    private Map<String, ConfigurationAttribute> getAttributesForView(final SEBSettingsView.ViewType viewType) {
        final Set<Long> attrIds = getAttributeIdsForView(viewType);
        if (attrIds == null) {
            return Collections.emptyMap();
        }

        return configurationAttributeDAO.allOf(attrIds)
                .map(attrs -> attrs
                        .stream()
                        .collect(Collectors.toMap(a -> a.name, Function.identity())))
                .getOrThrow();
    }

    private Map<String, SEBSettingsView.Value> getSingleValues(
            final Long configId,
            final Map<String, ConfigurationAttribute> attributes) {

        final Map<Long, ConfigurationAttribute> attrIdsMap = attributes.values().stream()
                .filter( attr -> attr.parentId == null)
                .collect(Collectors.toMap(attr -> attr.id, Function.identity()));
        return configurationValueDAO
                .getConfigAttributeValues(configId, attrIdsMap.keySet())
                .map( attrs -> attrs.stream()
                        .collect(Collectors.toMap(
                                val -> attrIdsMap.get(val.attributeId).name,
                                val -> new SEBSettingsView.Value(val.id, val.value) )))
                .getOrThrow();
    }

    private Result<List<SEBSettingsView.TableRowValues>> getTableValues(
            final Configuration followUpConfig,
            final ConfigurationAttribute attribute) {

        final Map<Long, String> mapping = configurationAttributeDAO
                .allChildAttributes(attribute.id)
                .getOrThrow()
                .stream()
                .collect(Collectors.toMap(a -> a.id, a -> a.name));

        return configurationValueDAO.getOrderedTableValues(
                        followUpConfig.institutionId,
                        followUpConfig.getId(),
                        attribute.id,
                        true)
                .map(val -> {
                            final List<SEBSettingsView.TableRowValues> res = new ArrayList<>();
                            int i = 0;
                            for (final List<ConfigurationValue> row : val) {
                                final SEBSettingsView.TableRowValues tableRowValues = new SEBSettingsView.TableRowValues(attribute.name, i, row.stream()
                                        .filter(Objects::nonNull).
                                        collect(Collectors.toMap(
                                                v -> mapping.get(v.attributeId),
                                                v -> new SEBSettingsView.Value(v.id, v.value))));
                                res.add(tableRowValues);
                                i++;
                            }
                            return res;
                        }
                );
    }

    private Map<String, List<SEBSettingsView.TableRowValues>> getTableRowMapping(
            final Configuration followUpConfig,
            final Map<String, ConfigurationAttribute> attributes,
            final Map<Long, ConfigurationAttribute> attrIdMapping) {

        final Map<String, List<List<ConfigurationValue>>> tableVals = getTableValues(followUpConfig.institutionId, followUpConfig.id, attributes);
        final Map<String, List<SEBSettingsView.TableRowValues>> tableValues = new HashMap<>();

        tableVals.forEach((key, attrValues) -> {
            final List<SEBSettingsView.TableRowValues> rows = new ArrayList<>();
            int i = 0;
            for (final List<ConfigurationValue> row : attrValues) {
                final SEBSettingsView.TableRowValues tableRowValues = new SEBSettingsView.TableRowValues(key, i, row.stream()
                        .filter(v -> v != null &&  attrIdMapping.get(v.attributeId) != null)
                        .collect(Collectors.toMap(
                                v -> attrIdMapping.get(v.attributeId).name,
                                v -> new SEBSettingsView.Value(v.id, v.value))));
                rows.add(tableRowValues);
                i++;
            }
            tableValues.put(key, rows);
        });
        return tableValues;
    }

    private Map<String, List<List<ConfigurationValue>>> getTableValues(
            final Long institutionId,
            final Long configId,
            final Map<String, ConfigurationAttribute> attributes) {

        final Map<String, List<List<ConfigurationValue>>> listValues = new HashMap<>();
        attributes.values().forEach(attr -> {
            if (attr.type == AttributeType.TABLE || attr.type == AttributeType.COMPOSITE_TABLE) {
                final List<List<ConfigurationValue>> tableValues = configurationValueDAO
                        .getOrderedTableValues(
                                institutionId,
                                configId,
                                attr.id,
                                true)
                        .getOrThrow();
                listValues.put(attr.name, tableValues);
            }
        });
        return listValues;
    }

    private Long processExamConfigurationChange(final Long configurationNodeId) {
        examConfigUpdateService
                .processExamConfigurationChange(configurationNodeId)
                .getOrThrow();
        return configurationNodeId;
    }
}
