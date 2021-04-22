/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import java.io.ByteArrayInputStream;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.bouncycastle.jcajce.provider.keystore.pkcs12.PKCS12KeyStoreSpi;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.CertificateInfo;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Certificates;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.CertificateRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.CertificateRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.CertificateRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.CertificateDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;

@Lazy
@Component
@WebServiceProfile
public class CertificateDAOImpl implements CertificateDAO {

    private final CertificateRecordMapper certificateRecordMapper;
    private final Cryptor cryptor;

    public CertificateDAOImpl(
            final CertificateRecordMapper certificateRecordMapper,
            final Cryptor cryptor) {

        this.certificateRecordMapper = certificateRecordMapper;
        this.cryptor = cryptor;
    }

    @Override
    @Transactional
    public Result<Certificates> getCertificates(final Long institutionId) {

        return getCertificatesFromPersistent(institutionId)
                .flatMap(this::toDomainObject)
                .onErrorDo(error -> createNewCertificateStore(institutionId),
                        ResourceNotFoundException.class);

    }

    @Override
    @Transactional
    public Result<CertificateInfo> addCertificate(
            final Long institutionId,
            final String alias,
            final Certificate certificate) {

        return getCertificatesFromPersistent(institutionId)
                .flatMap(record -> addCertificate(record, alias, certificate))
                .flatMap(this::storeUpdate)
                .flatMap(certs -> CertificateDAO.getDataFromCertificate(certs, alias))
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<Certificates> removeCertificate(final Long institutionId, final String alias) {

        return getCertificatesFromPersistent(institutionId)
                .flatMap(record -> removeCertificate(record, alias))
                .flatMap(this::storeUpdate)
                .onError(TransactionHandler::rollback);
    }

    private Certificates createNewCertificateStore(final Long institutionId) {
        return this.cryptor.createNewEmptyKeyStore()
                .map(store -> new CertificateRecord(
                        null,
                        institutionId,
                        "",
                        storeToBinary(store)))
                .flatMap(record -> {
                    final int insert = this.certificateRecordMapper.insert(record);
                    if (insert != 1) {
                        throw new IllegalStateException("Insert new certificate store failed!");
                    }
                    return getCertificatesFromPersistent(institutionId);
                })
                .flatMap(this::toDomainObject)
                .onError(TransactionHandler::rollback)
                .getOrThrow();
    }

    private Result<Certificates> addCertificate(
            final CertificateRecord record,
            final String alias,
            final Certificate certificate) {

        return loadCertificateStore(record.getCertStore())
                .map(store -> {
                    try {
                        store.engineSetCertificateEntry(alias, certificate);
                    } catch (final KeyStoreException e) {
                        throw new RuntimeException("Failed to add certificate to keystore. Cause: ", e);
                    }
                    return new Certificates(
                            record.getId(),
                            record.getInstitutionId(),
                            joinAliases(record.getAliases(), alias),
                            store);
                });
    }

    private Result<Certificates> removeCertificate(
            final CertificateRecord record,
            final String alias) {

        return loadCertificateStore(record.getCertStore())
                .map(store -> {
                    try {
                        store.engineDeleteEntry(alias);
                    } catch (final KeyStoreException e) {
                        throw new RuntimeException("Failed to remove certificate from keystore. Cause: ", e);
                    }
                    return new Certificates(
                            record.getId(),
                            record.getInstitutionId(),
                            joinAliases(record.getAliases(), alias),
                            store);
                });
    }

    private Collection<String> joinAliases(final String aliases, final String newAlias) {
        if (StringUtils.isBlank(aliases)) {
            return Arrays.asList(newAlias);
        } else {
            final Collection<String> listFromString = Utils.getListFromString(aliases);
            listFromString.add(newAlias);
            return listFromString;
        }
    }

    private Result<Certificates> storeUpdate(final Certificates certificates) {

        return Result.tryCatch(() -> {
            final CertificateRecord record = new CertificateRecord(
                    certificates.id,
                    null,
                    StringUtils.join(certificates.aliases, Constants.COMMA),
                    storeToBinary(certificates.keyStore));

            this.certificateRecordMapper.updateByPrimaryKey(record);
            return certificates;
        });
    }

    private Result<Certificates> toDomainObject(final CertificateRecord record) {
        return loadCertificateStore(record.getCertStore())
                .map(store -> new Certificates(
                        record.getId(),
                        record.getInstitutionId(),
                        Utils.getListFromString(record.getAliases()),
                        store));
    }

    private Result<CertificateRecord> getCertificatesFromPersistent(final Long institutionId) {
        return Result.tryCatch(() -> {
            final List<CertificateRecord> result = this.certificateRecordMapper.selectByExample()
                    .where(CertificateRecordDynamicSqlSupport.institutionId, SqlBuilder.isEqualTo(institutionId))
                    .build()
                    .execute();

            if (result != null && result.size() > 0) {
                return result.get(1);
            } else {
                throw new ResourceNotFoundException(EntityType.CERTIFICATE, String.valueOf(institutionId));
            }
        });
    }

    private byte[] storeToBinary(final PKCS12KeyStoreSpi store) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        this.cryptor.storeKeyStore(store, out).getOrThrow();
        final byte[] byteArray = out.toByteArray();
        IOUtils.closeQuietly(out);
        return byteArray;
    }

    private Result<PKCS12KeyStoreSpi> loadCertificateStore(final byte[] certStore) {
        return Result.tryCatch(() -> new ByteArrayInputStream(certStore))
                .flatMap(input -> this.cryptor.loadKeyStore(input));
    }

}
