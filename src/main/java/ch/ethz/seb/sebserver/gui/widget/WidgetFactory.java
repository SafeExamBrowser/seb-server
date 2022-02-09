/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.widget;

import static ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService.POLYGLOT_WIDGET_FUNCTION_KEY;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.client.service.JavaScriptExecutor;
import org.eclipse.rap.rwt.widgets.WidgetUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.Threshold;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.impl.DefaultPageLayout;
import ch.ethz.seb.sebserver.gui.service.push.ServerPushService;

@Lazy
@Service
@GuiProfile
public class WidgetFactory {

    private static final String ADD_HTML_ATTR_ARIA_ROLE = "role";
    private static final String ADD_HTML_ATTR_ARIA_LABEL = "aria-label";
    private static final String ADD_HTML_ATTR_TEST_ID = "data-test";
    private static final String SUB_TITLE_TExT_SUFFIX = ".subtitle";

    public enum AriaRole {
        link,
        button,
        list,
        listitem,
        grid,
        row,
        rowgroup,
        columnheader,
        gridcell,
        dialog
    }

    private static final Logger log = LoggerFactory.getLogger(WidgetFactory.class);

    public static final int TEXT_AREA_INPUT_MIN_HEIGHT = 100;
    public static final int TEXT_INPUT_MIN_HEIGHT = 24;

    public enum ImageIcon {
        MAXIMIZE("maximize.png"),
        MINIMIZE("minimize.png"),
        MANDATORY("mandatory.png"),
        ADD("add.png"),
        REMOVE("remove.png"),
        ADD_BOX("add_box.png"),
        ADD_BOX_WHITE("add_box_w.png"),
        REMOVE_BOX("remove_box.png"),
        REMOVE_BOX_WHITE("remove_box_w.png"),
        EDIT("edit.png"),
        EDIT_SETTINGS("settings.png"),
        TEST("test.png"),
        COPY("copy.png"),
        IMPORT("import.png"),
        CANCEL("cancel.png"),
        CANCEL_EDIT("cancelEdit.png"),
        SHOW("show.png"),
        PROCTOR_SINGLE("proctorSingle.png"),
        PROCTOR_ROOM("proctorRoom.png"),
        PROCTOR_ALL("show.png"),
        ACTIVE("active.png"),
        INACTIVE("inactive.png"),
        TOGGLE_ON("toggle_on.png"),
        TOGGLE_OFF("toggle_off.png"),
        SWITCH("switch.png"),
        YES("yes.png"),
        NO("no.png"),
        SAVE("save.png"),
        EXPORT("export.png"),
        SECURE("secure.png"),
        NEW("new.png"),
        DELETE("delete.png"),
        SEARCH("lens.png"),
        UNDO("undo.png"),
        COLOR("color.png"),
        USER("user.png"),
        INSTITUTION("institution.png"),
        LMS_SETUP("lmssetup.png"),
        INDICATOR("indicator.png"),
        TEMPLATE("template.png"),
        DISABLE("disable.png"),
        SEND_QUIT("send-quit.png"),
        HELP("help.png"),
        LOCK("lock.png"),
        UNLOCK("unlock.png"),
        RESTRICTION("restriction.png"),
        VISIBILITY("visibility.png"),
        VISIBILITY_OFF("visibility_off.png"),
        NOTIFICATION("notification.png");

        public String fileName;
        private ImageData image = null;
        private ImageData greyedImage = null;

        ImageIcon(final String fileName) {
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

        public Image getGreyedImage(final Device device) {
            if (this.greyedImage == null) {
                try {
                    final InputStream resourceAsStream =
                            WidgetFactory.class.getResourceAsStream("/static/images/" + this.fileName);
                    this.greyedImage = new ImageData(resourceAsStream);
                    this.greyedImage.alpha = -1;
                    for (int y = 0; y < this.greyedImage.height; y++) {
                        for (int x = 0; x < this.greyedImage.width; x++) {
                            this.greyedImage.setAlpha(x, y, this.greyedImage.getAlpha(x, y) / 3);
                        }
                    }
                } catch (final Exception e) {
                    log.error("Failed to load resource image: {}", this.fileName, e);
                }
            }

            return new Image(device, this.greyedImage);
        }
    }

