/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.table;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

public class TableNavigator {

    private final static int PAGE_NAV_SIZE = 10;

    private final Composite composite;
    private final EntityTable<?> entityTable;

    TableNavigator(final EntityTable<?> entityTable) {
        this.composite = new Composite(entityTable.composite, SWT.NONE);
        final GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        this.composite.setLayoutData(gridData);
        final GridLayout layout = new GridLayout(3, true);
        layout.marginLeft = 20;
        this.composite.setLayout(layout);

        this.entityTable = entityTable;
    }

    public Page<?> update(final Page<?> pageData) {
        // clear all
        PageService.clearComposite(this.composite);

        if (pageData.isEmpty()) {
            // show empty message
            if (this.entityTable.emptyMessage != null) {
                this.entityTable.widgetFactory.labelLocalized(
                        this.composite,
                        CustomVariant.TEXT_H3,
                        this.entityTable.emptyMessage);
            }
            return pageData;
        }

        if (this.entityTable.hideNavigation) {
            return pageData;
        }

        final int pageNumber = pageData.getPageNumber();
        final int numberOfPages = pageData.getNumberOfPages();

        createPagingHeader(pageNumber, numberOfPages);

        final Composite numNav = new Composite(this.composite, SWT.NONE);
        final GridData gridData = new GridData(SWT.CENTER, SWT.TOP, true, false);
        numNav.setLayoutData(gridData);
        final RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);

        numNav.setLayout(rowLayout);

        if (numberOfPages > 1) {
            createBackwardLabel(pageNumber > 1, pageNumber, numNav);
            final int pageNavSize = (numberOfPages > PAGE_NAV_SIZE) ? PAGE_NAV_SIZE : numberOfPages;
            final int half = pageNavSize / 2;
            int start = pageNumber - half;
            if (start < 1) {
                start = 1;
            }
            int end = start + pageNavSize;
            if (end > numberOfPages) {
                end = numberOfPages + 1;
                start = end - pageNavSize;
            }

            for (int i = start; i < end; i++) {
                createPageNumberLabel(i, i != pageNumber, numNav);
            }

            createForwardLabel(pageNumber < numberOfPages, pageNumber, numNav);
        }

        return pageData;
    }

    private void createPagingHeader(final int page, final int of) {
        final Label pageHeader = new Label(this.composite, SWT.NONE);
        pageHeader.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        pageHeader.setText("Page " + page + "/" + of);
    }

    private void createPageNumberLabel(
            final int page,
            final boolean selectable,
            final Composite parent) {

        final Label pageLabel = new Label(parent, SWT.NONE);

        pageLabel.setText(" " + String.valueOf(page) + " ");
        if (selectable) {
            pageLabel.setData(RWT.CUSTOM_VARIANT, "action");
            pageLabel.addListener(SWT.MouseDown, event -> {
                this.entityTable.selectPage(page);
            });
        }
    }

    private void createForwardLabel(
            final boolean visible,
            final int pageNumber,
            final Composite parent) {

        final Label forward = new Label(parent, SWT.NONE);
        forward.setText(">");
        forward.setData(RWT.CUSTOM_VARIANT, "action");
        if (visible) {
            forward.addListener(SWT.MouseDown, event -> {
                this.entityTable.selectPage(pageNumber + 1);
            });
        } else {
            forward.setVisible(false);
        }
    }

    private void createBackwardLabel(
            final boolean visible,
            final int pageNumber,
            final Composite parent) {

        final Label backward = new Label(parent, SWT.NONE);
        backward.setText("<");
        backward.setData(RWT.CUSTOM_VARIANT, "action");
        if (visible) {
            backward.addListener(SWT.MouseDown, event -> {
                this.entityTable.selectPage(pageNumber - 1);
            });
        } else {
            backward.setVisible(false);
        }
    }

}
