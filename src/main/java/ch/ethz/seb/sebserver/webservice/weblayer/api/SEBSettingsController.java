/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.*;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.WebserviceConfig;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.SEB_SETTINGS_ENDPOINT)
@SecurityRequirement(name = WebserviceConfig.SWAGGER_AUTH_ADMIN_API)
public class SEBSettingsController {

    private static final Logger log = LoggerFactory.getLogger(SEBSettingsController.class);
    
    private final ConfigurationDAO configurationDAO;
    private final ConfigurationAttributeDAO configurationAttributeDAO;
    private final ConfigurationValueDAO configurationValueDAO;
    private final ExamConfigurationMapDAO examConfigurationMapDAO;
    private final AuthorizationService authorizationService;
    private final ExamDAO examDAO;
    
    
    /** Mapping of all SEB Settings/ConfigurationAttributes for different SEB Settings view.
     *  These are mostly specialised SEB Settings views with fewer Settings than the original view.
      */
    private final EnumMap<SEBSettingsView.ViewType, Set<Long>> viewAttributeMapping = 
            new EnumMap<>(SEBSettingsView.ViewType.class);

    public SEBSettingsController(
            final ConfigurationDAO configurationDAO,
            final ConfigurationAttributeDAO configurationAttributeDAO,
            final ConfigurationValueDAO configurationValueDAO,
            final ExamConfigurationMapDAO examConfigurationMapDAO,
            final AuthorizationService authorizationService,
            final ExamDAO examDAO) {
        
        this.configurationDAO = configurationDAO;
        this.configurationAttributeDAO = configurationAttributeDAO;
        this.configurationValueDAO = configurationValueDAO;
        this.examConfigurationMapDAO = examConfigurationMapDAO;
        this.authorizationService = authorizationService;
        this.examDAO = examDAO;

        /* Ids of all SEB Setting/Configuration Attributes for the Application view */
        viewAttributeMapping.put(
                SEBSettingsView.ViewType.APPLICATION,
                new HashSet<>(Arrays.asList( 
                        73L, 74L, 75L, 76L, 77L, 78L, 79L, 81L, 82L, 85L,
                        86L, 87L, 88L, 89L, 90L, 91L, 92L, 93L, 94L, 95L, 
                        96L, 97L, 98L, 99L, 100L, 1200L, 1577L, 1630L, 
                        1631L, 1632L, 1633L, 1634L))
        );
        
        /* Ids id all SEB Settings/ConfigurationAttributes for the Network vew */
        viewAttributeMapping.put(
                SEBSettingsView.ViewType.NETWORK,
                new HashSet<>(Arrays.asList(
                        200L, 201L, 202L, 203L, 204L, 205L, 206L, 210L, 220L,
                        221L, 222L, 223L, 231L, 233L, 234L, 235L, 236L, 237L,
                        238L, 239L, 240L, 241L, 242L, 243L, 244L, 245L, 246L,
                        247L, 248L, 249L, 250L, 251L, 252L, 253L, 254L, 255L, 
                        256L, 257L, 258L, 259L, 260L, 261L, 262L, 263L, 264L,
                        265L
                ))
        );
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SEBSettingsView getSEBSettingsForExam(
            @PathVariable(name =API.PARAM_MODEL_ID) final Long examId, 
            @RequestParam(name = SEBSettingsView.ATTR_VIEW_TYPE) final SEBSettingsView.ViewType viewType) {
        
        authorizationService.hasReadGrant(examDAO.byPK(examId).getOrThrow());
        
        // Get mapped configurationNodeId last (open) configurationId
        final Long configurationNodeId = examConfigurationMapDAO
                .getDefaultConfigurationNode(examId)
                .getOrThrow();
        final Configuration followUpConfig = configurationDAO.
                getFollowupConfiguration(configurationNodeId)
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

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT + API.SEB_SETTINGS_TABLE_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SEBSettingsView.TableRowValues> getTableValues(
            @PathVariable(name =API.PARAM_MODEL_ID) final Long examId,
            @RequestParam(name = Domain.CONFIGURATION_ATTRIBUTE.ATTR_NAME) final String attributeName) {

        authorizationService.hasReadGrant(examDAO.byPK(examId).getOrThrow());

        // Get mapped configurationNodeId last (open) configurationId
        final Long configurationNodeId = examConfigurationMapDAO
                .getDefaultConfigurationNode(examId)
                .getOrThrow();
        final Configuration followUpConfig = configurationDAO.
                getFollowupConfiguration(configurationNodeId)
                .getOrThrow();

        final ConfigurationAttribute tableAttribute = configurationAttributeDAO
                .getAttributeIdByName(attributeName)
                .flatMap(configurationAttributeDAO::byPK)
                .getOrThrow();
        
        if (tableAttribute.type != AttributeType.TABLE && tableAttribute.type != AttributeType.COMPOSITE_TABLE) {
            throw new APIMessage.APIMessageException(APIMessage.ErrorMessage.BAD_REQUEST.of());
        }

        return getTableRowMapping(followUpConfig, tableAttribute)
                .getOrThrow();
    }

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SEBSettingsView.Value saveSingleValue(
            @PathVariable(name =API.PARAM_MODEL_ID) final Long examId,
            @RequestParam(name = Domain.CONFIGURATION_VALUE.ATTR_ID) final Long valueId,
            @RequestParam(name = Domain.CONFIGURATION_VALUE.ATTR_VALUE) final String value) {

        authorizationService.hasModifyGrant(examDAO.byPK(examId).getOrThrow());
        
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

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT + API.SEB_SETTINGS_TABLE_PATH_SEGMENT + API.SEB_SETTINGS_TABLE_ROW_PATH_SEGMENT,
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SEBSettingsView.TableRowValues saveTableRowValues(
            @PathVariable(name =API.PARAM_MODEL_ID) final Long examId,
            @RequestBody final SEBSettingsView.TableRowValues values) {

        final Exam exam = examDAO.byPK(examId).getOrThrow();
        authorizationService.hasModifyGrant(exam);

        values.rowValues().forEach((key, value) -> {
            try {
                final Long vid = value.valueId();
                if (vid == null || vid < 0) {
                    final Long configurationNodeId = examConfigurationMapDAO
                            .getDefaultConfigurationNode(examId)
                            .getOrThrow();
                    final Configuration followUpConfig = configurationDAO.
                            getFollowupConfiguration(configurationNodeId)
                            .getOrThrow();
                    final Long attrId = configurationAttributeDAO
                            .getAttributeIdByName(key)
                            .getOrThrow();
                    final ConfigurationValue newCValue = configurationValueDAO.save(new ConfigurationValue(
                                    null,
                                    exam.institutionId,
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

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT + API.SEB_SETTINGS_TABLE_PATH_SEGMENT + API.SEB_SETTINGS_TABLE_ROW_PATH_SEGMENT,
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SEBSettingsView.TableRowValues addNewTableRow(
            @PathVariable(name =API.PARAM_MODEL_ID) final Long examId,
            @RequestParam(name = Domain.CONFIGURATION_ATTRIBUTE.ATTR_NAME) final String attributeName) {

        authorizationService.hasModifyGrant(examDAO.byPK(examId).getOrThrow());

        final ConfigurationAttribute tableAttribute = configurationAttributeDAO
                .getAttributeIdByName(attributeName)
                .flatMap(configurationAttributeDAO::byPK)
                .getOrThrow();

        if (tableAttribute.type != AttributeType.TABLE) {
            throw new APIMessage.APIMessageException(APIMessage.ErrorMessage.BAD_REQUEST.of());
        }

        final Long configurationNodeId = examConfigurationMapDAO
                .getDefaultConfigurationNode(examId)
                .getOrThrow();
        final Configuration followUpConfig = configurationDAO.
                getFollowupConfiguration(configurationNodeId)
                .getOrThrow();

        final Collection<ConfigurationAttribute> columns = configurationAttributeDAO
                .allChildAttributes(tableAttribute.id)
                .getOrThrow();

        final List<List<ConfigurationValue>> tableValues = configurationValueDAO
                .getOrderedTableValues(followUpConfig.institutionId, followUpConfig.id, tableAttribute.id)
                .getOrThrow();
        
        final int index = tableValues.size();
        final Map<String, SEBSettingsView.Value> rowValues = new HashMap<>();

        columns.forEach( column -> {

            // TODO try to batch create
            final ConfigurationValue newValue = configurationValueDAO.createNew(new ConfigurationValue(
                            null,
                            followUpConfig.institutionId,
                            followUpConfig.id,
                            column.id,
                            index,
                            column.defaultValue))
                    .getOrThrow();

            rowValues.put(column.name, new SEBSettingsView.Value(newValue.id, newValue.value));
        });
        
        return new SEBSettingsView.TableRowValues(tableAttribute.name, index, rowValues);
    }

    

    @RequestMapping(
            path = API.MODEL_ID_VAR_PATH_SEGMENT + 
                    API.SEB_SETTINGS_TABLE_PATH_SEGMENT + 
                    API.SEB_SETTINGS_TABLE_ROW_PATH_SEGMENT,
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SEBSettingsView.TableRowValues> deleteTableRow(
            @PathVariable(name =API.PARAM_MODEL_ID) final Long examId,
            @RequestParam(name = Domain.CONFIGURATION_ATTRIBUTE.ATTR_NAME) final String attributeName,
            @RequestParam(name = Domain.CONFIGURATION_VALUE.ATTR_LIST_INDEX) final int index) {
        
        if (index < 0) {
            throw new APIMessage.APIMessageException(APIMessage.ErrorMessage.BAD_REQUEST.of("Negative row index not allowed"));
        }

        authorizationService.hasModifyGrant(examDAO.byPK(examId).getOrThrow());

        final ConfigurationAttribute tableAttribute = configurationAttributeDAO
                .getAttributeIdByName(attributeName)
                .flatMap(configurationAttributeDAO::byPK)
                .getOrThrow();

        if (tableAttribute.type != AttributeType.TABLE) {
            throw new APIMessage.APIMessageException(APIMessage.ErrorMessage.BAD_REQUEST.of("Attribute is not of type TABLE"));
        }

        final Long configurationNodeId = examConfigurationMapDAO
                .getDefaultConfigurationNode(examId)
                .getOrThrow();
        final Configuration followUpConfig = configurationDAO.
                getFollowupConfiguration(configurationNodeId)
                .getOrThrow();
        final List<List<ConfigurationValue>> tableValues = configurationValueDAO
                .getOrderedTableValues(followUpConfig.institutionId, followUpConfig.id, tableAttribute.id)
                .getOrThrow();
        
        if (index >= tableValues.size()) {
            throw new APIMessage.APIMessageException(APIMessage.ErrorMessage.BAD_REQUEST.of("No table row index: " + index));
        }
        
        final List<ConfigurationValue> configurationValues = tableValues.get(index);
        final Set<EntityKey> toDelete = configurationValues
                .stream()
                .filter(Objects::nonNull)
                .map(Entity::getEntityKey)
                .collect(Collectors.toSet());
        final Collection<EntityKey> deleted = configurationValueDAO.delete(toDelete).getOrThrow();
        
        return getTableValues(examId, attributeName);
    }

    private Map<String, ConfigurationAttribute> getAttributesForView(final SEBSettingsView.ViewType viewType) {
        // TODO Note this is hard coded for now but should be dynamic

        final Set<Long> attrIds = viewAttributeMapping.get(viewType);
        if (attrIds == null) {
            return Collections.emptyMap();
        }
        
        return configurationAttributeDAO.allOf(attrIds)
                .map(attrs -> attrs.stream()
                        .collect(Collectors.toMap( 
                                a -> a.name, 
                                Function.identity())))
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

    private Result<List<SEBSettingsView.TableRowValues>> getTableRowMapping(
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
                attribute.id)
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
                        .filter(Objects::nonNull).
                        collect(Collectors.toMap(
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
                final List<List<ConfigurationValue>> tableValues = configurationValueDAO.getOrderedTableValues(
                        institutionId, 
                        configId, 
                        attr.id).getOrThrow();
                listValues.put(attr.name, tableValues);
            }
        });
        return listValues;
    }
}
