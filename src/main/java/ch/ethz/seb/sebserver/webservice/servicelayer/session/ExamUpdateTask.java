/*
 * Copyright (c) 2023 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

/** Defines a exam update task. Exam update tasks are called in a fixed time interval on the master
 * Webservice instance to update various exam data like state, LMS data and so on.
 * A ExamUpdateTask can define a processing order on with the overall scheduler acts. Lower order first processed.
 */
public interface ExamUpdateTask {

    int examUpdateTaskProcessingOrder();

    void processExamUpdateTask();

}
