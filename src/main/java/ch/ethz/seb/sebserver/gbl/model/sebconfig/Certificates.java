/*
 * Copyright (c) 2021 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.sebconfig;

import java.util.Collection;

import org.bouncycastle.jcajce.provider.keystore.pkcs12.PKCS12KeyStoreSpi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.model.Domain.CERTIFICATE;
import ch.ethz.seb.sebserver.gbl.util.Utils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Certificates {

    @JsonProperty(CERTIFICATE.ATTR_ID)
    public final Long id;

    @JsonProperty(CERTIFICATE.ATTR_INSTITUTION_ID)
    public final Long institutionId;

    @JsonProperty(CERTIFICATE.ATTR_ALIASES)
    public final Collection<String> aliases;

    @JsonIgnore
    public final PKCS12KeyStoreSpi keyStore;

    @JsonCreator
    public Certificates(
            @JsonProperty(CERTIFICATE.ATTR_ID) final Long id,
            @JsonProperty(CERTIFICATE.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(CERTIFICATE.ATTR_ALIASES) final Collection<String> aliases) {

        this.id = id;
        this.institutionId = institutionId;
        this.aliases = aliases;
        this.keyStore = null;
    }

    public Certificates(
            final Long id,
            final Long institutionId,
            final Collection<String> aliases,
            final PKCS12KeyStoreSpi keyStore) {

        this.id = id;
        this.institutionId = institutionId;
        this.aliases = Utils.immutableCollectionOf(aliases);
        this.keyStore = keyStore;
    }

    public Long getId() {
        return this.id;
    }

    public Long getInstitutionId() {
        return this.institutionId;
    }

    public Collection<String> getAliases() {
        return this.aliases;
    }

    @JsonIgnore
    public PKCS12KeyStoreSpi getKeyStore() {
        return this.keyStore;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Certificates [id=");
        builder.append(this.id);
        builder.append(", institutionId=");
        builder.append(this.institutionId);
        builder.append(", aliases=");
        builder.append(this.aliases);
        builder.append("]");
        return builder.toString();
    }

}
