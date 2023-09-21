/*
 * Copyright (c) 2023 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ScreenProctoringSettings;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface ProctoringSettingsDAO {

    Result<ProctoringServiceSettings> getProctoringSettings(EntityKey entityKey);

    Result<ProctoringServiceSettings> saveProctoringServiceSettings(
            EntityKey entityKey,
            ProctoringServiceSettings proctoringServiceSettings);

    Result<ScreenProctoringSettings> getScreenProctoringSettings(EntityKey entityKey);

    Result<ScreenProctoringSettings> storeScreenProctoringSettings(
            final EntityKey entityKey,
            final ScreenProctoringSettings screenProctoringSettings);

}
