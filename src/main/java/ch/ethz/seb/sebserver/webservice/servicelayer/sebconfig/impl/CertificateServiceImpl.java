/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.CertificateInfo;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.CertificateInfo.CertificateFileType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Certificates;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.CertificateDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.CertificateService;

@Lazy
@Service
@WebServiceProfile
public class CertificateServiceImpl implements CertificateService {

    private final CertificateDAO certificateDAO;

    public CertificateServiceImpl(final CertificateDAO certificateDAO) {
        this.certificateDAO = certificateDAO;
    }

    @Override
    public Result<CertificateInfo> getCertificateInfo(final Long institutionId, final String alias) {

        return this.certificateDAO
                .getCertificates(institutionId)
                .flatMap(certs -> CertificateDAO.getDataFromCertificate(certs, alias));
    }

    @Override
    public Result<Collection<CertificateInfo>> getCertificateInfo(
            final Long institutionId,
            final FilterMap filterMap) {

        return this.certificateDAO
                .getCertificates(institutionId)
                .map(certs -> this.getDataFromCertificates(certs, createFilter(filterMap)));
    }

    @Override
    public Result<Certificates> getCertificates(final Long institutionId) {

        return this.certificateDAO
                .getCertificates(institutionId);
    }

    @Override
    public Result<CertificateInfo> addCertificate(
            final Long institutionId,
            final CertificateFileType certificateFileType,
            final String alias,
            final InputStream in) {

        return loadCertFromInput(institutionId, certificateFileType, in)
                .flatMap(cert -> this.certificateDAO.addCertificate(
                        institutionId,
                        CertificateDAO.extractAlias(cert, alias),
                        cert));
    }

    @Override
    public Result<Certificates> removeCertificate(final Long institutionId, final String alias) {
        return this.certificateDAO.removeCertificate(institutionId, alias);
    }

    @Override
    public Result<Collection<CertificateInfo>> toCertificateInfo(final Certificates certificates) {
        return Result.tryCatch(() -> getDataFromCertificates(certificates));
    }

    @Override
    public Result<String> getBase64Encoded(final Long institutionId, final String alias) {
        return this.certificateDAO
                .getCertificates(institutionId)
                .map(certs -> certs.keyStore.engineGetCertificate(alias))
                .map(this::getBase64Encoded);
    }

    private String getBase64Encoded(final Certificate cert) {
        try {
            return Base64.getEncoder().encodeToString(cert.getEncoded());
        } catch (final CertificateEncodingException e) {
            throw new RuntimeException();
        }
    }

    private Collection<CertificateInfo> getDataFromCertificates(
            final Certificates certificates,
            final Predicate<CertificateInfo> predicate) {

        return certificates.aliases
                .stream()
                .map(alias -> CertificateDAO.getDataFromCertificate(certificates, alias))
                .flatMap(Result::onErrorLogAndSkip)
                .filter(predicate)
                .collect(Collectors.toList());
    }

    private Collection<CertificateInfo> getDataFromCertificates(final Certificates certificates) {
        return getDataFromCertificates(certificates, data -> true);
    }

    private Result<X509Certificate> loadCertFromInput(
            final Long institutionId,
            final CertificateFileType certificateFileType,
            final InputStream in) {

        switch (certificateFileType) {
            case PEM:
                return loadCertFromPEM(institutionId, in);
            case PKCS12:
                return Result.ofRuntimeError("Not supported yet");
            default:
                return Result.ofRuntimeError("Unsupported certificate type");
        }
    }

    private Result<X509Certificate> loadCertFromPEM(final Long institutionId, final InputStream in) {
        return Result.tryCatch(() -> {
            final CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certFactory.generateCertificate(in);
        });
    }

}
