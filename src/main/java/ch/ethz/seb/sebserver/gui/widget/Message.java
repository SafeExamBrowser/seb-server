/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.widget;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

public final class Message extends MessageBox {

    private static final int NORMAL_WIDTH = 400;
    private static final long serialVersionUID = 6973272221493264432L;

    public Message(
            final Shell parent,
            final String title,
            final String message,
            final int type) {

        super(parent, type);
        super.setText(title);
        super.setMessage(message);
    }

    @Override
    protected void prepareOpen() {
        super.prepareOpen();
        final GridLayout layout = (GridLayout) super.shell.getLayout();
        layout.marginTop = 10;
        layout.marginBottom = 10;
        super.shell.setData(RWT.CUSTOM_VARIANT, "message");
        final Rectangle bounds = super.shell.getBounds();
        if (bounds.width < NORMAL_WIDTH) {
            bounds.x = bounds.x - (NORMAL_WIDTH - bounds.width) / 2;
            bounds.width = NORMAL_WIDTH;
            super.shell.setBounds(bounds);
        }
    }

}
