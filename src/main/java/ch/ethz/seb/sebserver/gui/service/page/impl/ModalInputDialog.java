/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.impl;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
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

    private static final LocTextKey CANCEL_TEXT_KEY =
            new LocTextKey("sebserver.overall.action.cancel");
    private static final LocTextKey OK_TEXT_KEY =
            new LocTextKey("sebserver.overall.action.ok");
    private static final LocTextKey CLOSE_TEXT_KEY =
            new LocTextKey("sebserver.overall.action.close");

    private final WidgetFactory widgetFactory;
    private int dialogWidth = 400;

    public ModalInputDialog(
            final Shell parent,
            final WidgetFactory widgetFactory) {

        super(parent, SWT.BORDER | SWT.TITLE | SWT.APPLICATION_MODAL);
        this.widgetFactory = widgetFactory;

    }

    public ModalInputDialog<T> setDialogWidth(final int dialogWidth) {
        this.dialogWidth = dialogWidth;
        return this;
    }

    public void open(
            final LocTextKey title,
            final Consumer<T> callback,
            final Runnable cancelCallback,
            final ModalInputDialogComposer<T> contentComposer) {

        // Create the selection dialog window
        final Shell shell = new Shell(getParent(), getStyle());
        shell.setText(getText());
        shell.setData(RWT.CUSTOM_VARIANT, CustomVariant.MESSAGE.key);
        shell.setText(this.widgetFactory.getI18nSupport().getText(title));
        shell.setLayout(new GridLayout(2, true));
        final GridData gridData2 = new GridData(SWT.FILL, SWT.TOP, false, false);
        gridData2.widthHint = this.dialogWidth;
        shell.setLayoutData(gridData2);

        final Composite main = new Composite(shell, SWT.NONE);
        main.setLayout(new GridLayout());
        final GridData gridData = new GridData(SWT.FILL, SWT.TOP, false, false);
        gridData.horizontalSpan = 2;
        gridData.widthHint = this.dialogWidth;
        //  gridData.heightHint = 400;
        main.setLayoutData(gridData);
        main.setBackground(new Color(shell.getDisplay(), new RGB(1, 2, 3)));

        final Supplier<T> valueSuppier = contentComposer.compose(main);

        final Button ok = this.widgetFactory.buttonLocalized(shell, OK_TEXT_KEY);
        GridData data = new GridData(GridData.HORIZONTAL_ALIGN_END);
        data.widthHint = 100;
        ok.setLayoutData(data);
        ok.addListener(SWT.Selection, event -> {
            if (valueSuppier != null) {
                final T result = valueSuppier.get();
                callback.accept(result);
                shell.close();
            } else {
                shell.close();
            }
        });

        shell.setDefaultButton(ok);

        final Button cancel = this.widgetFactory.buttonLocalized(shell, CANCEL_TEXT_KEY);
        data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        data.widthHint = 100;
        cancel.setLayoutData(data);
        cancel.addListener(SWT.Selection, event -> {
            if (cancelCallback != null) {
                cancelCallback.run();
            }
            shell.close();
        });

        finishUp(shell);
    }

    public void open(
            final LocTextKey title,
            final PageContext pageContext,
            final Consumer<PageContext> contentComposer) {

        // Create the info dialog window
        final Shell shell = new Shell(getParent(), getStyle());
        shell.setText(getText());
        shell.setData(RWT.CUSTOM_VARIANT, CustomVariant.MESSAGE.key);
        shell.setText(this.widgetFactory.getI18nSupport().getText(title));
        shell.setLayout(new GridLayout());
        shell.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        final Composite main = new Composite(shell, SWT.NONE);
        main.setLayout(new GridLayout());
        final GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, true);
        main.setLayoutData(gridData);
        final PageContext internalPageContext = pageContext.copyOf(main);
        contentComposer.accept(internalPageContext);

        final Button close = this.widgetFactory.buttonLocalized(shell, CLOSE_TEXT_KEY);
        final GridData data = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        data.widthHint = 100;
        close.setLayoutData(data);
        close.addListener(SWT.Selection, event -> {
            shell.close();
        });

        finishUp(shell);
    }

    private void finishUp(final Shell shell) {
        shell.pack();
        final Rectangle bounds = shell.getBounds();
        final Rectangle bounds2 = super.getParent().getDisplay().getBounds();
        //bounds.width = bounds.width;
        bounds.x = (bounds2.width - bounds.width) / 2;
        bounds.y = (bounds2.height - bounds.height) / 2;
        shell.setBounds(bounds);

        shell.open();
    }

}
