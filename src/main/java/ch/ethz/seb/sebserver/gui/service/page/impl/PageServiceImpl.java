/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import javax.servlet.http.HttpSession;

import org.eclipse.rap.rwt.RWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService;
import ch.ethz.seb.sebserver.gui.service.page.ComposerService;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageState;
import ch.ethz.seb.sebserver.gui.service.page.PageState.Type;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEvent;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionPublishEvent;
import ch.ethz.seb.sebserver.gui.service.page.event.PageEvent;
import ch.ethz.seb.sebserver.gui.service.page.event.PageEventListener;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.AuthorizationContextHolder;
import ch.ethz.seb.sebserver.gui.table.TableBuilder;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Service
@GuiProfile
public class PageServiceImpl implements PageService {

    private static final Logger log = LoggerFactory.getLogger(PageServiceImpl.class);

    private static final LocTextKey MSG_GO_AWAY_FROM_EDIT =
            new LocTextKey("sebserver.overall.action.goAwayFromEditPageConfirm");

    private static final String ATTR_PAGE_STATE = "PAGE_STATE";
    private static final ListenerComparator LIST_COMPARATOR = new ListenerComparator();

    private final JSONMapper jsonMapper;
    private final WidgetFactory widgetFactory;
    private final PolyglotPageService polyglotPageService;
    private final ResourceService resourceService;
    private final AuthorizationContextHolder authorizationContextHolder;

    public PageServiceImpl(
            final JSONMapper jsonMapper,
            final WidgetFactory widgetFactory,
            final PolyglotPageService polyglotPageService,
            final ResourceService resourceService,
            final AuthorizationContextHolder authorizationContextHolder) {

        this.jsonMapper = jsonMapper;
        this.widgetFactory = widgetFactory;
        this.polyglotPageService = polyglotPageService;
        this.resourceService = resourceService;
        this.authorizationContextHolder = authorizationContextHolder;
    }

    @Override
    public WidgetFactory getWidgetFactory() {
        return this.widgetFactory;
    }

    @Override
    public PolyglotPageService getPolyglotPageService() {
        return this.polyglotPageService;
    }

    @Override
    public AuthorizationContextHolder getAuthorizationContextHolder() {
        return this.authorizationContextHolder;
    }

    @Override
    public I18nSupport getI18nSupport() {
        return this.widgetFactory.getI18nSupport();
    }

    @Override
    public ResourceService getResourceService() {
        return this.resourceService;
    }

    @Override
    public JSONMapper getJSONMapper() {
        return this.jsonMapper;
    }

    @Override
    public PageState getCurrentState() {
        try {

            final HttpSession httpSession = RWT
                    .getUISession()
                    .getHttpSession();

            return (PageState) httpSession.getAttribute(ATTR_PAGE_STATE);

        } catch (final Exception e) {
            log.error("Failed to get current PageState: ", e);
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends PageEvent> void firePageEvent(final T event, final PageContext pageContext) {
        final Class<? extends PageEvent> typeClass = event.getClass();
        final List<PageEventListener<T>> listeners = new ArrayList<>();
        ComposerService.traversePageTree(
                pageContext.getRoot(),
                c -> {
                    final PageEventListener<?> listener =
                            (PageEventListener<?>) c.getData(PageEventListener.LISTENER_ATTRIBUTE_KEY);
                    return listener != null && listener.match(typeClass);
                },
                c -> listeners.add(((PageEventListener<T>) c.getData(PageEventListener.LISTENER_ATTRIBUTE_KEY))));

        if (listeners.isEmpty()) {
            return;
        }

        listeners.stream()
                .sorted(LIST_COMPARATOR)
                .forEach(listener -> {
                    try {
                        listener.notify(event);
                    } catch (final Exception e) {
                        log.error("Unexpected error while notify PageEventListener: ", e);
                    }
                });
    }

    @Override
    public void executePageAction(final PageAction pageAction, final Consumer<Result<PageAction>> callback) {
        final PageState currentState = getCurrentState();

        // TODO should there be a check to reload or not to reload the page if the state is the same?

        if (!pageAction.ignoreMoveAwayFromEdit && currentState != null && currentState.type() == Type.FORM_EDIT) {
            pageAction.pageContext().applyConfirmDialog(
                    MSG_GO_AWAY_FROM_EDIT,
                    confirm -> {
                        if (confirm) {
                            exec(pageAction, callback);
                        } else {
                            callback.accept(Result.ofRuntimeError("Confirm denied"));
                        }
                    });
        } else {
            exec(pageAction, callback);
        }
    }

    private void exec(final PageAction pageAction, final Consumer<Result<PageAction>> callback) {
        pageAction.applyAction(result -> {
            if (!result.hasError()) {
                final PageAction action = result.get();
                if (pageAction.fireActionEvent) {
                    firePageEvent(new ActionEvent(action), action.pageContext());
                }

                try {

                    final HttpSession httpSession = RWT
                            .getUISession()
                            .getHttpSession();

                    log.debug("SET session PageState: {} : {}", pageAction.definition.targetState, httpSession.getId());
                    httpSession.setAttribute(ATTR_PAGE_STATE, pageAction.definition.targetState);

                } catch (final Exception e) {
                    log.error("Failed to set current PageState: ", e);
                }

            }
            callback.accept(result);
        });

    }

    @Override
    public void publishAction(final PageAction pageAction) {
        this.firePageEvent(new ActionPublishEvent(pageAction), pageAction.pageContext());
    }

    @Override
    public FormBuilder formBuilder(final PageContext pageContext, final int rows) {
        return new FormBuilder(this, pageContext, rows);
    }

    @Override
    public <T extends Entity> TableBuilder<T> entityTableBuilder(final RestCall<Page<T>> apiCall) {
        return new TableBuilder<>(this, apiCall);
    }

    @Override
    public void logout(final PageContext pageContext) {
        this.clearState();

        try {
            final boolean logoutSuccessful = this.authorizationContextHolder
                    .getAuthorizationContext()
                    .logout();

            if (!logoutSuccessful) {
                // TODO error handling
            }

        } catch (final Exception e) {
            log.info("Cleanup logout failed: {}", e.getMessage());
        } finally {
            pageContext.forwardToLoginPage();
        }

    }

    @Override
    public void clearState() {
        try {

            final HttpSession httpSession = RWT
                    .getUISession()
                    .getHttpSession();

            log.debug("Clear session PageState: {}", httpSession.getId());
            httpSession.removeAttribute(ATTR_PAGE_STATE);

        } catch (final Exception e) {
            log.error("Failed to clear current PageState: ", e);
        }
    }

    private static final class ListenerComparator implements Comparator<PageEventListener<?>>, Serializable {

        private static final long serialVersionUID = 2571739214439340404L;

        @Override
        public int compare(final PageEventListener<?> o1, final PageEventListener<?> o2) {
            final int x = o1.priority();
            final int y = o2.priority();
            return (x < y) ? -1 : ((x == y) ? 0 : 1);
        }
    }

}
