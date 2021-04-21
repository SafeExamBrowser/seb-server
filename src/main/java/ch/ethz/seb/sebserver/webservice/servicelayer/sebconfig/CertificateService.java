/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig;

import java.io.InputStream;
import java.util.Collection;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.CertificateData;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.CertificateData.CertificateFileType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.CertificateData.CertificateType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Certificates;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;

public interface CertificateService {

    Result<CertificateData> getCertificateData(Long institutionId, String alias);

    Result<Collection<CertificateData>> getCertificateData(Long institutionId, FilterMap filterMap);

    Result<Certificates> getCertificates(Long institutionId);

    Result<CertificateData> addCertificate(
            Long institutionId,
            CertificateFileType certificateFileType,
            String alias,
            InputStream in);

    Result<Certificates> removeCertificate(Long institutionId, String alias);

    Result<Collection<CertificateData>> toCertificateData(Certificates certificates);

    default Predicate<CertificateData> createFilter(final FilterMap filterMap) {
        final String aliasFilter = filterMap.getString(CertificateData.FILTER_ATTR_ALIAS);
        final CertificateType typeFilter = filterMap.getEnum(
                CertificateData.FILTER_ATTR_TYPE,
                CertificateType.class);
        return certificateData -> {
            if (StringUtils.isNotBlank(aliasFilter) &&
                    !certificateData.alias.contains(aliasFilter)) {
                return false;
            }
            if (typeFilter != null && !certificateData.types.contains(typeFilter)) {
                return false;
            }

            return true;
        };
    }

}
