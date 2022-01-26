/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.impl;

import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.form.FormPostException;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.PageStateDefinition.Type;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCallError;

public final class PageAction {

    private static final Logger log = LoggerFactory.getLogger(PageAction.class);

    public final ActionDefinition definition;
    private final Function<PageAction, LocTextKey> confirm;
    private final Supplier<Set<EntityKey>> selectionSupplier;
    private final LocTextKey noSelectionMessage;
    private PageContext pageContext;
    private final Function<PageAction, PageAction> exec;
    final boolean fireActionEvent;
    final boolean ignoreMoveAwayFromEdit;
    private PageAction switchAction;
    final Object[] titleArgs;

    final LocTextKey successMessage;

    public PageAction(
            final ActionDefinition definition,
            final Function<PageAction, LocTextKey> confirm,
            final LocTextKey successMessage,
            final Supplier<Set<EntityKey>> selectionSupplier,
            final LocTextKey noSelectionMessage,
            final PageContext pageContext,
            final Function<PageAction, PageAction> exec,
            final boolean fireActionEvent,
            final boolean ignoreMoveAwayFromEdit,
            final PageAction switchAction,
            final Object[] titleArgs) {

        this.definition = definition;
        this.confirm = confirm;
        this.successMessage = successMessage;
        this.selectionSupplier = selectionSupplier;
        this.noSelectionMessage = noSelectionMessage;
        this.pageContext = (pageContext != null) ? pageContext.copy() : null;
        this.exec = (exec != null) ? exec : Function.identity();
        this.fireActionEvent = fireActionEvent;
        this.ignoreMoveAwayFromEdit = ignoreMoveAwayFromEdit;
        this.switchAction = switchAction;
        if (this.switchAction != null) {
            this.switchAction.switchAction = this;
        }
        this.titleArgs = titleArgs;

        if (this.pageContext != null) {
            this.pageContext = pageContext.withAttribute(AttributeKeys.READ_ONLY, Constants.TRUE_STRING);
            if (definition.targetState != null) {
                final Type type = definition.targetState.type();
                if (type.name().equals(Type.FORM_EDIT.name()) || type.name().equals(Type.FORM_IN_TIME_EDIT.name())) {
                    this.pageContext = pageContext.withAttribute(AttributeKeys.READ_ONLY, Constants.FALSE_STRING);
                }
            }
        }
    }

    public String getName() {
        if (this.definition != null) {
            return this.definition.name();
        }

        return Constants.EMPTY_NOTE;
    }

    public LocTextKey getTitle() {
        if (this.titleArgs != null) {
            return this.definition.getTitle(this.titleArgs);
        } else {
            return this.definition.title;
        }
    }

    public void setTitleArgument(final int argIndex, final Object value) {
        if (this.titleArgs == null || this.titleArgs.length <= argIndex) {
            return;
        }

        this.titleArgs[argIndex] = value;
    }

    public PageAction getSwitchAction() {
        return this.switchAction;
    }

    public EntityKey getEntityKey() {
        return this.pageContext.getEntityKey();
    }

    public EntityKey getParentEntityKey() {
        return this.pageContext.getParentEntityKey();
    }

    public PageContext pageContext() {
        return this.pageContext;
    }

    public EntityKey getSingleSelection() {
        final Set<EntityKey> selection = getMultiSelection();
        if (selection != null && selection.size() > 0) {
            return selection.iterator().next();
        }

        return null;
    }

    public Set<EntityKey> getMultiSelection() {
        try {
            if (this.selectionSupplier != null) {
                final Set<EntityKey> selection = this.selectionSupplier.get();
                if (selection.isEmpty()) {
                    if (this.noSelectionMessage != null) {
                        throw new PageMessageException(this.noSelectionMessage);
                    }

                    return Collections.emptySet();
                }

                return selection;
            }

            if (this.noSelectionMessage != null) {
                throw new PageMessageException(this.noSelectionMessage);
            }

            return Collections.emptySet();
        } catch (final PageMessageException e) {
            throw e;
        } catch (final Exception e) {
            log.error("Unexpected error while trying to get current selection: ", e);
            throw new PageMessageException(
                    "Unexpected error while trying to get current selection: "
                            + e.getMessage());
        }
    }