    public enum CustomVariant {
        TEXT_H1("h1"),
        TEXT_H2("h2"),
        TEXT_H3("h3"),
        SUBTITLE("subtitle"),
        IMAGE_BUTTON("imageButton"),
        TEXT_ACTION("action"),
        TEXT_READONLY("readonlyText"),

        ACTIONS("actions"),

        FORM_CENTER("form-center"),
        SELECTION("selection"),
        SELECTED("selected"),

        ACTIVITY_TREE_SECTION("treesection"),

        FOOTER("footer"),
        TITLE_LABEL("head"),

        MESSAGE("message"),
        ERROR("error"),
        WARNING("warning"),
        CONFIG_INPUT_READONLY("inputreadonly"),

        DARK_COLOR_LABEL("colordark"),
        LIGHT_COLOR_LABEL("colorlight"),

        LOGIN("login"),
        LOGIN_BACK("login-back"),
        SCROLL("scroll"),

        LIST_NAVIGATION("list-nav"),
        PLAIN_PWD("pwdplain"),
        COLOR_BOX("colorbox"),

        REGISTER_FORM("register")

        ;

        public final String key;

        CustomVariant(final String key) {
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

    public Composite voidComposite(final Composite parent) {
        final Composite content = new Composite(parent, SWT.NONE);
        final GridLayout contentLayout = new GridLayout();
        contentLayout.marginLeft = 0;
        contentLayout.marginHeight = 0;
        content.setLayout(contentLayout);
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        content.setLayoutData(gridData);
        return content;
    }

    public Composite defaultPageLayout(final Composite parent) {
        final Composite content = new Composite(parent, SWT.NONE);
        final GridLayout contentLayout = new GridLayout();
        contentLayout.marginLeft = 10;
        contentLayout.marginHeight = 0;
        content.setLayout(contentLayout);
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        content.setLayoutData(gridData);
        return content;
    }

    public Composite defaultPageLayout(
            final Composite parent,
            final LocTextKey title) {

        final Composite defaultPageLayout = defaultPageLayout(parent);
        final Label labelLocalizedTitle = labelLocalizedTitle(defaultPageLayout, title);
        final GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        labelLocalizedTitle.setLayoutData(gridData);

        // sub title if defined in language properties
        final LocTextKey subTitleTextKey = new LocTextKey(title.name + SUB_TITLE_TExT_SUFFIX);
        if (this.i18nSupport.hasText(subTitleTextKey)) {
            final GridData gridDataSub = new GridData(SWT.FILL, SWT.FILL, true, false);
            final Label labelLocalizedSubTitle = labelLocalized(
                    defaultPageLayout,
                    CustomVariant.SUBTITLE,
                    subTitleTextKey);
            labelLocalizedSubTitle.setLayoutData(gridDataSub);
        }

        return defaultPageLayout;
    }

    public void addFormSubContextHeader(final Composite parent, final LocTextKey titleTextKey,
            final LocTextKey tooltipTextKey) {
        final GridData gridData = new GridData(SWT.FILL, SWT.BOTTOM, true, false);
        gridData.horizontalIndent = 8;
        gridData.verticalIndent = 10;
        final Label subContextLabel = labelLocalized(
                parent,
                CustomVariant.TEXT_H3,
                titleTextKey,
                tooltipTextKey);
        subContextLabel.setLayoutData(gridData);
        final GridData gridData1 = new GridData(SWT.FILL, SWT.BOTTOM, true, false);
        gridData1.heightHint = 5;
        gridData1.horizontalIndent = 8;
        final Label subContextSeparator = labelSeparator(parent);
        subContextSeparator.setLayoutData(gridData1);
    }

    public Composite formGrid(final Composite parent, final int rows) {
        final Composite grid = new Composite(parent, SWT.NONE);
        final GridLayout layout = new GridLayout(rows, true);
        layout.horizontalSpacing = 10;
        layout.verticalSpacing = 10;
        layout.marginBottom = 10;
        layout.marginLeft = 4;
        layout.marginTop = 10;
        grid.setLayout(layout);
        grid.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        return grid;
    }

    /** Use this to create a scrolled Composite for usual popup forms
     *
     * @param parent The parent Composite
     * @return the scrolled Composite to add the form content */
    public Composite createPopupScrollComposite(final Composite parent) {
        return PageService.createManagedVScrolledComposite(
                parent,
                scrolledComposite -> {
                    final Composite g = new Composite(scrolledComposite, SWT.NONE);
                    g.setLayout(new GridLayout());
                    g.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
                    return g;
                },
                false,
                true,
                false);
    }

    /** Use this to create a scrolled Composite for usual popup forms
     *
     * @param parent The parent Composite
     * @return the scrolled Composite to add the form content */
    public Composite createPopupScrollCompositeFilled(final Composite parent) {
        return PageService.createManagedVScrolledComposite(
                parent,
                scrolledComposite -> {
                    final Composite g = new Composite(scrolledComposite, SWT.NONE);
                    g.setLayout(new GridLayout());
                    g.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
                    return g;
                },
                false,
                true,
                true);
    }

    public Composite createWarningPanel(final Composite parent) {
        return createWarningPanel(parent, 20);
    }

    public Composite createWarningPanel(final Composite parent, final int margin) {
        final Composite composite = new Composite(parent, SWT.NONE);
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        composite.setLayoutData(gridData);
        final GridLayout gridLayout = new GridLayout(1, true);
        gridLayout.marginWidth = margin;
        gridLayout.marginHeight = margin;
        composite.setLayout(gridLayout);
        composite.setData(RWT.CUSTOM_VARIANT, CustomVariant.WARNING.key);
        return composite;
    }

    public Button buttonLocalized(final Composite parent, final String locTextKey) {
        final Button button = new Button(parent, SWT.NONE);
        setARIARole(button, AriaRole.button);
        setTestId(button, locTextKey);
        this.polyglotPageService.injectI18n(button, new LocTextKey(locTextKey));
        return button;
    }

    public Button buttonLocalized(final Composite parent, final LocTextKey locTextKey) {
        final Button button = new Button(parent, SWT.NONE);
        setARIARole(button, AriaRole.button);
        setTestId(button, locTextKey.name);
        this.polyglotPageService.injectI18n(button, locTextKey);
        return button;
    }

    public Button buttonLocalized(final Composite parent, final CustomVariant variant, final String locTextKey) {
        final Button button = new Button(parent, SWT.NONE);
        setARIARole(button, AriaRole.button);
        setTestId(button, locTextKey);
        this.polyglotPageService.injectI18n(button, new LocTextKey(locTextKey));
        button.setData(RWT.CUSTOM_VARIANT, variant.key);
        return button;
    }

    public Button buttonLocalized(
            final Composite parent,
            final int type,
            final LocTextKey locTextKey,
            final LocTextKey toolTipKey,
            final String testKey,
            final LocTextKey ariaLabel) {

        final Button button = new Button(parent, type);
        setARIARole(button, AriaRole.button);
        if (ariaLabel != null) {
            setARIALabel(button, this.i18nSupport.getText(ariaLabel));
        }
        if (testKey != null) {
            setTestId(button, testKey);
        }
        this.polyglotPageService.injectI18n(button, locTextKey, toolTipKey);
        return button;
    }

    public Label label(final Composite parent, final String text) {
        final Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        return label;
    }

    public Label labelLocalized(final Composite parent, final String locTextKey) {
        final Label label = new Label(parent, SWT.NONE);
        this.polyglotPageService.injectI18n(label, new LocTextKey(locTextKey));
        return label;
    }

    public Label labelLocalized(final Composite parent, final LocTextKey locTextKey) {
        return labelLocalized(parent, locTextKey, false);
    }

    public Label labelLocalized(final Composite parent, final LocTextKey locTextKey, final boolean enableMarkup) {
        final Label label = new Label(parent, SWT.NONE);
        if (enableMarkup) {
            label.setData(RWT.MARKUP_ENABLED, true);
        }
        this.polyglotPageService.injectI18n(label, locTextKey);
        return label;
    }

    public Label labelLocalized(final Composite parent, final CustomVariant variant, final LocTextKey locTextKey) {
        final Label label = new Label(parent, SWT.NONE);
        label.setData(RWT.MARKUP_ENABLED, true);
        this.polyglotPageService.injectI18n(label, locTextKey);
        label.setData(RWT.CUSTOM_VARIANT, variant.key);
        return label;
    }

    public Label labelLocalized(
            final Composite parent,
            final LocTextKey locTextKey,
            final LocTextKey locToolTextKey) {

        final Label label = new Label(parent, SWT.NONE);
        this.polyglotPageService.injectI18n(label, locTextKey, locToolTextKey);
        return label;
    }

    public Label labelLocalized(
            final Composite parent,
            final CustomVariant variant,
            final LocTextKey locTextKey,
            final LocTextKey locToolTextKey) {

        final Label label = new Label(parent, SWT.NONE);
        this.polyglotPageService.injectI18n(label, locTextKey, locToolTextKey);
        label.setData(RWT.CUSTOM_VARIANT, variant.key);
        return label;
    }

    public Label labelLocalizedTitle(final Composite content, final LocTextKey locTextKey) {
        final Label labelLocalized = labelLocalized(content, CustomVariant.TEXT_H1, locTextKey);
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        labelLocalized.setLayoutData(gridData);
        return labelLocalized;
    }

    public Text textInput(final Composite content, final LocTextKey ariaLabel) {
        return textInput(content, false, false, ariaLabel.name, ariaLabel);
    }

    public Text textInput(final Composite content, final String testKey, final LocTextKey ariaLabel) {
        return textInput(content, false, false, testKey, ariaLabel);
    }

    public Text textInput(final Composite content, final String testKey, final String ariaLabel) {
        final Text textInput = textInput(content, false, false, testKey, null);
        if (ariaLabel != null) {
            WidgetFactory.setARIALabel(textInput, this.i18nSupport.getText(ariaLabel));
        }
        return textInput;
    }

    public Text passwordInput(final Composite content, final String testKey, final LocTextKey ariaLabel) {
        return textInput(content, true, false, testKey, ariaLabel);
    }

    public Text textAreaInput(
            final Composite content,
            final boolean readonly,
            final String testKey,
            final LocTextKey ariaLabel) {

        final Text input = readonly
                ? new Text(content, SWT.LEFT | SWT.MULTI)
                : new Text(content, SWT.LEFT | SWT.BORDER | SWT.MULTI);
        if (ariaLabel != null) {
            WidgetFactory.setARIALabel(input, this.i18nSupport.getText(ariaLabel));
        }
        if (testKey != null) {
            setTestId(input, testKey);
        }
        return input;
    }

    public Text textInput(
            final Composite content,
            final boolean password,
            final boolean readonly,
            final String testKey,
            final LocTextKey ariaLabel) {

        final Text input = readonly
                ? new Text(content, SWT.LEFT)
                : new Text(content, (password)
                        ? SWT.LEFT | SWT.BORDER | SWT.PASSWORD
                        : SWT.LEFT | SWT.BORDER);

        if (ariaLabel != null) {
            WidgetFactory.setARIALabel(input, this.i18nSupport.getText(ariaLabel));
        }
        if (testKey != null) {
            setTestId(input, testKey);
        }
        return input;
    }

    public Text numberInput(
            final Composite content,
            final Consumer<String> numberCheck,
            final boolean readonly,
            final String testKey,
            final LocTextKey ariaLabel) {

        final Text numberInput = new Text(content, (readonly) ? SWT.LEFT | SWT.READ_ONLY : SWT.RIGHT | SWT.BORDER);
        if (ariaLabel != null) {
            setARIALabel(numberInput, this.i18nSupport.getText(ariaLabel));
        }
        if (testKey != null) {
            setTestId(numberInput, testKey);
        }
        if (numberCheck != null) {
            numberInput.addListener(SWT.Verify, event -> {
                final String value = event.text;
                if (StringUtils.isBlank(value)) {
                    return;
                }
                try {
                    numberCheck.accept(value);
                } catch (final Exception e) {
                    event.doit = false;
                }
            });
        }
        return numberInput;
    }

    public Group groupLocalized(
            final Composite parent,
            final int columns,
            final LocTextKey locTextKey) {

        return groupLocalized(parent, columns, locTextKey, null);
    }

    public Group groupLocalized(
            final Composite parent,
            final int columns,
            final LocTextKey locTextKey,
            final LocTextKey locTooltipKey) {

        final Group group = new Group(parent, SWT.NONE);
        final GridLayout gridLayout = new GridLayout(columns, true);
        gridLayout.verticalSpacing = 0;
        gridLayout.horizontalSpacing = 0;
        gridLayout.marginHeight = 0;
        group.setLayout(gridLayout);

        this.polyglotPageService.injectI18n(group, locTextKey, locTooltipKey);
        return group;
    }

    public ExpandBar expandBarLocalized(
            final Composite parent,
            final LocTextKey locTooltipKey,
            final boolean exclusive) {

        final ExpandBar expandBar = new ExpandBar(parent, SWT.NONE);
        this.polyglotPageService.injectI18n(expandBar, locTooltipKey);

        if (exclusive) {
            expandBar.addListener(SWT.Expand, event -> {
                try {
                    final Widget expandItem = event.item;
                    Arrays.asList(expandBar.getItems())
                            .stream()
                            .filter(item -> !item.equals(expandItem))
                            .forEach(item -> item.setExpanded(false));
                } catch (final Exception e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Failed to automate ExpandBar", e);
                    }
                }
            });
            expandBar.addListener(SWT.Collapse, event -> {
                try {
                    final Widget expandItem = event.item;
                    int itemIndex = Arrays.asList(expandBar.getItems()).indexOf(expandItem) + 1;
                    if (itemIndex >= expandBar.getItemCount()) {
                        itemIndex = 0;
                    }
                    expandBar.getItem(itemIndex).setExpanded(true);
                } catch (final Exception e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Failed to automate ExpandBar", e);
                    }
                }
            });
        }

