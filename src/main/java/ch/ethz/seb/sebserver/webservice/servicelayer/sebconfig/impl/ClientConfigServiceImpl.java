/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import ch.ethz.seb.sebserver.WebSecurityConfig;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentialService;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentials;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig.ConfigPurpose;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig.VDIType;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.CertificateDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.SEBClientConfigDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ClientConfigService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SEBConfigEncryptionContext;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SEBConfigEncryptionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ZipService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.SEBConfigEncryptionServiceImpl.EncryptionContext;
import ch.ethz.seb.sebserver.webservice.weblayer.oauth.WebserviceResourceConfiguration;

@Lazy
@Service
@WebServiceProfile
public class ClientConfigServiceImpl implements ClientConfigService {

    private static final Logger log = LoggerFactory.getLogger(ClientConfigServiceImpl.class);

    //@formatter:off
    private static final String SEB_CLIENT_CONFIG_EXAM_PROP_NAME = "exam";
    private static final String SEB_CLIENT_CONFIG_TEMPLATE_XML =
            "  <dict>%n" +
            "    <key>sebMode</key>%n" +
            "    <integer>%s</integer>%n" +                     // sebMode value
            "    <key>sebConfigPurpose</key>%n" +
            "    <integer>%s</integer>%n" +                     // sebConfigPurpose value
            "    <key>sebServerFallback</key>%n" +
            "    <%s />%n" +
            "%s" +                                              // fallback addition
            "    <key>sebServerURL</key>%n" +
            "    <string>%s</string>%n" +                       // sebServerURL value
            "    <key>sebServerConfiguration</key>%n" +
            "    <dict>%n" +
            "        <key>institution</key>%n" +
            "        <string>%s</string>%n%s" +                 // institution value
            "        <key>clientName</key>%n" +
            "        <string>%s</string>%n" +                   // client name value
            "        <key>clientSecret</key>%n" +
            "        <string>%s</string>%n" +                   // client secret value
            "        <key>apiDiscovery</key>%n" +
            "        <string>%s</string>%n" +                   // apiDiscovery value
            "        <key>pingInterval</key>%n" +
            "        <integer>%s</integer>%n" +                 // ping interval value
            "    </dict>%n" +
            "%s" +                                              // VDI additions
            "  </dict>%n";

    private final static String SEB_CLIENT_CONFIG_INTEGER_TEMPLATE =
            "    <key>%s</key>%n" +
            "    <integer>%s</integer>%n";

    private final static String SEB_CLIENT_CONFIG_STRING_TEMPLATE =
            "    <key>%s</key>%n" +
            "    <string>%s</string>%n";

    private final static String SEB_CLIENT_CONFIG_VDI_TEMPLATE =
            "    <key>enableSebBrowser</key>%n" +
            "    <false/>%n" +
            "    <key>permittedProcesses</key>%n" +
            "    <array>%n" +
            "        <dict>%n" +
            "            <key>active</key>%n" +
            "            <true/>%n" +
            "            <key>allowUserToChooseApp</key>%n" +
            "            <false/>%n" +
            "            <key>allowedExecutables</key>%n" +
            "            <string></string>%n" +
            "            <key>arguments</key>%n" +
            "            <array>%n" +
            "%s" +                                              // VDI argument additions
            "            </array>%n" +
            "            <key>autohide</key>%n" +
            "            <true/>%n" +
            "            <key>autostart</key>%n" +
            "            <true/>%n" +
            "            <key>description</key>%n" +
            "            <string>VDI View</string>%n" +
            "            <key>executable</key>%n" +
            "            <string>%s</string>%n" +               // VDI executable value
            "            <key>iconInTaskbar</key>%n" +
            "            <true/>%n" +
            "            <key>identifier</key>%n" +
            "            <string></string>%n" +
            "            <key>path</key>%n" +
            "            <string>%s</string>%n" +               // VDI path value
            "            <key>runInBackground</key>%n" +
            "            <false/>%n" +
            "            <key>strongKill</key>%n" +
            "            <false/>%n" +
            "            <key>title</key>%n" +
            "            <string>VDI View</string>%n" +
            "            <key>windowHandlingProcess</key>%n" +
            "            <string></string>%n" +
            "        </dict>%n" +
            "    </array>%n"

