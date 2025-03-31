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
import ch.ethz.seb.sebserver.gbl.util.Utils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientGroup implements ClientGroupData, Comparable<ClientGroup> {

    public static final String ATTR_SCREEN_PROCTORING_GROUP = "isSPSGroup";
    public static final String FILTER_ATTR_EXAM_ID = "examId";

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

    @JsonProperty(ATTR_IP_RANGE_START)
    public final String ipRangeStart;

    @JsonProperty(ATTR_IP_RANGE_END)
    public final String ipRangeEnd;

    @JsonProperty(ATTR_CLIENT_OS)
    public final ClientOS clientOS;

    @JsonProperty(ATTR_NAME_RANGE_START_LETTER)
    public final String nameRangeStartLetter;

    @JsonProperty(ATTR_NAME_RANGE_END_LETTER)
    public final String nameRangeEndLetter;
    
    @JsonProperty(ATTR_SCREEN_PROCTORING_GROUP)
    public final Boolean isSPSGroup;

    @JsonCreator
    public ClientGroup(
            @JsonProperty(CLIENT_GROUP.ATTR_ID) final Long id,
            @JsonProperty(CLIENT_GROUP.ATTR_EXAM_ID) final Long examId,
            @JsonProperty(CLIENT_GROUP.ATTR_NAME) final String name,
            @JsonProperty(CLIENT_GROUP.ATTR_TYPE) final ClientGroupType type,
            @JsonProperty(CLIENT_GROUP.ATTR_COLOR) final String color,
            @JsonProperty(CLIENT_GROUP.ATTR_ICON) final String icon,
            @JsonProperty(ATTR_IP_RANGE_START) final String ipRangeStart,
            @JsonProperty(ATTR_IP_RANGE_END) final String ipRangeEnd,
            @JsonProperty(ATTR_CLIENT_OS) final ClientOS clientOS,
            @JsonProperty(ATTR_NAME_RANGE_START_LETTER) final String nameRangeStartLetter,
            @JsonProperty(ATTR_NAME_RANGE_END_LETTER) final String nameRangeEndLetter,
            @JsonProperty(ATTR_SCREEN_PROCTORING_GROUP) final Boolean isSPSGroup) {

        super();
        this.id = id;
        this.examId = examId;
        this.name = name;
        this.type = type == null ? ClientGroupType.NONE : type;
        this.color = color;
        this.icon = icon;
        this.ipRangeStart = ipRangeStart;
        this.ipRangeEnd = ipRangeEnd;
        this.clientOS = clientOS == null ? ClientOS.NONE : clientOS;
        this.nameRangeStartLetter = nameRangeStartLetter;
        this.nameRangeEndLetter = nameRangeEndLetter;
        this.isSPSGroup = isSPSGroup;
    }

    public ClientGroup(
            final Long id,
            final Long examId,
            final String name,
            final ClientGroupType type,
            final String color,
            final String icon,
            final String data,
            final Boolean isSPSGroup) {

        super();
        this.id = id;
        this.examId = examId;
        this.name = name;
        this.type = type == null ? ClientGroupType.NONE : type;
        this.color = color;
        this.icon = icon;
        this.isSPSGroup = isSPSGroup;

        switch (this.type) {
            case IP_V4_RANGE: {
                if (StringUtils.isNotBlank(data)) {
                    final String[] split = StringUtils.split(data, Constants.EMBEDDED_LIST_SEPARATOR);
                    this.ipRangeStart = (split.length > 0 && StringUtils.isNotBlank(split[0])) ? split[0] : null;
                    this.ipRangeEnd = (split.length > 1 && StringUtils.isNotBlank(split[1])) ? split[1] : null;
                } else {
                    this.ipRangeStart = null;
                    this.ipRangeEnd = null;
                }
                this.clientOS = ClientOS.NONE;
                this.nameRangeStartLetter = null;
                this.nameRangeEndLetter = null;
                break;
            }
            case CLIENT_OS: {
                this.ipRangeStart = null;
                this.ipRangeEnd = null;
                this.clientOS = Utils.enumFromString(data, ClientOS.class, ClientOS.NONE);
                this.nameRangeStartLetter = null;
                this.nameRangeEndLetter = null;
                break;
            }
            case NAME_ALPHABETICAL_RANGE: {
                this.ipRangeStart = null;
                this.ipRangeEnd = null;
                this.clientOS = ClientOS.NONE;
                if (StringUtils.isNotBlank(data)) {
                    final String[] split = StringUtils.split(data, Constants.EMBEDDED_LIST_SEPARATOR);
                    this.nameRangeStartLetter = (split.length > 0 && StringUtils.isNotBlank(split[0])) ? split[0] : null;
                    this.nameRangeEndLetter = (split.length > 1 && StringUtils.isNotBlank(split[1])) ? split[1] : null;
                } else {
                    this.nameRangeStartLetter = null;
                    this.nameRangeEndLetter = null;
                }
                break;
            }
            default: {
                this.ipRangeStart = null;
                this.ipRangeEnd = null;
                this.clientOS = ClientOS.NONE;
                this.nameRangeStartLetter = null;
                this.nameRangeEndLetter = null;
                break;
            }
        }
    }

    public ClientGroup(final Long examId, final POSTMapper postParams) {
        this.id = null;
        this.examId = examId;
        this.name = postParams.getString(CLIENT_GROUP.ATTR_NAME);
        this.type = postParams.getEnum(CLIENT_GROUP.ATTR_TYPE, ClientGroupType.class);
        this.color = postParams.getString(CLIENT_GROUP.ATTR_COLOR);
        this.icon = postParams.getString(CLIENT_GROUP.ATTR_ICON);
        this.ipRangeStart = postParams.getString(ATTR_IP_RANGE_START);
        this.ipRangeEnd = postParams.getString(ATTR_IP_RANGE_END);
        this.clientOS = postParams.getEnum(ATTR_CLIENT_OS, ClientOS.class);
        this.nameRangeStartLetter = postParams.getString(ATTR_NAME_RANGE_START_LETTER);
        this.nameRangeEndLetter = postParams.getString(ATTR_NAME_RANGE_END_LETTER);
        this.isSPSGroup = postParams.getBooleanObject(ATTR_SCREEN_PROCTORING_GROUP);
    }

    public static ClientGroup createNew(final String examId) {
        return new ClientGroup(null, Long.parseLong(examId), null, null, null, null, null, null, null, null, null, false);
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

    public Long getExamId() {
        return this.examId;
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

    public Boolean getSPSGroup() {
        return isSPSGroup;
    }

    @JsonIgnore
    public String getData() {
        switch (this.type) {
            case IP_V4_RANGE: {
                return this.ipRangeStart + Constants.EMBEDDED_LIST_SEPARATOR + this.ipRangeEnd;
            }
            case CLIENT_OS: {
                return this.clientOS.name();
            }
            case NAME_ALPHABETICAL_RANGE: {
                return this.nameRangeStartLetter + Constants.EMBEDDED_LIST_SEPARATOR + this.nameRangeEndLetter;
            }
            default: {
                return StringUtils.EMPTY;
            }
        }
    }
    
    @Override
    public String toString() {
        return "ClientGroup{" +
                "id=" + id +
                ", examId=" + examId +
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
    public int compareTo(final ClientGroup o) {
        return o == null ? -1 : this.id.compareTo(o.id);
    }

}
