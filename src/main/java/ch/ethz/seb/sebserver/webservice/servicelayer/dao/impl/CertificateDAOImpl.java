/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;
import static org.mybatis.dynamic.sql.SqlBuilder.isIn;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.jcajce.provider.keystore.pkcs12.PKCS12KeyStoreSpi;
import org.joda.time.DateTime;
import org.mybatis.dynamic.sql.SqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.APIMessage.FieldValidationException;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityDependency;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.CertificateInfo;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.CertificateInfo.CertificateType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Certificates;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.CertificateRecordDynamicSqlSupport;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.mapper.CertificateRecordMapper;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.CertificateRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.CertificateDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.EntityDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ResourceNotFoundException;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.TransactionHandler;

@Lazy
@Component
@WebServiceProfile
public class CertificateDAOImpl implements CertificateDAO {

    private static final Logger log = LoggerFactory.getLogger(CertificateDAOImpl.class);

    private final CertificateRecordMapper certificateRecordMapper;
    private final Cryptor cryptor;

    public CertificateDAOImpl(
            final CertificateRecordMapper certificateRecordMapper,
            final Cryptor cryptor) {

        this.certificateRecordMapper = certificateRecordMapper;
        this.cryptor = cryptor;
    }

    @Override
    public EntityType entityType() {
        return EntityType.CERTIFICATE;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Certificate> getCertificate(final Long institutionId, final String alias) {
        return getCertificates(institutionId)
                .map(certs -> {
                    if (!certs.aliases.contains(alias)) {
                        throw new ResourceNotFoundException(EntityType.CERTIFICATE, alias);
                    }
                    return certs.keyStore.engineGetCertificate(alias);
                });
    }

    @Override
    @Transactional(readOnly = true)
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

        return this.addCertificate(institutionId, alias, certificate, null);
    }

