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

import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityKey;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputDialog;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;

@Lazy
@Component
@GuiProfile
public class SecurityKeyGrantPopup {

    private static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.exam.signaturekey.grant.title");
    private static final LocTextKey TITLE_TEXT_FORM_SIGNATURE =
            new LocTextKey("sebserver.exam.signaturekey.grant.key");
    private static final LocTextKey TITLE_TEXT_FORM_TAG =
            new LocTextKey("sebserver.exam.signaturekey.grant.tag");
    private static final LocTextKey TITLE_TEXT_FORM_TYPE =
            new LocTextKey("sebserver.exam.signaturekey.grant.type");

    private final PageService pageService;

    public SecurityKeyGrantPopup(final PageService pageService) {
        this.pageService = pageService;
    }

    public PageAction showGrantPopup(final PageAction action, final SecurityKey securityKey) {

        final PopupComposer popupComposer = new PopupComposer(this.pageService, securityKey);
        try {
            final ModalInputDialog<FormHandle<?>> dialog =
                    new ModalInputDialog<>(
                            action.pageContext().getParent().getShell(),
                            this.pageService.getWidgetFactory());
            dialog.setDialogWidth(800);

            dialog.open(
                    TITLE_TEXT_KEY,
                    action.pageContext(),
                    popupComposer::compose);

        } catch (final Exception e) {
            action.pageContext().notifyUnexpectedError(e);
        }
        return action;
    }

    private static final class PopupComposer {

        private final PageService pageService;
        private final SecurityKey securityKey;

        protected PopupComposer(final PageService pageService, final SecurityKey securityKey) {
            this.pageService = pageService;
            this.securityKey = securityKey;
        }

        public void compose(final PageContext pageContext) {

            this.pageService.formBuilder(pageContext)
                    .readonly(true)
                    .addField(FormBuilder.text(
                            Domain.SEB_SECURITY_KEY_REGISTRY.ATTR_KEY_VALUE,
                            TITLE_TEXT_FORM_SIGNATURE,
                            String.valueOf(this.securityKey.key))
                            .readonly(true))

                    .addField(FormBuilder.text(
                            Domain.SEB_SECURITY_KEY_REGISTRY.ATTR_TAG,
                            TITLE_TEXT_FORM_TAG,
                            this.securityKey.tag))

                    .addField(FormBuilder.text(
                            Domain.SEB_SECURITY_KEY_REGISTRY.ATTR_KEY_TYPE,
                            TITLE_TEXT_FORM_TYPE,
                            this.securityKey.keyType.name()))

                    .build();
        }
    }

}
