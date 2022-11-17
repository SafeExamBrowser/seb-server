/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.monitoring;

import java.util.function.Predicate;
import java.util.function.Supplier;

import org.eclipse.swt.widgets.Composite;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.institution.SecurityKey;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.form.FormBuilder;
import ch.ethz.seb.sebserver.gui.form.FormHandle;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.ModalInputDialogComposer;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.impl.ModalInputDialog;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.session.GrantClientConnectionSecurityKey;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class SignatureKeyGrantPopup {

    private static final LocTextKey TITLE_TEXT_KEY =
            new LocTextKey("sebserver.monitoring.signaturegrant.title");
    private static final LocTextKey TITLE_TEXT_INFO =
            new LocTextKey("sebserver.monitoring.signaturegrant.info");

    private static final LocTextKey TITLE_TEXT_FORM_SIGNATURE =
            new LocTextKey("sebserver.monitoring.signaturegrant.signature");
    private static final LocTextKey TITLE_TEXT_FORM_TAG =
            new LocTextKey("sebserver.monitoring.signaturegrant.tag");

    private final PageService pageService;

    protected SignatureKeyGrantPopup(final PageService pageService) {
        this.pageService = pageService;
    }

    public PageAction showGrantPopup(final PageAction action, final SecurityKey securityKey) {
        final PageContext pageContext = action.pageContext();
        final PopupComposer popupComposer = new PopupComposer(this.pageService, pageContext, securityKey);
        try {
            final ModalInputDialog<FormHandle<?>> dialog =
                    new ModalInputDialog<>(
                            action.pageContext().getParent().getShell(),
                            this.pageService.getWidgetFactory());
            dialog.setDialogWidth(700);

            final Predicate<FormHandle<?>> applyGrant = formHandle -> applyGrant(
                    pageContext,
                    formHandle);

            dialog.open(
                    TITLE_TEXT_KEY,
                    applyGrant,
                    Utils.EMPTY_EXECUTION,
                    popupComposer);

        } catch (final Exception e) {
            action.pageContext().notifyUnexpectedError(e);
        }
        return action;
    }

    private final class PopupComposer implements ModalInputDialogComposer<FormHandle<?>> {

        private final PageService pageService;
        private final PageContext pageContext;
        private final SecurityKey securityKey;

        protected PopupComposer(
                final PageService pageService,
                final PageContext pageContext,
                final SecurityKey securityKey) {

            this.pageService = pageService;
            this.pageContext = pageContext;
            this.securityKey = securityKey;
        }

        @Override
        public Supplier<FormHandle<?>> compose(final Composite parent) {
            final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();
            widgetFactory.addFormSubContextHeader(parent, TITLE_TEXT_INFO, null);

            //final Composite defaultPageLayout = widgetFactory.defaultPageLayout(parent, TITLE_TEXT_INFO);
            final PageContext formContext = this.pageContext.copyOf(parent);

            final FormHandle<?> form = this.pageService.formBuilder(formContext)
                    .addField(FormBuilder.text(
                            Domain.SEB_SECURITY_KEY_REGISTRY.ATTR_TAG,
                            TITLE_TEXT_FORM_TAG,
                            this.securityKey.tag))
                    .addField(FormBuilder.text(
                            Domain.SEB_SECURITY_KEY_REGISTRY.ATTR_KEY_VALUE,
                            TITLE_TEXT_FORM_SIGNATURE,
                            String.valueOf(this.securityKey.key))
                            .readonly(true))
                    .build();

            return () -> form;
        }
    }

    private boolean applyGrant(
            final PageContext pageContext,
            final FormHandle<?> formHandle) {

        final EntityKey examKey = pageContext.getParentEntityKey();
        final EntityKey connectionKey = pageContext.getEntityKey();

        return this.pageService
                .getRestService()
                .getBuilder(GrantClientConnectionSecurityKey.class)
                .withURIVariable(API.PARAM_PARENT_MODEL_ID, examKey.modelId)
                .withURIVariable(API.PARAM_MODEL_ID, connectionKey.modelId)
                .withFormBinding(formHandle.getFormBinding())
                .call()
                .onError(formHandle::handleError)
                .hasValue();
    }

}
