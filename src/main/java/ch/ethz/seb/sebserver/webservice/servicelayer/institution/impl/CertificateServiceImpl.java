/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.institution.impl;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.APIMessageException;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.CertificateInfo;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.CertificateInfo.CertificateFileType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Certificates;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Pair;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.CertificateDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.SEBClientConfigDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.institution.CertificateService;

@Lazy
@Service
@WebServiceProfile
public class CertificateServiceImpl implements CertificateService {

    private final CertificateDAO certificateDAO;
    private final SEBClientConfigDAO sebClientConfigDAO;

    public CertificateServiceImpl(
            final CertificateDAO certificateDAO,
            final SEBClientConfigDAO sebClientConfigDAO) {

        this.certificateDAO = certificateDAO;
        this.sebClientConfigDAO = sebClientConfigDAO;
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    @Override
    public Result<CertificateInfo> getCertificateInfo(final Long institutionId, final String alias) {

        return this.certificateDAO
                .getCertificates(institutionId)
                .flatMap(certs -> this.certificateDAO.getDataFromCertificate(certs, alias));
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
            final CharSequence password,
            final InputStream in) {

        switch (certificateFileType) {
            case PEM:
                return loadCertFromPEM(in)
                        .flatMap(cert -> this.certificateDAO.addCertificate(
                                institutionId,
                                this.certificateDAO.extractAlias(cert, alias),
                                cert));

            case PKCS12:
                return loadCertFromPKC(in, password)
                        .flatMap(pair -> this.certificateDAO.addCertificate(
                                institutionId,
                                this.certificateDAO.extractAlias(pair.a, alias),
                                pair.a,
                                pair.b));
            default:
                return Result.ofRuntimeError("Unsupported certificate type");
        }
    }

    @Override
    public Result<EntityKey> removeCertificate(final Long institutionId, final String alias) {

        // TODO check if certificate is in use
        if (this.sebClientConfigDAO.all(institutionId, true)
                .getOr(Collections.emptyList())
                .stream()
                .filter(config -> alias.equals(config.encryptCertificateAlias))
                .findFirst()
                .isPresent()) {

            throw new APIMessageException(APIMessage.ErrorMessage.INTEGRITY_VALIDATION);
        }

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
                .map(alias -> this.certificateDAO.getDataFromCertificate(certificates, alias))
                .flatMap(Result::onErrorLogAndSkip)
                .filter(predicate)
                .collect(Collectors.toList());
    }

    private Collection<CertificateInfo> getDataFromCertificates(final Certificates certificates) {
        return getDataFromCertificates(certificates, data -> true);
    }

    private Result<X509Certificate> loadCertFromPEM(final InputStream in) {
        return Result.tryCatch(() -> {
            final CertificateFactory certFactory = CertificateFactory.getInstance(Constants.X_509);
            return (X509Certificate) certFactory.generateCertificate(in);
        });
    }

    private Result<Pair<X509Certificate, PrivateKey>> loadCertFromPKC(
            final InputStream in,
            final CharSequence password) {

        return Result.tryCatch(() -> {
            final KeyStore ks = KeyStore.getInstance(Constants.PKCS_12);
            ks.load(in, Utils.toCharArray(password));
            final Enumeration<String> aliases = ks.aliases();
            final String alias = aliases.nextElement();
            final X509Certificate certificate = (X509Certificate) ks.getCertificate(alias);
            final PrivateKey pKey = (PrivateKey) ks.getKey(alias, Utils.toCharArray(password));
            return new Pair<>(certificate, pKey);
        });
    }

}
