/*
 * Copyright (c) 2021 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.institution;

import java.io.InputStream;
import java.util.Collection;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.CertificateInfo;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.CertificateInfo.CertificateFileType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.CertificateInfo.CertificateType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Certificates;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;

public interface CertificateService {

    /** Get info for the certificate with given alias.
     *
     * @param institutionId The institution identifier
     * @param alias the certificate alias
     * @return Result refer to the CertificateInfo or to an error when happened */
    Result<CertificateInfo> getCertificateInfo(Long institutionId, String alias);

    /** Get list of info for the certificates that apply the given filter criteria
     *
     * @param institutionId The institution identifier
     * @param filterMap the certificate filter criteria map
     * @return Result refer to the list of CertificateInfo or to an error when happened */
    Result<Collection<CertificateInfo>> getCertificateInfo(Long institutionId, FilterMap filterMap);

    /** Get all certificates within a given institution.
     *
     * @param institutionId The institution identifier
     * @return Result refer to Certificates or to an error when happened */
    Result<Certificates> getCertificates(Long institutionId);

    /** Add a given certificate to the certificate store.
     *
     * @param institutionId The institution identifier
     * @param certificateFileType The file type of the certificate
     * @param alias The alias of the certificate
     * @param password the password if the certificate is encrypted
     * @param in the input stream to read the certificate from
     * @return Result refer to the info of the just imported certificate or to an error when happened */
    Result<CertificateInfo> addCertificate(
            Long institutionId,
            CertificateFileType certificateFileType,
            String alias,
            CharSequence password,
            InputStream in);

    /** Remove a certificate from the certificate store.
     *
     * @param institutionId The institution identifier
     * @param alias the alias of the certificate to remove
     * @return Result refer to the EntityKey of the removed certificate or to an error when happened */
    Result<EntityKey> removeCertificate(Long institutionId, String alias);

    /** Used to extract infos from given certificates.
     *
     * @param certificates certificates
     * @return Result refer to list of CertificateInfo or to an error when happened. */
    Result<Collection<CertificateInfo>> toCertificateInfo(Certificates certificates);

    /** Get the certificate as base 64 encoded String value.
     *
     * @param institutionId The institution identifier
     * @param alias The alias of the certificate
     * @return Result refer to the base 64 encoded String value of the certificate or to an error when happened */
    Result<String> getBase64Encoded(Long institutionId, String alias);

    /** Used to create a predicate for CertificateInfo from given filterMap.
     *
     * @param filterMap the FilterMap containing filter criteria
     * @return Predicate for CertificateInfo that reflects the given FilterMap */
    default Predicate<CertificateInfo> createFilter(final FilterMap filterMap) {
        final String aliasFilter = filterMap.getString(CertificateInfo.FILTER_ATTR_ALIAS);
        final CertificateType typeFilter = filterMap.getEnum(
                CertificateInfo.FILTER_ATTR_TYPE,
                CertificateType.class);
        return certificateInfo -> {
            if (StringUtils.isNotBlank(aliasFilter) &&
                    !certificateInfo.alias.contains(aliasFilter)) {
                return false;
            }
            if (typeFilter != null && !certificateInfo.types.contains(typeFilter)) {
                return false;
            }

            return true;
        };
    }

}
