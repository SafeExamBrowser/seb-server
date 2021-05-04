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

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.CertificateInfo;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.CertificateInfo.CertificateFileType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.CertificateInfo.CertificateType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Certificates;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;

public interface CertificateService {

    Result<CertificateInfo> getCertificateInfo(Long institutionId, String alias);

    Result<Collection<CertificateInfo>> getCertificateInfo(Long institutionId, FilterMap filterMap);

    Result<Certificates> getCertificates(Long institutionId);

    Result<CertificateInfo> addCertificate(
            Long institutionId,
            CertificateFileType certificateFileType,
            String alias,
            CharSequence password,
            InputStream in);

    Result<EntityKey> removeCertificate(Long institutionId, String alias);

    Result<Collection<CertificateInfo>> toCertificateInfo(Certificates certificates);

    Result<String> getBase64Encoded(Long institutionId, String alias);

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
