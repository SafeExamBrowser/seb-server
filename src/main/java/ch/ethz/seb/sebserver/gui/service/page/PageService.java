/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page;

import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.event.PageEvent;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.AuthorizationContextHolder;
import ch.ethz.seb.sebserver.gui.table.TableBuilder;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

/** The main page service that provides functionality to build a page
 * with forms and tables as well as dealing with page actions */
public interface PageService {

    Logger log = LoggerFactory.getLogger(PageService.class);

    /** Get the WidgetFactory service
     *
     * @return the WidgetFactory service */
    WidgetFactory getWidgetFactory();

    /** Get the polyglotPageService service
     *
     * @return the polyglotPageService service */
    PolyglotPageService getPolyglotPageService();

    /** Get the underling AuthorizationContextHolder for the current user session
     *
     * @return the underling AuthorizationContextHolder for the current user session */
    AuthorizationContextHolder getAuthorizationContextHolder();

    /** Get the I18nSupport (internationalization support) service
     *
     * @return the I18nSupport (internationalization support) service */
    I18nSupport getI18nSupport();

    /** Get the ResourceService
     *
     * @return the ResourceService */
    ResourceService getResourceService();

    /** get the JSONMapper for parse, read and write JSON
     *
     * @return the JSONMapper for parse, read and write JSON */
    JSONMapper getJSONMapper();

    /** Get the PageState of the current user.
     *
     * @return PageState of the current user. */
    PageState getCurrentState();

    /** Publishes a given PageEvent to the current page tree
     * This goes through the page-tree and collects all listeners the are listen to
     * the specified page event type.
     *
     * @param event the concrete PageEvent instance */
    <T extends PageEvent> void firePageEvent(T event, PageContext pageContext);

    /** Executes the given PageAction and if successful, propagate an ActionEvent to the current page.
     *
     * @param pageAction the PageAction to execute */
    default void executePageAction(final PageAction pageAction) {
        executePageAction(pageAction, result -> {
        });
    }

    /** Executes the given PageAction and if successful, propagate an ActionEvent to the current page.
     *
     * @param pageAction the PageAction to execute
     * @param callback a callback to react on PageAction execution. The Callback gets a Result referencing to
     *            the executed PageAction or to an error if the PageAction has not been executed */
    void executePageAction(PageAction pageAction, Consumer<Result<PageAction>> callback);

    /** Publishes a PageAction to the current page. This uses the firePageEvent form
     * PageContext of the given PageAction and fires a ActionPublishEvent for the given PageAction
     *
     * All ActionPublishEventListeners that are registered within the current page will
     * receive the ActionPublishEvent sent by this.
     *
     * @param pageAction the PageAction to publish */
    void publishAction(final PageAction pageAction);

    /** Get a new FormBuilder for the given PageContext and with number of rows.
     *
     * @param pageContext the PageContext on that the FormBuilder should work
     * @param rows the number of rows of the from
     * @return a FormBuilder instance for the given PageContext and with number of rows */
    FormBuilder formBuilder(final PageContext pageContext, final int rows);

    /** Get an new TableBuilder for specified page based RestCall.
     *
     * @param apiCall the SEB Server API RestCall that feeds the table with data
     * @param <T> the type of the Entity of the table
     * @return TableBuilder of specified type */
    <T extends Entity> TableBuilder<T> entityTableBuilder(final RestCall<Page<T>> apiCall);

    /** Get a new PageActionBuilder for a given PageContext.
     *
     * @param pageContext the PageContext that is used by the new PageActionBuilder
     * @return new PageActionBuilder to build PageAction */
    default PageActionBuilder pageActionBuilder(final PageContext pageContext) {
        return new PageActionBuilder(this, pageContext);
    }

    /** This triggers a logout on the current authorization context to logout the current user
     * and forward to the login page with showing a successful logout message to the user. */
    void logout(PageContext pageContext);

    default <T> T logoutOnError(final Throwable t, final PageContext pageContext) {
        log.error("Unexpected, Current User related error.Automatically logout and cleanup current user session. ", t);
        logout(pageContext);
        return null;
    }

    /** Clears the PageState of the current users page */
    void clearState();

    default PageAction onEmptyEntityKeyGoTo(final PageAction action, final ActionDefinition gotoActionDef) {
        if (action.getEntityKey() == null) {
            final PageContext pageContext = action.pageContext();
            return pageActionBuilder(pageContext)
                    .newAction(gotoActionDef)
                    .create();
        }

        return action;
    }

    /** Key to store the ScrolledComposite update function within Control data map */
    static String SCROLLED_COMPOSITE_UPDATE = "SCROLLED_COMPOSITE_UPDATE";