        return expandBar;
    }

    public ExpandItem expandItemLocalized(
            final ExpandBar parent,
            final int columns,
            final LocTextKey locTextKey) {

        final int itemCount = parent.getItemCount();
        final ExpandItem expandItem = new ExpandItem(parent, SWT.NONE);
        final Composite body = new Composite(expandItem.getParent(), SWT.NONE);
        final GridLayout gridLayout = new GridLayout(columns, true);
        gridLayout.verticalSpacing = 0;
        gridLayout.horizontalSpacing = 0;
        gridLayout.marginHeight = 0;
        body.setLayout(gridLayout);
        expandItem.setControl(body);
        expandItem.setExpanded(itemCount == 0);
        this.polyglotPageService.injectI18n(expandItem, locTextKey);

        return expandItem;
    }

    public Tree treeLocalized(final Composite parent, final int style) {
        final Tree tree = new Tree(parent, style);
        this.polyglotPageService.injectI18n(tree);
        setARIARole(tree, AriaRole.list);
        return tree;
    }

    public TreeItem treeItemLocalized(final Tree parent, final String locTextKey) {
        final TreeItem item = new TreeItem(parent, SWT.NONE);
        this.polyglotPageService.injectI18n(item, new LocTextKey(locTextKey));
        setARIARole(item, AriaRole.listitem);
        setTestId(item, locTextKey);
        return item;
    }

