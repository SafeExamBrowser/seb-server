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

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEvent;
import ch.ethz.seb.sebserver.gui.service.page.event.PageEvent;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.table.TableBuilder;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

/** The main page service that provides functionality to build a page
 * with forms and tables as well as dealing with page actions */
public interface PageService {

    /** get the WidgetFactory service
     *
     * @return the WidgetFactory service */
    WidgetFactory getWidgetFactory();

    /** get the polyglotPageService service
     *
     * @return the polyglotPageService service */
    PolyglotPageService getPolyglotPageService();

    /** get the I18nSupport (internationalization support) service
     *
     * @return the I18nSupport (internationalization support) service */
    I18nSupport getI18nSupport();

    /** get the JSONMapper for parse, read and write JSON
     *
     * @return the JSONMapper for parse, read and write JSON */
    JSONMapper getJSONMapper();

    PageState initPageState(PageState initState);

    PageState getCurrentState();

    /** Publishes a given PageEvent to the current page tree
     * This goes through the page-tree and collects all listeners the are listen to
     * the specified page event type.
     *
     * @param event the concrete PageEvent instance */
    <T extends PageEvent> void firePageEvent(T event, PageContext pageContext);

    default void executePageAction(final PageAction pageAction) {
        executePageAction(pageAction, result -> {
        });
    }

    void executePageAction(PageAction pageAction, Consumer<Result<PageAction>> callback);

    void publishAction(final PageAction pageAction);

    FormBuilder formBuilder(final PageContext pageContext, final int rows);

    <T extends Entity> TableBuilder<T> entityTableBuilder(final RestCall<Page<T>> apiCall);

    void clear();

    default PageActionBuilder pageActionBuilder(final PageContext pageContext) {
        return new PageActionBuilder(this, pageContext);
    }

    default PageAction onEmptyEntityKeyGoTo(final PageAction action, final ActionDefinition gotoActionDef) {
        if (action.getEntityKey() == null) {
            final PageContext pageContext = action.pageContext();
            final PageAction activityHomeAction = pageActionBuilder(pageContext)
                    .newAction(gotoActionDef)
                    .create();
            firePageEvent(new ActionEvent(activityHomeAction), activityHomeAction.pageContext());
        }

        return action;
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

        public PageActionBuilder withSimpleRestCall(
                final RestService restService,
                final Class<? extends RestCall<?>> restCallType) {

            this.exec = action -> {
                restService.getBuilder(restCallType)
                        .withURIVariable(
                                API.PARAM_MODEL_ID,
                                action.pageContext().getAttribute(AttributeKeys.ENTITY_ID))
                        .call()
                        .onErrorDo(t -> action.pageContext().notifyError(t));
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
