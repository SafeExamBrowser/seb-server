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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEvent;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionPublishEvent;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;

public final class Action implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(Action.class);

    public final ActionDefinition definition;
    String confirmationMessage;
    BooleanSupplier confirmComdition = () -> true;
    String successMessage;
    boolean updateOnSelection;

    final RestService restService;
    final PageContext pageContext;
    Function<Action, Result<?>> exec;
    Supplier<Set<String>> selectionSupplier;

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
        if (StringUtils.isNotBlank(this.confirmationMessage) && this.confirmComdition.getAsBoolean()) {
            this.pageContext.applyConfirmDialog(
                    this.confirmationMessage,
                    () -> exec());
        } else {
            exec();
        }
    }

    private void exec() {
        try {

            this.exec.apply(this)
                    .map(value -> {
                        this.pageContext.publishPageEvent(
                                new ActionEvent(this.definition, value));
                        return value;
                    })
                    .getOrThrow();

        } catch (final PageMessageException pme) {
            Action.this.pageContext.publishPageMessage(pme);
        } catch (final Throwable t) {
            log.error("Failed to execute action: {}", Action.this, t);
            Action.this.pageContext.notifyError("action.error.unexpected.message", t);
        }
    }

    public Action withExec(final Function<Action, Result<?>> exec) {
        this.exec = exec;
        return this;
    }

    public Action withSelectionSupplier(final Supplier<Set<String>> selectionSupplier) {
        this.selectionSupplier = selectionSupplier;
        return this;
    }

    public Action withConfirm(final String confirmationMessage) {
        this.confirmationMessage = confirmationMessage;
        return this;
    }

    public Action withSuccess(final String successMessage) {
        this.successMessage = successMessage;
        return this;
    }

    public Action withUpdateOnSelection() {
        this.updateOnSelection = true;
        return this;
    }

    public PageContext publish() {
        this.pageContext.publishPageEvent(new ActionPublishEvent(this));
        return this.pageContext;
    }

}
