/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.widget;

import static ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService.*;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEvent;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEventListener;
import ch.ethz.seb.sebserver.gui.service.push.ServerPushService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestCall;
import ch.ethz.seb.sebserver.gui.service.table.TableBuilder;

@Lazy
@Service
@GuiProfile
public class WidgetFactory {

    private static final Logger log = LoggerFactory.getLogger(WidgetFactory.class);

    public enum ImageIcon {
        MAXIMIZE("maximize.png"),
        MINIMIZE("minimize.png"),
        EDIT("edit.png"),
        CANCEL("cancel.png"),
        CANCEL_EDIT("cancelEdit.png"),
        SHOW("show.png"),
        ACTIVE("active.png"),
        INACTIVE("inactive.png"),
        SAVE("save.png"),
        NEW("new.png"),
        DELETE("delete.png"),
        SEARCH("lens.png");

        private String fileName;
        private ImageData image = null;

        private ImageIcon(final String fileName) {
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

    public enum CustomVariant {
        TEXT_H1("h1"),
        TEXT_H2("h2"),
        TEXT_H3("h3"),
        IMAGE_BUTTON("imageButton"),
        TEXT_ACTION("action"),

        FOOTER("footer"),

        ;

        public final String key;

        private CustomVariant(final String key) {
            this.key = key;
        }
    }

    private final PolyglotPageService polyglotPageService;
    private final I18nSupport i18nSupport;
    private final ServerPushService serverPushService;

    public WidgetFactory(
            final PolyglotPageService polyglotPageService,
            final ServerPushService serverPushService) {

        this.polyglotPageService = polyglotPageService;
        this.i18nSupport = polyglotPageService.getI18nSupport();
        this.serverPushService = serverPushService;
    }

    public I18nSupport getI18nSupport() {
        return this.i18nSupport;
    }

    public Composite defaultPageLayout(final Composite parent) {
        final Composite content = new Composite(parent, SWT.NONE);
        final GridLayout contentLayout = new GridLayout();
        contentLayout.marginLeft = 10;
        content.setLayout(contentLayout);
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        return content;
    }

    public Composite defaultPageLayout(final Composite parent, final LocTextKey title) {
        final Composite defaultPageLayout = defaultPageLayout(parent);
        final Label labelLocalizedTitle = labelLocalizedTitle(defaultPageLayout, title);
        labelLocalizedTitle.setLayoutData(new GridData(SWT.TOP, SWT.LEFT, true, false));
        return defaultPageLayout;
    }

    public Composite defaultPageLayout(
            final Composite parent,
            final LocTextKey title,
            final ActionDefinition actionDefinition,
            final Function<Label, Consumer<ActionEvent>> eventFunction) {

        final Composite defaultPageLayout = defaultPageLayout(parent);
        final Label labelLocalizedTitle = labelLocalizedTitle(defaultPageLayout, title);
        labelLocalizedTitle.setLayoutData(new GridData(SWT.TOP, SWT.LEFT, true, false));
        ActionEventListener.injectListener(
                labelLocalizedTitle,
                actionDefinition,
                eventFunction.apply(labelLocalizedTitle));
        return defaultPageLayout;
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

    public Button buttonLocalized(final Composite parent, final CustomVariant variant, final String locTextKey) {
        final Button button = new Button(parent, SWT.NONE);
        this.injectI18n(button, new LocTextKey(locTextKey));
        button.setData(RWT.CUSTOM_VARIANT, variant.key);
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

    public Label labelLocalized(final Composite parent, final CustomVariant variant, final LocTextKey locTextKey) {
        final Label label = new Label(parent, SWT.NONE);
        this.injectI18n(label, locTextKey);
        label.setData(RWT.CUSTOM_VARIANT, variant.key);
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
            final CustomVariant variant,
            final LocTextKey locTextKey,
            final LocTextKey locToolTextKey) {

        final Label label = new Label(parent, SWT.NONE);
        this.injectI18n(label, locTextKey, locToolTextKey);
        label.setData(RWT.CUSTOM_VARIANT, variant.key);
        return label;
    }

    public Label labelLocalizedTitle(final Composite content, final LocTextKey locTextKey) {
        final Label labelLocalized = labelLocalized(content, CustomVariant.TEXT_H1, locTextKey);
        labelLocalized.setLayoutData(new GridData(SWT.TOP, SWT.LEFT, true, false));
        return labelLocalized;
    }

    public Tree treeLocalized(final Composite parent, final int style) {
        final Tree tree = new Tree(parent, style);
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

    public <T extends Entity> TableBuilder<T> entityTableBuilder(final RestCall<Page<T>> apiCall) {
        return new TableBuilder<>(this, apiCall);
    }

    public Table tableLocalized(final Composite parent) {
        final Table table = new Table(parent, SWT.SINGLE | SWT.NO_SCROLL);
        this.injectI18n(table);
        return table;
    }

    public TableColumn tableColumnLocalized(
            final Table table,
            final LocTextKey locTextKey,
            final LocTextKey toolTipKey) {

        final TableColumn tableColumn = new TableColumn(table, SWT.NONE);
        this.injectI18n(tableColumn, locTextKey, toolTipKey);
        return tableColumn;
    }

    public Label labelSeparator(final Composite parent) {
        final Label label = new Label(parent, SWT.SEPARATOR);
        return label;
    }

    public Label imageButton(
            final ImageIcon type,
            final Composite parent,
            final LocTextKey toolTip,
            final Listener listener) {

        final Label imageButton = labelLocalized(parent, (LocTextKey) null, toolTip);
        imageButton.setData(RWT.CUSTOM_VARIANT, CustomVariant.IMAGE_BUTTON.name());
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
        label.setText((StringUtils.isNoneBlank(value)) ? value : Constants.EMPTY_NOTE);
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
        if (value != null) {
            textInput.setText(value);
        }
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

    public LanguageSelector countrySelector(final Composite parent) {
        return new LanguageSelector(parent, this.i18nSupport);
    }

    public ImageUpload formImageUpload(
            final Composite parent,
            final String value,
            final LocTextKey locTextKey,
            final int hspan,
            final int vspan,
            final boolean readonly) {

        final ImageUpload imageUpload = imageUploadLocalized(parent, locTextKey, readonly);
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false, hspan, vspan);
        imageUpload.setLayoutData(gridData);
        imageUpload.setImageBase64(value);
        return imageUpload;
    }

    public ImageUpload imageUploadLocalized(
            final Composite parent,
            final LocTextKey locTextKey,
            final boolean readonly) {

        final ImageUpload imageUpload = new ImageUpload(parent, this.serverPushService, readonly);
        injectI18n(imageUpload, locTextKey);
        return imageUpload;
    }

    public void injectI18n(final ImageUpload imageUpload, final LocTextKey locTextKey) {
        final Consumer<ImageUpload> imageUploadFunction = imageUploadFunction(locTextKey, this.i18nSupport);
        imageUpload.setData(POLYGLOT_WIDGET_FUNCTION_KEY, imageUploadFunction);
        imageUploadFunction.accept(imageUpload);
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

    public void injectI18n(final TableColumn tableColumn, final LocTextKey locTextKey, final LocTextKey locTooltipKey) {
        tableColumn.setData(POLYGLOT_TREE_ITEM_TEXT_DATA_KEY, locTextKey);
        tableColumn.setText(this.i18nSupport.getText(locTextKey));

        if (locTooltipKey != null) {
            tableColumn.setData(POLYGLOT_TREE_ITEM_TOOLTIP_DATA_KEY, locTooltipKey);
            tableColumn.setToolTipText(this.i18nSupport.getText(locTooltipKey));
        }
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

    public static void clearComposite(final Composite parent) {
        if (parent == null) {
            return;
        }

        for (final Control control : parent.getChildren()) {
            control.dispose();
        }
    }

    private static Consumer<ImageUpload> imageUploadFunction(
            final LocTextKey locTextKey,
            final I18nSupport i18nSupport) {

        return imageUpload -> {
            if (locTextKey != null) {
                imageUpload.setSelectionText(i18nSupport.getText(locTextKey));
            }
        };
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
                // TODO managing a tool-tip delay is not working as expected
                //      is there another way to achieve this?
//                label.addListener(SWT.MouseEnter, event -> {
//                    System.out.println("*************** set tooltip delay");
//                    label.getDisplay().timerExec(1000, () -> {
//                        System.out.println("*************** set tooltip");
//                        label.setToolTipText(i18nSupport.getText(locToolTipKey));
//                    });
//                });
//                label.addListener(SWT.MouseExit, event -> {
//                    label.setToolTipText(null);
//                });
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
