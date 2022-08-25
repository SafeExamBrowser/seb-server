/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.exam;

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.exam.ClientGroup.ClientGroupType;

public interface ClientGroupData extends Entity {

    Long getId();

    ClientGroupType getType();

    String getColor();

    String getIcon();

    String getIpRangeStart();

    String getIpRangeEnd();

    String getClientOS();
}
