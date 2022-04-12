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
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.servlet.http.HttpSession;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.widgets.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.Activatable;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.ModelIdAware;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService;
import ch.ethz.seb.sebserver.gui.service.page.ComposerService;
import ch.ethz.seb.sebserver.gui.service.page.MultiPageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageStateDefinition.Type;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEvent;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionPublishEvent;
import ch.ethz.seb.sebserver.gui.service.page.event.PageEvent;
import ch.ethz.seb.sebserver.gui.service.page.event.PageEventListener;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall.CallType;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.AuthorizationContextHolder;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.table.RemoteListPageSupplier;
import ch.ethz.seb.sebserver.gui.table.TableBuilder;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Service
@GuiProfile
public class PageServiceImpl implements PageService {

    private static final Logger log = LoggerFactory.getLogger(PageServiceImpl.class);

    private static final LocTextKey CONFIRM_DEACTIVATION_NO_DEP_KEY =
            new LocTextKey("sebserver.dialog.confirm.deactivation.noDependencies");
    private static final String CONFIRM_DEACTIVATION_KEY = "sebserver.dialog.confirm.deactivation";
    private static final LocTextKey MSG_GO_AWAY_FROM_EDIT =
            new LocTextKey("sebserver.overall.action.goAwayFromEditPageConfirm");

    private static final String ATTR_PAGE_STATE = "PAGE_STATE";
    private static final ListenerComparator LIST_COMPARATOR = new ListenerComparator();

    private final Cryptor cryptor;
    private final JSONMapper jsonMapper;
    private final WidgetFactory widgetFactory;
    private final PolyglotPageService polyglotPageService;
    private final ResourceService resourceService;
    private final CurrentUser currentUser;

