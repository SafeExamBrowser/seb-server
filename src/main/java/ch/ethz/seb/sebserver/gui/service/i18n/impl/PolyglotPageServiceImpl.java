/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.i18n.impl;

import java.util.Locale;
import java.util.function.Consumer;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService;
import ch.ethz.seb.sebserver.gui.service.page.ComposerService;

/** Service that supports page language change on the fly */
@Lazy
@Service
@GuiProfile
public final class PolyglotPageServiceImpl implements PolyglotPageService {

    private final I18nSupport i18nSupport;

    public PolyglotPageServiceImpl(final I18nSupport i18nSupport) {
        this.i18nSupport = i18nSupport;
    }

    @Override
    public I18nSupport getI18nSupport() {
        return this.i18nSupport;
    }

    @Override
    public void setDefaultPageLocale(final Composite root) {
        setPageLocale(root, this.i18nSupport.getCurrentLocale());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setPageLocale(final Composite root, final Locale locale) {
        this.i18nSupport.setSessionLocale(locale);
        ComposerService.traversePageTree(
                root,
                comp -> comp.getData(POLYGLOT_WIDGET_FUNCTION_KEY) != null,
                comp -> ((Consumer<Control>) comp.getData(POLYGLOT_WIDGET_FUNCTION_KEY)).accept(comp));

        root.layout(true, true);
    }

}
