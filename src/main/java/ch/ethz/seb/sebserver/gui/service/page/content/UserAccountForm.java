/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.content;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.user.UserAccount;
import ch.ethz.seb.sebserver.gbl.model.user.UserMod;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.form.PageFormService;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.GetUserAccount;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class UserAccountForm implements TemplateComposer {

    private static final Logger log = LoggerFactory.getLogger(UserAccountForm.class);

    private final PageFormService pageFormService;
    private final RestService restService;
    private final CurrentUser currentUser;

    protected UserAccountForm(
            final PageFormService pageFormService,
            final RestService restService,
            final CurrentUser currentUser) {

        this.pageFormService = pageFormService;
        this.restService = restService;
        this.currentUser = currentUser;
    }

    @Override
    public void compose(final PageContext pageContext) {

        if (log.isDebugEnabled()) {
            log.debug("Compose User Account Form within PageContext: {}", pageContext);
        }

        final WidgetFactory widgetFactory = this.pageFormService.getWidgetFactory();
        final EntityKey entityKey = pageContext.getEntityKey();

        // get data or create new and handle error
        final UserAccount userAccount = (entityKey == null)
                ? new UserMod(
                        UUID.randomUUID().toString(),
                        this.currentUser.get().institutionId)
                : this.restService
                        .getBuilder(GetUserAccount.class)
                        .withURIVariable(API.PARAM_MODEL_ID, entityKey.modelId)
                        .call()
                        .get(pageContext::notifyError);

        if (userAccount == null) {
            log.error(
                    "Failed to get UserAccount. Error was notified to the User. See previous logs for more infomation");
            return;
        }

    }

}
