/*
 * Copyright (c) 2023 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl;

import ch.ethz.seb.sebserver.gbl.model.exam.CollectingStrategy;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringServerType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;

public interface FeatureService {

    String FEATURE_SETTINGS_PREFIX = "sebserver.feature.";

    boolean isEnabled(LmsType LmsType);

    boolean isEnabled(ProctoringServerType proctoringServerType);

    boolean isEnabled(CollectingStrategy collectingRoomStrategy);

    boolean isScreenProcteringEnabled();

}
