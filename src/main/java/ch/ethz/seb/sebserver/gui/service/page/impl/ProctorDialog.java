/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.impl;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Shell;

import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;

public class ProctorDialog extends Dialog {

    private static final long serialVersionUID = -847313001481968137L;

    public ProctorDialog(final Shell parent) {
        super(parent, SWT.BORDER | SWT.TITLE | SWT.CLOSE | SWT.RESIZE);
    }

    public void open(
            final LocTextKey title,
            final String url) {

        this.shell = new Shell(getParent(), getStyle());
        this.shell.setText(getText());
        this.shell.setLayout(new GridLayout());
        final GridData gridData2 = new GridData(SWT.FILL, SWT.TOP, true, true);
        this.shell.setLayoutData(gridData2);

        final Browser browser = new Browser(this.shell, SWT.NONE);
        browser.setLayout(new GridLayout());
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.heightHint = 100;
        gridData.widthHint = 200;
        browser.setLayoutData(gridData);
        this.shell.open();
        browser.setUrl(url);
        browser.layout();
    }

}