            ;

    private final static String SEB_CLIENT_CONFIG_VDI_ATTRIBUTE_TEMPLATE =
            "                <dict>%n" +
            "                    <key>active</key>%n "+
            "                    <true/>%n" +
            "                    <key>argument</key>%n" +
            "                    <string>%s</string>%n" +
            "                </dict>%n";

    //@formatter:on

    private final SEBClientConfigDAO sebClientConfigDAO;
    private final ClientCredentialService clientCredentialService;
    private final SEBConfigEncryptionService sebConfigEncryptionService;
    private final PasswordEncoder clientPasswordEncoder;
    private final ZipService zipService;
    private final WebserviceInfo webserviceInfo;
    private final CertificateDAO certificateDAO;
    private final long defaultPingInterval;

    protected ClientConfigServiceImpl(
            final SEBClientConfigDAO sebClientConfigDAO,
            final ClientCredentialService clientCredentialService,
            final SEBConfigEncryptionService sebConfigEncryptionService,
            final ZipService zipService,
            final WebserviceInfo webserviceInfo,
            final CertificateDAO certificateDAO,
            @Qualifier(WebSecurityConfig.CLIENT_PASSWORD_ENCODER_BEAN_NAME) final PasswordEncoder clientPasswordEncoder,
            @Value("${sebserver.webservice.api.exam.defaultPingInterval:1000}") final long defaultPingInterval) {

        this.sebClientConfigDAO = sebClientConfigDAO;
        this.clientCredentialService = clientCredentialService;
        this.sebConfigEncryptionService = sebConfigEncryptionService;
        this.zipService = zipService;
        this.clientPasswordEncoder = clientPasswordEncoder;
        this.webserviceInfo = webserviceInfo;
        this.certificateDAO = certificateDAO;
        this.defaultPingInterval = defaultPingInterval;
    }

    @Override
    public boolean hasSEBClientConfigurationForInstitution(final Long institutionId) {
        final Result<Collection<SEBClientConfig>> all = this.sebClientConfigDAO.all(institutionId, true);
        return all != null && !all.hasError() && !all.getOrThrow().isEmpty();
    }

    @Override
    public Result<ClientDetails> getClientConfigDetails(final String clientName) {
        return this.getEncodedClientConfigSecret(clientName)
                .map(pwd -> {

                    final BaseClientDetails baseClientDetails = new BaseClientDetails(
                            Utils.toString(clientName),
                            WebserviceResourceConfiguration.EXAM_API_RESOURCE_ID,
                            null,
                            Constants.OAUTH2_GRANT_TYPE_CLIENT_CREDENTIALS,
                            StringUtils.EMPTY);

                    baseClientDetails.setScope(Collections.emptySet());
                    baseClientDetails.setClientSecret(Utils.toString(pwd));
                    baseClientDetails.setAccessTokenValiditySeconds(-1); // not expiring

                    if (log.isDebugEnabled()) {
                        log.debug("Created new BaseClientDetails for id: {}", clientName);
                    }

                    return baseClientDetails;
                });
    }

    @Override
    public void exportSEBClientConfiguration(
            final OutputStream output,
            final String modelId,
            final Long examId) {

        final SEBClientConfig config = this.sebClientConfigDAO
                .byModelId(modelId).getOrThrow();

        exportSEBClientConfiguration(output, examId, config);
    }

