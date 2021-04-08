/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.session.proctoring;

import ch.ethz.seb.sebserver.gui.service.session.proctoring.ProctoringGUIService.ProctoringWindowData;

/** Defines a proctoring window script resolver that generates the
 * proctoring window html and script code for a particular
 * proctoring service. */
public interface ProctoringWindowScriptResolver {

    /** Indicates if the concrete implementation applies to given proctoring data.
     * Usually this looks after the proctoring service type within the given data
     * and returns true if the implementation is compatible with the given proctoring
     * service type.
     * 
     * @param data ProctoringWindowData instance containing actual proctoring data
     * @return true if a concrete implementation applies to the given data */
    boolean applies(ProctoringWindowData data);

    /** Produces the html and java script page to open in a proctoring window pop-up.
     *
     * @param data ProctoringWindowData instance containing actual proctoring data
     * @return the html and java script page to open in a proctoring window pop-up */
    String getProctoringWindowScript(ProctoringWindowData data);

}
