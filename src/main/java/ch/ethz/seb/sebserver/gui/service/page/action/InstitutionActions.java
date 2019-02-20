/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.action;

import java.util.Collection;
import java.util.function.Function;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.institution.Institution;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.activity.ActivitySelection;
import ch.ethz.seb.sebserver.gui.service.page.activity.ActivitySelection.Activity;
import ch.ethz.seb.sebserver.gui.service.page.event.ActivitySelectionEvent;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.ActivateInstitution;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.DeactivateInstitution;

/** Defines the action execution functions for all Institution action. */
public final class InstitutionActions {

    public static Function<Institution, Institution> postSaveAdapter(final PageContext pageContext) {
        return inst -> {
            goToInstitution(pageContext, inst.getModelId(), false);
            return inst;
        };
    }

    public static Result<?> newInstitution(final Action action) {
        return Result.of(goToInstitution(action.pageContext, null, true));
    }

    public static Result<?> viewInstitution(final Action action) {
        return fromInstitution(action, false);
    }

    public static Result<?> editInstitutionFromList(final Action action) {
        return fromInstitution(action, true);
    }

    public static Result<?> editInstitution(final Action action) {
        return Result.of(goToInstitution(
                action.pageContext,
                action.pageContext.getAttribute(AttributeKeys.ENTITY_ID),
                true));
    }

    public static Result<?> cancelEditInstitution(final Action action) {
        return Result.of(goToInstitution(
                action.pageContext,
                action.pageContext.getAttribute(AttributeKeys.ENTITY_ID),
                false));
    }

    public static Result<?> activateInstitution(final Action action) {
        return action.restService
                .getBuilder(ActivateInstitution.class)
                .withURIVariable(
                        API.PARAM_MODEL_ID,
                        action.pageContext.getAttribute(AttributeKeys.ENTITY_ID))
                .call()
                .map(report -> goToInstitution(action.pageContext, report.getSingleSource().modelId, false));
    }

    public static Result<?> deactivateInstitution(final Action action) {
        return action.restService
                .getBuilder(DeactivateInstitution.class)
                .withURIVariable(
                        API.PARAM_MODEL_ID,
                        action.pageContext.getAttribute(AttributeKeys.ENTITY_ID))
                .call()
                .map(report -> goToInstitution(action.pageContext, report.getSingleSource().modelId, false));
    }

    private static Result<?> fromInstitution(final Action action, final boolean edit) {
        final Collection<String> selection = action.selectionSupplier.get();
        if (selection.isEmpty()) {
            return Result.ofError(new PageMessageException("sebserver.institution.info.pleaseSelect"));
        }

        return Result.of(goToInstitution(action.pageContext, selection.iterator().next(), edit));
    }

    private static ActivitySelection goToInstitution(final PageContext pageContext, final String modelId,
            final boolean edit) {
        final ActivitySelection activitySelection = Activity.INSTITUTION_FORM
                .createSelection()
                .withEntity(new EntityKey(modelId, EntityType.INSTITUTION))
                .withAttribute(AttributeKeys.READ_ONLY, String.valueOf(!edit))
                .withAttribute(AttributeKeys.CREATE_NEW, (modelId != null) ? "false" : "true");
        pageContext.publishPageEvent(new ActivitySelectionEvent(activitySelection));
        return activitySelection;
    }

}
