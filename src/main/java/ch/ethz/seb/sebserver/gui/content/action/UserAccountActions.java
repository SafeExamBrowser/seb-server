/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.action;

import java.util.Collection;
import java.util.function.Function;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gui.content.activity.Activity;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.action.Action;
import ch.ethz.seb.sebserver.gui.service.page.activity.ActivitySelection;
import ch.ethz.seb.sebserver.gui.service.page.event.ActivitySelectionEvent;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.ActivateUserAccount;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.useraccount.DeactivateUserAccount;

public final class UserAccountActions {

    public static Function<UserInfo, UserInfo> postSaveAdapter(final PageContext pageContext) {
        return userAccount -> {
            goToUserAccount(pageContext, userAccount.getModelId(), false);
            return userAccount;
        };
    }

    public static Result<?> newUserAccount(final Action action) {
        return Result.of(goToUserAccount(action.pageContext, null, true));
    }

    public static Result<?> viewUserAccountFromList(final Action action) {
        return fromSelection(action, false);
    }

    public static Result<?> editUserAccountFromList(final Action action) {
        return fromSelection(action, true);
    }

    public static Result<?> editUserAccount(final Action action) {
        return Result.of(goToUserAccount(
                action.pageContext,
                action.pageContext.getAttribute(AttributeKeys.ENTITY_ID),
                true));
    }

    public static Result<?> cancelEditUserAccount(final Action action) {
        if (action.pageContext.getEntityKey() == null) {
            final ActivitySelection toList = Activity.USER_ACCOUNT_LIST.createSelection();
            action.pageContext.publishPageEvent(new ActivitySelectionEvent(toList));
            return Result.of(toList);
        } else {
            return Result.of(goToUserAccount(
                    action.pageContext,
                    action.pageContext.getAttribute(AttributeKeys.ENTITY_ID),
                    false));
        }
    }

    public static Result<?> activateUserAccount(final Action action) {
        return action.restService
                .getBuilder(ActivateUserAccount.class)
                .withURIVariable(
                        API.PARAM_MODEL_ID,
                        action.pageContext.getAttribute(AttributeKeys.ENTITY_ID))
                .call()
                .map(report -> goToUserAccount(action.pageContext, report.getSingleSource().modelId, false));
    }

    public static Result<?> deactivateUserAccount(final Action action) {
        return action.restService
                .getBuilder(DeactivateUserAccount.class)
                .withURIVariable(
                        API.PARAM_MODEL_ID,
                        action.pageContext.getAttribute(AttributeKeys.ENTITY_ID))
                .call()
                .map(report -> goToUserAccount(action.pageContext, report.getSingleSource().modelId, false));
    }

    private static Result<?> fromSelection(final Action action, final boolean edit) {
        return Result.tryCatch(() -> {
            final Collection<String> selection = action.getSelectionSupplier().get();
            if (selection.isEmpty()) {
                throw new PageMessageException("sebserver.useraccount.info.pleaseSelect");
            }

            return goToUserAccount(action.pageContext, selection.iterator().next(), edit);
        });
    }

    private static ActivitySelection goToUserAccount(
            final PageContext pageContext,
            final String modelId,
            final boolean edit) {

        final ActivitySelection activitySelection = Activity.USER_ACCOUNT_FORM
                .createSelection()
                .withAttribute(AttributeKeys.READ_ONLY, String.valueOf(!edit));

        if (modelId != null) {
            activitySelection.withEntity(new EntityKey(modelId, EntityType.USER));
        }

        pageContext.publishPageEvent(new ActivitySelectionEvent(activitySelection));
        return activitySelection;
    }

}
