/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.user.UserFeatures;
import ch.ethz.seb.sebserver.gbl.model.exam.SPSAPIAccessData;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.FeatureService;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.WebserviceInfoDAO;

@Lazy
@Service
@WebServiceProfile
public class WebserviceInfo {

    private static final Logger log = LoggerFactory.getLogger(WebserviceInfo.class);

    private static final String VERSION_KEY = "sebserver.version";
    private static final String WEB_SERVICE_TEST_PROPERTY = "sebserver.test.property";
    private static final String WEB_SERVICE_SERVER_NAME_KEY = "sebserver.webservice.http.external.servername";
    private static final String WEB_SERVICE_HTTP_SCHEME_KEY = "sebserver.webservice.http.external.scheme";
    private static final String WEB_SERVICE_HTTP_PORT = "sebserver.webservice.http.external.port";
    private static final String WEB_SERVICE_HOST_ADDRESS_KEY = "server.address";
    private static final String WEB_SERVICE_SERVER_PORT_KEY = "server.port";
    private static final String WEB_SERVICE_EXAM_API_DISCOVERY_ENDPOINT_KEY =
            "sebserver.webservice.api.exam.endpoint.discovery";
    private static final String WEB_SERVICE_EXTERNAL_ADDRESS_ALIAS = "sebserver.webservice.lms.address.alias";
    private static final String WEB_SERVICE_CONTEXT_PATH = "server.servlet.context-path";

    private final String sebServerVersion;
    private final String testProperty;
    private final String httpScheme;
    private final String hostAddress; // internal
    private final String webserverName; // external
    private final String serverPort; // internal
    private final String webserverPort; // external
    private final String discoveryEndpoint;
    private final String contextPath;

    private final boolean isLightSetup;
    private final String serverURLPrefix;
    private final boolean isDistributed;
    private final String webserviceUUID;

    private final long distributedUpdateInterval;

    private Map<String, String> lmsExternalAddressAlias;
    private final Set<String> activeProfiles;

    private final WebserviceInfoDAO webserviceInfoDAO;
    private final FeatureService featureService;
    private boolean isMaster = false;

    @Value("${sebserver.webservice.api.admin.accessTokenValiditySeconds:3600}")
    private int adminAccessTokenValSec;
    @Value("${sebserver.webservice.api.admin.refreshTokenValiditySeconds:-1}")
    private int adminRefreshTokenValSec;
    @Value("${sebserver.webservice.api.exam.accessTokenValiditySeconds:43200}")
    private int examAPITokenValiditySeconds;

    private final ScreenProctoringServiceBundle screenProctoringServiceBundle;

