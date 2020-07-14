/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.impl;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Shell;

import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

public class ModelInputWizard<T> extends Dialog {

    private static final long serialVersionUID = -3314062148477979319L;

    private final WidgetFactory widgetFactory;
    private int dialogWidth = ModalInputDialog.DEFAULT_DIALOG_WIDTH;
    private int dialogHeight = ModalInputDialog.DEFAULT_DIALOG_HEIGHT;
    private int buttonWidth = ModalInputDialog.DEFAULT_DIALOG_BUTTON_WIDTH;

    public ModelInputWizard(
            final Shell parent,
            final WidgetFactory widgetFactory) {

        super(parent, SWT.BORDER | SWT.TITLE | SWT.APPLICATION_MODAL | SWT.CLOSE);
        this.widgetFactory = widgetFactory;
    }

    public ModelInputWizard<T> setDialogWidth(final int dialogWidth) {
        this.dialogWidth = dialogWidth;
        return this;
    }

    public ModelInputWizard<T> setLargeDialogWidth() {
        this.dialogWidth = ModalInputDialog.LARGE_DIALOG_WIDTH;
        return this;
    }

    public ModelInputWizard<T> setVeryLargeDialogWidth() {
        this.dialogWidth = ModalInputDialog.VERY_LARGE_DIALOG_WIDTH;
        return this;
    }

    public ModelInputWizard<T> setDialogHeight(final int dialogHeight) {
        this.dialogHeight = dialogHeight;
        return this;
    }

    public ModelInputWizard<T> setButtonWidth(final int buttonWidth) {
        this.buttonWidth = buttonWidth;
        return this;
    }

    @SafeVarargs
    public final void open(
            final LocTextKey title,
            final Runnable cancelCallback,
            final WizardPage<T>... pages) {

        // Create the selection dialog window
        final Shell shell = new Shell(getParent(), getStyle());
        shell.setText(getText());
        shell.setData(RWT.CUSTOM_VARIANT, CustomVariant.MESSAGE.key);
        shell.setText(this.widgetFactory.getI18nSupport().getText(title));
        shell.setLayout(new GridLayout(1, true));
        final GridData gridData2 = new GridData(SWT.FILL, SWT.TOP, false, false);
        shell.setLayoutData(gridData2);

        // the content composite
        final Composite main = new Composite(shell, SWT.NONE);
        main.setLayout(new GridLayout());
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.widthHint = this.dialogWidth;
        main.setLayoutData(gridData);

        final Composite actionComp = new Composite(shell, SWT.NONE);
        actionComp.setLayout(new GridLayout());
        actionComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        final Composite actionsComp = new Composite(actionComp, SWT.NONE);
        actionsComp.setLayout(new RowLayout(SWT.HORIZONTAL));
        actionComp.setLayoutData(new GridData(SWT.TRAIL, SWT.FILL, true, true));

        final List<WizardPage<T>> pageList = Utils.immutableListOf(pages);
        createPage(null, pageList, null, main, actionsComp);

        if (cancelCallback != null) {
            final Button cancel = this.widgetFactory.buttonLocalized(actionsComp, ModalInputDialog.CANCEL_TEXT_KEY);
            final RowData data = new RowData();
            data.width = this.buttonWidth;
            cancel.setLayoutData(data);
            cancel.addListener(SWT.Selection, event -> {
                if (cancelCallback != null) {
                    cancelCallback.run();
                }
                shell.close();
            });
        }

        gridData.heightHint = calcDialogHeight(main);
        finishUp(shell);
    }

    private void createPage(
            final String pageId,
            final List<WizardPage<T>> pages,
            final T valueFromPrevPage,
            final Composite contentComp,
            final Composite actionsComp) {

        final Optional<WizardPage<T>> newPage = (pageId != null)
                ? pages.stream()
                        .filter(p -> pageId.equals(p.id))
                        .findFirst()
                : pages.stream()
                        .filter(page -> page.isStart)
                        .findFirst();

        if (!newPage.isPresent()) {
            return;
        }

        final WizardPage<T> page = newPage.get();
        PageService.clearComposite(contentComp);
        PageService.clearComposite(actionsComp);

        final Supplier<T> valueSupplier = page.contentCompose.apply(valueFromPrevPage, contentComp);

        if (page.actions != null) {
            for (final WizardAction<T> action : page.actions) {
                final Button actionButton = this.widgetFactory.buttonLocalized(actionsComp, action.name);
                final RowData data = new RowData();
                data.width = this.buttonWidth;
                actionButton.setLayoutData(data);

                actionButton.addListener(SWT.Selection, event -> {
                    if (valueSupplier != null) {
                        final T result = valueSupplier.get();
                        if (action.toPage != null) {
                            createPage(action.toPage, pages, result, contentComp, actionsComp);
                        } else {
                            action.callback.test(result);
                            this.shell.close();
                        }
                    } else {
                        this.shell.close();
                    }
                });
            }
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
        final int actualHeight = main.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        final int displayHeight = main.getDisplay().getClientArea().height;
        final int availableHeight = (displayHeight < actualHeight + 100)
                ? displayHeight - 100
                : actualHeight;
        return Math.min(availableHeight, this.dialogHeight);
    }

    public static final class WizardPage<T> {
        public final String id;
        public final boolean isStart;
        public final BiFunction<T, Composite, Supplier<T>> contentCompose;
        public final List<WizardAction<T>> actions;

        @SafeVarargs
        public WizardPage(
                final String id,
                final boolean isStart,
                final BiFunction<T, Composite, Supplier<T>> contentCompose,
                final WizardAction<T>... actions) {

            this.id = id;
            this.isStart = isStart;
            this.contentCompose = contentCompose;
            this.actions = Utils.asImmutableList(actions);
        }
    }

    public static final class WizardAction<T> {

        public final LocTextKey name;
        public final String toPage;
        public final Predicate<T> callback;

        public WizardAction(final LocTextKey name, final String toPage) {
            this.name = name;
            this.toPage = toPage;
            this.callback = null;
        }

        public WizardAction(final LocTextKey name, final Predicate<T> callback) {
            this.name = name;
            this.toPage = null;
            this.callback = callback;
        }
    }

}