    /** Creates a ScrolledComposite with content supplied the given content creation function.
     * The content creation function is used to create the content Composite as a child of the
     * newly created ScrolledComposite.
     * Also adds an update function within the ScrolledComposite Data mapping. If a child inside
     * the ScrolledComposite changes its dimensions the method updateScrolledComposite must be
     * called to update the ScrolledComposite scrolled content.
     *
     * @param parent the parent Composite of the ScrolledComposite
     * @param contentFunction the content creation function
     * @param showScrollbars indicates whether the scrollbar shall always be shown
     * @return the child composite that is scrolled by the newly created ScrolledComposite */
    static Composite createManagedVScrolledComposite(
            final Composite parent,
            final Function<ScrolledComposite, Composite> contentFunction,
            final boolean showScrollbars) {

        final ScrolledComposite scrolledComposite = new ScrolledComposite(parent, SWT.BORDER | SWT.V_SCROLL);
        scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));

        final Composite content = contentFunction.apply(scrolledComposite);
        scrolledComposite.setContent(content);
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);
        final Point parentSize = parent.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        scrolledComposite.setSize(parentSize);
        if (showScrollbars) {
            scrolledComposite.setAlwaysShowScrollBars(true);
        }

        final Runnable update = () -> {
            final Point computeSize = content.computeSize(SWT.DEFAULT - 20, SWT.DEFAULT);
            scrolledComposite.setMinSize(computeSize);

        };

        scrolledComposite.addListener(SWT.Resize, event -> update.run());
        scrolledComposite.setData(SCROLLED_COMPOSITE_UPDATE, update);

        return content;
    }

    /** Used to update the crolledComposite when some if its content has dynamically changed
     * its dimensions.
     *
     * @param composite The Component that changed its dimensions */
    static void updateScrolledComposite(final Composite composite) {
        if (composite == null) {
            return;
        }

        Composite parent = composite.getParent();
        while (parent != null) {
            final Object update = parent.getData(SCROLLED_COMPOSITE_UPDATE);
            if (update != null) {
                ((Runnable) update).run();
                return;
            }
            parent = parent.getParent();
        }
    }

    public class PageActionBuilder {
        private final PageService pageService;
        private final PageContext originalPageContext;

        private PageContext pageContext;
        private ActionDefinition definition;
        private Supplier<LocTextKey> confirm;
        private LocTextKey successMessage;
        private Supplier<Set<EntityKey>> selectionSupplier;
        private LocTextKey noSelectionMessage;
        private Function<PageAction, PageAction> exec;
        private boolean fireActionEvent = true;
        private boolean ignoreMoveAwayFromEdit = false;

        protected PageActionBuilder(final PageService pageService, final PageContext pageContext) {
            this.pageService = pageService;
            this.originalPageContext = pageContext;
        }

        public PageActionBuilder newAction(final ActionDefinition definition) {
            pageContext = originalPageContext.copy();
            this.definition = definition;
            confirm = null;
            successMessage = null;
            selectionSupplier = null;
            noSelectionMessage = null;
            exec = null;
            fireActionEvent = true;
            ignoreMoveAwayFromEdit = false;
            return this;
        }

        public PageAction create() {
            return new PageAction(
                    definition,
                    confirm,
                    successMessage,
                    selectionSupplier,
                    noSelectionMessage,
                    pageContext,
                    exec,
                    fireActionEvent,
                    ignoreMoveAwayFromEdit);
        }

        public PageActionBuilder publish() {
            pageService.publishAction(create());
            return this;
        }

        public PageActionBuilder publishIf(final BooleanSupplier condition) {
            if (!condition.getAsBoolean()) {
                return this;
            }

            return this.publish();
        }

        public PageActionBuilder withExec(final Function<PageAction, PageAction> exec) {
            this.exec = exec;
            return this;
        }

        public PageActionBuilder withSelectionSupplier(final Supplier<Set<EntityKey>> selectionSupplier) {
            this.selectionSupplier = selectionSupplier;
            return this;
        }

        public PageActionBuilder withSelect(
                final Supplier<Set<EntityKey>> selectionSupplier,
                final Function<PageAction, PageAction> exec,
                final LocTextKey noSelectionMessage) {

            this.selectionSupplier = selectionSupplier;
            this.exec = exec;
            this.noSelectionMessage = noSelectionMessage;
            return this;
        }

        public <T> PageActionBuilder withSimpleRestCall(
                final RestService restService,
                final Class<? extends RestCall<T>> restCallType) {

            this.exec = action -> {
                restService.getBuilder(restCallType)
                        .withURIVariable(
                                API.PARAM_MODEL_ID,
                                action.pageContext().getAttribute(AttributeKeys.ENTITY_ID))
                        .call()
                        .onError(t -> action.pageContext().notifyError(t));
                return action;
            };

            return this;
        }

        public PageActionBuilder withConfirm(final String confirmationMessageKey) {
            this.confirm = () -> new LocTextKey(confirmationMessageKey);
            return this;
        }

        public PageActionBuilder withConfirm(final Supplier<LocTextKey> confirm) {
            this.confirm = confirm;
            return this;
        }

        public PageActionBuilder withSuccess(final String successMessageKey) {
            this.successMessage = new LocTextKey(successMessageKey);
            return this;
        }

        public PageActionBuilder noEventPropagation() {
            this.fireActionEvent = false;
            return this;
        }

        public PageActionBuilder withEntityKey(final EntityKey entityKey) {
            this.pageContext = this.pageContext.withEntityKey(entityKey);
            return this;
        }

        public PageActionBuilder ignoreMoveAwayFromEdit() {
            this.ignoreMoveAwayFromEdit = true;
            return this;
        }

        public PageActionBuilder withEntityKey(final Long modelId, final EntityType entityType) {
            if (modelId != null) {
                return withEntityKey(String.valueOf(modelId), entityType);
            }

            return this;
        }

        public PageActionBuilder withEntityKey(final String modelId, final EntityType entityType) {
            if (modelId == null || entityType == null) {
                return this;
            }

            this.pageContext = this.pageContext.withEntityKey(new EntityKey(modelId, entityType));
            return this;
        }

        public PageActionBuilder withParentEntityKey(final EntityKey entityKey) {
            this.pageContext = this.pageContext.withParentEntityKey(entityKey);
            return this;
        }

        public PageActionBuilder withAttribute(final String name, final String value) {
            this.pageContext = this.pageContext.withAttribute(name, value);
            return this;
        }

    }

}
