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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.CustomVariant;

public class TableNavigator {

    private final static int PAGE_NAV_SIZE = 9;
    private final static int NAV_BUTTON_WIDTH = 35;
    private final static int NAV_BUTON_HEIGHT = 16;

    private final Composite composite;
    private final EntityTable<?> entityTable;

    TableNavigator() {
        this.composite = null;
        this.entityTable = null;
    }

    TableNavigator(final EntityTable<?> entityTable) {
        this.composite = new Composite(entityTable.composite, SWT.NONE);
        final GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, true, true);
        this.composite.setLayoutData(gridData);
        final GridLayout layout = new GridLayout(3, false);
        this.composite.setLayout(layout);

        this.entityTable = entityTable;
    }

    public Page<?> update(final Page<?> pageData) {
        if (this.composite == null) {
            return pageData;
        }

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
        final GridLayout rowLayout = new GridLayout(PAGE_NAV_SIZE + 5, true);
        numNav.setLayout(rowLayout);

        if (numberOfPages > 1) {
            createBackwardLabel(pageNumber > 1, pageNumber, numNav);
            final int pageNavSize = Math.min(numberOfPages, PAGE_NAV_SIZE);
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

            createForwardLabel(pageNumber < numberOfPages, pageNumber, numberOfPages, numNav);
        }

        return pageData;
    }

    private void createPagingHeader(final int page, final int of) {
        final Label pageHeader = new Label(this.composite, SWT.NONE);
        final GridData gridData = new GridData(SWT.CENTER, SWT.CENTER, true, false);
        gridData.widthHint = 100;
        gridData.minimumWidth = 100;
        gridData.heightHint = 16;
        pageHeader.setLayoutData(gridData);
        pageHeader.setText("Page " + page + " / " + of);
    }

    private void createPageNumberLabel(
            final int page,
            final boolean selectable,
            final Composite parent) {

        final GridData rowData = new GridData(NAV_BUTTON_WIDTH, NAV_BUTON_HEIGHT);
        rowData.verticalAlignment = SWT.CENTER;
        rowData.horizontalAlignment = SWT.CENTER;
        rowData.verticalIndent = 0;

        final Label pageLabel = new Label(parent, SWT.NONE);
        pageLabel.setText(" " + page + " ");
        pageLabel.setLayoutData(rowData);
        pageLabel.setAlignment(SWT.CENTER);
        if (selectable) {
            pageLabel.setData(RWT.CUSTOM_VARIANT, CustomVariant.LIST_NAVIGATION.key);
            pageLabel.addListener(SWT.MouseDown, event -> this.entityTable.selectPage(page));
        }
    }

    private void createForwardLabel(
            final boolean visible,
            final int pageNumber,
            final int numberOfPages,
            final Composite parent) {

        final GridData rowData = new GridData(NAV_BUTTON_WIDTH, NAV_BUTON_HEIGHT);
        final Label forward = new Label(parent, SWT.NONE);
        forward.setText(">");
        forward.setData(RWT.CUSTOM_VARIANT, CustomVariant.LIST_NAVIGATION.key);
        forward.setLayoutData(rowData);
        forward.setAlignment(SWT.CENTER);
        if (visible) {
            forward.addListener(SWT.MouseDown, event -> this.entityTable.selectPage(pageNumber + 1));
        } else {
            forward.setVisible(false);
        }

        final Label end = new Label(parent, SWT.NONE);
        end.setText(">>");
        end.setData(RWT.CUSTOM_VARIANT, CustomVariant.LIST_NAVIGATION.key);
        end.setLayoutData(rowData);
        end.setAlignment(SWT.CENTER);
        if (visible) {
            end.addListener(SWT.MouseDown, event -> this.entityTable.selectPage(numberOfPages));
        } else {
            end.setVisible(false);
        }
    }

    private void createBackwardLabel(
            final boolean visible,
            final int pageNumber,
            final Composite parent) {

        final GridData rowData = new GridData(NAV_BUTTON_WIDTH, NAV_BUTON_HEIGHT);
        final Label start = new Label(parent, SWT.NONE);
        start.setText("<<");
        start.setLayoutData(rowData);
        start.setAlignment(SWT.CENTER);
        start.setData(RWT.CUSTOM_VARIANT, CustomVariant.LIST_NAVIGATION.key);
        if (visible) {
            start.addListener(SWT.MouseDown, event -> this.entityTable.selectPage(1));
        } else {
            start.setVisible(false);
        }

        final Label backward = new Label(parent, SWT.NONE);
        backward.setText("<");
        backward.setLayoutData(rowData);
        backward.setAlignment(SWT.CENTER);
        backward.setData(RWT.CUSTOM_VARIANT, CustomVariant.LIST_NAVIGATION.key);
        if (visible) {
            backward.addListener(SWT.MouseDown, event -> this.entityTable.selectPage(pageNumber - 1));
        } else {
            backward.setVisible(false);
        }

    }

}