    public TreeItem treeItemLocalized(final Tree parent, final LocTextKey locTextKey) {
        final TreeItem item = new TreeItem(parent, SWT.NONE);
        this.polyglotPageService.injectI18n(item, locTextKey);
        setARIARole(item, AriaRole.listitem);
        setTestId(item, locTextKey.name);
        return item;
    }

    public TreeItem treeItemLocalized(final TreeItem parent, final String locTextKey) {
        final TreeItem item = new TreeItem(parent, SWT.NONE);
        this.polyglotPageService.injectI18n(item, new LocTextKey(locTextKey));
        setARIARole(item, AriaRole.listitem);
        setTestId(item, locTextKey);
        return item;
    }

    public TreeItem treeItemLocalized(final TreeItem parent, final LocTextKey locTextKey) {
        final TreeItem item = new TreeItem(parent, SWT.NONE);
        this.polyglotPageService.injectI18n(item, locTextKey);
        setARIARole(item, AriaRole.listitem);
        setTestId(item, locTextKey.name);
        return item;
    }

    public Table tableLocalized(final Composite parent) {
        final Table table = new Table(parent, SWT.NO_SCROLL);
        this.polyglotPageService.injectI18n(table);
        return table;
    }

    public Table tableLocalized(final Composite parent, final int style) {
        final Table table = new Table(parent, style);
        this.polyglotPageService.injectI18n(table);
        return table;
    }