    @Override
    @Transactional
    public Result<CertificateInfo> addCertificate(
            final Long institutionId,
            final String alias,
            final Certificate certificate,
            final PrivateKey privateKey) {

        return getCertificatesFromPersistent(institutionId)
                .flatMap(record -> addCertificate(record, alias, certificate, privateKey))
                .flatMap(this::storeUpdate)
                .flatMap(certs -> getDataFromCertificate(certs, alias))
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional
    public Result<EntityKey> removeCertificate(final Long institutionId, final String alias) {

        return getCertificatesFromPersistent(institutionId)
                .flatMap(record -> removeCertificate(record, alias))
                .flatMap(this::storeUpdate)
                .map(cert -> new EntityKey(alias, EntityType.CERTIFICATE))
                .onError(TransactionHandler::rollback);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<EntityDependency> getDependencies(final BulkAction bulkAction) {
        // all of institution
        if (bulkAction.sourceType == EntityType.INSTITUTION) {
            return getDependencies(bulkAction, this::allIdsOfInstitution);
        }

        return Collections.emptySet();
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<String>> getAllIdentityAlias(final Long institutionId) {
        return getCertificates(institutionId)
                .map(certs -> certs.aliases
                        .stream()
                        .filter(alias -> this.cryptor
                                .getPrivateKey(certs.keyStore, alias)
                                .hasValue())
                        .collect(Collectors.toList()));
    }

    @Override
    public Result<CertificateInfo> getDataFromCertificate(final Certificates certificates, final String alias) {
        return Result.tryCatch(() -> {
            final X509Certificate certificate = (X509Certificate) certificates.keyStore.engineGetCertificate(alias);
            if (certificate != null) {

                final X509Certificate cert = certificate;

                return new CertificateInfo(
                        StringUtils.isNotBlank(alias) ? alias : extractAlias(cert),
                        new DateTime(cert.getNotBefore()),
                        new DateTime(cert.getNotAfter()),
                        getTypes(certificates, cert));

            } else {
                throw new NoSuchElementException("X509Certificate with alias: " + alias);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Collection<String>> getIdentityAlias(final Long institutionId) {
        return getCertificates(institutionId)
                .map(certs -> certs.aliases
                        .stream()
                        .filter(alias -> this.cryptor.getPrivateKey(certs.keyStore, alias).hasValue())
                        .collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public Result<Collection<EntityKey>> delete(final Set<EntityKey> all) {
        return Result.tryCatch(() -> {

            final List<Long> ids = new ArrayList<>(EntityDAO.extractPKsFromKeys(all, EntityType.CERTIFICATE));

            if (ids.isEmpty()) {
                return Collections.emptyList();
            }

            this.certificateRecordMapper.deleteByExample()
                    .where(CertificateRecordDynamicSqlSupport.id, isIn(ids))
                    .build()
                    .execute();

            return ids.stream()
                    .map(id -> new EntityKey(id, EntityType.CERTIFICATE))
                    .collect(Collectors.toList());
        });
    }

    @Override
    public String extractAlias(final X509Certificate certificate) {

        try {
            final X500Name x500name = new JcaX509CertificateHolder(certificate).getSubject();
            final RDN cn = x500name.getRDNs(BCStyle.CN)[0];

            final String dn = IETFUtils.valueToString(cn.getFirst().getValue());

            if (StringUtils.isBlank(dn)) {
                return String.valueOf(certificate.getSerialNumber());
            } else {
                return dn.replace(" ", "_").toLowerCase(Locale.ENGLISH);
            }
        } catch (final Exception e) {
            log.error("Error while trying to get alias from certificate subject name: {}", e.getMessage());
            try {
                final String name = certificate.getSubjectX500Principal().getName();
                if (StringUtils.isNotBlank(name)) {
                    final String al = name.replace(" ", "").replace(",", "_").replace("=", "-");
                    log.info("Certificate import: DN name as alias: {}", al);
                    return al;
                } else {
                    final BigInteger serialNumber = certificate.getSerialNumber();
                    log.info("Certificate import: Use serial number as alias: {}", serialNumber);
                    return String.valueOf(serialNumber);
                }
            } catch (final Exception ee) {
                final BigInteger serialNumber = certificate.getSerialNumber();
                log.info("Certificate import: Use serial number as alias: {}", serialNumber);
                return String.valueOf(serialNumber);
            }
        }
    }

    private EnumSet<CertificateType> getTypes(
            final Certificates certificates,
            final X509Certificate cert) {

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

        if (keyUsage != null) {
            // digitalSignature
            if (keyUsage[0]) {
                result.add(CertificateType.DIGITAL_SIGNATURE);
            }

            // dataEncipherment
            if (keyUsage[2] || keyUsage[3]) {
                result.add(CertificateType.DATA_ENCIPHERMENT);
            }

            // keyCertSign
            if (keyUsage[5]) {
                result.add(CertificateType.KEY_CERT_SIGN);
            }
        } else {
            result.add(CertificateType.DIGITAL_SIGNATURE);
        }

        final String alias = certificates.keyStore.engineGetCertificateAlias(cert);
        if (this.cryptor.getPrivateKey(certificates.keyStore, alias).hasValue()) {
            result.add(CertificateType.DATA_ENCIPHERMENT_PRIVATE_KEY);
        }

        if (result.isEmpty()) {
            result.add(CertificateType.UNKNOWN);
        }

        return result;
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
            final Certificate certificate,
            final PrivateKey privateKey) {

        return loadCertificateStore(record.getCertStore())
                .map(store -> checkPresent(alias, store, certificate))
                .map(store -> {
                    addToStore(alias, certificate, privateKey, store);
                    return new Certificates(
                            record.getId(),
                            record.getInstitutionId(),
                            joinAliases(record.getAliases(), alias),
                            store);
                });
    }

    private void addToStore(
            final String alias,
            final Certificate certificate,
            final PrivateKey privateKey,
            final PKCS12KeyStoreSpi store) {

        try {
            // Add the certificate to the key store
            store.engineSetCertificateEntry(alias, certificate);
            // Add the private key to the key store with internal password protection
            if (privateKey != null) {
                this.cryptor.addPrivateKey(store, privateKey, alias, certificate)
                        .onError(error -> log.error("Failed to add private key for certificate: {}", alias, error));
            }

        } catch (final KeyStoreException e) {
            throw new RuntimeException("Failed to add certificate to keystore. Cause: ", e);
        }
    }

    @SuppressWarnings("unchecked")
    private PKCS12KeyStoreSpi checkPresent(
            final String alias,
            final PKCS12KeyStoreSpi store,
            final Certificate certificate) {

        if (store.engineContainsAlias(alias)) {
            throw new FieldValidationException("name", "institution:name:exists");
            //throw new RuntimeException("Alias name already exists: " + alias);
        }

        Collections.list(store.engineAliases())
                .stream()
                .forEach(key -> {
                    try {
                        final Certificate cert = store.engineGetCertificate(String.valueOf(key));
                        if (cert.equals(certificate)) {
                            throw new RuntimeException("Certificate already exists: " + key);
                        }
                    } catch (final Exception e) {
                        log.warn("Failed to check certificate duplicate for alias: {}", key);
                    }
                });

        return store;
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
                            removeAlias(record.getAliases(), alias),
                            store);
                });
    }

    private Collection<String> joinAliases(final String aliases, final String newAlias) {
        if (StringUtils.isBlank(aliases)) {
            return Arrays.asList(newAlias);
        } else {
            final Collection<String> listFromString = new ArrayList<>(Utils.getListFromString(aliases));
            listFromString.add(newAlias);
            return listFromString;
        }
    }

    private Collection<String> removeAlias(final String aliases, final String alias) {
        final Collection<String> listFromString = new ArrayList<>(Utils.getListFromString(aliases));
        listFromString.remove(alias);
        return listFromString;
    }

    private Result<Certificates> storeUpdate(final Certificates certificates) {

        return Result.tryCatch(() -> {
            final CertificateRecord record = new CertificateRecord(
                    certificates.id,
                    null,
                    StringUtils.join(certificates.aliases, Constants.COMMA),
                    storeToBinary(certificates.keyStore));

            this.certificateRecordMapper.updateByPrimaryKeySelective(record);
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
                return result.get(0);
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

    private Result<Collection<EntityDependency>> allIdsOfInstitution(final EntityKey institutionKey) {
        return Result.tryCatch(() -> this.certificateRecordMapper.selectByExample()
                .where(CertificateRecordDynamicSqlSupport.institutionId,
                        isEqualTo(Long.valueOf(institutionKey.modelId)))
                .build()
                .execute()
                .stream()
                .map(rec -> new EntityDependency(
                        institutionKey,
                        new EntityKey(rec.getId(), EntityType.CERTIFICATE),
                        rec.getAliases(),
                        rec.getAliases()))
                .collect(Collectors.toList()));
    }

}
