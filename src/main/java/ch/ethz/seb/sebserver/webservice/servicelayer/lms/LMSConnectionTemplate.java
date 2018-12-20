/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms;

import java.util.Collection;

import ch.ethz.seb.sebserver.gbl.model.institution.CourseData;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;

public interface LMSConnectionTemplate {

    LmsSetup lmsSetup();

    Collection<String> courseNames();

    Collection<CourseData> searchCourses(String name, Long from, Long to);

    CourseData course(String uuid);

}
