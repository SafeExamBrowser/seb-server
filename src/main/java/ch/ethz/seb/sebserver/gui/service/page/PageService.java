/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page;

import java.util.Collection;
import java.util.List;
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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.Activatable;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.ModelIdAware;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionActivationEvent;
import ch.ethz.seb.sebserver.gui.service.page.event.PageEvent;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageState;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.AuthorizationContextHolder;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.table.TableBuilder;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

/** The main page service that provides functionality to build a page
 * with forms and tables as well as dealing with page actions */
public interface PageService {

    LocTextKey MESSAGE_NO_MULTISELECTION =
            new LocTextKey("sebserver.overall.action.toomanyselection");

    enum FormTooltipMode {
        RIGHT,
        INPUT,
        LEFT,
    }

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

    /** Get the RestService bean
     *
     * @return the RestService bean */
    RestService getRestService();

    /** Use this to get the CurrentUser facade
     *
     * @return the CurrentUser facade */
    CurrentUser getCurrentUser();

    /** Get the PageState of the current user.
     *
     * @return PageState of the current user. */
    PageState getCurrentState();

    /** Get the configured tooltip mode for input forms
     *
     * @return the configured tooltip mode for input forms */
    FormTooltipMode getFormTooltipMode();

    /** Get a PageAction function to go back to the current state.
     *
     * @return a PageAction function to go back to the current state. */
    default Function<PageAction, PageAction> backToCurrentFunction() {
        final PageState currentState = this.getCurrentState();
        return action -> (currentState != null) ? currentState.gotoAction : action;
    }

    /** Get a page action execution function for switching the activity of currently selected
     * entities from a given entity-table.
     *
     * @param table the entity table
     * @param noSelectionText LocTextKey for missing selection message
     * @param testBeforeActivation a function to test before activation. This function shall throw an error if test
     *            fails.
     *            My be null if no specific test is needed before activation
     * @return page action execution function for switching the activity */
    <T extends Entity & Activatable> Function<PageAction, PageAction> activationToggleActionFunction(
            EntityTable<T> table,
            LocTextKey noSelectionText,
            Function<PageAction, PageAction> testBeforeActivation);

    /** Get a page action execution function for switching the activity of currently selected
     * entities from a given entity-table.
     *
     * @param table the entity table
     * @param noSelectionText LocTextKey for missing selection message
     * @return page action execution function for switching the activity */
    default <T extends Entity & Activatable> Function<PageAction, PageAction> activationToggleActionFunction(
            final EntityTable<T> table,
            final LocTextKey noSelectionText) {
        return this.activationToggleActionFunction(table, noSelectionText, null);
    }

//    /** Get a message supplier to notify deactivation dependencies to the user for all given entities
//     *
//     * @param entities Set of entities to collect the dependencies for
//     * @return a message supplier to notify deactivation dependencies to the user */
//    Supplier<LocTextKey> confirmDeactivation(final Set<EntityKey> keys);

    /** Get a message supplier to notify deactivation dependencies to the user for given entity
     *
     * @param entity the entity instance
     * @return a message supplier to notify deactivation dependencies to the user */
    <T extends Entity & Activatable> Supplier<LocTextKey> confirmDeactivation(final T entity);

    /** Get a message supplier to notify deactivation dependencies to the user for given entity table selection
     *
     * @param table the entity table
     * @return a message supplier to notify deactivation dependencies to the user */
    default <T extends Entity & Activatable> Supplier<LocTextKey> confirmDeactivation(final EntityTable<T> table) {
        return () -> {
            final Set<EntityKey> multiSelection = table.getMultiSelection();
            if (multiSelection.size() > 1) {
                throw new PageMessageException(MESSAGE_NO_MULTISELECTION);
            }
            final T entity = table.getSingleSelectedROWData();
            if (!entity.isActive()) {
                return null;
            }
            return confirmDeactivation(entity).get();
        };
    }

    /** Use this to get an action activation publisher that processes the action activation.
     *
     * @param pageContext the current PageContext
     * @param actionDefinitions list of action definitions that activity should be toggled on table selection
     * @return the action activation publisher that can be used to control the activity of an certain action */
    default Consumer<Boolean> getActionActiviationPublisher(
            final PageContext pageContext,
            final ActionDefinition... actionDefinitions) {

        return avtivate -> firePageEvent(
                new ActionActivationEvent(avtivate, actionDefinitions),
                pageContext);
    }

    /** Use this to get an table selection action publisher that processes the action
     * activation on table selection.
     *
     * @param pageContext the current PageContext
     * @param actionDefinitions list of action definitions that activity should be toggled on table selection
     * @return the selection publisher that handles this defines action activation on table selection */
    default <T extends ModelIdAware> Consumer<EntityTable<T>> getSelectionPublisher(
            final PageContext pageContext,
            final ActionDefinition... actionDefinitions) {

        return table -> firePageEvent(
                new ActionActivationEvent(table.hasSelection(), actionDefinitions),
                pageContext);
    }

