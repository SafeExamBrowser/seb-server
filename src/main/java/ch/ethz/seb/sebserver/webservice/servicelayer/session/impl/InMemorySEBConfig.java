/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

public final class InMemorySEBConfig {

    public final Long configId;
    public final Long follwupId;
    public final Long examId;
    private final byte[] data;

    protected InMemorySEBConfig(
            final Long configId,
            final Long follwupId,
            final Long examId,
            final byte[] data) {

        super();
        this.configId = configId;
        this.follwupId = follwupId;
        this.examId = examId;
        this.data = data;
    }

    public Long getConfigId() {
        return this.configId;
    }

    public Long getExamId() {
        return this.examId;
    }

    public byte[] getData() {
        return this.data;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.configId == null) ? 0 : this.configId.hashCode());
        result = prime * result + ((this.examId == null) ? 0 : this.examId.hashCode());
        result = prime * result + ((this.follwupId == null) ? 0 : this.follwupId.hashCode());
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
        final InMemorySEBConfig other = (InMemorySEBConfig) obj;
        if (this.configId == null) {
            if (other.configId != null)
                return false;
        } else if (!this.configId.equals(other.configId))
            return false;
        if (this.examId == null) {
            if (other.examId != null)
                return false;
        } else if (!this.examId.equals(other.examId))
            return false;
        if (this.follwupId == null) {
            if (other.follwupId != null)
                return false;
        } else if (!this.follwupId.equals(other.follwupId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("InMemorySEBConfig [configId=");
        builder.append(this.configId);
        builder.append(", follwupId=");
        builder.append(this.follwupId);
        builder.append(", examId=");
        builder.append(this.examId);
        builder.append("]");
        return builder.toString();
    }

}
