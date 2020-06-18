/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.i18n;

import java.util.Locale;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.widget.ImageUploadSelection;

public interface PolyglotPageService {

    String POLYGLOT_WIDGET_FUNCTION_KEY = "POLYGLOT_WIDGET_FUNCTION";
    String POLYGLOT_ITEM_TEXT_DATA_KEY = "POLYGLOT_ITEM_TEXT_DATA";
    String POLYGLOT_ITEM_TOOLTIP_DATA_KEY = "POLYGLOT_ITEM_TOOLTIP_DATA";

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

    /** Used to inject a localized text within the given Control (Widget) that automatically gets changed on language
     * change.
     *
     * @param imageUpload the Control instance
     * @param locTextKey the localized text key to inject */
    void injectI18n(ImageUploadSelection imageUpload, LocTextKey locTextKey);

    /** Used to inject a localized text within the given Control (Widget) that automatically gets changed on language
     * change.
     *
     * @param label the Control instance
     * @param locTextKey the localized text key to inject */
    void injectI18n(Label label, LocTextKey locTextKey);

    /** Used to inject a localized text within the given Control (Widget) that automatically gets changed on language
     * change.
     *
     * @param label the Control instance
     * @param locTextKey the localized text key to inject
     * @param locToolTipKey the localized text key for the tool-tip to inject */
    void injectI18n(Label label, LocTextKey locTextKey, LocTextKey locToolTipKey);

    /** Used to inject a localized text within the given Control (Widget) that automatically gets changed on language
     * change.
     *
     * @param group the Control instance
     * @param locTextKey the localized text key to inject
     * @param locTooltipKey the localized text key for the tool-tip to inject */
    void injectI18n(Group group, LocTextKey locTextKey, LocTextKey locTooltipKey);

    /** Used to inject a localized text within the given Control (Widget) that automatically gets changed on language
     * change.
     *
     * @param button the Control instance
     * @param locTextKey the localized text key to inject */
    void injectI18n(Button button, LocTextKey locTextKey);

    /** Used to inject a localized text within the given Control (Widget) that automatically gets changed on language
     * change.
     *
     * @param button the Control instance
     * @param locTextKey the localized text key to inject
     * @param locToolTipKey the localized text key for the tool-tip to inject */
    void injectI18n(Button button, LocTextKey locTextKey, LocTextKey locToolTipKey);

    /** Used to inject a localized text within the given Control (Widget) that automatically gets changed on language
     * change.
     *
     * @param tree the Control instance */
    void injectI18n(Tree tree);

    /** Used to inject a localized text within the given Control (Widget) that automatically gets changed on language
     * change.
     *
     * @param treeItem the Control instance
     * @param locTextKey the localized text key to inject */
    void injectI18n(TreeItem treeItem, LocTextKey locTextKey);

    /** Used to inject a localized text within the given Control (Widget) that automatically gets changed on language
     * change.
     *
     * @param table the Control instance */
    void injectI18n(Table table);

    /** Used to inject a localized text within the given Control (Widget) that automatically gets changed on language
     * change.
     *
     * @param tabFolder the Control instance */
    void injectI18n(TabFolder tabFolder);

    /** Used to inject a localized text within the given Control (Widget) that automatically gets changed on language
     * change.
     *
     * @param tableColumn the Control instance
     * @param locTextKey the localized text key to inject
     * @param locTooltipKey the localized text key for the tooltip to inject */
    void injectI18n(TableColumn tableColumn, LocTextKey locTextKey, LocTextKey locTooltipKey);

    /** Used to inject a localized text within the given Control (Widget) that automatically gets changed on language
     * change.
     *
     * @param tabItem the Control instance
     * @param locTextKey the localized text key to inject
     * @param locTooltipKey the localized text key for the tool-tip to inject */
    void injectI18n(TabItem tabItem, LocTextKey locTextKey, LocTextKey locTooltipKey);

    /** Used to inject a localized tool-tip text within the given Control (Widget) that automatically gets changed on
     * language change.
     *
     * @param control the Control instance
     * @param locTooltipKey the localized text key for the tool-tip to inject */
    void injectI18nTooltip(Control control, LocTextKey locTooltipKey);

    /** Used to create the page language selector if needed
     *
     * @param composerCtx the PageContext */
    void createLanguageSelector(PageContext composerCtx);

}