    /** Use this to get an table selection action publisher that processes the action
     * activation on table selection.
     * </p>
     * This additional has the ability to define a entity activity action that is toggles in
     * case of the selected entity
     *
     * @param toggle the base entity activity action definition
     * @param activate the entity activation action definition
     * @param deactivate the entity deactivation action definition
     * @param pageContext the current PageContext
     * @param actionDefinitions list of action definitions that activity should be toggled on table selection
     * @return the selection publisher that handles this defines action activation on table selection */
    default <T extends Activatable & ModelIdAware> Consumer<EntityTable<T>> getSelectionPublisher(
            final ActionDefinition toggle,
            final ActionDefinition activate,
            final ActionDefinition deactivate,
            final PageContext pageContext,
            final ActionDefinition... actionDefinitions) {

        return table -> {
            final Set<T> rows = table.getPageSelectionData();
            if (!rows.isEmpty()) {
                firePageEvent(
                        new ActionActivationEvent(
                                true,
                                new Tuple<>(
                                        toggle,
                                        rows.iterator().next().isActive()
                                                ? deactivate
                                                : activate),
                                actionDefinitions),
                        pageContext);
            } else {
                firePageEvent(
                        new ActionActivationEvent(false, actionDefinitions),
                        pageContext);
            }
        };
    }

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
     * @param pageAction the PageAction to publish
     * @param active indicates whether the action is active or not */
    void publishAction(final PageAction pageAction, boolean active);

    /** Publishes a PageAction to the current page. This uses the firePageEvent form
     * PageContext of the given PageAction and fires a ActionPublishEvent for the given PageAction
     *
     * All ActionPublishEventListeners that are registered within the current page will
     * receive the ActionPublishEvent sent by this.
     *
     * @param pageAction the PageAction to publish
     * @param actionConsumer An consumer that gets the actions TreeItem after creation */
    void publishAction(final PageAction pageAction, Consumer<TreeItem> actionConsumer);

    /** Get a new FormBuilder for the given PageContext
     * This FormBuilder uses the standard form grid which has 8 rows (2 title, 5 input and 1 right-space)
     *
     * @param pageContext the PageContext on that the FormBuilder should work
     * @return a FormBuilder instance for the given PageContext and with number of rows */
    default FormBuilder formBuilder(final PageContext pageContext) {
        return formBuilder(pageContext, 8);
    }

    /** Get a new FormBuilder for the given PageContext and with number of rows.
     *
     * @param pageContext the PageContext on that the FormBuilder should work
     * @param rows the number of rows of the from
     * @return a FormBuilder instance for the given PageContext and with number of rows */
    FormBuilder formBuilder(PageContext pageContext, int rows);

    /** Get an new TableBuilder for specified page based RestCall.
     *
     * @param apiCall the SEB Server API RestCall that feeds the table with data
     * @param <T> the type of the Entity of the table
     * @return TableBuilder of specified type */
    default <T extends Entity> TableBuilder<T> entityTableBuilder(final Class<? extends RestCall<Page<T>>> apiCall) {
        return entityTableBuilder(this.getRestService().getRestCall(apiCall));
    }

    /** Get an new TableBuilder for specified page based RestCall.
     *
     * @param apiCall the SEB Server API RestCall that feeds the table with data
     * @param <T> the type of the Entity of the table
     * @return TableBuilder of specified type */
    default <T extends ModelIdAware> TableBuilder<T> entityTableBuilder(final RestCall<Page<T>> apiCall) {
        return entityTableBuilder(apiCall.getClass().getSimpleName(), apiCall);
    }

    /** Get an new TableBuilder for specified page based RestCall.
     *
     * @param name The name of the table to build
     * @param apiCall the SEB Server API RestCall that feeds the table with data
     * @param <T> the type of the Entity of the table
     * @return TableBuilder of specified type */
    <T extends ModelIdAware> TableBuilder<T> entityTableBuilder(
            String name,
            RestCall<Page<T>> apiCall);

    <T extends ModelIdAware> TableBuilder<T> staticListTableBuilder(final List<T> staticList, EntityType entityType);

    <T extends ModelIdAware> TableBuilder<T> remoteListTableBuilder(RestCall<Collection<T>> apiCall,
            EntityType entityType);

    /** Get a new PageActionBuilder for a given PageContext.
     *
     * @param pageContext the PageContext that is used by the new PageActionBuilder
     * @return new PageActionBuilder to build PageAction */
    default PageActionBuilder pageActionBuilder(final PageContext pageContext) {
        return new PageActionBuilder(this, pageContext);
    }

    /** This triggers a logout on the current authorization context to logout the current user
     * and forward to the login page with showing a successful logout message to the user. */
    boolean logout(PageContext pageContext);

