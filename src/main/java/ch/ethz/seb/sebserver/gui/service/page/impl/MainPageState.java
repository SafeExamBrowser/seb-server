/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.impl;

public final class MainPageState {

//    private static final Logger log = LoggerFactory.getLogger(MainPageState.class);
//
//    private PageAction action = null;
//
//    private MainPageState() {
//    }
//
//    public action
//
//    public boolean changeTo(final PageAction action) {
//        if (this.action.definition != action.definition) {
//            this.action = action;
//            return true;
//        }
//
//        return false;
//    }
//
//    public static MainPageState get() {
//        try {
//
//            final HttpSession httpSession = RWT
//                    .getUISession()
//                    .getHttpSession();
//
//            MainPageState mainPageState = (MainPageState) httpSession.getAttribute(MainPage.ATTR_MAIN_PAGE_STATE);
//            if (mainPageState == null) {
//                mainPageState = new MainPageState();
//                httpSession.setAttribute(MainPage.ATTR_MAIN_PAGE_STATE, mainPageState);
//            }
//
//            return mainPageState;
//
//        } catch (final RuntimeException re) {
//            throw re;
//        } catch (final Exception e) {
//            log.error("Unexpected error while trying to get MainPageState from user-session");
//        }
//
//        return null;
//    }
//
//    public static void clear() {
//        final MainPageState mainPageState = get();
//        mainPageState.action = null;
//    }
}