/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.widget;

import static ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService.POLYGLOT_TREE_ITEM_TEXT_DATA_KEY;
import static ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService.POLYGLOT_WIDGET_FUNCTION_KEY;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;

@Lazy
@Service
@GuiProfile
public class WidgetFactory {

    private static final Logger log = LoggerFactory.getLogger(WidgetFactory.class);

    public enum IconButtonType {
        MAXIMIZE("maximize.png"),
        MINIMIZE("minimize.png"),
        SAVE_ACTION("saveAction.png"),
        NEW_ACTION("newAction.png"),
        DELETE_ACTION("deleteAction.png"),
        ;

        private String fileName;
        private ImageData image = null;

        private IconButtonType(final String fileName) {
            this.fileName = fileName;
        }

        public Image getImage(final Device device) {
            if (this.image == null) {
                try {
                    final InputStream resourceAsStream =
                            WidgetFactory.class.getResourceAsStream("/static/images/" + this.fileName);
                    this.image = new ImageData(resourceAsStream);
                } catch (final Exception e) {
                    log.error("Failed to load resource image: {}", this.fileName, e);
                }
            }

            return new Image(device, this.image);
        }

    }

    private final PolyglotPageService polyglotPageService;
    private final I18nSupport i18nSupport;

    public WidgetFactory(final PolyglotPageService polyglotPageService) {
        this.polyglotPageService = polyglotPageService;
        this.i18nSupport = polyglotPageService.getI18nSupport();
    }

    public Button buttonLocalized(final Composite parent, final String locTextKey) {
        final Button button = new Button(parent, SWT.NONE);
        this.injectI18n(button, new LocTextKey(locTextKey));
        return button;
    }

    public Button buttonLocalized(final Composite parent, final LocTextKey locTextKey) {
        final Button button = new Button(parent, SWT.NONE);
        this.injectI18n(button, locTextKey);
        return button;
    }

    public Button buttonLocalized(final Composite parent, final String style, final String locTextKey) {
        final Button button = new Button(parent, SWT.NONE);
        this.injectI18n(button, new LocTextKey(locTextKey));
        button.setData(RWT.CUSTOM_VARIANT, style);
        return button;
    }

    public Label label(final Composite parent, final String text) {
        final Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        return label;
    }

    public Label labelLocalized(final Composite parent, final String locTextKey) {
        final Label label = new Label(parent, SWT.NONE);
        this.injectI18n(label, new LocTextKey(locTextKey));
        return label;
    }

    public Label labelLocalized(final Composite parent, final LocTextKey locTextKey) {
        final Label label = new Label(parent, SWT.NONE);
        this.injectI18n(label, locTextKey);
        return label;
    }

    public Label labelLocalized(final Composite parent, final String style, final LocTextKey locTextKey) {
        final Label label = new Label(parent, SWT.NONE);
        this.injectI18n(label, locTextKey);
        label.setData(RWT.CUSTOM_VARIANT, style);
        return label;
    }

    public Label labelLocalized(
            final Composite parent,
            final LocTextKey locTextKey,
            final LocTextKey locToolTextKey) {

        final Label label = new Label(parent, SWT.NONE);
        this.injectI18n(label, locTextKey, locToolTextKey);
        return label;
    }

    public Label labelLocalized(
            final Composite parent,
            final String style,
            final LocTextKey locTextKey,
            final LocTextKey locToolTextKey) {

        final Label label = new Label(parent, SWT.NONE);
        this.injectI18n(label, locTextKey, locToolTextKey);
        label.setData(RWT.CUSTOM_VARIANT, style);
        return label;
    }

    public Tree treeLocalized(final Composite parent, final int style) {
        final Tree tree = new Tree(parent, SWT.SINGLE | SWT.FULL_SELECTION);
        this.injectI18n(tree);
        return tree;
    }

    public TreeItem treeItemLocalized(final Tree parent, final String locTextKey) {
        final TreeItem item = new TreeItem(parent, SWT.NONE);
        this.injectI18n(item, new LocTextKey(locTextKey));
        return item;
    }

    public TreeItem treeItemLocalized(final Tree parent, final LocTextKey locTextKey) {
        final TreeItem item = new TreeItem(parent, SWT.NONE);
        this.injectI18n(item, locTextKey);
        return item;
    }

    public TreeItem treeItemLocalized(final TreeItem parent, final String locTextKey) {
        final TreeItem item = new TreeItem(parent, SWT.NONE);
        this.injectI18n(item, new LocTextKey(locTextKey));
        return item;
    }

    public TreeItem treeItemLocalized(final TreeItem parent, final LocTextKey locTextKey) {
        final TreeItem item = new TreeItem(parent, SWT.NONE);
        this.injectI18n(item, locTextKey);
        return item;
    }