    public TableColumn tableColumnLocalized(
            final Table table,
            final LocTextKey locTextKey) {

        return tableColumnLocalized(table, locTextKey, null);
    }

    public TableColumn tableColumnLocalized(
            final Table table,
            final LocTextKey locTextKey,
            final LocTextKey toolTipKey) {

        final TableColumn tableColumn = new TableColumn(table, SWT.NONE);
        this.polyglotPageService.injectI18n(tableColumn, locTextKey, toolTipKey);
        return tableColumn;
    }

    public TabFolder tabFolderLocalized(final Composite parent) {
        final TabFolder tabs = new TabFolder(parent, SWT.NONE);
        this.polyglotPageService.injectI18n(tabs);
        return tabs;
    }

    public TabItem tabItemLocalized(
            final TabFolder parent,
            final LocTextKey locTextKey) {

        return this.tabItemLocalized(parent, locTextKey, null);
    }

    public TabItem tabItemLocalized(
            final TabFolder parent,
            final LocTextKey locTextKey,
            final LocTextKey toolTipKey) {

        final TabItem tabItem = new TabItem(parent, SWT.NONE);
        this.polyglotPageService.injectI18n(tabItem, locTextKey, toolTipKey);
        return tabItem;
    }

    public Label labelSeparator(final Composite parent) {
        final Label label = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        final GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
        label.setLayoutData(data);
        return label;
    }

