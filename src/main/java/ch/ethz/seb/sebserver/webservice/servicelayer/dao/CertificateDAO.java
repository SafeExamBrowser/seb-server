/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.EnumSet;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.joda.time.DateTime;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.CertificateInfo;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.CertificateInfo.CertificateType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Certificates;
import ch.ethz.seb.sebserver.gbl.util.Result;

/** Concrete EntityDAO interface of Certificate entities */
public interface CertificateDAO {

    Result<Certificates> getCertificates(Long institutionId);

    Result<CertificateInfo> addCertificate(Long institutionId, String alias, Certificate certificate);

    Result<EntityKey> removeCertificate(Long institutionId, String alias);

    static Result<CertificateInfo> getDataFromCertificate(final Certificates certificates, final String alias) {
        return Result.tryCatch(() -> {
            final X509Certificate certificate = (X509Certificate) certificates.keyStore.engineGetCertificate(alias);
            if (certificate != null) {

                final X509Certificate cert = certificate;
                return new CertificateInfo(
                        extractAlias(cert, alias),
                        new DateTime(cert.getNotBefore()),
                        new DateTime(cert.getNotAfter()),
                        getTypes(cert));

            } else {
                throw new NoSuchElementException("X509Certificate with alias: " + alias);
            }
        });
    }

    static String extractAlias(final X509Certificate certificate, final String alias) {
        if (StringUtils.isNotBlank(alias)) {
            return alias;
        }

        try {
            final X500Name x500name = new JcaX509CertificateHolder(certificate).getSubject();
            final RDN cn = x500name.getRDNs(BCStyle.CN)[0];
            final String dn = IETFUtils.valueToString(cn.getFirst().getValue());

            if (StringUtils.isBlank(dn)) {
                return String.valueOf(certificate.getSerialNumber());
            } else {
                return dn.replace(" ", "_").toLowerCase();
            }
        } catch (final CertificateEncodingException e) {
            return String.valueOf(certificate.getSerialNumber());
        }
    }

    static EnumSet<CertificateType> getTypes(final X509Certificate cert) {

        // KeyUsage ::= BIT STRING {
        //     digitalSignature        (0),
        //     nonRepudiation          (1),
        //     keyEncipherment         (2),
        //     dataEncipherment        (3),
        //     keyAgreement            (4),
        //     keyCertSign             (5),
        //     cRLSign                 (6),
        //     encipherOnly            (7),
        //     decipherOnly            (8) }
        final boolean[] keyUsage = cert.getKeyUsage();
        final EnumSet<CertificateType> result = EnumSet.noneOf(CertificateType.class);

        // digitalSignature
        if (keyUsage[0]) {
            result.add(CertificateType.DIGITAL_SIGNATURE);
        }

        // dataEncipherment
        if (keyUsage[3]) {
            result.add(CertificateType.DATA_ENCIPHERMENT);
        }

        // keyCertSign
        if (keyUsage[5]) {
            result.add(CertificateType.KEY_CERT_SIGN);
        }

        if (result.isEmpty()) {
            result.add(CertificateType.UNKNOWN);
        }

        return result;
    }

}
