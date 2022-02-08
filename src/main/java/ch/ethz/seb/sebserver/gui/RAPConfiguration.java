/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.application.AbstractEntryPoint;
import org.eclipse.rap.rwt.application.Application;
import org.eclipse.rap.rwt.application.ApplicationConfiguration;
import org.eclipse.rap.rwt.application.EntryPoint;
import org.eclipse.rap.rwt.application.EntryPointFactory;
import org.eclipse.rap.rwt.client.WebClient;
import org.eclipse.rap.rwt.internal.theme.ThemeUtil;
import org.eclipse.rap.rwt.service.ResourceLoader;
import org.eclipse.rap.rwt.service.ServiceManager;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

//import com.eclipsesource.rap.aria.Aria;

import ch.ethz.seb.sebserver.gui.service.remote.download.DownloadService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.AuthorizationContextHolder;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.SEBServerAuthorizationContext;

public class RAPConfiguration implements ApplicationConfiguration {

    public static final String ATTR_USER_SESSION_TIMEOUT = "sebserver.gui.session.timeout";
    private static final String DEFAULT_THEME_NAME = "sebserver";
    private static final Logger log = LoggerFactory.getLogger(RAPConfiguration.class);

    @Override
    public void configure(final Application application) {
        try {

            final String guiEntrypoint = StaticApplicationPropertyResolver
                    .getProperty("sebserver.gui.entrypoint", "/gui");
            final String proctoringEntrypoint = StaticApplicationPropertyResolver
                    .getProperty("sebserver.gui.remote.proctoring.entrypoint", "/remote-proctoring");

            application.addStyleSheet(DEFAULT_THEME_NAME, "resource/theme/default.css");
            application.addStyleSheet(DEFAULT_THEME_NAME, "static/css/sebserver.css");
            application.addStyleSheet("sms", "resource/theme/default.css");
            application.addStyleSheet("sms", "static/css/sms.css");

            application.addResource("fav_icon", new ResourceLoader() {
                @Override
                public InputStream getResourceAsStream(final String resourceName) throws IOException {
                    return RAPConfiguration.class.getClassLoader().getResourceAsStream("static/images/fav_icon.png");
                }
            });

            final Map<String, String> properties = new HashMap<>();
            properties.put(WebClient.PAGE_TITLE, "SEB Server");
            properties.put(WebClient.BODY_HTML, "<big>Loading Application<big>");
            properties.put(WebClient.THEME_ID, DEFAULT_THEME_NAME);
            properties.put(WebClient.FAVICON, "fav_icon");

            application.addEntryPoint(guiEntrypoint, new RAPSpringEntryPointFactory(), properties);

            properties.put(WebClient.PAGE_TITLE, "SEB Server Proctoring");
            properties.put(WebClient.BODY_HTML, "<big>Loading Application<big>");
            properties.put(WebClient.THEME_ID, DEFAULT_THEME_NAME);
            properties.put(WebClient.FAVICON, "fav_icon");

            application.addEntryPoint(proctoringEntrypoint, new RAPRemoteProcotringEntryPointFactory(), properties);

        } catch (final RuntimeException re) {
            throw re;
        } catch (final Exception e) {
            log.error("Error during CSS parsing. Please check the custom CSS files for errors.", e);
        }
    }

    public interface EntryPointService {

        void loadLoginPage(final Composite parent);

        void loadMainPage(final Composite parent);

        void loadProctoringView(Composite parent);

    }

    public static final class RAPRemoteProcotringEntryPointFactory implements EntryPointFactory {

        @Override
        public EntryPoint create() {
            return new AbstractEntryPoint() {

                private static final long serialVersionUID = -1299125117752916270L;

                @Override
                protected void createContents(final Composite parent) {

                    final HttpSession httpSession = RWT
                            .getUISession(parent.getDisplay())
                            .getHttpSession();

                    final WebApplicationContext webApplicationContext = getWebApplicationContext(httpSession);
                    final boolean authenticated = isAuthenticated(httpSession, webApplicationContext);
                    if (authenticated) {
                        final EntryPointService entryPointService = webApplicationContext
                                .getBean(EntryPointService.class);
                        entryPointService.loadProctoringView(parent);
                    } else {
                        final HttpServletResponse response = RWT.getResponse();
                        response.setStatus(HttpStatus.FORBIDDEN.value());
                    }
                }
            };
        }
    }

