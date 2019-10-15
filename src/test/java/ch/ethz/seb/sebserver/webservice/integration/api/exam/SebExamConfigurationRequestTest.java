/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.exam;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.jdbc.Sql;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.ClientConnectionDataInternal;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.ExamSessionCacheService;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
public class SebExamConfigurationRequestTest extends ExamAPIIntegrationTester {

    private static final long EXAM_ID = 2L;

    @Test
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
    public void testGetExamConfigOnAFullyEstablishedConnection() throws Exception {
        final String accessToken = super.obtainAccessToken("test", "test", "SEBClient");
        assertNotNull(accessToken);

        final MockHttpServletResponse createConnection = super.createConnection(accessToken, 1L, null);
        assertNotNull(createConnection);

        final String connectionToken = createConnection.getHeader(API.EXAM_API_SEB_CONNECTION_TOKEN);
        assertNotNull(connectionToken);

        final MockHttpServletResponse establishedConnection = super.establishConnection(
                accessToken,
                connectionToken,
                EXAM_ID,
                "userSessionId");

        // check correct response
        assertTrue(HttpStatus.OK.value() == establishedConnection.getStatus());

        // try to download Exam Configuration
        final MockHttpServletResponse configResponse = super.getExamConfig(
                accessToken,
                connectionToken,
                null);

        // check correct response
        assertTrue(HttpStatus.OK.value() == configResponse.getStatus());

        final String contentAsString = configResponse.getContentAsString();
        assertEquals(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?><!DOCTYPE plist PUBLIC \"-//Apple Computer//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\"><plist version=\"1.0\"><dict><key>allowAudioCapture</key><false /><key>allowBrowsingBackForward</key><false /><key>allowDictionaryLookup</key><false /><key>allowDisplayMirroring</key><false /><key>allowDownUploads</key><true /><key>allowedDisplayBuiltin</key><true /><key>allowedDisplaysMaxNumber</key><integer>1</integer><key>allowFlashFullscreen</key><false /><key>allowPDFPlugIn</key><true /><key>allowPreferencesWindow</key><true /><key>allowQuit</key><true /><key>allowScreenSharing</key><false /><key>allowSiri</key><false /><key>allowSpellCheck</key><false /><key>allowSpellCheckDictionary</key><array><string>da-DK</string><string>en-AU</string><string>en-GB</string><string>en-US</string><string>es-ES</string><string>fr-FR</string><string>pt-PT</string><string>sv-SE</string><string>sv-FI</string></array><key>allowSwitchToApplications</key><false /><key>allowUserAppFolderInstall</key><false /><key>allowVideoCapture</key><false /><key>allowVirtualMachine</key><false /><key>allowWlan</key><false /><key>audioControlEnabled</key><false /><key>audioMute</key><false /><key>audioSetVolumeLevel</key><false /><key>audioVolumeLevel</key><integer>25</integer><key>blacklistURLFilter</key><string /><key>blockPopUpWindows</key><false /><key>browserMessagingPingTime</key><integer>120000</integer><key>browserMessagingSocket</key><string>ws:\\\\localhost:8706</string><key>browserScreenKeyboard</key><false /><key>browserUserAgent</key><string /><key>browserUserAgentMac</key><integer>0</integer><key>browserUserAgentMacCustom</key><string /><key>browserUserAgentWinDesktopMode</key><integer>0</integer><key>browserUserAgentWinDesktopModeCustom</key><string /><key>browserUserAgentWinTouchMode</key><integer>0</integer><key>browserUserAgentWinTouchModeCustom</key><string /><key>browserViewMode</key><integer>0</integer><key>browserWindowAllowReload</key><true /><key>browserWindowTitleSuffix</key><string /><key>chooseFileToUploadPolicy</key><integer>0</integer><key>createNewDesktop</key><true /><key>createNewDesktop</key><true /><key>detectStoppedProcess</key><true /><key>downloadAndOpenSebConfig</key><true /><key>downloadDirectoryOSX</key><string /><key>downloadDirectoryWin</key><string /><key>downloadPDFFiles</key><true /><key>enableAltEsc</key><false /><key>enableAltF4</key><false /><key>enableAltMouseWheel</key><false /><key>enableAltTab</key><true /><key>enableAppSwitcherCheck</key><true /><key>enableBrowserWindowToolbar</key><false /><key>enableCtrlEsc</key><false /><key>enableEsc</key><false /><key>enableF1</key><false /><key>enableF10</key><false /><key>enableF11</key><false /><key>enableF12</key><false /><key>enableF2</key><false /><key>enableF3</key><false /><key>enableF4</key><false /><key>enableF5</key><false /><key>enableF6</key><false /><key>enableF7</key><false /><key>enableF8</key><false /><key>enableF9</key><false /><key>enableJava</key><false /><key>enableJavaScript</key><true /><key>enableLogging</key><false /><key>enablePlugIns</key><true /><key>enablePrintScreen</key><false /><key>enablePrivateClipboard</key><true /><key>enableRightMouse</key><false /><key>enableSebBrowser</key><true /><key>enableStartMenu</key><false /><key>enableTouchExit</key><false /><key>enableZoomPage</key><true /><key>enableZoomText</key><true /><key>exitKey1</key><integer>2</integer><key>exitKey2</key><integer>10</integer><key>exitKey3</key><integer>5</integer><key>forceAppFolderInstall</key><true /><key>hashedAdminPassword</key><string /><key>hashedQuitPassword</key><string /><key>hideBrowserWindowToolbar</key><false /><key>hookKeys</key><true /><key>ignoreExitKeys</key><false /><key>insideSebEnableChangeAPassword</key><false /><key>insideSebEnableEaseOfAccess</key><false /><key>insideSebEnableLockThisComputer</key><false /><key>insideSebEnableLogOff</key><false /><key>insideSebEnableNetworkConnectionSelector</key><false /><key>insideSebEnableShutDown</key><false /><key>insideSebEnableStartTaskManager</key><false /><key>insideSebEnableSwitchUser</key><false /><key>insideSebEnableVmWareClientShade</key><false /><key>killExplorerShell</key><false /><key>killExplorerShell</key><false /><key>logDirectoryOSX</key><string>~/Documents</string><key>logDirectoryWin</key><string /><key>logLevel</key><integer>1</integer><key>mainBrowserWindowHeight</key><string>100%</string><key>mainBrowserWindowPositioning</key><integer>1</integer><key>mainBrowserWindowWidth</key><string>100%</string><key>minMacOSVersion</key><integer>0</integer><key>monitorProcesses</key><false /><key>newBrowserWindowAllowReload</key><true /><key>newBrowserWindowByLinkBlockForeign</key><false /><key>newBrowserWindowByLinkHeight</key><string>100%</string><key>newBrowserWindowByLinkPolicy</key><integer>2</integer><key>newBrowserWindowByLinkPositioning</key><integer>2</integer><key>newBrowserWindowByLinkWidth</key><string>100%</string><key>newBrowserWindowByScriptBlockForeign</key><false /><key>newBrowserWindowByScriptPolicy</key><integer>2</integer><key>newBrowserWindowNavigation</key><true /><key>newBrowserWindowShowReloadWarning</key><false /><key>openDownloads</key><false /><key>originatorVersion</key><string>SEB_Server_0.3.0</string><key>permittedProcesses</key><array /><key>prohibitedProcesses</key><array /><key>proxies</key><dict><key>AutoConfigurationEnabled</key><false /><key>AutoConfigurationJavaScript</key><string /><key>AutoConfigurationURL</key><string /><key>AutoDiscoveryEnabled</key><false /><key>ExceptionsList</key><array></array><key>ExcludeSimpleHostnames</key><false /><key>FTPEnable</key><false /><key>FTPPassive</key><true /><key>FTPPassword</key><string /><key>FTPPort</key><integer>21</integer><key>FTPProxy</key><string /><key>FTPRequiresPassword</key><false /><key>FTPUsername</key><string /><key>HTTPEnable</key><false /><key>HTTPPassword</key><string /><key>HTTPPort</key><integer>80</integer><key>HTTPProxy</key><string /><key>HTTPRequiresPassword</key><false /><key>HTTPSEnable</key><false /><key>HTTPSPassword</key><string /><key>HTTPSPort</key><integer>443</integer><key>HTTPSProxy</key><string /><key>HTTPSRequiresPassword</key><false /><key>HTTPSUsername</key><string /><key>HTTPUsername</key><string /><key>RTSPEnable</key><false /><key>RTSPPassword</key><string /><key>RTSPPort</key><integer>1080</integer><key>RTSPProxy</key><string /><key>RTSPRequiresPassword</key><false /><key>RTSPUsername</key><string /><key>SOCKSEnable</key><false /><key>SOCKSPassword</key><string /><key>SOCKSPort</key><integer>1080</integer><key>SOCKSProxy</key><string /><key>SOCKSRequiresPassword</key><false /><key>SOCKSUsername</key><string /></dict><key>proxySettingsPolicy</key><integer>0</integer><key>quitURL</key><string /><key>quitURLConfirm</key><true /><key>removeBrowserProfile</key><false /><key>removeLocalStorage</key><false /><key>restartExamPasswordProtected</key><true /><key>restartExamText</key><string /><key>restartExamURL</key><string /><key>restartExamUseStartURL</key><false /><key>sebConfigPurpose</key><integer>0</integer><key>sebServicePolicy</key><integer>2</integer><key>showInputLanguage</key><false /><key>showMenuBar</key><false /><key>showReloadButton</key><true /><key>showReloadWarning</key><true /><key>showTaskBar</key><true /><key>showTime</key><true /><key>taskBarHeight</key><integer>40</integer><key>touchOptimized</key><false /><key>URLFilterEnable</key><false /><key>URLFilterEnableContentFilter</key><false /><key>URLFilterRules</key><array /><key>useAsymmetricOnlyEncryption</key><false /><key>whitelistURLFilter</key><string /><key>zoomMode</key><integer>0</integer></dict></plist>",
                contentAsString);

        // check cache
        final Cache cache = this.cacheManager
                .getCache(ExamSessionCacheService.CACHE_NAME_SEB_CONFIG_EXAM);
        final ValueWrapper config = cache.get(EXAM_ID);
        assertNotNull(config);
    }

