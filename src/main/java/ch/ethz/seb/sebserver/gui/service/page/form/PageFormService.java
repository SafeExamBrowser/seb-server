/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.form;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class PageFormService {

    private final JSONMapper jsonMapper;
    private final WidgetFactory widgetFactory;
    private final PolyglotPageService polyglotPageService;

    public PageFormService(
            final JSONMapper jsonMapper,
            final WidgetFactory widgetFactory,
            final PolyglotPageService polyglotPageService) {

        this.jsonMapper = jsonMapper;
        this.widgetFactory = widgetFactory;
        this.polyglotPageService = polyglotPageService;
    }

    public FormBuilder getBuilder(
            final PageContext pageContext,
            final int rows) {

        return new FormBuilder(
                pageContext.getEntityKey(),
                this.jsonMapper,
                this.widgetFactory,
                this.polyglotPageService,
                pageContext,
                rows);
    }

    public FormBuilder getBuilder(
            final EntityKey entityKey,
            final PageContext pageContext,
            final int rows) {

        return new FormBuilder(
                entityKey,
                this.jsonMapper,
                this.widgetFactory,
                this.polyglotPageService,
                pageContext,
                rows);
    }

    public WidgetFactory getWidgetFactory() {
        return this.widgetFactory;
    }

    public PolyglotPageService getPolyglotPageService() {
        return this.polyglotPageService;
    }

}