    public PageServiceImpl(
            final Cryptor cryptor,
            final JSONMapper jsonMapper,
            final WidgetFactory widgetFactory,
            final PolyglotPageService polyglotPageService,
            final ResourceService resourceService,
            final CurrentUser currentUser) {

        this.cryptor = cryptor;
        this.jsonMapper = jsonMapper;
        this.widgetFactory = widgetFactory;
        this.polyglotPageService = polyglotPageService;
        this.resourceService = resourceService;
        this.currentUser = currentUser;
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
        return this.currentUser.getAuthorizationContextHolder();
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
    public RestService getRestService() {
        if (this.resourceService == null) {
            return null;
        }

        return this.resourceService.getRestService();
    }

    @Override
    public CurrentUser getCurrentUser() {
        return this.currentUser;
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
    public FormTooltipMode getFormTooltipMode() {
        return FormTooltipMode.INPUT;
    }

    @SuppressWarnings("unchecked")
    @Override
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

    @Override
    public <T extends Entity & Activatable> Supplier<LocTextKey> confirmDeactivation(final T entity) {
        final RestService restService = this.resourceService.getRestService();
        return () -> {
            try {
                final int dependencies = restService.<Set<EntityKey>> getBuilder(
                        entity.entityType(),
                        CallType.GET_DEPENDENCIES)
                        .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(entity.getModelId()))
                        .withQueryParam(API.PARAM_BULK_ACTION_TYPE, BulkActionType.DEACTIVATE.name())
                        .call()
                        .getOrThrow()
                        .size();

                if (dependencies > 0) {
                    return new LocTextKey(CONFIRM_DEACTIVATION_KEY, String.valueOf(dependencies));
                } else {
                    return CONFIRM_DEACTIVATION_NO_DEP_KEY;
                }

            } catch (final Exception e) {
                log.warn("Failed to get dependencies. Error: {}", e.getMessage());
                return new LocTextKey(CONFIRM_DEACTIVATION_KEY, "");
            }
        };
    }

    @Override
    public <T extends Entity & Activatable> Function<PageAction, PageAction> activationToggleActionFunction(
            final EntityTable<T> table,
            final LocTextKey noSelectionText,
            final Function<PageAction, PageAction> testBeforeActivation) {

        return action -> {
            final Set<EntityKey> multiSelection = table.getMultiSelection();
            if (multiSelection == null || multiSelection.isEmpty()) {
                throw new PageMessageException(noSelectionText);
            }
            if (multiSelection.size() > 1) {
                throw new PageMessageException(MESSAGE_NO_MULTISELECTION);
            }

            final RestService restService = this.resourceService.getRestService();
            final EntityType entityType = table.getEntityType();
            final Collection<Exception> errors = new ArrayList<>();
            final T singleSelection = table.getSingleSelectedROWData();
            if (singleSelection == null) {
                throw new PageMessageException(noSelectionText);
            }

            if (!singleSelection.isActive()) {
                final RestCall<T>.RestCallBuilder restCallBuilder = restService.<T> getBuilder(
                        entityType,
                        CallType.ACTIVATION_ACTIVATE)
                        .withURIVariable(API.PARAM_MODEL_ID, singleSelection.getModelId());
                if (testBeforeActivation != null) {
                    try {
                        action.withEntityKey(singleSelection.getEntityKey());
                        testBeforeActivation.apply(action);
                        restCallBuilder
                                .call()
                                .onError(errors::add);
                    } catch (final Exception e) {
                        errors.add(e);
                    }
                } else {
                    restCallBuilder
                            .call()
                            .onError(errors::add);
                }
            } else {
                restService.<T> getBuilder(entityType, CallType.ACTIVATION_DEACTIVATE)
                        .withURIVariable(API.PARAM_MODEL_ID, singleSelection.getModelId())
                        .call()
                        .onError(errors::add);
            }

            if (!errors.isEmpty()) {
                final String entityTypeName = this.resourceService.getEntityTypeName(entityType);
                throw new MultiPageMessageException(
                        new LocTextKey(PageContext.GENERIC_ACTIVATE_ERROR_TEXT_KEY, entityTypeName),
                        errors);
            }

            return action;
        };
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

                    if (action != null &&
                            action.fireActionEvent &&
                            action.definition != null &&
                            action.definition.targetState != null) {

                        final PageState pageState = new PageState(action.definition.targetState, action);
                        if (log.isDebugEnabled()) {
                            log.debug("Set session PageState: {} : {}", pageState, httpSession.getId());
                        }
                        httpSession.setAttribute(ATTR_PAGE_STATE, pageState);
                    }
                } catch (final Exception e) {
                    log.error("Failed to set current PageState: ", e);
                }
            } else {

            }
            callback.accept(result);
        });
    }

    @Override
    public void publishAction(final PageAction pageAction, final boolean active) {
        this.firePageEvent(new ActionPublishEvent(pageAction, active), pageAction.pageContext());
    }

    @Override
    public void publishAction(final PageAction pageAction, final Consumer<TreeItem> actionConsumer) {
        this.firePageEvent(new ActionPublishEvent(pageAction, actionConsumer), pageAction.pageContext());
    }

    @Override
    public FormBuilder formBuilder(final PageContext pageContext, final int rows) {
        return new FormBuilder(this, pageContext, this.cryptor, rows);
    }

    @Override
    public <T extends ModelIdAware> TableBuilder<T> entityTableBuilder(
            final String name,
            final RestCall<Page<T>> apiCall) {

        return new TableBuilder<>(name, this, apiCall);
    }

    @Override
    public <T extends ModelIdAware> TableBuilder<T> staticListTableBuilder(
            final List<T> staticList,
            final EntityType entityType) {

        return new TableBuilder<>(
                (entityType != null)
                        ? entityType.name()
                        : "",
                this, staticList, entityType);
    }

    @Override
    public <T extends ModelIdAware> TableBuilder<T> remoteListTableBuilder(
            final RestCall<Collection<T>> apiCall,
            final EntityType entityType) {

        return new TableBuilder<>(
                (entityType != null)
                        ? entityType.name()
                        : "",
                this,
                new RemoteListPageSupplier<>(apiCall, entityType),
                entityType);
    }

    @Override
    public boolean logout(final PageContext pageContext) {
        this.clearState();

        try {
            final boolean logoutSuccessful = this.currentUser.logout();

            if (!logoutSuccessful) {
                log.warn("Failed to logout. See log-files for more information");
                return false;
            }

        } catch (final Exception e) {
            log.info("Cleanup logout failed: {}", e.getMessage(), e);
            pageContext.forwardToMainPage();
            pageContext.notifyError(new LocTextKey("sebserver.error.logout"), e);
            return false;
        }

        pageContext.forwardToLoginPage();
        return true;
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
            return Integer.compare(o1.priority(), o2.priority());
        }
    }

}
