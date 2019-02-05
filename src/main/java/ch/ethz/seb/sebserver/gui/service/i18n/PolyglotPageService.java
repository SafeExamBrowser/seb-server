/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.i18n;

import java.util.Locale;

import org.eclipse.swt.widgets.Composite;

public interface PolyglotPageService {

    String POLYGLOT_WIDGET_FUNCTION_KEY = "POLYGLOT_WIDGET_FUNCTION";
    String POLYGLOT_TREE_ITEM_TEXT_DATA_KEY = "POLYGLOT_TREE_ITEM_TEXT_DATA";
    String POLYGLOT_TREE_ITEM_TOOLTIP_DATA_KEY = "POLYGLOT_TREE_ITEM_TOOLTIP_DATA";

    /** Gets the underling I18nSupport
     *
     * @return the underling I18nSupport */
    I18nSupport getI18nSupport();

    /** The default locale for the page.
     * Uses I18nSupport.getCurrentLocale to do so.
     *
     * @param root the root Composite of the page to change the language */
    void setDefaultPageLocale(Composite root);

    /** Sets the given Locale and if needed, updates the page language according to the
     * given Locale
     *
     * @param root root the root Composite of the page to change the language
     * @param locale the Locale to set */
    void setPageLocale(Composite root, Locale locale);

}