    public WebserviceInfo(
            final WebserviceInfoDAO webserviceInfoDAO,
            final FeatureService featureService,
            final Environment environment,
            final Cryptor cryptor) {

        this.webserviceInfoDAO = webserviceInfoDAO;
        this.featureService = featureService;
        this.sebServerVersion = environment.getRequiredProperty(VERSION_KEY);
        this.testProperty = environment.getProperty(WEB_SERVICE_TEST_PROPERTY, "NOT_AVAILABLE");
        this.httpScheme = environment.getRequiredProperty(WEB_SERVICE_HTTP_SCHEME_KEY);
        this.hostAddress = environment.getRequiredProperty(WEB_SERVICE_HOST_ADDRESS_KEY);
        this.webserverName = environment.getProperty(WEB_SERVICE_SERVER_NAME_KEY, "");
        this.serverPort = environment.getRequiredProperty(WEB_SERVICE_SERVER_PORT_KEY);
        this.webserverPort = environment.getProperty(WEB_SERVICE_HTTP_PORT);
        this.discoveryEndpoint = environment.getRequiredProperty(WEB_SERVICE_EXAM_API_DISCOVERY_ENDPOINT_KEY);
        this.contextPath = environment.getProperty(WEB_SERVICE_CONTEXT_PATH, "");
        this.webserviceUUID = UUID.randomUUID().toString()
                + Constants.UNDERLINE
                + this.sebServerVersion;

        this.distributedUpdateInterval = environment.getProperty(
                "sebserver.webservice.distributed.updateInterval",
                Long.class,
                2000L);

        this.activeProfiles = new HashSet<>(Arrays.asList(environment.getActiveProfiles()));

        if (StringUtils.isEmpty(this.webserverName)) {
            log.warn("NOTE: External server name, property : 'sebserver.webservice.http.external.servername' "
                    + "is not set from configuration. The external server name is set to the server address!");
        }

        final UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                .scheme(this.httpScheme)
                .host((StringUtils.isNotBlank(this.webserverName))
                        ? this.webserverName
                        : this.hostAddress);
        if (StringUtils.isNotBlank(this.webserverPort)) {
            builder.port(this.webserverPort);
        }
        if (StringUtils.isNotBlank(this.contextPath) && !this.contextPath.equals("/")) {
            builder.path(this.contextPath);
        }
        this.serverURLPrefix = builder.toUriString();

        this.isLightSetup = BooleanUtils.toBoolean(environment.getProperty(
                "sebserver.webservice.light.setup",
                Constants.FALSE_STRING));
        this.isDistributed = BooleanUtils.toBoolean(environment.getProperty(
                "sebserver.webservice.distributed",
                Constants.FALSE_STRING));

        final String addressAlias = environment.getProperty(WEB_SERVICE_EXTERNAL_ADDRESS_ALIAS, "");
        if (StringUtils.isNotBlank(addressAlias)) {
            try {
                final String[] aliass = StringUtils.split(addressAlias, Constants.LIST_SEPARATOR);
                final Map<String, String> mapping = new LinkedHashMap<>();
                for (final String alias : aliass) {
                    final String[] nameValue =
                            StringUtils.split(alias, Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR);
                    mapping.put(nameValue[0], nameValue[1]);
                }
                this.lmsExternalAddressAlias = Collections.unmodifiableMap(mapping);
            } catch (final Exception e) {
                log.error("Failed to parse sebserver.webservice.lms.address.alias: ", e);
                this.lmsExternalAddressAlias = Collections.emptyMap();
            }
        } else {
            this.lmsExternalAddressAlias = Collections.emptyMap();
        }

        final boolean spsEnabled = BooleanUtils.toBoolean(environment.getProperty(
                "sebserver.feature.exam.seb.screenProctoring.enabled",
                Constants.FALSE_STRING));
        final boolean spsBundled = BooleanUtils.toBoolean(environment.getProperty(
                "sebserver.feature.exam.seb.screenProctoring.bundled",
                Constants.FALSE_STRING));
        if (spsEnabled && spsBundled) {
            this.screenProctoringServiceBundle = new ScreenProctoringServiceBundle(
                    environment.getProperty("sebserver.feature.exam.seb.screenProctoring.bundled.url"),
                    environment.getProperty("sebserver.feature.exam.seb.screenProctoring.bundled.clientId"),
                    cryptor.encrypt(
                            environment.getProperty("sebserver.feature.exam.seb.screenProctoring.bundled.clientPassword"))
                            .getOrThrow(),
                    environment.getProperty("sebserver.feature.exam.seb.screenProctoring.bundled.sebserveraccount.username"),
                    cryptor.encrypt(environment
                            .getProperty("sebserver.feature.exam.seb.screenProctoring.bundled.sebserveraccount.password"))
                            .getOrThrow());
        } else {
            this.screenProctoringServiceBundle = new ScreenProctoringServiceBundle();
        }
    }

    public Map<String, Boolean> configuredFeatures() {
        return new TreeMap<>( Arrays.stream(UserFeatures.Feature.values()).collect(Collectors.toMap(
                f -> f.featureName,
                featureService::isEnabledByConfig
        )));
    }

    public boolean isMaster() {
        return this.isMaster;
    }

    public void updateMaster() {
        this.isMaster = this.webserviceInfoDAO.isMaster(this.getWebserviceUUID());
    }

    public String getWebserviceUUID() {
        return this.webserviceUUID;
    }

    public String getSEBServerVersion() {
        return this.sebServerVersion;
    }

    public String getTestProperty() {
        return this.testProperty;
    }

    public boolean hasProfile(final String profile) {
        return this.activeProfiles.contains(profile);
    }

    public String getHttpScheme() {
        return this.httpScheme;
    }

    public String getHostAddress() {
        return this.hostAddress;
    }

    public String getWebserviceDomainName() {
        return this.webserverName;
    }

    public String getServerPort() {
        return this.serverPort;
    }

    public String getServerExternalPort() {
        return this.webserverPort;
    }

    public Object getContextPath() {
        return this.contextPath;
    }

    public String getDiscoveryEndpoint() {
        return this.discoveryEndpoint;
    }

    public String getDiscoveryEndpointAddress() {
        return this.serverURLPrefix + this.discoveryEndpoint;
    }

    public long getDistributedUpdateInterval() {
        return this.distributedUpdateInterval;
    }

    public ScreenProctoringServiceBundle getScreenProctoringServiceBundle() {
        return this.screenProctoringServiceBundle;
    }

