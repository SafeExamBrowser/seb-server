/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.action;

import static ch.ethz.seb.sebserver.gui.service.page.activity.ActivitySelection.Activity.INSTITUTION_NODE;

import java.util.Collection;
import java.util.function.Function;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.EntityType;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gui.service.page.PageMessageException;
import ch.ethz.seb.sebserver.gui.service.page.event.ActivitySelectionEvent;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.NewInstitution;
import ch.ethz.seb.sebserver.gui.service.table.EntityTable;

public final class InstitutionActions {

    public static Result<?> newInstitution(final Action action) {
        return action.restService
                .getBuilder(NewInstitution.class)
                .call();
    }

    public static Function<Action, Result<?>> editInstitution(final EntityTable<?> fromTable) {
        return action -> {
            final Collection<String> selection = fromTable.getSelection();
            if (selection.isEmpty()) {
                return Result.ofError(new PageMessageException("sebserver.institution.info.pleaseSelect"));
            }

            final EntityKey entityKey = new EntityKey(
                    selection.iterator().next(),
                    EntityType.INSTITUTION);
            action.pageContext.publishPageEvent(new ActivitySelectionEvent(
                    INSTITUTION_NODE
                            .createSelection()
                            .withEntity(entityKey)));

            return Result.of(entityKey);
        };
    }

//    /** Use this higher-order function to create a new Institution action function.
//     *
//     * @return */
//    static Runnable newInstitution(final PageContext composerCtx, final RestServices restServices) {
//        return () -> {
//            final IdAndName newInstitutionId = restServices
//                    .sebServerAPICall(NewInstitution.class)
//                    .doAPICall()
//                    .onErrorThrow("Unexpected Error");
//            composerCtx.notify(new ActionEvent(ActionDefinition.INSTITUTION_NEW, newInstitutionId));
//        };
//    }
//
//    /** Use this higher-order function to create a delete Institution action function.
//     *
//     * @return */
//    static Runnable deleteInstitution(final PageContext composerCtx, final RestServices restServices,
//            final String instId) {
//        return () -> {
//            restServices
//                    .sebServerAPICall(DeleteInstitution.class)
//                    .attribute(AttributeKeys.INSTITUTION_ID, instId)
//                    .doAPICall()
//                    .onErrorThrow("Unexpected Error");
//            composerCtx.notify(new ActionEvent(ActionDefinition.INSTITUTION_DELETE, instId));
//        };
//    }

}