    protected void exportSEBClientConfiguration(
            final OutputStream output,
            final Long examId,
            final SEBClientConfig config) {

        final String plainTextXMLContent = extractXMLContent(config, examId);

        PipedOutputStream pOut = null;
        PipedInputStream pIn = null;
        PipedOutputStream zipOut = null;
        PipedInputStream zipIn = null;

        try {

            final InputStream plainIn = IOUtils.toInputStream(
                    Constants.XML_VERSION_HEADER + "\n" +
                            Constants.XML_DOCTYPE_HEADER + "\n" +
                            Constants.XML_PLIST_START_V1 + "\n" +
                            plainTextXMLContent +
                            Constants.XML_PLIST_END,
                    StandardCharsets.UTF_8.name());

            pOut = new PipedOutputStream();
            pIn = new PipedInputStream(pOut);

            zipOut = new PipedOutputStream();
            zipIn = new PipedInputStream(zipOut);

            // ZIP plain text
            this.zipService.write(pOut, plainIn);

            if (StringUtils.isNotBlank(config.encryptCertificateAlias)) {
                certificateEncryption(zipOut, config, pIn);
            } else if (config.hasEncryptionSecret()) {
                passwordEncryption(zipOut, config, pIn);
            } else {
                // just add plain text header
                this.sebConfigEncryptionService.streamEncrypted(
                        zipOut,
                        pIn,
                        EncryptionContext.contextOfPlainText(config.institutionId));
            }

            // ZIP again to finish up
            this.zipService.write(output, zipIn);

            if (log.isDebugEnabled()) {
                log.debug("*** Finished SEB client configuration download streaming composition");
            }

        } catch (final Exception e) {
            log.error("Error while zip and encrypt seb client config stream: ", e);
            IOUtils.closeQuietly(pIn);
            IOUtils.closeQuietly(pOut);
            IOUtils.closeQuietly(zipIn);
            IOUtils.closeQuietly(zipOut);
        }
    }

    private SEBConfigEncryptionContext buildCertificateEncryptionContext(final SEBClientConfig config) {

        final Certificate certificate = this.certificateDAO.getCertificate(
                config.institutionId,
                String.valueOf(config.getEncryptCertificateAlias()))
                .getOrThrow();

        return EncryptionContext.contextOf(
                config.institutionId,
                (config.encryptCertificateAsym)
                        ? SEBConfigEncryptionService.Strategy.PUBLIC_KEY_HASH
                        : SEBConfigEncryptionService.Strategy.PUBLIC_KEY_HASH_SYMMETRIC_KEY,
                certificate);
    }

    private SEBConfigEncryptionContext buildPasswordEncryptionContext(final SEBClientConfig config) {
        final CharSequence encryptionPassword = this.sebClientConfigDAO
                .getConfigPasswordCipher(config.getModelId())
                .getOr((config.getConfigPurpose() == ConfigPurpose.START_EXAM) ? null : StringUtils.EMPTY);
        final CharSequence plainTextPassword = (StringUtils.isNotBlank(encryptionPassword))
                ? getPlainTextPassword(
                        encryptionPassword,
                        config.configPurpose)
                : null;
        return EncryptionContext.contextOf(
                config.institutionId,
                (config.configPurpose == ConfigPurpose.CONFIGURE_CLIENT)
                        ? SEBConfigEncryptionService.Strategy.PASSWORD_PWCC
                        : SEBConfigEncryptionService.Strategy.PASSWORD_PSWD,
                plainTextPassword);
    }

    private String extractXMLContent(final SEBClientConfig config, final Long examId) {

        final String fallbackAddition = getFallbackAddition(config);
        final String vdiAddition = getVDIAddition(config);
        final boolean hasVDI = config.vdiType != VDIType.NO;

        String examIdAddition = "";
        if (examId != null) {
            examIdAddition = String.format(
                    SEB_CLIENT_CONFIG_STRING_TEMPLATE,
                    SEB_CLIENT_CONFIG_EXAM_PROP_NAME,
                    examId);
        }

        final ClientCredentials sebClientCredentials = this.sebClientConfigDAO
                .getSEBClientCredentials(config.getModelId())
                .getOrThrow();
        final CharSequence plainClientId = sebClientCredentials.clientId;
        final CharSequence plainClientSecret = this.clientCredentialService
                .getPlainClientSecret(sebClientCredentials)
                .getOrThrow();

        final String plainTextConfig = String.format(
                SEB_CLIENT_CONFIG_TEMPLATE_XML,
                hasVDI ? "2" : "1",
                config.configPurpose.ordinal(),
                (StringUtils.isNotBlank(config.fallbackStartURL))
                        ? "true"
                        : "false",
                fallbackAddition,
                this.webserviceInfo.getExternalServerURL(),
                config.institutionId,
                examIdAddition,
                plainClientId,
                plainClientSecret,
                this.webserviceInfo.getDiscoveryEndpoint(),
                (config.sebServerPingTime != null) ? config.sebServerPingTime : this.defaultPingInterval,
                vdiAddition);

        if (log.isDebugEnabled()) {
            log.debug("SEB client configuration export:\n {}", plainTextConfig);
        }

        return plainTextConfig;
    }