    public Table tableLocalized(final Composite parent) {
        final Table table = new Table(parent, SWT.NONE);
        this.injectI18n(table);
        return table;
    }

    public TableColumn tableColumnLocalized(final Table table, final String locTextKey) {
        final TableColumn tableColumn = new TableColumn(table, SWT.NONE);
        this.injectI18n(tableColumn, new LocTextKey(locTextKey));
        return tableColumn;
    }

    public Label labelSeparator(final Composite parent) {
        final Label label = new Label(parent, SWT.SEPARATOR);
        return label;
    }

    public Label imageButton(
            final IconButtonType type,
            final Composite parent,
            final LocTextKey toolTip,
            final Listener listener) {

        final Label imageButton = labelLocalized(parent, (LocTextKey) null, toolTip);
        imageButton.setData(RWT.CUSTOM_VARIANT, "imageButton");
        imageButton.setImage(type.getImage(parent.getDisplay()));
        if (listener != null) {
            imageButton.addListener(SWT.MouseDown, listener);
        }
        return imageButton;
    }

    public Label formLabelLocalized(final Composite parent, final String locTextKey) {
        final Label label = labelLocalized(parent, locTextKey);
        final GridData gridData = new GridData(SWT.RIGHT, SWT.CENTER, true, false);
        label.setLayoutData(gridData);
        return label;
    }

