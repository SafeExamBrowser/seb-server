/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.impl;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Shell;

import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.ModalInputDialogComposer;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

public class ModalInputDialog<T> extends Dialog {

    private static final long serialVersionUID = -3448614119078234374L;

    public static final int DEFAULT_DIALOG_WIDTH = 400;
    public static final int DEFAULT_DIALOG_HEIGHT = 600;
    public static final int DEFAULT_DIALOG_BUTTON_WIDTH = 100;
    public static final int LARGE_DIALOG_WIDTH = 600;
    public static final int VERY_LARGE_DIALOG_WIDTH = 800;

    public static final LocTextKey CANCEL_TEXT_KEY =
            new LocTextKey("sebserver.overall.action.cancel");
    public static final LocTextKey OK_TEXT_KEY =
            new LocTextKey("sebserver.overall.action.ok");
    public static final LocTextKey CLOSE_TEXT_KEY =
            new LocTextKey("sebserver.overall.action.close");

    private final WidgetFactory widgetFactory;
    private int dialogWidth = DEFAULT_DIALOG_WIDTH;
    private int dialogHeight = DEFAULT_DIALOG_HEIGHT;
    private int buttonWidth = DEFAULT_DIALOG_BUTTON_WIDTH;
    private boolean forceHeight = false;

    public ModalInputDialog(
            final Shell parent,
            final WidgetFactory widgetFactory) {

        super(parent, SWT.BORDER | SWT.TITLE | SWT.APPLICATION_MODAL | SWT.CLOSE);
        this.widgetFactory = widgetFactory;
    }

    public ModalInputDialog<T> setDialogWidth(final int dialogWidth) {
        this.dialogWidth = dialogWidth;
        return this;
    }

    public ModalInputDialog<T> setLargeDialogWidth() {
        this.dialogWidth = LARGE_DIALOG_WIDTH;
        return this;
    }

    public ModalInputDialog<T> setVeryLargeDialogWidth() {
        this.dialogWidth = VERY_LARGE_DIALOG_WIDTH;
        return this;
    }

    public ModalInputDialog<T> setDialogHeight(final int dialogHeight) {
        this.dialogHeight = dialogHeight;
        this.forceHeight = true;
        return this;
    }

    public ModalInputDialog<T> setButtonWidth(final int buttonWidth) {
        this.buttonWidth = buttonWidth;
        return this;
    }

    public void open(
            final LocTextKey title,
            final ModalInputDialogComposer<T> contentComposer) {

        open(
                title,
                t -> true,
                () -> {
                }, contentComposer);
    }

    public void open(
            final LocTextKey title,
            final Consumer<T> callback,
            final Runnable cancelCallback,
            final ModalInputDialogComposer<T> contentComposer) {

        final Predicate<T> predicate = result -> {
            callback.accept(result);
            return true;
        };

        open(title, predicate, cancelCallback, contentComposer);
    }

    public void open(
            final LocTextKey title,
            final Predicate<T> callback,
            final Runnable cancelCallback,
            final ModalInputDialogComposer<T> contentComposer) {

        // Create the selection dialog window
        this.shell = new Shell(getParent(), getStyle());
        this.shell.setText(getText());
        this.shell.setData(RWT.CUSTOM_VARIANT, CustomVariant.MESSAGE.key);
        this.shell.setText(this.widgetFactory.getI18nSupport().getText(title));
        this.shell.setLayout(new GridLayout(2, true));
        final GridData gridData2 = new GridData(SWT.FILL, SWT.TOP, false, false);
        this.shell.setLayoutData(gridData2);

        final Composite main = new Composite(this.shell, SWT.NONE);
        main.setLayout(new GridLayout());
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.horizontalSpan = 2;
        gridData.widthHint = this.dialogWidth;
        main.setLayoutData(gridData);

        final Supplier<T> valueSupplier = contentComposer.compose(main);
        gridData.heightHint = calcDialogHeight(main);

        final Button ok = this.widgetFactory.buttonLocalized(this.shell, OK_TEXT_KEY);
        GridData data = new GridData(GridData.HORIZONTAL_ALIGN_END);
        data.widthHint = this.buttonWidth;
        ok.setLayoutData(data);
        ok.addListener(SWT.Selection, event -> {
            if (valueSupplier != null) {
                final T result = valueSupplier.get();
                if (callback.test(result)) {
                    this.shell.close();
                }
            } else {
                this.shell.close();
            }
        });

        this.shell.setDefaultButton(ok);

        final Button cancel = this.widgetFactory.buttonLocalized(this.shell, CANCEL_TEXT_KEY);
        data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        data.widthHint = this.buttonWidth;
        cancel.setLayoutData(data);
        cancel.addListener(SWT.Selection, event -> {
            if (cancelCallback != null) {
                cancelCallback.run();
            }
            this.shell.close();
        });

        finishUp(this.shell);
    }

    public void open(
            final LocTextKey title,
            final PageContext pageContext,
            final Consumer<PageContext> contentComposer) {

        // Create the info dialog window
        this.shell = new Shell(getParent(), getStyle());
        this.shell.setText(getText());
        this.shell.setData(RWT.CUSTOM_VARIANT, CustomVariant.MESSAGE.key);
        this.shell.setText(this.widgetFactory.getI18nSupport().getText(title));
        this.shell.setLayout(new GridLayout());
        final GridData gridData2 = new GridData(SWT.FILL, SWT.TOP, true, true);

        this.shell.setLayoutData(gridData2);

        final Composite main = new Composite(this.shell, SWT.NONE);
        main.setLayout(new GridLayout());
        final GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, true);
        gridData.widthHint = this.dialogWidth;
        main.setLayoutData(gridData);

        contentComposer.accept(pageContext.copyOf(main));
        gridData.heightHint = calcDialogHeight(main);

        final Button close = this.widgetFactory.buttonLocalized(this.shell, CLOSE_TEXT_KEY);
        final GridData data = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        data.widthHint = this.buttonWidth;
        close.setLayoutData(data);
        close.addListener(SWT.Selection, event -> this.shell.close());

        finishUp(this.shell);
    }

    public void close() {
        if (this.shell != null) {
            this.shell.close();
        }
    }

    private void finishUp(final Shell shell) {
        shell.pack();
        final Rectangle bounds = shell.getBounds();
        final Rectangle bounds2 = super.getParent().getDisplay().getBounds();
        bounds.x = (bounds2.width - bounds.width) / 2;
        bounds.y = (bounds2.height - bounds.height) / 2;
        shell.setBounds(bounds);

        shell.open();
    }

    private int calcDialogHeight(final Composite main) {
        if (this.forceHeight) {
            return this.dialogHeight;
        }
        final int actualHeight = main.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        final int displayHeight = main.getDisplay().getClientArea().height;
        final int availableHeight = (displayHeight < actualHeight + 100)
                ? displayHeight - 100
                : actualHeight;
        return Math.min(availableHeight, this.dialogHeight);
    }

}
