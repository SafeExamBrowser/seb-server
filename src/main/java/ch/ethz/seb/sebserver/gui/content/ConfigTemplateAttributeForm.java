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
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;

@Lazy
@Component
@GuiProfile
public class ConfigTemplateAttributeForm implements TemplateComposer {

    private final PageService pageService;
    private final RestService restService;
    private final CurrentUser currentUser;
    private final ResourceService resourceService;

    protected ConfigTemplateAttributeForm(
            final PageService pageService,
            final RestService restService,
            final CurrentUser currentUser) {

        this.pageService = pageService;
        this.restService = restService;
        this.currentUser = currentUser;
        this.resourceService = pageService.getResourceService();

    }

    @Override
    public void compose(final PageContext pageContext) {
        // TODO Auto-generated method stub

    }

}
