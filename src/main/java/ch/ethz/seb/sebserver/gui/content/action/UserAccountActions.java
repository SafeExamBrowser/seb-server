/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.action;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.action.Action;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEvent;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.ActivateUserAccount;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.DeactivateUserAccount;

public final class UserAccountActions {

    public static Action newUserAccount(final Action action) {
        return goToUserAccount(action, null, true);
    }

    public static Action viewUserAccountFromList(final Action action) {
        return fromSelection(action, false);
    }

    public static Action editUserAccountFromList(final Action action) {
        return fromSelection(action, true);
    }

    public static Action editUserAccount(final Action action) {
        return goToUserAccount(action, null, true);
    }

    public static Action cancelEditUserAccount(final Action action) {
        if (action.pageContext().getEntityKey() == null) {
            final Action toList = action.pageContext().createAction(ActionDefinition.USER_ACCOUNT_VIEW_LIST);
            action.pageContext().publishPageEvent(new ActionEvent(toList, false));
            return toList;
        } else {
            return goToUserAccount(action, null, false);
        }
    }

    public static Action activateUserAccount(final Action action) {
        return action.restService
                .getBuilder(ActivateUserAccount.class)
                .withURIVariable(
                        API.PARAM_MODEL_ID,
                        action.pageContext().getAttribute(AttributeKeys.ENTITY_ID))
                .call()
                .map(report -> goToUserAccount(action, report.getSingleSource().modelId, false))
                .getOrThrow();
    }

    public static Action deactivateUserAccount(final Action action) {
        return action.restService
                .getBuilder(DeactivateUserAccount.class)
                .withURIVariable(
                        API.PARAM_MODEL_ID,
                        action.pageContext().getAttribute(AttributeKeys.ENTITY_ID))
                .call()
                .map(report -> goToUserAccount(action, report.getSingleSource().modelId, false))
                .getOrThrow();
    }

    private static Action fromSelection(final Action action, final boolean edit) {
        final Collection<String> selection = action.getSelectionSupplier().get();
        if (selection.isEmpty()) {
            throw new PageMessageException("sebserver.useraccount.info.pleaseSelect");
        }

        return goToUserAccount(action, selection.iterator().next(), edit);
    }

    private static Action goToUserAccount(
            final Action action,
            final String modelId,
            final boolean edit) {

        action.withAttribute(AttributeKeys.READ_ONLY, String.valueOf(!edit));
        if (modelId != null) {
            action.withEntity(new EntityKey(modelId, EntityType.USER));
        }

        return action;
    }

}