    public Label formValueLabel(final Composite parent, final String value, final int span) {
        final Label label = new Label(parent, SWT.NONE);
        label.setText(value);
        final GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, true, false, span, 1);
        label.setLayoutData(gridData);
        return label;
    }

    public Text formTextInput(final Composite parent, final String value) {
        return formTextInput(parent, value, 1, 1);
    }

    public Text formTextInput(final Composite parent, final String value, final int hspan, final int vspan) {
        final Text textInput = new Text(parent, SWT.LEFT | SWT.BORDER);
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false, hspan, vspan);
        gridData.heightHint = 15;
        textInput.setLayoutData(gridData);
        textInput.setText(value);
        return textInput;
    }

    public Combo formSingleSelectionLocalized(
            final Composite parent,
            final String selection,
            final List<Tuple<String>> items) {

        return formSingleSelectionLocalized(parent, selection, items, 1, 1);
    }

    public Combo formSingleSelectionLocalized(
            final Composite parent,
            final String selection,
            final List<Tuple<String>> items,
            final int hspan, final int vspan) {

        final SingleSelection combo = singleSelectionLocalized(parent, items);
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false, hspan, vspan);
        gridData.heightHint = 25;
        combo.setLayoutData(gridData);
        combo.select(selection);
        return combo;
    }

    public void formEmpty(final Composite parent) {
        formEmpty(parent, 1, 1);
    }

    public void formEmpty(final Composite parent, final int hspan, final int vspan) {
        final Label empty = new Label(parent, SWT.LEFT);
        empty.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, hspan, vspan));
        empty.setText("");
    }

    public SingleSelection singleSelectionLocalized(
            final Composite parent,
            final List<Tuple<String>> items) {

        final SingleSelection combo = new SingleSelection(parent, items);
        this.injectI18n(combo, combo.valueMapping);
        return combo;
    }

    public void injectI18n(final Label label, final LocTextKey locTextKey) {
        injectI18n(label, locTextKey, null);
    }

    public void injectI18n(final Label label, final LocTextKey locTextKey, final LocTextKey locToolTipKey) {
        final Consumer<Label> labelFunction = labelFunction(locTextKey, locToolTipKey, this.i18nSupport);
        label.setData(POLYGLOT_WIDGET_FUNCTION_KEY, labelFunction);
        labelFunction.accept(label);
    }

    public void injectI18n(final Button button, final LocTextKey locTextKey) {
        injectI18n(button, locTextKey, null);
    }

    public void injectI18n(final Button button, final LocTextKey locTextKey, final LocTextKey locToolTipKey) {
        final Consumer<Button> buttonFunction = buttonFunction(locTextKey, locToolTipKey, this.i18nSupport);
        button.setData(POLYGLOT_WIDGET_FUNCTION_KEY, buttonFunction);
        buttonFunction.accept(button);
    }

    public void injectI18n(final Tree tree) {
        tree.setData(POLYGLOT_WIDGET_FUNCTION_KEY, treeFunction(this.i18nSupport));
    }

    public void injectI18n(final TreeItem treeItem, final LocTextKey locTextKey) {
        treeItem.setData(POLYGLOT_TREE_ITEM_TEXT_DATA_KEY, locTextKey);
        treeItem.setText(this.i18nSupport.getText(locTextKey));
    }

    public void injectI18n(final Table table) {
        table.setData(POLYGLOT_WIDGET_FUNCTION_KEY, tableFunction(this.i18nSupport));
    }

    public void injectI18n(final TableColumn tableColumn, final LocTextKey locTextKey) {
        tableColumn.setData(POLYGLOT_TREE_ITEM_TEXT_DATA_KEY, locTextKey);
        tableColumn.setText(this.i18nSupport.getText(locTextKey));
    }

    public void injectI18n(final TableItem tableItem, final LocTextKey... locTextKey) {
        if (locTextKey == null) {
            return;
        }

        tableItem.setData(POLYGLOT_TREE_ITEM_TEXT_DATA_KEY, locTextKey);
        for (int i = 0; i < locTextKey.length; i++) {
            tableItem.setText(i, this.i18nSupport.getText(locTextKey[i]));
        }
    }

    public void injectI18n(final Combo combo, final List<String> items) {
        final Consumer<Combo> comboFunction = comboFunction(items, this.i18nSupport);
        combo.setData(POLYGLOT_WIDGET_FUNCTION_KEY, comboFunction);
        comboFunction.accept(combo);
    }

    public void createLanguageSelector(final PageContext composerCtx) {
        for (final Locale locale : this.i18nSupport.supportedLanguages()) {
            final Label languageSelection = new Label(composerCtx.getParent(), SWT.NONE);
            languageSelection.setData(POLYGLOT_WIDGET_FUNCTION_KEY,
                    langSelectionLabelFunction(locale, this.i18nSupport));
            languageSelection.setData(RWT.CUSTOM_VARIANT, "header");
            languageSelection.setText("|  " + locale.getLanguage().toUpperCase());
            //languageSelection.updateLocale(this.i18nSupport);
            languageSelection.addListener(SWT.MouseDown, event -> {
                this.polyglotPageService.setPageLocale(composerCtx.getRoot(), locale);
            });
        }
    }

    private static Consumer<Tree> treeFunction(final I18nSupport i18nSupport) {
        return tree -> updateLocale(tree.getItems(), i18nSupport);
    }

    private static Consumer<Table> tableFunction(final I18nSupport i18nSupport) {
        return table -> {
            updateLocale(table.getColumns(), i18nSupport);
            updateLocale(table.getItems(), i18nSupport);
        };
    }

    private static final Consumer<Label> langSelectionLabelFunction(
            final Locale locale,
            final I18nSupport i18nSupport) {

        return label -> label.setVisible(
                !i18nSupport.getCurrentLocale()
                        .getLanguage()
                        .equals(locale.getLanguage()));
    }

    private static final Consumer<Label> labelFunction(
            final LocTextKey locTextKey,
            final LocTextKey locToolTipKey,
            final I18nSupport i18nSupport) {

        return label -> {
            if (locTextKey != null) {
                label.setText(i18nSupport.getText(locTextKey));
            }
            if (locToolTipKey != null) {
                label.setToolTipText(i18nSupport.getText(locToolTipKey));
            }
        };
    }

    private static final Consumer<Combo> comboFunction(
            final List<String> items,
            final I18nSupport i18nSupport) {

        return combo -> {
            int i = 0;
            final Iterator<String> iterator = items.iterator();
            while (iterator.hasNext()) {
                combo.setItem(i, i18nSupport.getText(iterator.next()));
                i++;
            }
        };
    }

    private static final Consumer<Button> buttonFunction(
            final LocTextKey locTextKey,
            final LocTextKey locToolTipKey,
            final I18nSupport i18nSupport) {

        return button -> {
            if (locTextKey != null) {
                button.setText(i18nSupport.getText(locTextKey));
            }
            if (locToolTipKey != null) {
                button.setToolTipText(i18nSupport.getText(locToolTipKey));
            }
        };
    }

    private static final void updateLocale(final TreeItem[] items, final I18nSupport i18nSupport) {
        if (items == null) {
            return;
        }

        for (final TreeItem childItem : items) {
            final LocTextKey locTextKey = (LocTextKey) childItem.getData(POLYGLOT_TREE_ITEM_TEXT_DATA_KEY);
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
            final LocTextKey[] locTextKey = (LocTextKey[]) childItem.getData(POLYGLOT_TREE_ITEM_TEXT_DATA_KEY);
            if (locTextKey != null) {
                for (int i = 0; i < locTextKey.length; i++) {
                    if (locTextKey[i] != null) {
                        childItem.setText(i, i18nSupport.getText(locTextKey[i]));
                    }
                }
            }
        }
    }

    private static void updateLocale(final TableColumn[] columns, final I18nSupport i18nSupport) {
        if (columns == null) {
            return;
        }

        for (final TableColumn childItem : columns) {
            final LocTextKey locTextKey = (LocTextKey) childItem.getData(POLYGLOT_TREE_ITEM_TEXT_DATA_KEY);
            if (locTextKey != null) {
                childItem.setText(i18nSupport.getText(locTextKey));
            }
        }
    }

}
