/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.sebconfig;

import java.util.EnumSet;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Entity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CertificateData implements Entity {

    public static enum CertificateType {
        UNKNOWN,
        SSL_TLS,
        CA,
        IDENTITY
    }

    public static enum CertificateFileType {
        PEM("pem", "crt"),
        PKCS12("p12", "pfx");

        private String[] extentions;

        private CertificateFileType(final String... extentions) {
            this.extentions = extentions;
        }
    }

    public static final String FILTER_ATTR_ALIAS = "alias";
    public static final String FILTER_ATTR_TYPE = "type";

    public static final String ATTR_ALIAS = "alias";
    public static final String ATTR_VALIDITY_FROM = "validityFrom";
    public static final String ATTR_VALIDITY_TO = "validityTo";
    public static final String ATTR_CERT_TYPE = "certType";
    public static final String ATTR_CERT_BASE_64 = "cert";

    @JsonProperty(ATTR_ALIAS)
    public final String alias;

    @JsonProperty(ATTR_ALIAS)
    public final DateTime validityFrom;

    @JsonProperty(ATTR_ALIAS)
    public final DateTime validityTo;

    @JsonProperty(ATTR_CERT_TYPE)
    public final EnumSet<CertificateType> types;

    @JsonProperty(ATTR_CERT_BASE_64)
    public final String certBase64;

    @JsonCreator
    public CertificateData(
            @JsonProperty(ATTR_ALIAS) final String alias,
            @JsonProperty(ATTR_ALIAS) final DateTime validityFrom,
            @JsonProperty(ATTR_ALIAS) final DateTime validityTo,
            @JsonProperty(ATTR_CERT_TYPE) final EnumSet<CertificateType> types,
            @JsonProperty(ATTR_CERT_BASE_64) final String certBase64) {

        this.alias = alias;
        this.validityFrom = validityFrom;
        this.validityTo = validityTo;
        this.types = types;
        this.certBase64 = certBase64;
    }

    public String getAlias() {
        return this.alias;
    }

    public DateTime getValidityFrom() {
        return this.validityFrom;
    }

    public DateTime getValidityTo() {
        return this.validityTo;
    }

    public EnumSet<CertificateType> getTypes() {
        return this.types;
    }

    public String getCertBase64() {
        return this.certBase64;
    }

    @Override
    public String getModelId() {
        return this.alias;
    }

    @Override
    public EntityType entityType() {
        return EntityType.CERTIFICATE;
    }

    @Override
    public String getName() {
        return this.alias;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.alias == null) ? 0 : this.alias.hashCode());
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
        final CertificateData other = (CertificateData) obj;
        if (this.alias == null) {
            if (other.alias != null)
                return false;
        } else if (!this.alias.equals(other.alias))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("CertificateData [alias=");
        builder.append(this.alias);
        builder.append(", validityFrom=");
        builder.append(this.validityFrom);
        builder.append(", validityTo=");
        builder.append(this.validityTo);
        builder.append(", types=");
        builder.append(this.types);
        builder.append("]");
        return builder.toString();
    }

}
