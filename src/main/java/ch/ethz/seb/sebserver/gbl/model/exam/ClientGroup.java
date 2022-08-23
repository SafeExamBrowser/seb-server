/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain.CLIENT_GROUP;
import ch.ethz.seb.sebserver.gbl.model.Entity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientGroup implements Entity {

    public static final String FILTER_ATTR_EXAM_ID = "examId";

    public enum ClientGroupType {
        IP_V4_RANGE,
        CLIENT_OS
    }

    @JsonProperty(CLIENT_GROUP.ATTR_ID)
    public final Long id;

    @JsonProperty(CLIENT_GROUP.ATTR_EXAM_ID)
    @NotNull
    public final Long examId;

    @JsonProperty(CLIENT_GROUP.ATTR_NAME)
    @NotNull(message = "clientGroup:name:notNull")
    @Size(min = 3, max = 255, message = "clientGroup:name:size:{min}:{max}:${validatedValue}")
    public final String name;

    @JsonProperty(CLIENT_GROUP.ATTR_TYPE)
    @NotNull(message = "clientGroup:type:notNull")
    public final ClientGroupType type;

    @JsonProperty(CLIENT_GROUP.ATTR_COLOR)
    public final String color;

    @JsonProperty(CLIENT_GROUP.ATTR_ICON)
    public final String icon;

    @JsonProperty(CLIENT_GROUP.ATTR_DATA)
    public final String data;

    @JsonCreator
    public ClientGroup(
            @JsonProperty(CLIENT_GROUP.ATTR_ID) final Long id,
            @JsonProperty(CLIENT_GROUP.ATTR_EXAM_ID) final Long examId,
            @JsonProperty(CLIENT_GROUP.ATTR_NAME) final String name,
            @JsonProperty(CLIENT_GROUP.ATTR_TYPE) final ClientGroupType type,
            @JsonProperty(CLIENT_GROUP.ATTR_COLOR) final String color,
            @JsonProperty(CLIENT_GROUP.ATTR_ICON) final String icon,
            @JsonProperty(CLIENT_GROUP.ATTR_DATA) final String data) {

        super();
        this.id = id;
        this.examId = examId;
        this.name = name;
        this.type = type;
        this.color = color;
        this.icon = icon;
        this.data = data;
    }

    public ClientGroup(final Long examId, final POSTMapper postParams) {
        this.id = null;
        this.examId = examId;
        this.name = postParams.getString(CLIENT_GROUP.ATTR_NAME);
        this.type = postParams.getEnum(CLIENT_GROUP.ATTR_TYPE, ClientGroupType.class);
        this.color = postParams.getString(CLIENT_GROUP.ATTR_COLOR);
        this.icon = postParams.getString(CLIENT_GROUP.ATTR_ICON);
        this.data = postParams.getString(CLIENT_GROUP.ATTR_DATA);
    }

    @Override
    public String getModelId() {
        return (this.id == null) ? null : String.valueOf(this.id);
    }

    @Override
    public EntityType entityType() {
        return EntityType.CLIENT_GROUP;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public Long getId() {
        return this.id;
    }

    public Long getExamId() {
        return this.examId;
    }

    public ClientGroupType getType() {
        return this.type;
    }

    public String getColor() {
        return this.color;
    }

    public String getIcon() {
        return this.icon;
    }

    public String getData() {
        return this.data;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ClientGroup [id=");
        builder.append(this.id);
        builder.append(", examId=");
        builder.append(this.examId);
        builder.append(", name=");
        builder.append(this.name);
        builder.append(", type=");
        builder.append(this.type);
        builder.append(", color=");
        builder.append(this.color);
        builder.append(", icon=");
        builder.append(this.icon);
        builder.append(", data=");
        builder.append(this.data);
        builder.append("]");
        return builder.toString();
    }

}
