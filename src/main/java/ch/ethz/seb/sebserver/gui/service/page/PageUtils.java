/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.GetInstitutionDependency;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.GetInstitutionNames;

public final class PageUtils {

    private static final Logger log = LoggerFactory.getLogger(PageUtils.class);

    public static final Supplier<LocTextKey> confirmDeactivation(
            final Entity entity,
            final RestService restService) {

        return () -> {
            try {
                final Set<EntityKey> dependencies = restService.getBuilder(GetInstitutionDependency.class)
                        .withURIVariable(API.PARAM_MODEL_ID, String.valueOf(entity.getModelId()))
                        .withQueryParam(API.PARAM_BULK_ACTION_TYPE, BulkActionType.DEACTIVATE.name())
                        .call()
                        .getOrThrow();
                final int size = dependencies.size();
                if (size > 0) {
                    return new LocTextKey("sebserver.dialog.confirm.deactivation", String.valueOf(size));
                } else {
                    return new LocTextKey("sebserver.dialog.confirm.deactivation.noDependencies");
                }
            } catch (final Exception e) {
                log.error("Failed to get dependencyies for Entity: {}", entity, e);
                return new LocTextKey("sebserver.dialog.confirm.deactivation", "");
            }
        };
    }

    public static List<Tuple<String>> getInstitutionSelectionResource(final RestService restService) {
        return restService.getBuilder(GetInstitutionNames.class)
                .call()
                .getOr(Collections.emptyList())
                .stream()
                .map(entityName -> new Tuple<>(entityName.modelId, entityName.name))
                .collect(Collectors.toList());
    }

}
