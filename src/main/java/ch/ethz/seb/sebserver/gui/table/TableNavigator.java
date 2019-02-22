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
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

public class TableNavigator extends Composite {

    private static final long serialVersionUID = -7349918232061226192L;

    private final int pageNavSize = 3;
    private final EntityTable<?> entityTable;

    TableNavigator(final EntityTable<?> entityTable) {
        super(entityTable, SWT.NONE);
        super.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        final GridLayout layout = new GridLayout(3, true);
        layout.marginLeft = 20;
        super.setLayout(layout);

        this.entityTable = entityTable;
    }

    public Page<?> update(final Page<?> pageData) {
        // clear all
        WidgetFactory.clearComposite(this);

        final int pageNumber = pageData.getPageNumber();
        final int numberOfPages = pageData.getNumberOfPages();

        createPagingHeader(pageNumber, numberOfPages);

        final Composite numNav = new Composite(this, SWT.NONE);
        numNav.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        final RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
        rowLayout.spacing = 5;
        numNav.setLayout(rowLayout);

        if (numberOfPages > 1) {
            if (pageNumber > 1) {
                createRewardLabel(pageNumber, numNav);
            }

            for (int i = pageNumber - this.pageNavSize; i < pageNumber + this.pageNavSize; i++) {
                if (i >= 1 && i <= numberOfPages) {
                    createPageNumberLabel(i, i != pageNumber, numNav);
                }
            }

            if (pageNumber < numberOfPages) {
                createForwardLabel(pageNumber, numNav);
            }
        }

        return pageData;
    }

    private void createPagingHeader(final int page, final int of) {
        final Label pageHeader = new Label(this, SWT.NONE);
        pageHeader.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        pageHeader.setText("Page " + page + "/" + of);
    }

    private void createPageNumberLabel(final int page, final boolean selectable, final Composite parent) {
        final Label pageLabel = new Label(parent, SWT.NONE);

        pageLabel.setText(" " + String.valueOf(page) + " ");
        if (selectable) {
            pageLabel.setData(RWT.CUSTOM_VARIANT, "action");
            pageLabel.addListener(SWT.MouseDown, event -> {
                this.entityTable.selectPage(page);
            });
        }
    }

    private void createForwardLabel(final int pageNumber, final Composite parent) {
        final Label forward = new Label(parent, SWT.NONE);
        forward.setText(">");
        forward.setData(RWT.CUSTOM_VARIANT, "action");
        forward.addListener(SWT.MouseDown, event -> {
            this.entityTable.selectPage(pageNumber + 1);
        });
    }

    private void createRewardLabel(final int pageNumber, final Composite parent) {
        final Label reward = new Label(parent, SWT.NONE);
        reward.setText("<");
        reward.setData(RWT.CUSTOM_VARIANT, "action");
        reward.addListener(SWT.MouseDown, event -> {
            this.entityTable.selectPage(pageNumber - 1);
        });
    }

}
