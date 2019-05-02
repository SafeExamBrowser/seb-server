/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.widget;

import static ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService.*;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
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

import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.Threshold;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.push.ServerPushService;

@Lazy
@Service
@GuiProfile
public class WidgetFactory {

    private static final Logger log = LoggerFactory.getLogger(WidgetFactory.class);

    public enum ImageIcon {
        MAXIMIZE("maximize.png"),
        MINIMIZE("minimize.png"),
        ADD("add.png"),
        REMOVE("remove.png"),
        ADD_BOX("add_box.png"),
        REMOVE_BOX("remove_box.png"),
        EDIT("edit.png"),
        TEST("test.png"),
        IMPORT("import.png"),
        CANCEL("cancel.png"),
        CANCEL_EDIT("cancelEdit.png"),
        SHOW("show.png"),
        ACTIVE("active.png"),
        INACTIVE("inactive.png"),
        SAVE("save.png"),
        NEW("new.png"),
        DELETE("delete.png"),
        SEARCH("lens.png"),
        COLOR("color.png");

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

        SELECTION("selection"),
        SELECTED("selected"),
        SELECTION_READONLY("selectionReadonly"),

        FOOTER("footer"),
        TITLE_LABEL("head"),

        MESSAGE("message"),
        ERROR("error")

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
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        content.setLayoutData(gridData);
        return content;
    }

    public Composite defaultPageLayout(final Composite parent, final LocTextKey title) {
        final Composite defaultPageLayout = defaultPageLayout(parent);
        final Label labelLocalizedTitle = labelLocalizedTitle(defaultPageLayout, title);
        final GridData gridData = new GridData(SWT.TOP, SWT.LEFT, true, false);
        labelLocalizedTitle.setLayoutData(gridData);
        return defaultPageLayout;
    }