    @Test
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
    public void testGetExamConfigOnNoneEstablishedConnectionButExamIdExists() throws Exception {

        // If an connection was created but is not yet established, the download of a configuration should
        // work correctly as long as a examId is already defined for the connection or an examId is provided
        // by the configuration request. This tests the first case

        final String accessToken = super.obtainAccessToken("test", "test", "SEBClient");
        assertNotNull(accessToken);

        final MockHttpServletResponse createConnection = super.createConnection(accessToken, 1L, EXAM_ID);
        assertNotNull(createConnection);

        // check correct response
        assertTrue(HttpStatus.OK.value() == createConnection.getStatus());

        final String connectionToken = createConnection.getHeader(API.EXAM_API_SEB_CONNECTION_TOKEN);
        assertNotNull(connectionToken);

        // in this state the connection is created but no examId is set

        // try to download Exam Configuration
        final MockHttpServletResponse configResponse = super.getExamConfig(
                accessToken,
                connectionToken,
                null);

        // check correct response
        assertTrue(HttpStatus.OK.value() == configResponse.getStatus());

        // check error
        final String contentAsString = configResponse.getContentAsString();
        assertNotNull(contentAsString);
        assertTrue(contentAsString.startsWith("<?xml version=\"1.0\""));

        // check connection cache
        final Cache connectionCache = this.cacheManager
                .getCache(ExamSessionCacheService.CACHE_NAME_ACTIVE_CLIENT_CONNECTION);
        final ValueWrapper connection = connectionCache.get(connectionToken);
        assertNotNull(connection);
        final ClientConnectionDataInternal ccdi =
                (ClientConnectionDataInternal) connectionCache.get(connectionToken).get();
        assertNotNull(ccdi);
        assertNotNull(ccdi.clientConnection.examId);

        // check config cache
        final Cache cache = this.cacheManager
                .getCache(ExamSessionCacheService.CACHE_NAME_SEB_CONFIG_EXAM);
        final ValueWrapper config = cache.get(EXAM_ID);
        assertNotNull(config);
    }