    public Label mandatoryLabel(
            final ImageIcon type,
            final Composite parent,
            final LocTextKey toolTip) {

        final Label imageButton = labelLocalized(parent, (LocTextKey) null, toolTip);
        imageButton.setImage(type.getImage(parent.getDisplay()));
        return imageButton;
    }

    public Button imageButton(
            final ImageIcon type,
            final Composite parent,
            final LocTextKey toolTip,
            final Listener listener) {

        final Button imageButton = new Button(parent, SWT.NONE);
        this.polyglotPageService.injectI18n(imageButton, null, toolTip);
        imageButton.setData(RWT.CUSTOM_VARIANT, CustomVariant.IMAGE_BUTTON.key);
        final Image image = type.getImage(parent.getDisplay());
        imageButton.setImage(image);
        if (listener != null) {
            imageButton.addListener(SWT.Selection, listener);
        }
        setARIARole(imageButton, AriaRole.button);
        if (toolTip != null) {
            setARIALabel(imageButton, this.i18nSupport.getText(toolTip));
        }
        setTestId(imageButton, toolTip.name);
        return imageButton;
    }

    public Selection selectionLocalized(
            final Selection.Type type,
            final Composite parent,
            final Supplier<List<Tuple<String>>> itemsSupplier,
            final LocTextKey toolTipTextKey,
            final Supplier<List<Tuple<String>>> itemsToolTipSupplier,
            final String testKey,
            final String ariaLabel) {

        final Selection selection;
        switch (type) {
            case SINGLE:
                selection = new SingleSelection(parent, SWT.READ_ONLY, testKey);
                break;
            case SINGLE_COMBO:
                selection = new SingleSelection(parent, SWT.NONE, testKey);
                break;
            case RADIO:
                selection = new RadioSelection(parent, testKey);
                break;
            case MULTI:
                selection = new MultiSelection(parent, testKey);
                break;
            case MULTI_COMBO:
                selection = new MultiSelectionCombo(
                        parent,
                        this,
                        testKey,
                        // NOTE parent would work for firefox but on IE and Chrome only parent.getParent().getParent() works
                        parent.getParent().getParent());
                break;
            case MULTI_CHECKBOX:
                selection = new MultiSelectionCheckbox(parent, testKey);
                break;
            case COLOR:
                selection = new ColorSelection(parent, this, testKey);
                break;
            default:
                throw new IllegalArgumentException("Unsupported Selection.Type: " + type);
        }

        if (itemsSupplier != null) {
            final Consumer<Selection> updateFunction = ss -> {
                try {
                    ss.applyNewMapping(itemsSupplier.get());
                    if (toolTipTextKey != null) {
                        ss.setToolTipText(Utils.formatLineBreaks(this.i18nSupport.getText(toolTipTextKey)));
                    }
                    if (itemsToolTipSupplier != null) {
                        ss.applyToolTipsForItems(itemsToolTipSupplier.get());
                    }
                } catch (final Exception e) {
                    log.error("Unexpected error while trying to apply localization to selection widget", e);
                }
            };
            selection.adaptToControl().setData(POLYGLOT_WIDGET_FUNCTION_KEY, updateFunction);
            updateFunction.accept(selection);
        }

        if (ariaLabel != null) {
            selection.setAriaLabel(ariaLabel);
        }

        return selection;
    }

