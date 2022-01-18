/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.push;

import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.widget.Message;

public final class UpdateErrorHandler implements Function<Exception, Boolean> {

    private static final Logger log = LoggerFactory.getLogger(UpdateErrorHandler.class);

    private final PageService pageService;
    private final PageContext pageContext;

    private int errors = 0;

    public UpdateErrorHandler(
            final PageService pageService,
            final PageContext pageContext) {

        this.pageService = pageService;
        this.pageContext = pageContext;
    }

    private boolean checkUserSession() {
        try {
            this.pageService.getCurrentUser().get();
            return true;
        } catch (final Exception e) {
            try {
                this.pageContext.forwardToLoginPage();
                final MessageBox logoutSuccess = new Message(
                        this.pageContext.getShell(),
                        this.pageService.getI18nSupport().getText("sebserver.logout"),
                        Utils.formatLineBreaks(
                                this.pageService.getI18nSupport()
                                        .getText("sebserver.logout.invalid-session.message")),
                        SWT.ICON_INFORMATION,
                        this.pageService.getI18nSupport());
                logoutSuccess.open(null);
            } catch (final Exception ee) {
                log.warn("Unable to auto-logout: ", ee.getMessage());
            }
            return true;
        }
    }

    @Override
    public Boolean apply(final Exception error) {
        this.errors++;
        log.error("Failed to update server push: {}", error.getMessage());
        if (this.errors > 5) {
            checkUserSession();
        }
        return this.errors > 5;
    }
}