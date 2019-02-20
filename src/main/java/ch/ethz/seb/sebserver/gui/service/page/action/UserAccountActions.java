/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.action;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.activity.ActivitySelection;
import ch.ethz.seb.sebserver.gui.service.page.activity.ActivitySelection.Activity;
import ch.ethz.seb.sebserver.gui.service.page.event.ActivitySelectionEvent;

public final class UserAccountActions {

    public static Result<?> newUserAccount(final Action action) {
        return Result.of(goToUserAccount(action.pageContext, null, true));
    }

    public static Result<?> viewUserAccountFromList(final Action action) {
        return fromSelection(action, false);
    }

    public static Result<?> editUserAccountFromList(final Action action) {
        return fromSelection(action, true);
    }

    private static Result<?> fromSelection(final Action action, final boolean edit) {
        final Collection<String> selection = action.selectionSupplier.get();
        if (selection.isEmpty()) {
            return Result.ofError(new PageMessageException("sebserver.useraccount.info.pleaseSelect"));
        }

        return Result.of(goToUserAccount(action.pageContext, selection.iterator().next(), edit));
    }

    private static ActivitySelection goToUserAccount(
            final PageContext pageContext,
            final String modelId,
            final boolean edit) {

        final ActivitySelection activitySelection = Activity.USER_ACCOUNT_FORM
                .createSelection()
                .withEntity(new EntityKey(modelId, EntityType.USER))
                .withAttribute(AttributeKeys.READ_ONLY, String.valueOf(!edit));

        if (modelId != null) {
            activitySelection.withEntity(new EntityKey(modelId, EntityType.USER));
        }

        pageContext.publishPageEvent(new ActivitySelectionEvent(activitySelection));
        return activitySelection;
    }

}