    public static final class RAPSpringEntryPointFactory implements EntryPointFactory {

        private boolean initialized = false;

        @Override
        public EntryPoint create() {

            return new AbstractEntryPoint() {

                private static final long serialVersionUID = -1299125117752916270L;

                @Override
                protected void createContents(final Composite parent) {

                    final HttpSession httpSession = RWT
                            .getUISession(parent.getDisplay())
                            .getHttpSession();

                    log.debug("Create new GUI entrypoint. HttpSession: " + httpSession);
                    if (httpSession == null) {
                        log.error("HttpSession not available from RWT.getUISession().getHttpSession()");
                        throw new IllegalStateException(
                                "HttpSession not available from RWT.getUISession().getHttpSession()");
                    }

                    final Object themeId = httpSession.getAttribute("themeId");
                    if (themeId != null) {
                        ThemeUtil.setCurrentThemeId(RWT.getUISession(parent.getDisplay()), String.valueOf(themeId));
                        parent.redraw();
                        parent.layout(true);
                        parent.redraw();

                    }

                    final WebApplicationContext webApplicationContext = getWebApplicationContext(httpSession);
                    initSpringBasedRAPServices(webApplicationContext);

                    final Environment environment = webApplicationContext.getBean(Environment.class);
                    if (environment != null) {

                        final Integer sessionTimeout = environment.getProperty(
                                ATTR_USER_SESSION_TIMEOUT,
                                Integer.class,
                                18000);

                        httpSession.setMaxInactiveInterval(sessionTimeout);
                    } else {
                        httpSession.setMaxInactiveInterval(18000);
                    }

                    final EntryPointService entryPointService = webApplicationContext
                            .getBean(EntryPointService.class);

                    if (isAuthenticated(httpSession, webApplicationContext)) {
                        entryPointService.loadMainPage(parent);
                    } else {
                        entryPointService.loadLoginPage(parent);
                    }
                }
            };
        }

        private void initSpringBasedRAPServices(final WebApplicationContext webApplicationContext) {
            if (!this.initialized) {
                try {
                    final ServiceManager manager = RWT.getServiceManager();
                    final DownloadService downloadService = webApplicationContext.getBean(DownloadService.class);
                    manager.registerServiceHandler(DownloadService.DOWNLOAD_SERVICE_NAME, downloadService);

                    this.initialized = true;
                } catch (final IllegalArgumentException iae) {
                    log.warn("Failed to register DownloadService on ServiceManager. Already registered: ", iae);
                }
            }
        }
    }

    private static boolean isAuthenticated(
            final HttpSession httpSession,
            final WebApplicationContext webApplicationContext) {

        final AuthorizationContextHolder authorizationContextHolder = webApplicationContext
                .getBean(AuthorizationContextHolder.class);
        final SEBServerAuthorizationContext authorizationContext = authorizationContextHolder
                .getAuthorizationContext(httpSession);
        return authorizationContext.isValid() && authorizationContext.isLoggedIn();
    }

    private static WebApplicationContext getWebApplicationContext(final HttpSession httpSession) {
        try {
            final ServletContext servletContext = httpSession.getServletContext();

            log.debug("Initialize Spring-Context on Servlet-Context: " + servletContext);

            return WebApplicationContextUtils
                    .getRequiredWebApplicationContext(servletContext);

        } catch (final RuntimeException e) {
            log.error("Failed to initialize Spring-Context on HttpSession: " + httpSession);
            throw e;
        } catch (final Exception e) {
            log.error("Failed to initialize Spring-Context on HttpSession: " + httpSession);
            throw new RuntimeException("Failed to initialize Spring-Context on HttpSession: " + httpSession);
        }
    }
}
