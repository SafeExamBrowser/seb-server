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
import ch.ethz.seb.sebserver.gbl.model.sebconfig.*;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.WebserviceConfig;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@WebServiceProfile
@RestController
@RequestMapping("${sebserver.webservice.api.admin.endpoint}" + API.SEB_SETTINGS_ENDPOINT)
@SecurityRequirement(name = WebserviceConfig.SWAGGER_AUTH_ADMIN_API)
public class SEBSettingsController {
    
    private final ConfigurationDAO configurationDAO;
    private final ConfigurationAttributeDAO configurationAttributeDAO;
    private final ConfigurationValueDAO configurationValueDAO;
    private final ExamConfigurationMapDAO examConfigurationMapDAO;
    
    private final EnumMap<SEBSettingsView.ViewType, Set<Long>> viewAttributeMapping = 
            new EnumMap<>(SEBSettingsView.ViewType.class);

    public SEBSettingsController(
            final ConfigurationDAO configurationDAO,
            final ConfigurationAttributeDAO configurationAttributeDAO,
            final ConfigurationValueDAO configurationValueDAO,
            final ExamConfigurationMapDAO examConfigurationMapDAO) {
        
        this.configurationDAO = configurationDAO;
        this.configurationAttributeDAO = configurationAttributeDAO;
        this.configurationValueDAO = configurationValueDAO;
        this.examConfigurationMapDAO = examConfigurationMapDAO;

        viewAttributeMapping.put(
                SEBSettingsView.ViewType.APPLICATION,
                new HashSet<>(Arrays.asList( 
                        73L, 74L, 75L, 76L, 77L, 78L, 79L, 81L, 82L, 85L,
                        86L, 87L, 88L, 89L, 90L, 91L, 92L, 93L, 94L, 95L, 
                        96L, 97L, 98L, 99L, 100L, 1200L, 1577L, 1630L, 
                        1631L, 1632L, 1633L, 1634L))
        );
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
            path = API.SEB_SETTINGS_EXAM_PATH_SEGMENT + 
                    API.MODEL_ID_VAR_PATH_SEGMENT + 
                    API.SEB_SETTINGS_VIEW_TYPE_VAR_PATH_SEGMENT,
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SEBSettingsView getSEBSettingsForExam(
            @PathVariable(name =API.PARAM_MODEL_ID) final Long examId, 
            @PathVariable(name = API.SEB_SETTINGS_VIEW_TYPE) final SEBSettingsView.ViewType viewType) {

        // TODO check user read access on exam
        
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
        final Map<String, List<List<ConfigurationValue>>> tableVals = getTableValues(followUpConfig.institutionId, followUpConfig.id, attributes);
        final Map<String, List<Map<String, SEBSettingsView.Value>>> tableValues = new HashMap<>();

        tableVals.forEach((key, value) -> {
            final List<Map<String, SEBSettingsView.Value>> rows = value.stream()
                    .map(val -> val.stream()
                            .filter(Objects::nonNull).
                            collect(Collectors.toMap(
                    v -> attrIdMapping.get(v.attributeId).name,
                    v -> new SEBSettingsView.Value(v.id, v.value)))
            ).toList();

            tableValues.put(key, rows);
        });

        return new SEBSettingsView(
                viewType, 
                configurationNodeId, 
                followUpConfig.id,
                attributes,
                singleValues,
                tableValues);
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
