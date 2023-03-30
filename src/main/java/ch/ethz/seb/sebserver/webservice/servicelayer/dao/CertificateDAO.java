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
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionSupportDAO;

/** Concrete EntityDAO interface of Certificate entities */
public interface CertificateDAO extends BulkActionSupportDAO<CertificateInfo> {

    /** Get the certificate with given alias for specified institution.
     *
     * @param institutionId Institution identifier
     * @param alias the alias name of the certificate to get
     * @return Result refer to the Certificate or to an error when happened. */
    Result<Certificate> getCertificate(final Long institutionId, String alias);

    /** Get all certificates of a given institution,
     *
     * @param institutionId Institution identifier
     * @return Result to the Certificates or to an error when happend */
    Result<Certificates> getCertificates(Long institutionId);

    /** Add a new uploaded certificate to the certificate store of the institution.
     *
     * @param institutionId Institution identifier
     * @param alias the alias name of the institution
     * @param certificate the certificate to add.
     * @return Result refer to the generated CertificateInfo or to an error when happened */
    Result<CertificateInfo> addCertificate(
            Long institutionId,
            String alias,
            Certificate certificate);

    /** Add a new uploaded certificate with private key to the certificate store of the institution.
     *
     * @param institutionId Institution identifier
     * @param alias the alias name of the institution
     * @param certificate the certificate to add.
     * @param privateKey the private key of the certificate
     * @return Result refer to the generated CertificateInfo or to an error when happened */
    Result<CertificateInfo> addCertificate(
            Long institutionId,
            String alias,
            Certificate certificate,
            PrivateKey privateKey);

    /** Removes specified certificate from the certificate store of a given institution.
     *
     * @param institutionId The institution identifier
     * @param alias the alias name of the certificate
     * @return Result refer to the entity key of the removed certificate or to an error when happened */
    Result<EntityKey> removeCertificate(Long institutionId, String alias);

    /** Get all alias names of all certificated that exists for a given institution.
     *
     * @param institutionId The institution identifier
     * @return Result refer to the collection of all certificate alias names or to an error when happened */
    Result<Collection<String>> getAllIdentityAlias(Long institutionId);

    /** Get the certification information for a specific certificate from the the given Certificates.
     *
     * @param certificates The certificates bucket to get the info from
     * @param alias the alias name of the certificate to get the info from
     * @return Result refer to the certificate info or to an error when happened. */
    Result<CertificateInfo> getDataFromCertificate(Certificates certificates, String alias);

    /** Get a collection of all alias names of all identity certificates for a given institution.
     *
     * @param institutionId The institution identifier
     * @return Result refer to the collection of certificate alias or to an error when happened */
    Result<Collection<String>> getIdentityAlias(Long institutionId);

    /** Get or extract the alias name of a given certificate. If there is not given a explicit alias name
     * within the certificate, this will create one generic from the data that is available.
     *
     * @param certificate The X509Certificate to extract the alias name from
     * @return the extracted alias */
    String extractAlias(X509Certificate certificate);

}
