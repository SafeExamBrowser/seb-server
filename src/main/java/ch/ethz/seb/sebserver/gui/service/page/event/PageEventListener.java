/*
 * Copyright (c) 2018 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.event;

/** Defines a listener for PageEvent.
 *
 * Potentially every component on the actual page tree can listen to an certain page event
 * by adding a specified listener within the components setData functionality.
 *
 * @param <T> the type of the PageEvent to listen to */
public interface PageEventListener<T extends PageEvent> {

    /** The key name used to register PageEventListener instances within the
     * setData functionality of a component */
    String LISTENER_ATTRIBUTE_KEY = "PageEventListener";

    /** Used to check a concrete listener is interested in a specified type of PageEvent.
     *
     * @param eventType the PageEvent type
     * @return whether the listener is interested in being notified by the event or not */
    boolean match(Class<? extends PageEvent> eventType);

    /** The listeners priority.
     * Use this if a dedicated order or sequence of listener notification is needed.
     * Default priority is 1
     *
     * @return the priority of the listener that defines in witch sequence listeners of the same
     *         type get notified on a PageEvent propagation process on the current page-tree */
    default int priority() {
        return 1;
    }

    void notify(T event);

}