    private String getVDIAddition(final SEBClientConfig config) {
        String vdiAddition = "";
        if (config.vdiType != VDIType.NO) {
            vdiAddition = String.format(
                    SEB_CLIENT_CONFIG_VDI_TEMPLATE,
                    getVDIArguments(config),
                    config.vdiExecutable,
                    config.vdiPath);
        }
        return vdiAddition;
    }

    private String getVDIArguments(final SEBClientConfig config) {
        String arguments = "";
        if (StringUtils.isNotBlank(config.vdiArguments)) {
            final String[] args = StringUtils.split(config.vdiArguments, "\n");
            for (int i = 0; i < args.length; i++) {
                arguments += getVDIArgument(args[i]);
            }
        }
        return arguments;
    }

    private String getVDIArgument(final String attributeValue) {
        return String.format(
                SEB_CLIENT_CONFIG_VDI_ATTRIBUTE_TEMPLATE,
                attributeValue);
    }

    private String getFallbackAddition(final SEBClientConfig config) {
        String fallbackAddition = "";
        if (BooleanUtils.isTrue(config.fallback)) {

            fallbackAddition += String.format(
                    SEB_CLIENT_CONFIG_STRING_TEMPLATE,
                    SEBClientConfig.ATTR_FALLBACK_START_URL,
                    config.fallbackStartURL);

            fallbackAddition += String.format(
                    SEB_CLIENT_CONFIG_INTEGER_TEMPLATE,
                    SEBClientConfig.ATTR_FALLBACK_TIMEOUT,
                    config.fallbackTimeout);

            fallbackAddition += String.format(
                    SEB_CLIENT_CONFIG_INTEGER_TEMPLATE,
                    SEBClientConfig.ATTR_FALLBACK_ATTEMPTS,
                    config.fallbackAttempts);

            fallbackAddition += String.format(
                    SEB_CLIENT_CONFIG_INTEGER_TEMPLATE,
                    SEBClientConfig.ATTR_FALLBACK_ATTEMPT_INTERVAL,
                    config.fallbackAttemptInterval);

            if (StringUtils.isNotBlank(config.fallbackPassword)) {
                final CharSequence decrypt = this.clientCredentialService
                        .decrypt(config.fallbackPassword)
                        .getOrThrow();
                fallbackAddition += String.format(
                        SEB_CLIENT_CONFIG_STRING_TEMPLATE,
                        SEBClientConfig.ATTR_FALLBACK_PASSWORD,
                        Utils.hash_SHA_256_Base_16(decrypt));
            }

            if (StringUtils.isNotBlank(config.quitPassword)) {
                final CharSequence decrypt = this.clientCredentialService
                        .decrypt(config.quitPassword)
                        .getOrThrow();
                fallbackAddition += String.format(
                        SEB_CLIENT_CONFIG_STRING_TEMPLATE,
                        SEBClientConfig.ATTR_QUIT_PASSWORD,
                        Utils.hash_SHA_256_Base_16(decrypt));
            }
        }
        return fallbackAddition;
    }

