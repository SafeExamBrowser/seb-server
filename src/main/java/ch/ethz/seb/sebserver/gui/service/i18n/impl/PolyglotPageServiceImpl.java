/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.i18n.impl;

import java.util.Locale;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService;
import ch.ethz.seb.sebserver.gui.service.page.ComposerService;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.widget.ImageUploadSelection;

/** Service that supports page language change on the fly */
@Lazy
@Service
@GuiProfile
public final class PolyglotPageServiceImpl implements PolyglotPageService {

    private final I18nSupport i18nSupport;

    public PolyglotPageServiceImpl(final I18nSupport i18nSupport) {
        this.i18nSupport = i18nSupport;
    }

    @Override
    public I18nSupport getI18nSupport() {
        return this.i18nSupport;
    }

    @Override
    public void setDefaultPageLocale(final Composite root) {
        setPageLocale(root, this.i18nSupport.getUsersLanguageLocale());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setPageLocale(final Composite root, final Locale locale) {
        RWT.getUISession()
                .getHttpSession()
                .setAttribute(I18nSupport.ATTR_CURRENT_SESSION_LOCALE, locale);

        ComposerService.traversePageTree(
                root,
                comp -> comp.getData(POLYGLOT_WIDGET_FUNCTION_KEY) != null,
                comp -> ((Consumer<Control>) comp.getData(POLYGLOT_WIDGET_FUNCTION_KEY)).accept(comp));

        root.layout(true, true);
    }

    @Override
    public void injectI18n(final ImageUploadSelection imageUpload, final LocTextKey locTextKey) {
        final Consumer<ImageUploadSelection> imageUploadFunction = iu -> {
            if (locTextKey != null) {
                iu.setSelectionText(this.i18nSupport.getText(locTextKey));
            }
        };
        imageUpload.setData(POLYGLOT_WIDGET_FUNCTION_KEY, imageUploadFunction);
        imageUploadFunction.accept(imageUpload);
    }

    @Override
    public void injectI18n(final Label label, final LocTextKey locTextKey) {
        injectI18n(label, locTextKey, null);
    }

    @Override
    public void injectI18n(final Label label, final LocTextKey locTextKey, final LocTextKey locToolTipKey) {
        final Consumer<Label> labelFunction = labelFunction(locTextKey, locToolTipKey, this.i18nSupport);
        label.setData(POLYGLOT_WIDGET_FUNCTION_KEY, labelFunction);
        labelFunction.accept(label);
    }

    @Override
    public void injectI18n(final Group group, final LocTextKey locTextKey, final LocTextKey locTooltipKey) {
        final Consumer<Group> groupFunction = groupFunction(locTextKey, locTooltipKey, this.i18nSupport);
        group.setData(POLYGLOT_WIDGET_FUNCTION_KEY, groupFunction);
        groupFunction.accept(group);
    }

    @Override
    public void injectI18n(final ExpandBar expandBar, final LocTextKey locTooltipKey) {
        expandBar.setData(
                POLYGLOT_WIDGET_FUNCTION_KEY,
                (Consumer<ExpandBar>) _expandBar -> {
                    if (locTooltipKey != null) {
                        _expandBar.setToolTipText(this.i18nSupport.getText(locTooltipKey));
                    }
                    updateLocale(_expandBar.getItems(), this.i18nSupport);
                });
    }

    @Override
    public void injectI18n(final ExpandItem expandItem, final LocTextKey locTextKey) {
        expandItem.setData(POLYGLOT_ITEM_TEXT_DATA_KEY, locTextKey);
        expandItem.setText(this.i18nSupport.getText(locTextKey));
    }

    @Override
    public void injectI18n(final Button button, final LocTextKey locTextKey) {
        injectI18n(button, locTextKey, null);
    }

    @Override
    public void injectI18n(final Button button, final LocTextKey locTextKey, final LocTextKey locToolTipKey) {
        final Consumer<Button> buttonFunction = b -> {
            if (locTextKey != null) {
                b.setText(Utils.formatLineBreaks(this.i18nSupport.getText(locTextKey)));
            }
            if (this.i18nSupport.hasText(locToolTipKey)) {
                b.setToolTipText(Utils.formatLineBreaks(this.i18nSupport.getText(locToolTipKey)));
            }
        };
        button.setData(POLYGLOT_WIDGET_FUNCTION_KEY, buttonFunction);
        buttonFunction.accept(button);
    }

    @Override
    public void injectI18n(final Tree tree) {
        tree.setData(
                POLYGLOT_WIDGET_FUNCTION_KEY,
                (Consumer<Tree>) t -> updateLocale(t.getItems(), this.i18nSupport));
    }

    @Override
    public void injectI18n(final TreeItem treeItem, final LocTextKey locTextKey) {
        treeItem.setData(POLYGLOT_ITEM_TEXT_DATA_KEY, locTextKey);
        treeItem.setText(this.i18nSupport.getText(locTextKey));
    }

    @Override
    public void injectI18n(final Table table) {
        table.setData(
                POLYGLOT_WIDGET_FUNCTION_KEY,
                (Consumer<Table>) t -> {
                    updateLocale(t.getColumns(), this.i18nSupport);
                    updateLocale(t.getItems(), this.i18nSupport);
                });
    }

    @Override
    public void injectI18n(final TabFolder tabFolder) {
        tabFolder.setData(
                POLYGLOT_WIDGET_FUNCTION_KEY,
                (Consumer<TabFolder>) t -> updateLocale(t.getItems(), this.i18nSupport));
    }

    @Override
    public void injectI18n(final TableColumn tableColumn, final LocTextKey locTextKey, final LocTextKey locTooltipKey) {
        tableColumn.setData(POLYGLOT_ITEM_TEXT_DATA_KEY, locTextKey);
        tableColumn.setText(this.i18nSupport.getText(locTextKey));

        if (this.i18nSupport.hasText(locTooltipKey)) {
            tableColumn.setData(POLYGLOT_ITEM_TOOLTIP_DATA_KEY, locTooltipKey);
            tableColumn.setToolTipText(Utils.formatLineBreaks(this.i18nSupport.getText(locTooltipKey)));
        }
    }

    @Override
    public void injectI18n(final TabItem tabItem, final LocTextKey locTextKey, final LocTextKey locTooltipKey) {
        tabItem.setData(POLYGLOT_ITEM_TEXT_DATA_KEY, locTextKey);
        tabItem.setText(this.i18nSupport.getText(locTextKey));

        if (this.i18nSupport.hasText(locTooltipKey)) {
            tabItem.setData(POLYGLOT_ITEM_TOOLTIP_DATA_KEY, locTooltipKey);
            tabItem.setToolTipText(Utils.formatLineBreaks(this.i18nSupport.getText(locTooltipKey)));
        }
    }

    @Override
    public void injectI18nTooltip(final Control control, final LocTextKey locTooltipKey) {
        if (locTooltipKey == null || control == null) {
            return;
        }

        if (this.i18nSupport.hasText(locTooltipKey)
                && StringUtils.isNotBlank(this.i18nSupport.getText(locTooltipKey, ""))) {
            control.setData(POLYGLOT_ITEM_TOOLTIP_DATA_KEY, locTooltipKey);
            control.setToolTipText(Utils.formatLineBreaks(this.i18nSupport.getText(locTooltipKey)));
        }
    }

    @Override
    public void createLanguageSelector(final PageContext composerCtx) {
        for (final Locale locale : this.i18nSupport.supportedLanguages()) {
            final Label languageSelection = new Label(composerCtx.getParent(), SWT.NONE);
            languageSelection.setData(
                    POLYGLOT_WIDGET_FUNCTION_KEY,
                    (Consumer<Label>) label -> label.setVisible(
                            !this.i18nSupport.getUsersLanguageLocale()
                                    .getLanguage()
                                    .equals(locale.getLanguage())));
            languageSelection.setData(RWT.CUSTOM_VARIANT, "header");
            languageSelection.setText("|  " + locale.getLanguage().toUpperCase(locale));
            languageSelection.addListener(SWT.MouseDown, event -> this.setPageLocale(composerCtx.getRoot(), locale));
        }
    }

    private static Consumer<Label> labelFunction(
            final LocTextKey locTextKey,
            final LocTextKey locToolTipKey,
            final I18nSupport i18nSupport) {

        return label -> {
            if (locTextKey != null) {
                label.setText(i18nSupport.getText(locTextKey));
            }
            if (i18nSupport.hasText(locToolTipKey)) {
                label.setToolTipText(Utils.formatLineBreaks(i18nSupport.getText(locToolTipKey)));
            }
        };
    }

    private static Consumer<Group> groupFunction(
            final LocTextKey locTextKey,
            final LocTextKey locToolTipKey,
            final I18nSupport i18nSupport) {

        return group -> {
            if (locTextKey != null) {
                group.setText(i18nSupport.getText(locTextKey));
            }
            if (i18nSupport.hasText(locToolTipKey)) {
                group.setToolTipText(Utils.formatLineBreaks(i18nSupport.getText(locToolTipKey, StringUtils.EMPTY)));
            }
        };
    }

    private static void updateLocale(final TabItem[] items, final I18nSupport i18nSupport) {
        if (items == null) {
            return;
        }

        for (final TabItem childItem : items) {
            final LocTextKey locTextKey = (LocTextKey) childItem.getData(POLYGLOT_ITEM_TEXT_DATA_KEY);
            if (locTextKey != null) {
                childItem.setText(i18nSupport.getText(locTextKey));
            }
        }
    }

    private static void updateLocale(final TreeItem[] items, final I18nSupport i18nSupport) {
        if (items == null) {
            return;
        }

        for (final TreeItem childItem : items) {
            final LocTextKey locTextKey = (LocTextKey) childItem.getData(POLYGLOT_ITEM_TEXT_DATA_KEY);
            if (locTextKey != null) {
                childItem.setText(i18nSupport.getText(locTextKey));
            }
            updateLocale(childItem.getItems(), i18nSupport);
        }
    }

    private static void updateLocale(final TableItem[] items, final I18nSupport i18nSupport) {
        if (items == null) {
            return;
        }

        for (final TableItem childItem : items) {
            final LocTextKey[] locTextKey = (LocTextKey[]) childItem.getData(POLYGLOT_ITEM_TEXT_DATA_KEY);
            if (locTextKey != null) {
                for (int i = 0; i < locTextKey.length; i++) {
                    if (locTextKey[i] != null) {
                        childItem.setText(i, i18nSupport.getText(locTextKey[i]));
                    }
                }
            }
        }
    }

    private static void updateLocale(final ExpandItem[] items, final I18nSupport i18nSupport) {
        if (items == null) {
            return;
        }

        for (final ExpandItem childItem : items) {
            final LocTextKey locTextKey = (LocTextKey) childItem.getData(POLYGLOT_ITEM_TEXT_DATA_KEY);
            if (locTextKey != null) {
                childItem.setText(i18nSupport.getText(locTextKey));
            }
        }
    }

    private static void updateLocale(final TableColumn[] columns, final I18nSupport i18nSupport) {
        if (columns == null) {
            return;
        }

        for (final TableColumn childItem : columns) {
            final LocTextKey locTextKey = (LocTextKey) childItem.getData(POLYGLOT_ITEM_TEXT_DATA_KEY);
            if (locTextKey != null) {
                childItem.setText(i18nSupport.getText(locTextKey));
            }
        }
    }

}
