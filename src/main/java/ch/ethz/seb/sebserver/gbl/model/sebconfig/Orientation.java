/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.sebconfig;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Domain.ORIENTATION;
import ch.ethz.seb.sebserver.gbl.model.Entity;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class Orientation implements Entity {

    public static final String FILTER_ATTR_TEMPLATE_ID = "templateId";
    public static final String FILTER_ATTR_VIEW = "view";
    public static final String FILTER_ATTR_GROUP = "group";

    @JsonProperty(ORIENTATION.ATTR_ID)
    public final Long id;

    @NotNull
    @JsonProperty(ORIENTATION.ATTR_CONFIG_ATTRIBUTE_ID)
    public final Long attributeId;

    @JsonProperty(ORIENTATION.ATTR_TEMPLATE_ID)
    public final Long templateId;

    @JsonProperty(ORIENTATION.ATTR_VIEW)
    public final String view;

    @JsonProperty(ORIENTATION.ATTR_GROUP)
    public final String group;

    @JsonProperty(ORIENTATION.ATTR_X_POSITION)
    public final Integer xPosition;

    @JsonProperty(ORIENTATION.ATTR_Y_POSITION)
    public final Integer yPosition;

    @JsonProperty(ORIENTATION.ATTR_WIDTH)
    public final Integer width;

    @JsonProperty(ORIENTATION.ATTR_HEIGHT)
    public final Integer height;

    @JsonCreator
    public Orientation(
            @JsonProperty(ORIENTATION.ATTR_ID) final Long id,
            @JsonProperty(ORIENTATION.ATTR_CONFIG_ATTRIBUTE_ID) final Long attributeId,
            @JsonProperty(ORIENTATION.ATTR_TEMPLATE_ID) final Long templateId,
            @JsonProperty(ORIENTATION.ATTR_VIEW) final String view,
            @JsonProperty(ORIENTATION.ATTR_GROUP) final String group,
            @JsonProperty(ORIENTATION.ATTR_X_POSITION) final Integer xPosition,
            @JsonProperty(ORIENTATION.ATTR_Y_POSITION) final Integer yPosition,
            @JsonProperty(ORIENTATION.ATTR_WIDTH) final Integer width,
            @JsonProperty(ORIENTATION.ATTR_HEIGHT) final Integer height) {

        this.id = id;
        this.attributeId = attributeId;
        this.templateId = templateId;
        this.view = view;
        this.group = group;
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.width = width;
        this.height = height;
    }

    public Orientation(final ConfigurationAttribute attr, final POSTMapper postParams) {
        this.id = null;
        this.attributeId = attr.id;
        this.templateId = postParams.getLong(Domain.ORIENTATION.ATTR_TEMPLATE_ID);
        this.view = postParams.getString(Domain.ORIENTATION.ATTR_VIEW);
        this.group = postParams.getString(Domain.ORIENTATION.ATTR_GROUP);
        this.xPosition = postParams.getInteger(Domain.ORIENTATION.ATTR_X_POSITION);
        this.yPosition = postParams.getInteger(Domain.ORIENTATION.ATTR_Y_POSITION);
        this.width = postParams.getInteger(Domain.ORIENTATION.ATTR_WIDTH);
        this.height = postParams.getInteger(Domain.ORIENTATION.ATTR_HEIGHT);
    }

    @Override
    public String getModelId() {
        return (this.id != null)
                ? String.valueOf(this.id)
                : null;
    }

    @Override
    public EntityType entityType() {
        return EntityType.ORIENTATION;
    }

    @Override
    public String getName() {
        return getModelId();
    }

    public Long getId() {
        return this.id;
    }

    public Long getAttributeId() {
        return this.attributeId;
    }

    public Long getTemplateId() {
        return this.templateId;
    }

    public String getView() {
        return this.view;
    }

    public String getGroup() {
        return this.group;
    }

    public Integer getXPosition() {
        return this.xPosition;
    }

    public Integer getYPosition() {
        return this.yPosition;
    }

    public Integer getWidth() {
        return this.width;
    }

    public Integer getHeight() {
        return this.height;
    }

    @Override
    public String toString() {
        return "Orientation [id=" + this.id + ", attributeId=" + this.attributeId + ", templateId=" + this.templateId
                + ", view="
                + this.view + ", group=" + this.group + ", xPosition=" + this.xPosition + ", yPosition="
                + this.yPosition + ", width="
                + this.width + ", height=" + this.height + "]";
    }

}
