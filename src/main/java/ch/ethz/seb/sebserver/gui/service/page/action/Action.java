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
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;

public final class Action implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(Action.class);

    public final RestService restService;
    public final ActionDefinition definition;
    Supplier<LocTextKey> confirm;
    LocTextKey successMessage;
    boolean updateOnSelection;

    Supplier<Set<String>> selectionSupplier;

    private PageContext pageContext;
    private Function<Action, Action> exec = Function.identity();

    public Action(
            final ActionDefinition definition,
            final PageContext pageContext,
            final RestService restService) {

        this.definition = definition;
        this.pageContext = pageContext;
        this.restService = restService;
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

            this.pageContext.publishPageEvent(new ActionEvent(this.exec.apply(this), false));

//            this.exec.apply(this)
//                    .map(action -> {
//                        this.pageContext.publishPageEvent(
//                                new ActionEvent(action, false));
//                        return action;
//                    })
//                    .getOrThrow();

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

    public Supplier<Set<String>> getSelectionSupplier() {
        return this.selectionSupplier;
    }

    public Action withExec(final Function<Action, Action> exec) {
        this.exec = exec;
        return this;
    }

    public Action withSelectionSupplier(final Supplier<Set<String>> selectionSupplier) {
        this.selectionSupplier = selectionSupplier;
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

    public Action withUpdateOnSelection() {
        this.updateOnSelection = true;
        return this;
    }

    public Action withEntity(final EntityKey entityKey) {
        this.pageContext = this.pageContext.withEntityKey(entityKey);
        return this;
    }

    public Action withEntity(final Long modelId, final EntityType entityType) {
        if (modelId != null) {
            return withEntity(String.valueOf(modelId), entityType);
        }

        return this;
    }

    public Action withEntity(final String modelId, final EntityType entityType) {
        if (modelId == null || entityType == null) {
            return this;
        }

        this.pageContext = this.pageContext.withEntityKey(new EntityKey(modelId, entityType));
        return this;
    }

    public Action withParentEntity(final EntityKey entityKey) {
        this.pageContext = this.pageContext.withParentEntityKey(entityKey);
        return this;
    }

    public Action withAttribute(final String name, final String value) {
        this.pageContext = this.pageContext.withAttribute(name, value);
        return this;
    }

    public PageContext publish() {
        this.pageContext.publishPageEvent(new ActionPublishEvent(this));
        return this.pageContext;
    }

    public PageContext publishIf(final BooleanSupplier condition) {
        if (condition.getAsBoolean()) {
            publish();
        }

        return this.pageContext;
    }

    public EntityKey getEntityKey() {
        return this.pageContext.getEntityKey();
    }

    public PageContext pageContext() {
        return this.pageContext;
    }

    public Action readonly(final boolean b) {
        return this.withAttribute(AttributeKeys.READ_ONLY, "false");
    }

}
