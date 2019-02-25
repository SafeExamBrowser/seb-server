/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
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
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.action.Action;
import ch.ethz.seb.sebserver.gui.service.page.event.ActionEvent;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.ActivateInstitution;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.DeactivateInstitution;

/** Defines the action execution functions for all Institution action. */
public final class InstitutionActions {

    public static Action viewInstitutionFromList(final Action action) {
        return fromSelection(action, false);
    }

    public static Action editInstitutionFromList(final Action action) {
        return fromSelection(action, true);
    }

    public static Action editInstitution(final Action action) {
        return goToInstitution(action, null, true);
    }

    public static Action cancelEditInstitution(final Action action) {
        if (action.getEntityKey() == null) {
            final PageContext pageContext = action.pageContext();
            final Action toList = pageContext.createAction(ActionDefinition.INSTITUTION_VIEW_LIST);
            pageContext.publishPageEvent(new ActionEvent(toList, false));
            return toList;
        } else {
            return goToInstitution(action, null, false);
        }
    }

    public static Action activateInstitution(final Action action) {
        return action.restService
                .getBuilder(ActivateInstitution.class)
                .withURIVariable(
                        API.PARAM_MODEL_ID,
                        action.pageContext().getAttribute(AttributeKeys.ENTITY_ID))
                .call()
                .map(report -> goToInstitution(action, report.getSingleSource().modelId, false))
                .getOrThrow();
    }

    public static Action deactivateInstitution(final Action action) {
        return action.restService
                .getBuilder(DeactivateInstitution.class)
                .withURIVariable(
                        API.PARAM_MODEL_ID,
                        action.pageContext().getAttribute(AttributeKeys.ENTITY_ID))
                .call()
                .map(report -> goToInstitution(action, report.getSingleSource().modelId, false))
                .getOrThrow();
    }

    private static Action fromSelection(final Action action, final boolean edit) {
        final Collection<String> selection = action.getSelectionSupplier().get();
        if (selection.isEmpty()) {
            throw new PageMessageException("sebserver.institution.info.pleaseSelect");
        }

        return goToInstitution(action, selection.iterator().next(), edit);
    }

    private static Action goToInstitution(
            final Action action,
            final String modelId,
            final boolean edit) {

        action.withAttribute(AttributeKeys.READ_ONLY, String.valueOf(!edit));
        if (modelId != null) {
            action.withEntity(new EntityKey(modelId, EntityType.INSTITUTION));
        }

        return action;
    }

}
