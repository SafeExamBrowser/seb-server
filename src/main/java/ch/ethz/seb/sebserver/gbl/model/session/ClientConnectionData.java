/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.session;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import ch.ethz.seb.sebserver.gbl.monitoring.IndicatorValueHolder;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.monitoring.IndicatorValue;
import ch.ethz.seb.sebserver.gbl.monitoring.SimpleIndicatorValue;
import ch.ethz.seb.sebserver.gbl.util.Utils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientConnectionData implements GrantEntity {

    public static final String ATTR_CLIENT_CONNECTION = "cdat";
    public static final String ATTR_INDICATOR_VALUE = "iVal";
    public static final String ATTR_MISSING_PING = "miss";
    public static final String ATTR_PENDING_NOTIFICATION = "pnot";
    public static final String ATTR_CLIENT_GROUPS = "cg";

    @JsonProperty(ATTR_CLIENT_CONNECTION)
    public final ClientConnection clientConnection;
    @JsonProperty(ATTR_INDICATOR_VALUE)
    public final List<? extends IndicatorValue> indicatorValues;
    @JsonProperty(ATTR_CLIENT_GROUPS)
    public final Set<Long> groups;

    public final Boolean missingPing;
    public final Boolean pendingNotification;

    @JsonCreator
    public ClientConnectionData(
            @JsonProperty(ATTR_MISSING_PING) final Boolean missingPing,
            @JsonProperty(ATTR_PENDING_NOTIFICATION) final Boolean pendingNotification,
            @JsonProperty(ATTR_CLIENT_CONNECTION) final ClientConnection clientConnection,
            @JsonProperty(ATTR_INDICATOR_VALUE) final Collection<? extends SimpleIndicatorValue> indicatorValues,
            @JsonProperty(ATTR_CLIENT_GROUPS) final Set<Long> groups) {

        this.missingPing = missingPing;
        this.pendingNotification = pendingNotification;
        this.clientConnection = clientConnection;
        this.indicatorValues = Utils.immutableListOf(indicatorValues);
        this.groups = (groups == null) ? null : Utils.immutableSetOf(groups);
    }

    public ClientConnectionData(
            final ClientConnection clientConnection,
            final List<? extends IndicatorValue> indicatorValues,
            final Set<Long> groups) {

        this.missingPing = null;
        this.pendingNotification = Boolean.FALSE;
        this.clientConnection = clientConnection;
        this.indicatorValues = Utils.immutableListOf(indicatorValues);
        this.groups = (groups == null) ? null : Utils.immutableSetOf(groups);
    }

    @JsonIgnore
    public boolean isInClientGroup(final Long clientGroupId) {
        return this.groups != null && this.groups.contains(clientGroupId);
    }

    @JsonIgnore
    public boolean containsAllClientGroup(final Set<Long> clientGroupIds) {
        if (this.groups == null || clientGroupIds == null || clientGroupIds.isEmpty()) {
            return false;
        }
        return this.groups != null && this.groups.containsAll(clientGroupIds);
    }

    @JsonIgnore
    public boolean filter(final Set<Long> clientGroupIdsToHide) {
        if (this.groups == null || clientGroupIdsToHide == null || clientGroupIdsToHide.isEmpty()) {
            return true;
        }

        for (final Long id : this.groups) {
            if (!clientGroupIdsToHide.contains(id)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public EntityType entityType() {
        return this.clientConnection.entityType();
    }

    @Override
    public String getName() {
        return this.clientConnection.getName();
    }

    @Override
    public String getModelId() {
        return this.clientConnection.getModelId();
    }

    @Override
    public Long getInstitutionId() {
        return this.clientConnection.getInstitutionId();
    }

    @JsonProperty(ATTR_MISSING_PING)
    public Boolean getMissingPing() {
        return this.missingPing;
    }

    @JsonProperty(ATTR_PENDING_NOTIFICATION)
    public Boolean pendingNotification() {
        return this.pendingNotification;
    }

    @JsonIgnore
    public Long getConnectionId() {
        return this.clientConnection.id;
    }

    @JsonIgnore
    public boolean hasAnyIncident() {
        return this.missingPing || this.pendingNotification;
    }

    @JsonIgnore
    public Double getIndicatorValue(final Long indicatorId) {
        return this.indicatorValues
                .stream()
                .filter(indicatorValue -> indicatorValue.getIndicatorId().equals(indicatorId))
                .findFirst()
                .map(IndicatorValueHolder::getValue)
                .orElse(Double.NaN);
    }

    @JsonIgnore
    public String getIndicatorDisplayValue(final Indicator indicator) {
        return this.indicatorValues
                .stream()
                .filter(indicatorValue -> indicatorValue.getIndicatorId().equals(indicator.id))
                .findFirst()
                .map(iv -> IndicatorValue.getDisplayValue(iv, indicator.type))
                .orElse(Constants.EMPTY_NOTE);
    }

    public ClientConnection getClientConnection() {
        return this.clientConnection;
    }

    public Collection<? extends IndicatorValue> getIndicatorValues() {
        return this.indicatorValues;
    }

    public boolean dataEquals(final ClientConnectionData other) {
        if (!this.clientConnection.dataEquals(other.clientConnection)) {
            return false;
        }

        if (!Objects.equals(this.groups, other.groups)) {
            return false;
        }

        return true;
    }

    public boolean indicatorValuesEquals(final ClientConnectionData other) {
        if (this.indicatorValues.size() != other.indicatorValues.size()) {
            return false;
        }

        final Iterator<? extends IndicatorValue> i1 = this.indicatorValues.iterator();
        final Iterator<? extends IndicatorValue> i2 = other.indicatorValues.iterator();
        while (i1.hasNext()) {
            final IndicatorValue iv1 = i1.next();
            final IndicatorValue iv2 = i2.next();
            if (!iv1.dataEquals(iv2)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return "ClientConnectionData [clientConnection=" +
                this.clientConnection +
                ", indicatorValues=" +
                this.indicatorValues +
                "]";
    }

}
