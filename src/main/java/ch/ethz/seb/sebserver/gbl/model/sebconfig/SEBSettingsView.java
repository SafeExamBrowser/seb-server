/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.sebconfig;

import java.util.List;
import java.util.Map;

import ch.ethz.seb.sebserver.gbl.model.Domain;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SEBSettingsView(
        @JsonProperty(ATTR_VIEW_TYPE) ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBSettingsView.ViewType viewType,
        @JsonProperty(Domain.CONFIGURATION.ATTR_CONFIGURATION_NODE_ID) Long configurationNodeId,
        @JsonProperty(Domain.CONFIGURATION_VALUE.ATTR_CONFIGURATION_ID) Long configurationId,
        @JsonProperty(ATTR_ATTRIBUTES) Map<String, ConfigurationAttribute> attributes,
        @JsonProperty(ATTR_SINGLE_VALUES) Map<String, Value> singleValues,
        @JsonProperty(ATTR_TABLE_VALUES) Map<String, List<TableRowValues>> tableValues) {

    public final static String ATTR_ATTRIBUTES = "attributes";
    public final static String ATTR_VIEW_TYPE = "viewType";
    public final static String ATTR_SINGLE_VALUES = "singleValues";
    public final static String ATTR_TABLE_VALUES = "tableValues";
    public final static String ATTR_TABLE_ROW_VALUES = "rowValues";
    
    public enum ViewType {
        GENERAL(1L),
        USER_INTERFACE(2L),
        BROWSER(3L),
        DOWN_UPLOAD(4L),
        EXAM(5L),
        APPLICATION(6L),
        RESOURCES(5L),
        NETWORK(8L),
        SECURITY(9L),
        REGISTRY(10L),
        HOOKED_KEYS(11L),
        PROCTORING(12L)
        ;
        
        public final Long viewId;

        ViewType(final Long viewId) {
            this.viewId = viewId;
        }
    }
    
    public record TableRowValues(
            @JsonProperty(Domain.CONFIGURATION_ATTRIBUTE.ATTR_NAME) String attributeName,
            @JsonProperty(Domain.CONFIGURATION_VALUE.ATTR_LIST_INDEX) int index,
            @JsonProperty(ATTR_TABLE_ROW_VALUES) Map<String, Value> rowValues) {
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Value(
            @JsonProperty(Domain.CONFIGURATION_VALUE.ATTR_ID) Long valueId,
            @JsonProperty(Domain.CONFIGURATION_VALUE.ATTR_VALUE) String value) {
    }
}
