/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.action;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionPublishEvent;

public class SafeActionExecution implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(SafeActionExecution.class);

    private final PageContext pageContext;
    private final ActionPublishEvent actionEvent;
    private final Runnable actionExecution;

    public SafeActionExecution(
            final PageContext pageContext,
            final ActionPublishEvent actionEvent,
            final Runnable actionExecution) {

        this.pageContext = pageContext;
        this.actionEvent = actionEvent;
        this.actionExecution = actionExecution;
    }

    @Override
    public void run() {
        try {
            if (StringUtils.isNotBlank(this.actionEvent.confirmationMessage)) {
                this.pageContext.applyConfirmDialog(
                        this.actionEvent.confirmationMessage,
                        createConfirmationCallback());
            } else {
                this.actionExecution.run();
            }
        } catch (final Throwable t) {
            log.error("Failed to execute action: {}", this.actionEvent, t);
            this.pageContext.notifyError("action.error.unexpected.message", t);
        }
    }

    private final Runnable createConfirmationCallback() {
        return new Runnable() {

            @Override
            public void run() {
                try {
                    SafeActionExecution.this.actionExecution.run();
                } catch (final Throwable t) {
                    log.error("Failed to execute action: {}", SafeActionExecution.this.actionEvent, t);
                    SafeActionExecution.this.pageContext.notifyError("action.error.unexpected.message", t);
                }

            }
        };
    }

}
