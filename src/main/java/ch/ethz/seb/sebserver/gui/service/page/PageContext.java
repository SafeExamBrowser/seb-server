/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import ch.ethz.seb.sebserver.gui.service.page.event.PageEvent;

public interface PageContext {

    public static final class PageAttr {

        public final String name;
        public final String value;

        public PageAttr(final String name, final String value) {
            this.name = name;
            this.value = value;
        }
    }

    public interface AttributeKeys {

        public static final String ATTR_PAGE_TEMPLATE_COMPOSER_NAME = "ATTR_PAGE_TEMPLATE_COMPOSER_NAME";

        public static final String INSTITUTION_ID = "INSTITUTION_ID";

//        public static final String USER_NAME = "USER_NAME";
//        public static final String PASSWORD = "PASSWORD";
//

//
//        public static final String CONFIG_ID = "CONFIG_ID";
//        public static final String CONFIG_VIEW_NAME = "CONFIG_VIEW_NAME";
//        public static final String CONFIG_ATTRIBUTE_SAVE_TYPE = "CONFIG_ATTRIBUTE_SAVE_TYPE";
//        public static final String CONFIG_ATTRIBUTE_VALUE = "CONFIG_ATTRIBUTE_VALUE";
//
//        public static final String EXAM_ID = "EXAM_ID";
//        public static final String STATE_NAME = "STATE_NAME";
//
//        public static final String AUTHORIZATION_CONTEXT = "AUTHORIZATION_CONTEXT";
//        public static final String AUTHORIZATION_HEADER = "AUTHORIZATION_HEADER";
        public static final String AUTHORIZATION_FAILURE = "AUTHORIZATION_FAILURE";
        public static final String LGOUT_SUCCESS = "LGOUT_SUCCESS";

    }

    ComposerService composerService();

    Shell getShell();

    /** Get the page root Component.
     *
     * @return the page root Component. */
    Composite getRoot();

    /** Get the Component that is currently set as parent during page tree compose
     *
     * @return the parent Component */
    Composite getParent();

    /** Create a copy of this PageContext with a new parent Composite.
     *
     * @param parent the new parent Composite
     * @return a copy of this PageContext with a new parent Composite. */
    PageContext copyOf(Composite parent);

    /** Create a copy of this PageContext with and additionally page context attributes.
     * The additionally page context attributes will get merged with them already defined
     *
     * @param attributes additionally page context attributes.
     * @return a copy of this PageContext with with and additionally page context attributes. */
    PageContext copyOfAttributes(PageContext otherContext);

    /** Adds the specified attribute to the existing page context attributes.
     *
     * @param key the key of the attribute
     * @param value the value of the attribute
     * @return this PageContext instance (builder pattern) */
    PageContext withAttr(String key, String value);

    String getAttribute(String name);

    String getAttribute(String name, String def);

    boolean hasAttribute(String name);

    /** Publishes a given PageEvent to the current page tree
     * This goes through the page-tree and collects all listeners the are listen to
     * the specified page event type.
     *
     * @param event the concrete PageEvent instance */
    <T extends PageEvent> void publishPageEvent(T event);

    /** Apply a confirm dialog with a specified confirm message and a callback code
     * block that will be executed on users OK selection.
     *
     * @param confirmMessage
     * @param onOK callback code block that will be executed on users OK selection */
    void applyConfirmDialog(String confirmMessage, Runnable onOK);

    void forwardToPage(
            PageDefinition pageDefinition,
            PageContext pageContext);

    void forwardToMainPage(PageContext pageContext);

    void forwardToLoginPage(PageContext pageContext);

    /** Notify an error dialog to the user with specified error message and
     * optional exception instance
     *
     * @param errorMessage the error message to display
     * @param error the error as Throwable */
    void notifyError(String errorMessage, Throwable error);

    void notifyError(Throwable error);

    <T> T logoutOnError(Throwable error);

}
