/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.sebconfig;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Entity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CertificateInfo implements Entity {

    public static enum CertificateType {
        UNKNOWN,
        DIGITAL_SIGNATURE,
        DATA_ENCIPHERMENT,
        KEY_CERT_SIGN
    }

    public static enum CertificateFileType {
        PEM(".pem", ".crt", ".cer"),
        PKCS12(".p12", ".pfx");

        private String[] extentions;

        private CertificateFileType(final String... extentions) {
            this.extentions = extentions;
        }

        public boolean match(final String fileName) {
            return Stream.of(this.extentions)
                    .filter(ext -> fileName.endsWith(ext))
                    .findAny()
                    .isPresent();
        }

        public static String[] getAllExtensions() {
            return Arrays.asList(CertificateFileType.values())
                    .stream()
                    .flatMap(type -> Stream.of(type.extentions))
                    .collect(Collectors.toList())
                    .toArray(new String[0]);
        }

        public static CertificateFileType forFileName(final String fileName) {
            return Arrays.asList(CertificateFileType.values())
                    .stream()
                    .filter(type -> type.match(fileName))
                    .findFirst()
                    .orElse(null);
        }
    }

    public static final String FILTER_ATTR_ALIAS = "alias";
    public static final String FILTER_ATTR_TYPE = "type";

    public static final String ATTR_ALIAS = "alias";
    public static final String ATTR_VALIDITY_FROM = "validityFrom";
    public static final String ATTR_VALIDITY_TO = "validityTo";
    public static final String ATTR_CERT_TYPE = "certType";

    @JsonProperty(ATTR_ALIAS)
    public final String alias;

    @JsonProperty(ATTR_VALIDITY_FROM)
    public final DateTime validityFrom;

    @JsonProperty(ATTR_VALIDITY_TO)
    public final DateTime validityTo;

    @JsonProperty(ATTR_CERT_TYPE)
    public final EnumSet<CertificateType> types;

    @JsonCreator
    public CertificateInfo(
            @JsonProperty(ATTR_ALIAS) final String alias,
            @JsonProperty(ATTR_VALIDITY_FROM) final DateTime validityFrom,
            @JsonProperty(ATTR_VALIDITY_TO) final DateTime validityTo,
            @JsonProperty(ATTR_CERT_TYPE) final EnumSet<CertificateType> types) {

        this.alias = alias;
        this.validityFrom = validityFrom;
        this.validityTo = validityTo;
        this.types = types;
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
        final CertificateInfo other = (CertificateInfo) obj;
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