    default <T> T logoutOnError(final Exception t, final PageContext pageContext) {
        log.error("Unexpected, Current User related error.Automatically logout and cleanup current user session. ", t);
        logout(pageContext);
        return null;
    }

    /** Clears the PageState of the current users page */
    void clearState();

    /** Key to store the ScrolledComposite update function within Control data map */
    String SCROLLED_COMPOSITE_UPDATE = "SCROLLED_COMPOSITE_UPDATE";

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
        return createManagedVScrolledComposite(parent, contentFunction, showScrollbars, true, false);
    }

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
     * @param showBorder indicates whether a border is shown for the composite or not
     * @param fill indicates if the content shall be vertically filled
     * @return the child composite that is scrolled by the newly created ScrolledComposite */
    static Composite createManagedVScrolledComposite(
            final Composite parent,
            final Function<ScrolledComposite, Composite> contentFunction,
            final boolean showScrollbars,
            final boolean showBorder,
            final boolean fill) {

        final ScrolledComposite scrolledComposite = new ScrolledComposite(
                parent,
                (showBorder) ? SWT.BORDER | SWT.V_SCROLL : SWT.V_SCROLL);
        final GridData gridData = new GridData(SWT.FILL, (fill) ? SWT.FILL : SWT.TOP, true, true);
        scrolledComposite.setLayoutData(gridData);

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

    /** Used to update the scrolledComposite when some if its content has dynamically changed
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

    static void clearComposite(final Composite parent) {
        if (parent == null) {
            return;
        }

        for (final Control control : parent.getChildren()) {
            control.dispose();
        }
    }

    class PageActionBuilder {
        private final PageService pageService;
        private final PageContext originalPageContext;

        private PageContext pageContext;
        private ActionDefinition definition;
        private Function<PageAction, LocTextKey> confirm;
        private LocTextKey successMessage;
        private Supplier<Set<EntityKey>> selectionSupplier;
        private LocTextKey noSelectionMessage;
        private Function<PageAction, PageAction> exec;
        private boolean fireActionEvent = true;
        private boolean ignoreMoveAwayFromEdit = false;
        private PageAction switchAction;
        private Object[] titleArgs;

        protected PageActionBuilder(final PageService pageService, final PageContext pageContext) {
            this.pageService = pageService;
            this.originalPageContext = pageContext;
        }

        public PageActionBuilder newAction(final ActionDefinition definition) {
            final PageActionBuilder newBuilder = new PageActionBuilder(this.pageService, this.originalPageContext);
            newBuilder.pageContext = this.originalPageContext.copy();
            newBuilder.definition = definition;
            newBuilder.confirm = null;
            newBuilder.successMessage = null;
            newBuilder.selectionSupplier = null;
            newBuilder.noSelectionMessage = null;
            newBuilder.exec = null;
            newBuilder.fireActionEvent = true;
            newBuilder.ignoreMoveAwayFromEdit = false;
            newBuilder.switchAction = null;
            return newBuilder;
        }

        public PageAction create() {
            return new PageAction(
                    this.definition,
                    this.confirm,
                    this.successMessage,
                    this.selectionSupplier,
                    this.noSelectionMessage,
                    this.pageContext,
                    this.exec,
                    this.fireActionEvent,
                    this.ignoreMoveAwayFromEdit,
                    this.switchAction,
                    this.titleArgs);
        }

        public PageActionBuilder publish() {
            return publish(true);
        }

        public PageActionBuilder publish(final boolean active) {
            this.pageService.publishAction(create(), active);
            return this;
        }

        public PageActionBuilder publishIf(final BooleanSupplier condition) {
            return publishIf(condition, true);
        }

        public PageActionBuilder publishIf(final BooleanSupplier condition, final boolean active) {
            if (!condition.getAsBoolean()) {
                return this;
            }

            return this.publish(active);
        }

        public PageActionBuilder withSwitchAction(final PageAction switchAction) {
            this.switchAction = switchAction;
            return this;
        }

        public PageActionBuilder withExec(final Function<PageAction, PageAction> exec) {
            this.exec = exec;
            return this;
        }

        public PageActionBuilder withNameAttributes(final Object... attributes) {
            this.titleArgs = attributes;
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
                final PageContext pageContext = action.pageContext();
                restService.getBuilder(restCallType)
                        .withURIVariable(
                                API.PARAM_MODEL_ID,
                                action.pageContext().getAttribute(AttributeKeys.ENTITY_ID))
                        .call()
                        .onError(pageContext::notifyUnexpectedError);
                return action;
            };

            return this;
        }

        public PageActionBuilder withConfirm(final Supplier<LocTextKey> confirm) {
            this.confirm = action -> confirm.get();
            return this;
        }

        public PageActionBuilder withConfirm(final Function<PageAction, LocTextKey> confirm) {
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

        public PageActionBuilder withEntityKeys(final List<EntityKey> entityKeys) {
            this.pageContext = this.pageContext.withEntityKeys(entityKeys);
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