    public String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (final UnknownHostException e) {
            log.error("Failed to get local host name: {}", e.getMessage());
            return Constants.EMPTY_NOTE;
        }
    }

    public String getLocalHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (final UnknownHostException e) {
            log.error("Failed to get local host address: {}", e.getMessage());
            return Constants.EMPTY_NOTE;
        }
    }

    public String getLoopbackHostName() {
        return InetAddress.getLoopbackAddress().getHostName();
    }

    public String getLoopbackHostAddress() {
        return InetAddress.getLoopbackAddress().getHostAddress();
    }

    /** Get the server URL prefix in the form of;
     * [scheme{http|https}]://[server-address{DNS-name|IP}]:[port]
     * <p>
     * E.g.: https://seb.server.ch:8080
     *
     * @return the server URL prefix */
    public String getExternalServerURL() {
        return this.serverURLPrefix;
    }

    public String getOAuthTokenURI() {
        return getExternalServerURL() + API.OAUTH_TOKEN_ENDPOINT;
    }

    public boolean isLightSetup() {
        return this.isLightSetup;
    }
    public boolean isDistributed() {
        return this.isDistributed;
    }

    public Map<String, String> getLmsExternalAddressAlias() {
        return this.lmsExternalAddressAlias;
    }

    public String getLmsExternalAddressAlias(final String internalAddress) {
        return this.lmsExternalAddressAlias
                .entrySet()
                .stream()
                .filter(entry -> internalAddress.contains(entry.getKey()))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(null);
    }

    public int getAdminAccessTokenValSec() {
        return this.adminAccessTokenValSec;
    }

    public int getAdminRefreshTokenValSec() {
        return this.adminRefreshTokenValSec;
    }

    public int getExamAPITokenValiditySeconds() {
        return this.examAPITokenValiditySeconds;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("WebserviceInfo [testProperty=");
        builder.append(this.testProperty);
        builder.append(", httpScheme=");
        builder.append(this.httpScheme);
        builder.append(", hostAddress=");
        builder.append(this.hostAddress);
        builder.append(", webserverName=");
        builder.append(this.webserverName);
        builder.append(", serverPort=");
        builder.append(this.serverPort);
        builder.append(", webserverPort=");
        builder.append(this.webserverPort);
        builder.append(", discoveryEndpoint=");
        builder.append(this.discoveryEndpoint);
        builder.append(", serverURLPrefix=");
        builder.append(this.serverURLPrefix);
        builder.append(", isDistributed=");
        builder.append(this.isDistributed);
        builder.append(", lmsExternalAddressAlias=");
        builder.append(this.lmsExternalAddressAlias);
        builder.append("]");
        return builder.toString();
    }



    public static final class ScreenProctoringServiceBundle implements SPSAPIAccessData {

        public final boolean bundled;
        public final String serviceURL;
        public final String clientId;
        public final CharSequence clientSecret;
        public final String apiAccountName;
        public final CharSequence apiAccountPassword;

        public ScreenProctoringServiceBundle(
                final String serviceURL,
                final String clientId,
                final CharSequence clientSecret,
                final String apiAccountName,
                final CharSequence apiAccountPassword) {

            this.bundled = true;
            this.serviceURL = serviceURL;
            this.clientId = clientId;
            this.clientSecret = clientSecret;
            this.apiAccountName = apiAccountName;
            this.apiAccountPassword = apiAccountPassword;
        }

        public ScreenProctoringServiceBundle() {
            this.bundled = false;
            this.serviceURL = null;
            this.clientId = null;
            this.clientSecret = null;
            this.apiAccountName = null;
            this.apiAccountPassword = null;
        }

        @Override
        public Long getExamId() {
            return null;
        }

        @Override
        public String getSpsServiceURL() {
            return serviceURL;
        }

        @Override
        public String getSpsAPIKey() {
            return clientId;
        }

        @Override
        public CharSequence getSpsAPISecret() {
            return clientSecret;
        }

        @Override
        public String getSpsAccountId() {
            return apiAccountName;
        }

        @Override
        public CharSequence getSpsAccountPassword() {
            return apiAccountPassword;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("ScreenProctoringServiceBundle [bundled=");
            builder.append(this.bundled);
            builder.append(", serviceURL=");
            builder.append(this.serviceURL);
            builder.append(", clientId=");
            builder.append(this.clientId);
            builder.append(", apiAccountName=");
            builder.append(this.apiAccountName);
            builder.append("]");
            return builder.toString();
        }
    }

}
