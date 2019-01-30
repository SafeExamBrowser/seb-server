/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.impl;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Label;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;

@Lazy
@Component
public class TODOTemplate implements TemplateComposer {

    @Override
    public void compose(final PageContext composerCtx) {

        final Label tree = new Label(composerCtx.getParent(), SWT.NONE);
        tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        tree.setText("[TODO]");

    }

}