    public DateTime dateSelector(final Composite parent, final LocTextKey label, final String testKey) {
        RWT.setLocale(this.i18nSupport.getUsersFormatLocale());
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        final DateTime dateTime = new DateTime(parent, SWT.DATE | SWT.BORDER | SWT.DROP_DOWN);
        dateTime.setLayoutData(gridData);

        if (label != null) {
            setARIALabel(dateTime, this.i18nSupport.getText(label));
        }
        if (testKey != null) {
            setTestId(dateTime, testKey);
        }
        return dateTime;
    }

    public DateTime timeSelector(final Composite parent, final LocTextKey label, final String testKey) {
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        final DateTime dateTime = new DateTime(parent, SWT.TIME | SWT.BORDER | SWT.SHORT);
        dateTime.setLayoutData(gridData);

        if (label != null) {
            setARIALabel(dateTime, this.i18nSupport.getText(label));
        }
        if (testKey != null) {
            setTestId(dateTime, testKey);
        }
        return dateTime;
    }

    public DateTime timeSelectorWithSeconds(final Composite parent, final LocTextKey label, final String testKey) {
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        final DateTime dateTime = new DateTime(parent, SWT.TIME | SWT.BORDER | SWT.MEDIUM);
        dateTime.setLayoutData(gridData);

        if (label != null) {
            setARIALabel(dateTime, this.i18nSupport.getText(label));
        }
        if (testKey != null) {
            setTestId(dateTime, testKey);
        }
        return dateTime;
    }

    public ColorDialog getColorDialog(final Composite parent) {
        final ColorDialog colorDialog = new ColorDialog(parent.getShell(), SWT.NONE);
        return colorDialog;
    }

