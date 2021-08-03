/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page;

/** interface defining a RAP page template composer */
public interface TemplateComposer {

    /** Validate given PageContext for completeness to compose a specific TemplateComposer implementation
     * Default returns always true.
     *
     * @param pageContext The PageContext instance to check
     * @return true if the PageContext contains all mandatory data to compose this page template */
    default boolean validate(final PageContext pageContext) {
        return true;
    }

    /** Compose a specific page template for the given PageContext
     *
     * @param pageContext The PageContext instance */
    void compose(PageContext pageContext);

}