    @Test
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
    public void testGetExamConfigOnNoneEstablishedConnectionProvidingExamId() throws Exception {

        // If an connection was created but is not yet established, the download of a configuration should
        // work correctly as long as a examId is already defined for the connection or an examId is provided
        // by the configuration request. This tests the second case

        final String accessToken = super.obtainAccessToken("test", "test", "SEBClient");
        assertNotNull(accessToken);

        final MockHttpServletResponse createConnection = super.createConnection(accessToken, 1L, null);
        assertNotNull(createConnection);

        // check correct response
        assertTrue(HttpStatus.OK.value() == createConnection.getStatus());

        final String connectionToken = createConnection.getHeader(API.EXAM_API_SEB_CONNECTION_TOKEN);
        assertNotNull(connectionToken);

        // in this state the connection is created but no examId is set

        // try to download Exam Configuration
        final MockHttpServletResponse configResponse = super.getExamConfig(
                accessToken,
                connectionToken,
                EXAM_ID);

        // check correct response
        assertTrue(HttpStatus.OK.value() == configResponse.getStatus());

        // check error
        final String contentAsString = configResponse.getContentAsString();
        assertNotNull(contentAsString);
        assertTrue(contentAsString.startsWith("<?xml version=\"1.0\""));

        // check connection cache
        final Cache connectionCache = this.cacheManager
                .getCache(ExamSessionCacheService.CACHE_NAME_ACTIVE_CLIENT_CONNECTION);
        final ValueWrapper connection = connectionCache.get(connectionToken);
        assertNotNull(connection);
        final ClientConnectionDataInternal ccdi =
                (ClientConnectionDataInternal) connectionCache.get(connectionToken).get();
        assertNotNull(ccdi);
        assertNotNull(ccdi.clientConnection.examId);

        // check config cache
        final Cache cache = this.cacheManager
                .getCache(ExamSessionCacheService.CACHE_NAME_SEB_CONFIG_EXAM);
        final ValueWrapper config = cache.get(EXAM_ID);
        assertNotNull(config);

        // check connection has examId

        final MockHttpServletResponse establishConnectionResponse = super.establishConnection(
                accessToken, connectionToken, null, "test");
        assertTrue(HttpStatus.OK.value() == establishConnectionResponse.getStatus());
    }

