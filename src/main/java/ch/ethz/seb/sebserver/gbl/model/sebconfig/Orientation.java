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

    public static final String FILTER_ATTR_ATTRIBUTE_ID = "attributeId";
    public static final String FILTER_ATTR_TEMPLATE_ID = "templateId";
    public static final String FILTER_ATTR_VIEW_ID = "viewId";
    public static final String FILTER_ATTR_GROUP_ID = "groupId";

    @JsonProperty(ORIENTATION.ATTR_ID)
    public final Long id;

    @NotNull
    @JsonProperty(ORIENTATION.ATTR_CONFIG_ATTRIBUTE_ID)
    public final Long attributeId;

    @JsonProperty(ORIENTATION.ATTR_TEMPLATE_ID)
    public final Long templateId;

    @JsonProperty(ORIENTATION.ATTR_VIEW_ID)
    public final Long viewId;

    @JsonProperty(ORIENTATION.ATTR_GROUP_ID)
    public final String groupId;

    @JsonProperty(ORIENTATION.ATTR_X_POSITION)
    public final Integer xPosition;

    @JsonProperty(ORIENTATION.ATTR_Y_POSITION)
    public final Integer yPosition;

    @JsonProperty(ORIENTATION.ATTR_WIDTH)
    public final Integer width;

    @JsonProperty(ORIENTATION.ATTR_HEIGHT)
    public final Integer height;

    @JsonProperty(ORIENTATION.ATTR_TITLE)
    public final TitleOrientation title;

    @JsonCreator
    public Orientation(
            @JsonProperty(ORIENTATION.ATTR_ID) final Long id,
            @JsonProperty(ORIENTATION.ATTR_CONFIG_ATTRIBUTE_ID) final Long attributeId,
            @JsonProperty(ORIENTATION.ATTR_TEMPLATE_ID) final Long templateId,
            @JsonProperty(ORIENTATION.ATTR_VIEW_ID) final Long viewId,
            @JsonProperty(ORIENTATION.ATTR_GROUP_ID) final String groupId,
            @JsonProperty(ORIENTATION.ATTR_X_POSITION) final Integer xPosition,
            @JsonProperty(ORIENTATION.ATTR_Y_POSITION) final Integer yPosition,
            @JsonProperty(ORIENTATION.ATTR_WIDTH) final Integer width,
            @JsonProperty(ORIENTATION.ATTR_HEIGHT) final Integer height,
            @JsonProperty(ORIENTATION.ATTR_TITLE) final TitleOrientation title) {

        this.id = id;
        this.attributeId = attributeId;
        this.templateId = templateId;
        this.viewId = viewId;
        this.groupId = groupId;
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.width = width;
        this.height = height;
        this.title = (title != null) ? title : TitleOrientation.NONE;
    }

    public Orientation(final ConfigurationAttribute attr, final POSTMapper postParams) {
        this.id = null;
        this.attributeId = attr.id;
        this.templateId = postParams.getLong(Domain.ORIENTATION.ATTR_TEMPLATE_ID);
        this.viewId = postParams.getLong(Domain.ORIENTATION.ATTR_VIEW_ID);
        this.groupId = postParams.getString(Domain.ORIENTATION.ATTR_GROUP_ID);
        this.xPosition = postParams.getInteger(Domain.ORIENTATION.ATTR_X_POSITION);
        this.yPosition = postParams.getInteger(Domain.ORIENTATION.ATTR_Y_POSITION);
        this.width = postParams.getInteger(Domain.ORIENTATION.ATTR_WIDTH);
        this.height = postParams.getInteger(Domain.ORIENTATION.ATTR_HEIGHT);
        this.title = postParams.getEnum(
                Domain.ORIENTATION.ATTR_TITLE,
                TitleOrientation.class,
                TitleOrientation.NONE);
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

    public Long getViewId() {
        return this.viewId;
    }

    public String getGroupId() {
        return this.groupId;
    }

    public Integer getxPosition() {
        return this.xPosition;
    }

    public Integer getyPosition() {
        return this.yPosition;
    }

    public Integer getWidth() {
        return this.width;
    }

    public Integer getHeight() {
        return this.height;
    }

    public TitleOrientation getTitle() {
        return this.title;
    }

    public int xpos() {
        return this.xPosition != null ? this.xPosition : 0;
    }

    public int ypos() {
        return this.yPosition != null ? this.yPosition : 0;
    }

    public int width() {
        return this.width != null ? this.width : 1;
    }

    public int height() {
        return this.height != null ? this.height : 1;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Orientation [id=");
        builder.append(this.id);
        builder.append(", attributeId=");
        builder.append(this.attributeId);
        builder.append(", templateId=");
        builder.append(this.templateId);
        builder.append(", viewId=");
        builder.append(this.viewId);
        builder.append(", groupId=");
        builder.append(this.groupId);
        builder.append(", xPosition=");
        builder.append(this.xPosition);
        builder.append(", yPosition=");
        builder.append(this.yPosition);
        builder.append(", width=");
        builder.append(this.width);
        builder.append(", height=");
        builder.append(this.height);
        builder.append(", title=");
        builder.append(this.title);
        builder.append("]");
        return builder.toString();
    }

}
