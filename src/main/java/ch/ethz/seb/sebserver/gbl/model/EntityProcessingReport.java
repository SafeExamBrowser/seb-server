/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EntityProcessingReport {

    @JsonProperty(value = "source", required = true)
    public final Collection<Entity> source;
    @JsonProperty(value = "dependencies", required = true)
    public final Collection<EntityKeyAndName> dependencies;
    @JsonProperty(value = "errors", required = true)
    public final Map<EntityKeyAndName, String> errors;

    @JsonCreator
    public EntityProcessingReport(
            @JsonProperty(value = "source", required = true) final Collection<Entity> source,
            @JsonProperty(value = "dependencies", required = true) final Collection<EntityKeyAndName> dependencies,
            @JsonProperty(value = "errors", required = true) final Map<EntityKeyAndName, String> errors) {

        this.source = Collections.unmodifiableCollection(source);
        this.dependencies = Collections.unmodifiableCollection(dependencies);
        this.errors = Collections.unmodifiableMap(errors);
    }

}
