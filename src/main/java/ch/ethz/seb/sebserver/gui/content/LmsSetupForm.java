/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.form.PageFormService;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;

@Lazy
@Component
@GuiProfile
public class LmsSetupForm implements TemplateComposer {

    private final PageFormService pageFormService;
    private final ResourceService resourceService;

    protected LmsSetupForm(
            final PageFormService pageFormService,
            final ResourceService resourceService) {

        this.pageFormService = pageFormService;
        this.resourceService = resourceService;
    }

    @Override
    public void compose(final PageContext pageContext) {
        // TODO Auto-generated method stub

    }

}
