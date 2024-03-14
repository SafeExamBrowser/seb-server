/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.LoginPage;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.PageDefinition;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;

/** Default login page works with the DefaultPageLayout and the
 * SEBLogin template */
@Lazy
@Component
@GuiProfile
public class DefaultLoginPage implements PageDefinition {

    @Override
    public Class<? extends TemplateComposer> composer() {
        return DefaultPageLayout.class;
    }

    @Override
    public PageContext applyPageContext(final PageContext pageContext) {
        return pageContext.withAttribute(
                AttributeKeys.PAGE_TEMPLATE_COMPOSER_NAME,
                LoginPage.class.getName());
    }

}
