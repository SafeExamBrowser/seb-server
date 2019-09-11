/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.impl;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.PageStateDefinition.Type;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCallError;

public final class PageAction {

    private static final Logger log = LoggerFactory.getLogger(PageAction.class);

    public static final LocTextKey SUCCESS_MSG_TITLE = new LocTextKey("sebserver.page.message");

    public final ActionDefinition definition;
    private final Supplier<LocTextKey> confirm;
    final LocTextKey successMessage;
    private final Supplier<Set<EntityKey>> selectionSupplier;
    private final LocTextKey noSelectionMessage;
    private PageContext pageContext;
    private final Function<PageAction, PageAction> exec;
    final boolean fireActionEvent;
    final boolean ignoreMoveAwayFromEdit;

    public PageAction(
            final ActionDefinition definition,
            final Supplier<LocTextKey> confirm,
            final LocTextKey successMessage,
            final Supplier<Set<EntityKey>> selectionSupplier,
            final LocTextKey noSelectionMessage,
            final PageContext pageContext,
            final Function<PageAction, PageAction> exec,
            final boolean fireActionEvent,
            final boolean ignoreMoveAwayFromEdit) {

        this.definition = definition;
        this.confirm = confirm;
        this.successMessage = successMessage;
        this.selectionSupplier = selectionSupplier;
        this.noSelectionMessage = noSelectionMessage;
        this.pageContext = pageContext;
        this.exec = (exec != null) ? exec : Function.identity();
        this.fireActionEvent = fireActionEvent;
        this.ignoreMoveAwayFromEdit = ignoreMoveAwayFromEdit;

        this.pageContext = pageContext.withAttribute(AttributeKeys.READ_ONLY, Constants.TRUE_STRING);
        if (definition.targetState != null) {
            final Type type = definition.targetState.type();
            if (type.name().equals(Type.FORM_EDIT.name())) {
                this.pageContext = pageContext.withAttribute(AttributeKeys.READ_ONLY, Constants.FALSE_STRING);
            }
        }
    }

    public EntityKey getEntityKey() {
        return this.pageContext.getEntityKey();
    }

    public PageContext pageContext() {
        return this.pageContext;
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

    void applyAction(final Consumer<Result<PageAction>> callback) {
        if (this.confirm != null) {
            final LocTextKey confirmMessage = this.confirm.get();
            if (confirmMessage != null) {
                this.pageContext.applyConfirmDialog(confirmMessage,
                        confirm -> callback.accept((confirm)
                                ? exec()
                                : Result.ofRuntimeError("Confirm denied")));
            }
        } else {
            callback.accept(exec());
        }
    }

    private Result<PageAction> exec() {
        try {

            final PageAction apply = this.exec.apply(this);
            if (this.successMessage != null) {
                apply.pageContext.publishPageMessage(SUCCESS_MSG_TITLE, this.successMessage);
            }
            return Result.of(apply);

        } catch (final PageMessageException pme) {
            PageAction.this.pageContext.publishPageMessage(pme);
            return Result.ofError(pme);
        } catch (final RestCallError restCallError) {
            if (!restCallError.isFieldValidationError()) {
                log.error("Failed to execute action: {}", PageAction.this, restCallError);
                PageAction.this.pageContext.notifyError("action.error.unexpected.message", restCallError);
            }
            return Result.ofError(restCallError);
        } catch (final Exception e) {
            log.error("Failed to execute action: {}", PageAction.this, e);
            PageAction.this.pageContext.notifyError("action.error.unexpected.message", e);
            return Result.ofError(e);
        }
    }

    public PageAction withEntityKey(final EntityKey entityKey) {
        this.pageContext = this.pageContext.withEntityKey(entityKey);
        return this;
    }

    public PageAction withParentEntityKey(final EntityKey parentEntityKey) {
        this.pageContext = this.pageContext.withParentEntityKey(parentEntityKey);
        return this;
    }

    public PageAction withAttribute(final String name, final String value) {
        this.pageContext = this.pageContext.withAttribute(name, value);
        return this;
    }

    public static PageAction applySingleSelection(final PageAction action) {
        return action.withEntityKey(action.getSingleSelection());
    }

    public static PageAction copyOf(final PageAction source) {
        return new PageAction(
                source.definition,
                source.confirm,
                source.successMessage,
                source.selectionSupplier,
                source.noSelectionMessage,
                source.pageContext.copy(),
                source.exec,
                source.fireActionEvent,
                source.ignoreMoveAwayFromEdit);
    }

}
