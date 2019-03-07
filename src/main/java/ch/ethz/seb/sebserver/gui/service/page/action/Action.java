/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.action;

import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEvent;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionPublishEvent;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCallError;

public final class Action implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(Action.class);

    public final ActionDefinition definition;
    Supplier<LocTextKey> confirm;
    LocTextKey successMessage;

    Supplier<Set<EntityKey>> selectionSupplier;
    LocTextKey noSelectionMessage;

    private final PageContext originalPageContext;
    private PageContext pageContext;
    private Function<Action, Action> exec = Function.identity();

    public Action(
            final ActionDefinition definition,
            final PageContext pageContext) {

        this.definition = definition;
        this.originalPageContext = pageContext;
        this.pageContext = pageContext.withAttribute(
                AttributeKeys.READ_ONLY,
                String.valueOf(definition.readonly));
    }

    @Override
    public void run() {
        if (this.confirm != null) {
            final LocTextKey confirmMessage = this.confirm.get();
            if (confirmMessage != null) {
                this.pageContext.applyConfirmDialog(confirmMessage, () -> exec());
            }
        } else {
            exec();
        }
    }

    private void exec() {
        try {

            final Action executedAction = this.exec.apply(this);
            this.pageContext.publishPageEvent(new ActionEvent(executedAction, false));

        } catch (final PageMessageException pme) {
            Action.this.pageContext.publishPageMessage(pme);

        } catch (final RestCallError restCallError) {
            if (restCallError.isFieldValidationError()) {
                Action.this.pageContext.publishPageMessage(
                        new LocTextKey("sebserver.form.validation.error.title"),
                        new LocTextKey("sebserver.form.validation.error.message"));
            } else {
                log.error("Failed to execute action: {}", Action.this, restCallError);
                Action.this.pageContext.notifyError("action.error.unexpected.message", restCallError);
            }
        } catch (final Throwable t) {
            log.error("Failed to execute action: {}", Action.this, t);
            Action.this.pageContext.notifyError("action.error.unexpected.message", t);
        }
    }

    public Action withExec(final Function<Action, Action> exec) {
        this.exec = exec;
        return this;
    }

    public Action withSelectionSupplier(final Supplier<Set<EntityKey>> selectionSupplier) {
        this.selectionSupplier = selectionSupplier;
        return this;
    }

    public Action withSelect(
            final Supplier<Set<EntityKey>> selectionSupplier,
            final Function<Action, Action> exec,
            final LocTextKey noSelectionMessage) {

        this.selectionSupplier = selectionSupplier;
        this.exec = exec;
        this.noSelectionMessage = noSelectionMessage;
        return this;
    }

    public Action withConfirm(final String confirmationMessageKey) {
        this.confirm = () -> new LocTextKey(confirmationMessageKey);
        return this;
    }

    public Action withConfirm(final Supplier<LocTextKey> confirm) {
        this.confirm = confirm;
        return this;
    }

    public Action withSuccess(final String successMessageKey) {
        this.successMessage = new LocTextKey(successMessageKey);
        return this;
    }

    public Action resetEntityKey() {
        this.pageContext = this.pageContext.withEntityKey(null);
        return this;
    }

    public Action resetParentEntityKey() {
        this.pageContext = this.pageContext.withParentEntityKey(null);
        return this;
    }

    public EntityKey getEntityKey() {
        return this.pageContext.getEntityKey();
    }

    public PageContext pageContext() {
        return this.pageContext;
    }

    public Action withEntityKey(final EntityKey entityKey) {
        this.pageContext = this.pageContext.withEntityKey(entityKey);
        return this;
    }

    public Action withEntityKey(final Long modelId, final EntityType entityType) {
        if (modelId != null) {
            return withEntityKey(String.valueOf(modelId), entityType);
        }

        return this;
    }

    public Action withEntityKey(final String modelId, final EntityType entityType) {
        if (modelId == null || entityType == null) {
            return this;
        }

        this.pageContext = this.pageContext.withEntityKey(new EntityKey(modelId, entityType));
        return this;
    }

    public Action withParentEntityKey(final EntityKey entityKey) {
        this.pageContext = this.pageContext.withParentEntityKey(entityKey);
        return this;
    }

    public Action withAttribute(final String name, final String value) {
        this.pageContext = this.pageContext.withAttribute(name, value);
        return this;
    }

    public PageContext publish() {
        this.pageContext.publishPageEvent(new ActionPublishEvent(this));
        return this.originalPageContext;
    }

    public PageContext publishIf(final BooleanSupplier condition) {
        if (condition.getAsBoolean()) {
            publish();
        }

        return this.originalPageContext;
    }

    public EntityKey getSingleSelection() {
        final Set<EntityKey> selection = getMultiSelection();
        if (selection != null) {
            return selection.iterator().next();
        }

        return null;
    }

    public Set<EntityKey> getMultiSelection() {
        if (this.selectionSupplier != null) {
            final Set<EntityKey> selection = this.selectionSupplier.get();
            if (selection.isEmpty()) {
                if (this.noSelectionMessage != null) {
                    throw new PageMessageException(this.noSelectionMessage);
                }

                return null;
            }

            return selection;
        }

        if (this.noSelectionMessage != null) {
            throw new PageMessageException(this.noSelectionMessage);
        }

        return null;
    }

    public static Action applySingleSelection(final Action action) {
        return action.withEntityKey(action.getSingleSelection());
    }

    public static Action onEmptyEntityKeyGoToActivityHome(final Action action) {
        if (action.getEntityKey() == null) {
            final PageContext pageContext = action.pageContext();
            final Action activityHomeAction = pageContext.createAction(action.definition.activityAlias);
            action.pageContext.publishPageEvent(new ActionEvent(activityHomeAction, false));
            return activityHomeAction;
        }

        return action;
    }

}
