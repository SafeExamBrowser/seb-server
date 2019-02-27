/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.impl;

import javax.servlet.http.HttpSession;

import org.eclipse.rap.rwt.RWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gui.content.MainPage;
import ch.ethz.seb.sebserver.gui.service.page.action.Action;

public final class MainPageState {

    private static final Logger log = LoggerFactory.getLogger(MainPageState.class);

    public Action action = null;

    private MainPageState() {
    }

    public static MainPageState get() {
        try {

            final HttpSession httpSession = RWT
                    .getUISession()
                    .getHttpSession();

            MainPageState mainPageState = (MainPageState) httpSession.getAttribute(MainPage.ATTR_MAIN_PAGE_STATE);
            if (mainPageState == null) {
                mainPageState = new MainPageState();
                httpSession.setAttribute(MainPage.ATTR_MAIN_PAGE_STATE, mainPageState);
            }

            return mainPageState;

        } catch (final RuntimeException re) {
            throw re;
        } catch (final Exception e) {
            log.error("Unexpected error while trying to get MainPageState from user-session");
        }

        return null;
    }

    public static void clear() {
        final MainPageState mainPageState = get();
        mainPageState.action = null;
    }
}