    @Override
    public boolean checkAccess(final SEBClientConfig config) {
        if (!config.isActive()) {
            return false;
        }

        try {
            final RestTemplate restTemplate = new RestTemplate();
            String externalServerURL = this.webserviceInfo.getExternalServerURL() +
                    API.OAUTH_TOKEN_ENDPOINT;

            final String lmsExternalAddressAlias = this.webserviceInfo.getLmsExternalAddressAlias(externalServerURL);
            if (StringUtils.isNotBlank(lmsExternalAddressAlias)) {
                externalServerURL = lmsExternalAddressAlias;
            }

            final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            final ClientCredentials credentials = this.sebClientConfigDAO
                    .getSEBClientCredentials(config.getModelId())
                    .getOrThrow();
            final CharSequence plainClientSecret = this.clientCredentialService
                    .getPlainClientSecret(credentials)
                    .getOrThrow();
            final String basicAuth = credentials.clientId +
                    String.valueOf(Constants.COLON) +
                    plainClientSecret;
            final String encoded = Base64.getEncoder()
                    .encodeToString(basicAuth.getBytes(StandardCharsets.UTF_8));

            headers.add(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
            final HttpEntity<String> entity = new HttpEntity<>(
                    "grant_type=client_credentials&scope=read write",
                    headers);

            final ResponseEntity<String> exchange = restTemplate.exchange(
                    externalServerURL,
                    HttpMethod.POST,
                    entity,
                    String.class);

            if (exchange.getStatusCode().value() == HttpStatus.OK.value()) {
                return true;
            } else {
                log.warn("Failed to check access SEBClientConfig {} response: {}", config, exchange.getStatusCode());
                return false;
            }
        } catch (final Exception e) {
            log.warn("Failed to check access for SEBClientConfig: {} cause: {}", config, e.getMessage());
            return false;
        }
    }

    @Override
    public void initalCheckAccess(final SEBClientConfig config) {
        checkAccess(config);
    }

    private void certificateEncryption(
            final OutputStream out,
            final SEBClientConfig config,
            final InputStream in) {

        if (log.isDebugEnabled()) {
            log.debug("*** SEB client configuration with certificate based encryption");
        }

        final boolean withPasswordEncryption = config.hasEncryptionSecret();

        PipedOutputStream streamOut = null;
        PipedInputStream streamIn = null;
        try {
            streamOut = new PipedOutputStream();
            streamIn = new PipedInputStream(streamOut);

            if (withPasswordEncryption) {
                // encrypt with password first
                passwordEncryption(streamOut, config, in);
            } else {
                // just add plaintext header
                this.sebConfigEncryptionService.streamEncrypted(
                        streamOut,
                        in,
                        EncryptionContext.contextOfPlainText(config.institutionId));
            }

            this.sebConfigEncryptionService.streamEncrypted(
                    out,
                    streamIn,
                    buildCertificateEncryptionContext(config));

        } catch (final Exception e) {
            log.error("Unexpected error while tying to stream certificate encrypted config: ", e);
            IOUtils.closeQuietly(streamOut);
            IOUtils.closeQuietly(streamIn);
        }
    }

    private void passwordEncryption(
            final OutputStream output,
            final SEBClientConfig config,
            final InputStream input) {

        if (log.isDebugEnabled()) {
            log.debug("*** SEB client configuration with password based encryption");
        }

        this.sebConfigEncryptionService.streamEncrypted(
                output,
                input,
                buildPasswordEncryptionContext(config));
    }

    private CharSequence getPlainTextPassword(
            final CharSequence encryptionPassword,
            final ConfigPurpose configPurpose) {

        CharSequence plainTextPassword = (encryptionPassword == StringUtils.EMPTY)
                ? StringUtils.EMPTY
                : this.clientCredentialService
                        .decrypt(encryptionPassword)
                        .getOrThrow();

        if (configPurpose == ConfigPurpose.CONFIGURE_CLIENT && plainTextPassword != StringUtils.EMPTY) {
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-256");
                final byte[] hash = digest.digest(
                        plainTextPassword.toString().getBytes(StandardCharsets.UTF_8));
                final byte[] encode = Hex.encode(hash);
                plainTextPassword = new String(encode, StandardCharsets.UTF_8);

            } catch (final NoSuchAlgorithmException e) {
                log.error("Failed to generate password hash for config encryption.", e);
                plainTextPassword = StringUtils.EMPTY;
            }
        }
        return plainTextPassword;
    }

    /** Get a encoded clientSecret for the SEBClientConfiguration with specified clientId/clientName.
     *
     * @param clientId the clientId/clientName
     * @return encoded clientSecret for that SEBClientConfiguration with clientId or null of not existing */
    private Result<CharSequence> getEncodedClientConfigSecret(final String clientId) {
        return this.sebClientConfigDAO.getConfigPasswordCipherByClientName(clientId)
                .map(cipher -> this.clientPasswordEncoder
                        .encode(this.clientCredentialService
                                .decrypt(cipher)
                                .getOrThrow()));
    }

}
