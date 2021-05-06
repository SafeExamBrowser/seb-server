/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.CertificateInfo;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Certificates;
import ch.ethz.seb.sebserver.gbl.util.Result;

/** Concrete EntityDAO interface of Certificate entities */
public interface CertificateDAO {

    Result<Certificate> getCertificate(final Long institutionId, String alias);

    Result<Certificates> getCertificates(Long institutionId);

    Result<CertificateInfo> addCertificate(
            Long institutionId,
            String alias,
            Certificate certificate);

    Result<CertificateInfo> addCertificate(
            Long institutionId,
            String alias,
            Certificate certificate,
            PrivateKey privateKey);

    Result<EntityKey> removeCertificate(Long institutionId, String alias);

    Result<Collection<String>> getAllIdentityAlias(Long institutionId);

    Result<CertificateInfo> getDataFromCertificate(Certificates certificates, String alias);

    Result<Collection<String>> getIdentityAlias(Long institutionId);

    String extractAlias(X509Certificate a, String alias);

}
