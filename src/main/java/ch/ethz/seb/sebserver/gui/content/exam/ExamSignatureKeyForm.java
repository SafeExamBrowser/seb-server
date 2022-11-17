/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.exam;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class ExamSignatureKeyForm implements TemplateComposer {

    private static final LocTextKey TILE =
            new LocTextKey("sebserver.exam.signaturekey.title");

    private static final LocTextKey FORM_ENABLED =
            new LocTextKey("sebserver.exam.signaturekey.form.enabled");
    private static final LocTextKey FORM_STAT_GRANT_THRESHOLD =
            new LocTextKey("sebserver.exam.signaturekey.form.grant.threshold");

    private static final LocTextKey GRANT_LIST_TITLE =
            new LocTextKey("sebserver.exam.signaturekey.grantlist.title");
    private static final LocTextKey GRANT_LIST_KEY =
            new LocTextKey("sebserver.exam.signaturekey.grantlist.key");
    private static final LocTextKey GRANT_LIST_TAG =
            new LocTextKey("sebserver.exam.signaturekey.grantlist.tag");

    private static final LocTextKey APP_SIG_KEY_LIST_TITLE =
            new LocTextKey("sebserver.exam.signaturekey.keylist.title");
    private static final LocTextKey APP_SIG_KEY_LIST_KEY =
            new LocTextKey("sebserver.exam.signaturekey.keylist.key");
    private static final LocTextKey APP_SIG_KEY_LIST_NUM_CLIENTS =
            new LocTextKey("sebserver.exam.signaturekey.keylist.clients");

    private final PageService pageService;
    private final ResourceService resourceService;
    private final I18nSupport i18nSupport;

    public ExamSignatureKeyForm(
            final PageService pageService,
            final ResourceService resourceService,
            final I18nSupport i18nSupport) {

        this.pageService = pageService;
        this.resourceService = resourceService;
        this.i18nSupport = i18nSupport;
    }

    @Override
    public void compose(final PageContext pageContext) {
        final RestService restService = this.resourceService.getRestService();
        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();
        final EntityKey entityKey = pageContext.getEntityKey();

        // TODO Auto-generated method stub

    }

}