    public ThresholdList thresholdList(
            final Composite parent,
            final Composite updateAnchor,
            final Collection<Threshold> thresholds,
            final Supplier<IndicatorType> indicatorTypeSupplier) {

        final ThresholdList thresholdList = new ThresholdList(
                parent,
                updateAnchor,
                this,
                indicatorTypeSupplier);
        if (thresholds != null) {
            thresholdList.setThresholds(thresholds);
        }
        return thresholdList;
    }

    public ImageUploadSelection logoImageUploadLocalized(
            final Composite parent,
            final LocTextKey locTextKey,
            final boolean readonly,
            final LocTextKey label) {

        return imageUploadLocalized(
                parent,
                locTextKey,
                readonly,
                DefaultPageLayout.LOGO_IMAGE_MAX_WIDTH,
                DefaultPageLayout.LOGO_IMAGE_MAX_HEIGHT,
                label);
    }

    public ImageUploadSelection imageUploadLocalized(
            final Composite parent,
            final LocTextKey locTextKey,
            final boolean readonly,
            final int maxWidth,
            final int maxHeight,
            final LocTextKey label) {

        final ImageUploadSelection imageUpload = new ImageUploadSelection(
                parent,
                this.serverPushService,
                this.i18nSupport,
                readonly,
                maxWidth,
                maxHeight,
                label);

        this.polyglotPageService.injectI18n(imageUpload, locTextKey);
        return imageUpload;
    }

    public FileUploadSelection fileUploadSelection(
            final Composite parent,
            final boolean readonly,
            final Collection<String> supportedFiles,
            final LocTextKey label) {

        final FileUploadSelection fileUploadSelection = new FileUploadSelection(
                parent,
                this.i18nSupport,
                supportedFiles,
                readonly,
                (label != null) ? label.name : null,
                label);

        if (supportedFiles != null) {
            supportedFiles.forEach(fileUploadSelection::withSupportFor);
        }
        return fileUploadSelection;
    }

    public static void setTestId(final Widget widget, final String value) {
        setAttribute(widget, ADD_HTML_ATTR_TEST_ID, value);
    }

    public static void setARIARole(final Widget widget, final AriaRole role) {
        setAttribute(widget, ADD_HTML_ATTR_ARIA_ROLE, role.name());
    }

    public static void setARIALabel(final Widget widget, final String label) {
        setAttribute(
                widget,
                ADD_HTML_ATTR_ARIA_LABEL,
                Utils.escapeHTML_XML_EcmaScript(label));
    }

    public static void setAttribute(final Widget widget, final String name, final String value) {
        if (StringUtils.isBlank(name) || StringUtils.isBlank(value)) {
            log.warn("Missing name or value for HTML attribute: name {} value {}", name, value);
            return;
        }
        if (!widget.isDisposed()) {
            final String $el = widget instanceof Text ? "$input" : "$el";
            final String id = WidgetUtil.getId(widget);
            exec("rap.getObject( '", id, "' ).", $el, ".attr( '", name, "', '", value, "' );");
            resetTabindex(widget);
        }
    }

    public static void resetTabindex(final Widget widget) {
        if (!widget.isDisposed()) {
            final String $el = widget instanceof Text ? "$input" : "$el";
            final String id = WidgetUtil.getId(widget);

            exec("rap.getObject( '", id, "' ).", $el, ".attr( 'tabindex', '0' );");
            if (widget instanceof Text) {
                exec("rap.getObject( '", id, "' ).$el.attr( 'tabindex', '0' );");
            }
        }
    }

    private static void exec(final String... strings) {
        final StringBuilder builder = new StringBuilder();
        builder.append("try{");
        for (final String str : strings) {
            builder.append(str);
        }
        builder.append("}catch(e){}");
        final JavaScriptExecutor executor = RWT.getClient().getService(JavaScriptExecutor.class);
        executor.execute(builder.toString());
    }

}
