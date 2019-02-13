/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.PageDefinition;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;

/** Default main page works with the DefaultPageLayout and the
 * SEBMainPage template */
@Lazy
@Component
public class DefaultMainPage implements PageDefinition {

    @Override
    public Class<? extends TemplateComposer> composer() {
        return DefaultPageLayout.class;
    }

    @Override
    public PageContext applyPageContext(final PageContext pageContext) {
        return pageContext.withAttribute(
                AttributeKeys.PAGE_TEMPLATE_COMPOSER_NAME,
                SEBMainPage.class.getName());
    }

}
