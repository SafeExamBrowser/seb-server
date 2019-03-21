/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page;

import java.util.function.Consumer;
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
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

public class ModalInputDialog<T> extends Dialog {

    private static final long serialVersionUID = -3448614119078234374L;

    private final WidgetFactory widgetFactory;

    private T returnValue = null;

    public ModalInputDialog(
            final Shell parent,
            final WidgetFactory widgetFactory) {

        super(parent, SWT.BORDER | SWT.TITLE | SWT.APPLICATION_MODAL);
        this.widgetFactory = widgetFactory;
    }

    public void open(
            final String title,
            final PageContext pageContext,
            final Consumer<T> callback,
            final ModalInputDialogComposer<T> contentComposer) {

        // Create the dialog window
        final Shell shell = new Shell(getParent(), getStyle());
        shell.setText(getText());
        shell.setData(RWT.CUSTOM_VARIANT, "message");
        shell.setText(title);
        shell.setLayout(new GridLayout(2, true));
        shell.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        final Composite main = new Composite(shell, SWT.NONE);
        main.setLayout(new GridLayout());
        final GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, true);
        gridData.horizontalSpan = 2;
        main.setLayoutData(gridData);

        final PageContext internalPageContext = pageContext.copyOf(main);
        final Supplier<T> valueSuppier = contentComposer.compose(internalPageContext);

        final Button ok = this.widgetFactory.buttonLocalized(
                shell,
                new LocTextKey("sebserver.overall.action.ok"));
        GridData data = new GridData(GridData.HORIZONTAL_ALIGN_END);
        data.widthHint = 100;
        ok.setLayoutData(data);
        ok.addListener(SWT.Selection, event -> {
            callback.accept(valueSuppier.get());
            this.returnValue = valueSuppier.get();
            shell.close();
        });

        shell.setDefaultButton(ok);

        final Button cancel = this.widgetFactory.buttonLocalized(
                shell,
                new LocTextKey("sebserver.overall.action.cancel"));
        data = new GridData(GridData.CENTER);
        data.widthHint = 100;
        cancel.setLayoutData(data);
        cancel.addListener(SWT.Selection, event -> {
            shell.close();
        });

        shell.pack();
        final Rectangle bounds = shell.getBounds();
        final Rectangle bounds2 = super.getParent().getDisplay().getBounds();
        bounds.width = 400;
        bounds.x = (bounds2.width - bounds.width) / 2;
        bounds.y = (bounds2.height - bounds.height) / 2;
        shell.setBounds(bounds);

        shell.open();
    }

}
