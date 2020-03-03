/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page;

/** Defines a global SEB Server page */
public interface PageDefinition {

    /** Get the type class of the TemplateComposer that composes the page.
     *
     * @return the type class of the TemplateComposer that composes the page. */
    Class<? extends TemplateComposer> composer();

    PageContext applyPageContext(PageContext pageContext);
}