    @Test
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
    public void testGetExamConfigOnNoneEstablishedConnectionNoneExamId() throws Exception {

        final String accessToken = super.obtainAccessToken("test", "test", "SEBClient");
        assertNotNull(accessToken);

        final MockHttpServletResponse createConnection = super.createConnection(accessToken, 1L, null);
        assertNotNull(createConnection);

        // check correct response
        assertTrue(HttpStatus.OK.value() == createConnection.getStatus());

        final String connectionToken = createConnection.getHeader(API.EXAM_API_SEB_CONNECTION_TOKEN);
        assertNotNull(connectionToken);

        // in this state the connection is created but no examId is set

        // try to download Exam Configuration
        final MockHttpServletResponse configResponse = super.getExamConfig(
                accessToken,
                connectionToken,
                null);

        // check correct response
        assertTrue(HttpStatus.OK.value() != configResponse.getStatus());

        // check error
        final String contentAsString = configResponse.getContentAsString();
        assertEquals(
                "{\"messageCode\":\"0\",\"systemMessage\":\"Generic error message\",\"details\":\"Missing exam identider or requested exam is not running\",\"attributes\":[]}",
                contentAsString);

        // check connection cache
        final Cache connectionCache = this.cacheManager
                .getCache(ExamSessionCacheService.CACHE_NAME_ACTIVE_CLIENT_CONNECTION);
        final ValueWrapper connection = connectionCache.get(connectionToken);
        assertNotNull(connection);
        final ClientConnectionDataInternal ccdi =
                (ClientConnectionDataInternal) connectionCache.get(connectionToken).get();
        assertNotNull(ccdi);
        assertNull(ccdi.clientConnection.examId);
        assertTrue(ccdi.indicatorValues.isEmpty());

        // check config cache
        final Cache cache = this.cacheManager
                .getCache(ExamSessionCacheService.CACHE_NAME_SEB_CONFIG_EXAM);
        final ValueWrapper config = cache.get(EXAM_ID);
        assertNull(config);
    }

    @Test
    @Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
    public void testGetExamConfigOnConnectionNoExamIdSouldFail() throws Exception {
        final String accessToken = super.obtainAccessToken("test", "test", "SEBClient");
        assertNotNull(accessToken);

        final MockHttpServletResponse createConnection = super.createConnection(accessToken, 1L, null);
        assertNotNull(createConnection);

        final String connectionToken = createConnection.getHeader(API.EXAM_API_SEB_CONNECTION_TOKEN);
        assertNotNull(connectionToken);

        // try to download Exam Configuration
        final MockHttpServletResponse configResponse = super.getExamConfig(
                accessToken,
                connectionToken,
                null);

        // check correct response
        assertTrue(HttpStatus.OK.value() != configResponse.getStatus());
        final String contentAsString = configResponse.getContentAsString();
        assertEquals(
                "{\"messageCode\":\"0\",\"systemMessage\":\"Generic error message\",\"details\":\"Missing exam identider or requested exam is not running\",\"attributes\":[]}",
                contentAsString);
    }

}
