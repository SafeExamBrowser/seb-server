/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.event;

/** This is just a marker interface for all page events.
 * Page events can be published to the actual page tree by using PageContext.publishPageEvent
 *
 * Potentially every component on the actual page tree can listen to an certain page event
 * by adding a specified listener within the components setData functionality.
 * see PageListener for more information */
public interface PageEvent {
}
