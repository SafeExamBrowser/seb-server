/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.api.exam.indicator;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;

import ch.ethz.seb.sebserver.gui.api.PageToListCallAdapter;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

@Lazy
@Component

public class GetIndicators extends PageToListCallAdapter<Indicator> {

    public GetIndicators() {
        super(
                GetIndicatorPage.class,
                EntityType.INDICATOR,
                new TypeReference<List<Indicator>>() {
                },
                API.EXAM_INDICATOR_ENDPOINT);
    }
}
