/*
 * Copyright (c) 2018 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.session;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;
import ch.ethz.seb.sebserver.gbl.util.Utils;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class ClientConnection implements GrantEntity {

    public enum ConnectionStatus {
        UNDEFINED(0, false, false, false),
        CONNECTION_REQUESTED(1, true, false, false),
        READY(2, true, true, true),
        ACTIVE(3, false, true, true),
        CLOSED(4, false, false, true),
        DISABLED(5, false, false, false);

        public final int code;
        public final boolean connectingStatus;
        public final boolean establishedStatus;
        public final boolean clientActiveStatus;
        public final boolean duplicateCheckStatus;

        ConnectionStatus(
                final int code,
                final boolean connectingStatus,
                final boolean establishedStatus,
                final boolean duplicateCheckStatus) {

            this.code = code;
            this.connectingStatus = connectingStatus;
            this.establishedStatus = establishedStatus;
            this.clientActiveStatus = connectingStatus || establishedStatus;
            this.duplicateCheckStatus = duplicateCheckStatus;
        }
    }

    public enum ConnectionIssueStatus {
        ASK_GRANTED(0),
        SEB_VERSION_GRANTED(1);

        public final int code;

        ConnectionIssueStatus(final int code){
            this.code = code;
        }
    }

    public final static List<String> ACTIVE_STATES = Utils.immutableListOf(
            ConnectionStatus.ACTIVE.name(),
            ConnectionStatus.READY.name(),
            ConnectionStatus.CONNECTION_REQUESTED.name());

    public final static List<String> SECURE_STATES = Utils.immutableListOf(
            ConnectionStatus.ACTIVE.name(),
            ConnectionStatus.READY.name(),
            ConnectionStatus.CLOSED.name());

    public final static List<String> SECURE_CHECK_STATES = Utils.immutableListOf(
            ConnectionStatus.CONNECTION_REQUESTED.name(),
            ConnectionStatus.ACTIVE.name(),
            ConnectionStatus.READY.name());

    public static final ClientConnection EMPTY_CLIENT_CONNECTION = new ClientConnection(
            -1L, -1L, -1L,
            ConnectionStatus.UNDEFINED,
            null, null, null, null, null, null, null,
            false,
            null, null, null, null, false, null,
            false, false, null, false);

    public static final String ATTR_INFO = "seb_info";
    public static final String FILTER_ATTR_EXAM_ID = Domain.CLIENT_CONNECTION.ATTR_EXAM_ID;
    public static final String FILTER_ATTR_STATUS = Domain.CLIENT_CONNECTION.ATTR_STATUS;
    public static final String FILTER_ATTR_SESSION_ID = Domain.CLIENT_CONNECTION.ATTR_EXAM_USER_SESSION_ID;
    public static final String FILTER_ATTR_IP_STRING = Domain.CLIENT_CONNECTION.ATTR_CLIENT_ADDRESS;
    public static final String FILTER_ATTR_INFO = ATTR_INFO;
    public static final String FILTER_ATTR_TOKEN_LIST = "CONNECTION_TOKENS";

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_ID)
    public final Long id;

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_INSTITUTION_ID)
    public final Long institutionId;

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_EXAM_ID)
    public final Long examId;

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_STATUS)
    public final ConnectionStatus status;

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_CONNECTION_TOKEN)
    public final String connectionToken;

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_EXAM_USER_SESSION_ID)
    public final String userSessionId;

    @JsonProperty(ATTR_INFO)
    public final String info;

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_SECURITY_CHECK_GRANTED)
    public final Boolean securityCheckGranted;

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_CLIENT_VERSION_GRANTED)
    public final Boolean clientVersionGranted;

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_CLIENT_ADDRESS)
    public final String clientAddress;

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_CLIENT_OS_NAME)
    public final String sebOSName;

    @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_CLIENT_VERSION)
    public final String sebVersion;

    @JsonIgnore
    public final Long screenProctoringGroupId;
    @JsonIgnore
    public final Boolean screenProctoringGroupUpdate;
    @JsonIgnore
    public final Long remoteProctoringRoomId;
    @JsonIgnore
    public final String sebClientUserId;
    @JsonIgnore
    public final Long creationTime;
    @JsonIgnore
    public final Long updateTime;
    @JsonIgnore
    public final Boolean remoteProctoringRoomUpdate;
    @JsonIgnore // not used yet on GUI side
    public final Boolean vdi;
    @JsonIgnore // not used yet on GUI side
    public final String vdiPairToken;
    @JsonIgnore
    public final String sebMachineName;
    @JsonIgnore
    public final String ask;

    @JsonCreator
    public ClientConnection(
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_ID) final Long id,
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_EXAM_ID) final Long examId,
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_STATUS) final ConnectionStatus status,
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_CONNECTION_TOKEN) final String connectionToken,
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_EXAM_USER_SESSION_ID) final String userSessionId,
            @JsonProperty(ATTR_INFO) final String info,
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_CLIENT_ADDRESS) final String clientAddress,
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_CLIENT_OS_NAME) final String sebOSName,
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_CLIENT_VERSION) final String sebVersion,
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_SECURITY_CHECK_GRANTED) final Boolean securityCheckGranted,
            @JsonProperty(Domain.CLIENT_CONNECTION.ATTR_CLIENT_VERSION_GRANTED) final Boolean clientVersionGranted) {

        this.id = id;
        this.institutionId = institutionId;
        this.examId = examId;
        this.status = status;
        this.connectionToken = connectionToken;
        this.userSessionId = userSessionId;
        this.info = info;
        this.vdi = false;
        this.sebClientUserId = null;
        this.vdiPairToken = null;
        this.creationTime = 0L;
        this.updateTime = 0L;
        this.screenProctoringGroupId = null;
        this.remoteProctoringRoomId = null;
        this.remoteProctoringRoomUpdate = false;
        this.screenProctoringGroupUpdate = false;

        this.clientAddress = clientAddress;
        this.sebOSName = sebOSName;
        this.sebMachineName = Constants.EMPTY_NOTE;
        this.sebVersion = sebVersion;
        this.securityCheckGranted = securityCheckGranted;
        this.ask = null;
        this.clientVersionGranted = clientVersionGranted;
    }

    public ClientConnection(
            final Long id,
            final Long institutionId,
            final Long examId,
            final ConnectionStatus status,
            final String connectionToken,
            final String userSessionId,
            final String clientAddress,
            final String seb_os_name,
            final String seb_machine_name,
            final String seb_version,
            final String sebClientUserId,
            final Boolean vdi,
            final String vdiPairToken,
            final Long creationTime,
            final Long updateTime,
            final Long screenProctoringGroupId,
            final Boolean screenProctoringGroupUpdate,
            final Long remoteProctoringRoomId,
            final Boolean remoteProctoringRoomUpdate,
            final Boolean securityCheckGranted,
            final String ask,
            final Boolean clientVersionGranted) {

        this.id = id;
        this.institutionId = institutionId;
        this.examId = examId;
        this.status = status;
        this.connectionToken = connectionToken;
        this.userSessionId = userSessionId;
        this.clientAddress = clientAddress;
        this.sebOSName = seb_os_name;
        this.sebMachineName = seb_machine_name;
        this.sebVersion = seb_version;
        this.sebClientUserId = sebClientUserId;
        this.vdi = vdi;
        this.vdiPairToken = vdiPairToken;
        this.creationTime = creationTime;
        this.updateTime = updateTime;
        this.screenProctoringGroupId = screenProctoringGroupId;
        this.screenProctoringGroupUpdate = (screenProctoringGroupUpdate != null)
                ? screenProctoringGroupUpdate
                : false;
        this.remoteProctoringRoomId = remoteProctoringRoomId;
        this.remoteProctoringRoomUpdate = (remoteProctoringRoomUpdate != null)
                ? remoteProctoringRoomUpdate
                : false;

        this.info = new StringBuilder()
                .append(getSEBInfo(seb_version))
                .append(Constants.LIST_SEPARATOR)
                .append(getOSInfo(seb_os_name))
                .append(Constants.LIST_SEPARATOR)
                .append((clientAddress != null) ? " IP:" + clientAddress : Constants.EMPTY_NOTE)
                .toString();

        this.securityCheckGranted = securityCheckGranted;
        this.ask = ask;
        this.clientVersionGranted = clientVersionGranted;
    }

    @Override
    public EntityType entityType() {
        return EntityType.CLIENT_CONNECTION;
    }

    @Override
    public String getName() {
        return this.userSessionId;
    }

    @Override
    public String getModelId() {
        return (this.id != null)
                ? String.valueOf(this.id)
                : null;
    }

    public Long getId() {
        return this.id;
    }

    @Override
    public Long getInstitutionId() {
        return this.institutionId;
    }

    public Long getExamId() {
        return this.examId;
    }

    public ConnectionStatus getStatus() {
        return this.status;
    }

    public String getConnectionToken() {
        return this.connectionToken;
    }

    public String getClientAddress() {
        return this.clientAddress;
    }

    public String getUserSessionId() {
        return this.userSessionId;
    }

    @JsonIgnore
    public String getSebClientUserId() {
        return this.sebClientUserId;
    }

    @JsonIgnore // not used yet on GUI side
    public Boolean getVdi() {
        return this.vdi;
    }

    @JsonIgnore // not used yet on GUI side
    public String getVdiPairToken() {
        return this.vdiPairToken;
    }

    @JsonIgnore
    public Long getCreationTime() {
        return this.creationTime;
    }

    @JsonIgnore
    public Long getUpdateTime() {
        return this.updateTime;
    }

    @JsonIgnore
    public Long getScreenProctoringGroupId() {
        return this.screenProctoringGroupId;
    }

    @JsonIgnore
    public Boolean getScreenProctoringGroupUpdate() {
        return this.screenProctoringGroupUpdate;
    }

    @JsonIgnore
    public Long getRemoteProctoringRoomId() {
        return this.remoteProctoringRoomId;
    }

    @JsonIgnore
    public Boolean getRemoteProctoringRoomUpdate() {
        return this.remoteProctoringRoomUpdate;
    }

    public String getInfo() {
        return this.info;
    }

    @JsonIgnore
    public String getSebOSName() {
        return this.sebOSName;
    }

    @JsonIgnore
    public String getSebMachineName() {
        return this.sebMachineName;
    }

    @JsonIgnore
    public String getSebVersion() {
        return this.sebVersion;
    }

    public Boolean getSecurityCheckGranted() {
        return this.securityCheckGranted;
    }

    public Boolean getClientVersionGranted() {
        return this.clientVersionGranted;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ClientConnection other = (ClientConnection) obj;
        if (this.id == null) {
            if (other.id != null)
                return false;
        } else if (!this.id.equals(other.id))
            return false;
        return true;
    }

    public boolean dataEquals(final ClientConnection other) {
        if (other == null) {
            return true;
        }
        if (this.connectionToken == null) {
            if (other.connectionToken != null)
                return false;
        } else if (!this.connectionToken.equals(other.connectionToken))
            return false;

        if (this.status != other.status)
            return false;

        if (this.userSessionId == null) {
            if (other.userSessionId != null)
                return false;
        } else if (!this.userSessionId.equals(other.userSessionId)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ClientConnection [id=");
        builder.append(this.id);
        builder.append(", institutionId=");
        builder.append(this.institutionId);
        builder.append(", examId=");
        builder.append(this.examId);
        builder.append(", status=");
        builder.append(this.status);
        builder.append(", connectionToken=");
        builder.append(this.connectionToken);
        builder.append(", userSessionId=");
        builder.append(this.userSessionId);
        builder.append(", info=");
        builder.append(this.info);
        builder.append(", vdi=");
        builder.append(this.vdi);
        builder.append(", vdiPairToken=");
        builder.append(this.vdiPairToken);
        builder.append(", clientAddress=");
        builder.append(this.clientAddress);
        builder.append(", remoteProctoringRoomId=");
        builder.append(this.remoteProctoringRoomId);
        builder.append(", sebClientUserId=");
        builder.append(this.sebClientUserId);
        builder.append(", creationTime=");
        builder.append(this.creationTime);
        builder.append(", updateTime=");
        builder.append(this.updateTime);
        builder.append(", remoteProctoringRoomUpdate=");
        builder.append(this.remoteProctoringRoomUpdate);
        builder.append(", sebOSName=");
        builder.append(this.sebOSName);
        builder.append(", sebMachineName=");
        builder.append(this.sebMachineName);
        builder.append(", sebVersion=");
        builder.append(this.sebVersion);
        builder.append("]");
        return builder.toString();
    }

    public static Predicate<ClientConnection> getStatusPredicate(final ConnectionStatus status) {
        return connection -> connection.status == status;
    }

    public static Predicate<ClientConnection> getStatusPredicate(final ConnectionStatus... status) {
        final EnumSet<ConnectionStatus> states = EnumSet.noneOf(ConnectionStatus.class);
        if (status != null) {
            Collections.addAll(states, status);
        }
        return connection -> states.contains(connection.status);
    }

    private String getSEBInfo(final String seb_version) {
        return (seb_version != null) ? "SEB:" + seb_version : Constants.EMPTY_NOTE;
    }

    private String getOSInfo(final String seb_os_name) {
        if (seb_os_name != null) {
            final String[] split = StringUtils.split(seb_os_name, Constants.LIST_SEPARATOR);
            return " OS:" + split[0];
        }
        return Constants.EMPTY_NOTE;
    }

}
