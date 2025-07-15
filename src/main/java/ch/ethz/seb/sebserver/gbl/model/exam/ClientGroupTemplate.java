/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain.CLIENT_GROUP;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientGroupTemplate implements ClientGroupData {

    public static final String ATTR_EXAM_TEMPLATE_ID = "examTemplateId";

    @JsonProperty(CLIENT_GROUP.ATTR_ID)
    public final Long id;

    @JsonProperty(ATTR_EXAM_TEMPLATE_ID)
    public final Long examTemplateId;

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

    @JsonProperty(ClientGroup.ATTR_IP_RANGE_START)
    public final String ipRangeStart;

    @JsonProperty(ClientGroup.ATTR_IP_RANGE_END)
    public final String ipRangeEnd;

    @JsonProperty(ClientGroup.ATTR_CLIENT_OS)
    public final ClientOS clientOS;

    @JsonProperty(ATTR_NAME_RANGE_START_LETTER)
    public final String nameRangeStartLetter;

    @JsonProperty(ATTR_NAME_RANGE_END_LETTER)
    public final String nameRangeEndLetter;

    @JsonCreator
    public ClientGroupTemplate(
            @JsonProperty(CLIENT_GROUP.ATTR_ID) final Long id,
            @JsonProperty(CLIENT_GROUP.ATTR_EXAM_ID) final Long examTemplateId,
            @JsonProperty(CLIENT_GROUP.ATTR_NAME) final String name,
            @JsonProperty(CLIENT_GROUP.ATTR_TYPE) final ClientGroupType type,
            @JsonProperty(CLIENT_GROUP.ATTR_COLOR) final String color,
            @JsonProperty(CLIENT_GROUP.ATTR_ICON) final String icon,
            @JsonProperty(ClientGroup.ATTR_IP_RANGE_START) final String ipRangeStart,
            @JsonProperty(ClientGroup.ATTR_IP_RANGE_END) final String ipRangeEnd,
            @JsonProperty(ClientGroup.ATTR_CLIENT_OS) final ClientOS clientOS,
            @JsonProperty(ATTR_NAME_RANGE_START_LETTER) final String nameRangeStartLetter,
            @JsonProperty(ATTR_NAME_RANGE_END_LETTER) final String nameRangeEndLetter) {

        super();
        this.id = id;
        this.examTemplateId = examTemplateId;
        this.name = name;
        this.type = type;
        this.color = color;
        this.icon = icon;
        this.ipRangeStart = ipRangeStart;
        this.ipRangeEnd = ipRangeEnd;
        this.clientOS = clientOS;
        this.nameRangeStartLetter = nameRangeStartLetter;
        this.nameRangeEndLetter = nameRangeEndLetter;
    }

    public ClientGroupTemplate(final Long id, final Long examTemplateId, final POSTMapper postParams) {
        super();
        this.id = id;
        this.examTemplateId = examTemplateId;
        this.name = postParams.getString(CLIENT_GROUP.ATTR_NAME);
        this.type = postParams.getEnum(CLIENT_GROUP.ATTR_TYPE, ClientGroupType.class);
        this.color = postParams.getString(CLIENT_GROUP.ATTR_COLOR);
        this.icon = postParams.getString(CLIENT_GROUP.ATTR_ICON);
        this.ipRangeStart = postParams.getString(ClientGroup.ATTR_IP_RANGE_START);
        this.ipRangeEnd = postParams.getString(ClientGroup.ATTR_IP_RANGE_END);
        this.clientOS = postParams.getEnum(ClientGroup.ATTR_CLIENT_OS, ClientOS.class);
        this.nameRangeStartLetter = postParams.getString(ATTR_NAME_RANGE_START_LETTER);
        this.nameRangeEndLetter = postParams.getString(ATTR_NAME_RANGE_END_LETTER);
    }

    public ClientGroupTemplate(final Long id, final ClientGroupTemplate other) {
        super();
        this.id = id;
        this.examTemplateId = other.examTemplateId;
        this.name = other.name;
        this.type = other.type;
        this.color = other.color;
        this.icon = other.icon;
        this.ipRangeStart = other.ipRangeStart;
        this.ipRangeEnd = other.ipRangeEnd;
        this.clientOS = other.clientOS;
        this.nameRangeStartLetter = other.nameRangeStartLetter;
        this.nameRangeEndLetter = other.nameRangeEndLetter;
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

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public ClientGroupType getType() {
        return this.type;
    }

    @Override
    public String getColor() {
        return this.color;
    }

    @Override
    public String getIcon() {
        return this.icon;
    }

    public Long getExamTemplateId() {
        return this.examTemplateId;
    }

    @Override
    public String getIpRangeStart() {
        if (StringUtils.isBlank(this.ipRangeStart)) {
            return null;
        }

        try {
            return InetAddress.getByName(this.ipRangeStart).getHostAddress();
        } catch (final UnknownHostException e) {
            return null;
        }
    }

    @Override
    public String getIpRangeEnd() {
        if (StringUtils.isBlank(this.ipRangeEnd)) {
            return null;
        }

        try {
            return InetAddress.getByName(this.ipRangeEnd).getHostAddress();
        } catch (final UnknownHostException e) {
            return null;
        }
    }

    @Override
    public ClientOS getClientOS() {
        return this.clientOS;
    }

    @Override
    public String getNameRangeStartLetter() {
        return this.nameRangeStartLetter;
    }

    @Override
    public String getNameRangeEndLetter() {
        return this.nameRangeEndLetter;
    }

    @JsonIgnore
    public String getData() {
        return switch (this.type) {
            case IP_V4_RANGE -> this.ipRangeStart + Constants.EMBEDDED_LIST_SEPARATOR + this.ipRangeEnd;
            case CLIENT_OS -> this.clientOS.name();
            default -> StringUtils.EMPTY;
        };
    }

    @Override
    public String toString() {
        return "ClientGroupTemplate{" +
                "id=" + id +
                ", examTemplateId=" + examTemplateId +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", color='" + color + '\'' +
                ", icon='" + icon + '\'' +
                ", ipRangeStart='" + ipRangeStart + '\'' +
                ", ipRangeEnd='" + ipRangeEnd + '\'' +
                ", clientOS=" + clientOS +
                ", nameRangeStartLetter=" + nameRangeStartLetter +
                ", nameRangeEndLetter=" + nameRangeEndLetter +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ClientGroupTemplate that = (ClientGroupTemplate) o;
        return Objects.equals(id, that.id) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }
}