    void applyAction(final Consumer<Result<PageAction>> callback) {
        if (this.confirm != null) {
            // if selection is needed, check selection fist, before confirm dialog
            if (this.selectionSupplier != null) {
                try {
                    getMultiSelection();
                } catch (final PageMessageException pme) {
                    PageAction.this.pageContext.publishPageMessage(pme);
                    return;
                }
            }

            final LocTextKey confirmMessage = this.confirm.apply(this);
            if (confirmMessage != null) {
                this.pageContext.applyConfirmDialog(confirmMessage,
                        confirm -> callback.accept((confirm)
                                ? exec().onError(error -> this.pageContext.notifyUnexpectedError(error))
                                : Result.ofRuntimeError("Confirm denied")));
            } else {
                callback.accept(exec());
            }
        } else {
            callback.accept(exec());
        }
    }

    private Result<PageAction> exec() {
        try {

            final PageAction apply = this.exec.apply(this);
            if (this.successMessage != null) {
                apply.pageContext.publishPageMessage(
                        PageContext.SUCCESS_MSG_TITLE,
                        this.successMessage);
            }
            return Result.of(apply);

        } catch (final PageMessageException pme) {
            PageAction.this.pageContext.publishPageMessage(pme);
            return Result.ofError(pme);
        } catch (final RestCallError restCallError) {
            if (restCallError.isUnexpectedError()) {
                log.error("Failed to execute action: {} | error: {} | cause: {}",
                        PageAction.this.getName(),
                        restCallError.getMessage(),
                        Utils.getErrorCauseMessage(restCallError));
                PageAction.this.pageContext.notifyError(
                        PageContext.UNEXPECTED_ERROR_KEY,
                        restCallError);
            }
            return Result.ofError(restCallError);
        } catch (final FormPostException e) {
            log.error("Failed to execute action: {} | error: {} | cause: {}",
                    PageAction.this.getName(),
                    e.getMessage(),
                    Utils.getErrorCauseMessage(e));
            if (e.getCause() instanceof RestCallError) {
                return Result.ofError((RestCallError) e.getCause());
            }
            return Result.ofError(e);
        } catch (final Exception e) {
            log.error("Failed to execute action: {} | error: {} | cause: {}",
                    PageAction.this.getName(),
                    e.getMessage(),
                    Utils.getErrorCauseMessage(e));
            PageAction.this.pageContext.notifyError(
                    PageContext.UNEXPECTED_ERROR_KEY,
                    e);
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

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("PageAction [definition=");
        builder.append(this.definition);
        builder.append(", confirm=");
        builder.append(this.confirm);
        builder.append(", selectionSupplier=");
        builder.append(this.selectionSupplier);
        builder.append(", noSelectionMessage=");
        builder.append(this.noSelectionMessage);
        builder.append(", pageContext=");
        builder.append(this.pageContext);
        builder.append(", exec=");
        builder.append(this.exec);
        builder.append(", fireActionEvent=");
        builder.append(this.fireActionEvent);
        builder.append(", ignoreMoveAwayFromEdit=");
        builder.append(this.ignoreMoveAwayFromEdit);
        builder.append(", successMessage=");
        builder.append(this.successMessage);
        builder.append("]");
        return builder.toString();
    }

    public static PageAction applySingleSelectionAsEntityKey(final PageAction action) {
        return action.withEntityKey(action.getSingleSelection());
    }

    public static PageAction applySingleSelectionAsParentEntityKey(final PageAction action) {
        return action.withParentEntityKey(action.getSingleSelection());
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
                source.ignoreMoveAwayFromEdit,
                source.switchAction,
                source.titleArgs);
    }

}
