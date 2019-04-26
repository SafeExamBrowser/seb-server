/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig;

import java.util.Set;

import ch.ethz.seb.sebserver.gui.service.examconfig.impl.ViewContext;

public interface ValueChangeRule {

    Set<String> observedAttributeNames();

    void applyRule(ViewContext context, String attributeName, String value);

}
