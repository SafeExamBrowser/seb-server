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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

public class ModalInputWizard<T> extends Dialog {

    private static final Logger log = LoggerFactory.getLogger(ModalInputWizard.class);

    private static final long serialVersionUID = -3314062148477979319L;

    private final WidgetFactory widgetFactory;
    private int dialogWidth = ModalInputDialog.DEFAULT_DIALOG_WIDTH;
    private int dialogHeight = ModalInputDialog.DEFAULT_DIALOG_HEIGHT;
    private int buttonWidth = ModalInputDialog.DEFAULT_DIALOG_BUTTON_WIDTH;

    public ModalInputWizard(
            final Shell parent,
            final WidgetFactory widgetFactory) {

        super(parent, SWT.BORDER | SWT.TITLE | SWT.APPLICATION_MODAL | SWT.CLOSE);
        this.widgetFactory = widgetFactory;
    }

    public ModalInputWizard<T> setDialogWidth(final int dialogWidth) {
        this.dialogWidth = dialogWidth;
        return this;
    }

    public ModalInputWizard<T> setLargeDialogWidth() {
        this.dialogWidth = ModalInputDialog.LARGE_DIALOG_WIDTH;
        return this;
    }

    public ModalInputWizard<T> setVeryLargeDialogWidth() {
        this.dialogWidth = ModalInputDialog.VERY_LARGE_DIALOG_WIDTH;
        return this;
    }

    public ModalInputWizard<T> setDialogHeight(final int dialogHeight) {
        this.dialogHeight = dialogHeight;
        return this;
    }

    public ModalInputWizard<T> setButtonWidth(final int buttonWidth) {
        this.buttonWidth = buttonWidth;
        return this;
    }

    @SafeVarargs
    public final void open(
            final LocTextKey title,
            final Runnable cancelCallback,
            final WizardPage<T>... pages) {

        // Create the selection dialog window
        this.shell = new Shell(getParent(), getStyle());
        this.shell.setText(getText());
        this.shell.setData(RWT.CUSTOM_VARIANT, CustomVariant.MESSAGE.key);
        this.shell.setText(this.widgetFactory.getI18nSupport().getText(title));
        this.shell.setLayout(new GridLayout());
        final GridData gridData2 = new GridData(SWT.FILL, SWT.FILL, true, true);
        this.shell.setLayoutData(gridData2);

        // the content composite
        final Composite main = new Composite(this.shell, SWT.NONE);
        main.setLayout(new GridLayout());
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.widthHint = this.dialogWidth;
        main.setLayoutData(gridData);

        final Composite content = new Composite(main, SWT.NONE);
        content.setLayout(new GridLayout());
        final GridData gridDataContent = new GridData(SWT.FILL, SWT.FILL, true, true);
        content.setLayoutData(gridDataContent);
        content.setData(RWT.CUSTOM_VARIANT, CustomVariant.MESSAGE.key);

        final Composite actionComp = new Composite(main, SWT.NONE);
        actionComp.setLayout(new GridLayout());
        final GridData gridData3 = new GridData(SWT.FILL, SWT.FILL, true, true);
        actionComp.setLayoutData(gridData3);

        final Composite actionsComp = new Composite(actionComp, SWT.NONE);
        actionsComp.setLayout(new RowLayout(SWT.HORIZONTAL));
        final GridData gridData4 = new GridData(SWT.CENTER, SWT.FILL, true, true);
        actionComp.setLayoutData(gridData4);

        final List<WizardPage<T>> pageList = Utils.immutableListOf(pages);
        createPage(null, pageList, null, content, actionsComp, cancelCallback);
        finishUp(this.shell);
    }

    private void createPage(
            final String pageId,
            final List<WizardPage<T>> pages,
            final T valueFromPrevPage,
            final Composite contentComp,
            final Composite actionsComp,
            final Runnable cancelCallback) {

        try {

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
                                PageService.clearComposite(contentComp);
                                PageService.clearComposite(actionsComp);
                                createPage(action.toPage, pages, result, contentComp, actionsComp, cancelCallback);
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

            final Button cancel = this.widgetFactory.buttonLocalized(
                    actionsComp,
                    (page.actions.isEmpty()) ? ModalInputDialog.OK_TEXT_KEY : ModalInputDialog.CANCEL_TEXT_KEY);
            final RowData data = new RowData();
            data.width = this.buttonWidth;
            cancel.setLayoutData(data);
            cancel.addListener(SWT.Selection, event -> {
                if (cancelCallback != null) {
                    cancelCallback.run();
                }
                this.shell.close();
            });

            try {
                final Composite parent = contentComp.getParent();
                final int dialogHeight = calcDialogHeight(parent);
                ((GridData) parent.getLayoutData()).heightHint = dialogHeight;
                ((GridData) contentComp.getLayoutData()).heightHint = dialogHeight - 55;
                ((GridData) actionsComp.getLayoutData()).heightHint = 45;

                contentComp.getShell().layout(true, true);
            } catch (final Exception e) {
                log.warn("Failed to calculate dialog height: {}", e.getMessage());
            }

        } catch (final Exception e) {
            log.error("Unexpected error: ", e);
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