    public Composite defaultPageLayout(
            final Composite parent,
            final LocTextKey title,
            final ActionDefinition actionDefinition) {

        final Composite defaultPageLayout = defaultPageLayout(parent);
        final Label labelLocalizedTitle = labelLocalizedTitle(defaultPageLayout, title);
        labelLocalizedTitle.setLayoutData(new GridData(SWT.TOP, SWT.LEFT, true, false));
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

    public Label labelLocalized(
            final Composite parent,
            final LocTextKey locTextKey,
            final String defaultValue) {

        final Label label = new Label(parent, SWT.NONE);

        final String text = this.i18nSupport.getText(locTextKey, "");
        if (StringUtils.isNoneBlank(text)) {
            this.injectI18n(label, locTextKey);
        } else {
            this.injectI18n(label, new LocTextKey(defaultValue));
        }

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

    public Text textInput(final Composite content) {
        return textInput(content, false);
    }

    public Text passwordInput(final Composite content) {
        return textInput(content, true);
    }

    public Text textAreaInput(final Composite content) {
        return new Text(content, SWT.LEFT | SWT.BORDER | SWT.MULTI);
    }

    public Text textInput(final Composite content, final boolean password) {
        return new Text(content, (password)
                ? SWT.LEFT | SWT.BORDER | SWT.PASSWORD
                : SWT.LEFT | SWT.BORDER);
    }

    public Text numberInput(final Composite content, final Consumer<String> numberCheck) {
        final Text numberInput = new Text(content, SWT.RIGHT | SWT.BORDER);
        if (numberCheck != null) {
            numberInput.addListener(SWT.Verify, event -> {
                final String value = event.text;
                try {
                    numberCheck.accept(value);
                } catch (final Exception e) {
                    event.doit = false;
                }
            });
        }
        return numberInput;
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

    public TabFolder tabFolderLocalized(final Composite parent) {
        final TabFolder tabs = new TabFolder(parent, SWT.NONE);
        this.injectI18n(tabs);
        return tabs;
    }

    public TabItem tabItemLocalized(
            final TabFolder parent,
            final LocTextKey locTextKey,
            final LocTextKey toolTipKey) {

        final TabItem tabItem = new TabItem(parent, SWT.NONE);
        this.injectI18n(tabItem, locTextKey, toolTipKey);
        return tabItem;
    }

    public Label labelSeparator(final Composite parent) {
        final Label label = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        final GridData data = new GridData(SWT.FILL, SWT.TOP, true, true);
        label.setLayoutData(data);
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

    public Selection selectionLocalized(
            final Selection.Type type,
            final Composite parent,
            final Supplier<List<Tuple<String>>> itemsSupplier) {

        final Selection selection;
        switch (type) {
            case SINGLE:
                selection = new SingleSelection(parent);
                break;
            case MULTI:
                selection = new MultiSelection(parent);
                break;
            case MULTI_COMBO:
                selection = new MultiSelectionCombo(parent, this);
                break;
            case COLOR:
                selection = new ColorSelection(parent, this);
                break;
            default:
                throw new IllegalArgumentException("Unsupported Selection.Type: " + type);
        }

        if (itemsSupplier != null) {
            final Consumer<Selection> updateFunction = ss -> ss.applyNewMapping(itemsSupplier.get());
            selection.adaptToControl().setData(POLYGLOT_WIDGET_FUNCTION_KEY, updateFunction);
            updateFunction.accept(selection);
        }
        return selection;
    }

    public ThresholdList thresholdList(final Composite parent, final Collection<Threshold> values) {
        final ThresholdList thresholdList = new ThresholdList(parent, this);
        if (values != null) {
            thresholdList.setThresholds(values);
        }
        return thresholdList;
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
        final Consumer<ImageUpload> imageUploadFunction = iu -> {
            if (locTextKey != null) {
                iu.setSelectionText(this.i18nSupport.getText(locTextKey));
            }
        };
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
        final Consumer<Button> buttonFunction = b -> {
            if (locTextKey != null) {
                b.setText(this.i18nSupport.getText(locTextKey));
            }
            if (locToolTipKey != null) {
                b.setToolTipText(this.i18nSupport.getText(locToolTipKey));
            }
        };
        button.setData(POLYGLOT_WIDGET_FUNCTION_KEY, buttonFunction);
        buttonFunction.accept(button);
    }

    public void injectI18n(final Tree tree) {
        tree.setData(
                POLYGLOT_WIDGET_FUNCTION_KEY,
                (Consumer<Tree>) t -> updateLocale(t.getItems(), this.i18nSupport));
    }

    public void injectI18n(final TreeItem treeItem, final LocTextKey locTextKey) {
        treeItem.setData(POLYGLOT_ITEM_TEXT_DATA_KEY, locTextKey);
        treeItem.setText(this.i18nSupport.getText(locTextKey));
    }

    public void injectI18n(final Table table) {
        table.setData(
                POLYGLOT_WIDGET_FUNCTION_KEY,
                (Consumer<Table>) t -> {
                    updateLocale(t.getColumns(), this.i18nSupport);
                    updateLocale(t.getItems(), this.i18nSupport);
                });
    }

    public void injectI18n(final TabFolder tabFolder) {
        tabFolder.setData(
                POLYGLOT_WIDGET_FUNCTION_KEY,
                (Consumer<TabFolder>) t -> updateLocale(t.getItems(), this.i18nSupport));
    }

    public void injectI18n(final TableColumn tableColumn, final LocTextKey locTextKey, final LocTextKey locTooltipKey) {
        tableColumn.setData(POLYGLOT_ITEM_TEXT_DATA_KEY, locTextKey);
        tableColumn.setText(this.i18nSupport.getText(locTextKey));

        if (locTooltipKey != null) {
            tableColumn.setData(POLYGLOT_ITEM_TOOLTIP_DATA_KEY, locTooltipKey);
            tableColumn.setToolTipText(this.i18nSupport.getText(locTooltipKey));
        }
    }

    public void injectI18n(final TableItem tableItem, final LocTextKey... locTextKey) {
        if (locTextKey == null) {
            return;
        }

        tableItem.setData(POLYGLOT_ITEM_TEXT_DATA_KEY, locTextKey);
        for (int i = 0; i < locTextKey.length; i++) {
            tableItem.setText(i, this.i18nSupport.getText(locTextKey[i]));
        }
    }

    private void injectI18n(final TabItem tabItem, final LocTextKey locTextKey, final LocTextKey locTooltipKey) {
        tabItem.setData(POLYGLOT_ITEM_TEXT_DATA_KEY, locTextKey);
        tabItem.setText(this.i18nSupport.getText(locTextKey));

        if (locTooltipKey != null) {
            tabItem.setData(POLYGLOT_ITEM_TOOLTIP_DATA_KEY, locTooltipKey);
            tabItem.setToolTipText(this.i18nSupport.getText(locTooltipKey));
        }
    }

    public void createLanguageSelector(final PageContext composerCtx) {
        for (final Locale locale : this.i18nSupport.supportedLanguages()) {
            final Label languageSelection = new Label(composerCtx.getParent(), SWT.NONE);
            languageSelection.setData(
                    POLYGLOT_WIDGET_FUNCTION_KEY,
                    (Consumer<Label>) label -> label.setVisible(
                            !this.i18nSupport.getCurrentLocale()
                                    .getLanguage()
                                    .equals(locale.getLanguage())));
            languageSelection.setData(RWT.CUSTOM_VARIANT, "header");
            languageSelection.setText("|  " + locale.getLanguage().toUpperCase());
            //languageSelection.updateLocale(this.i18nSupport);
            languageSelection.addListener(SWT.MouseDown, event -> {
                this.polyglotPageService.setPageLocale(composerCtx.getRoot(), locale);
            });
        }
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

    private static final void updateLocale(final TabItem[] items, final I18nSupport i18nSupport) {
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

    private static final void updateLocale(final TreeItem[] items, final I18nSupport i18nSupport) {
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
