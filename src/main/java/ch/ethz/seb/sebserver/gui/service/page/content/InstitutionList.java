/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.page.content;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.GetInstitutions;
import ch.ethz.seb.sebserver.gui.service.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.service.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class InstitutionList implements TemplateComposer {

    private final WidgetFactory widgetFactory;
    private final RestService restService;

    protected InstitutionList(
            final WidgetFactory widgetFactory,
            final RestService restService) {

        this.widgetFactory = widgetFactory;
        this.restService = restService;
    }

    @Override
    public void compose(final PageContext pageContext) {
        final Composite content = new Composite(pageContext.getParent(), SWT.NONE);
        final GridLayout contentLayout = new GridLayout();
        contentLayout.marginLeft = 10;
        content.setLayout(contentLayout);
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        this.widgetFactory.labelLocalizedTitle(
                content,
                new LocTextKey("sebserver.institution.list.title"));

        this.widgetFactory.entityTableBuilder(this.restService.getRestCall(GetInstitutions.class))
                .withPaging(3)
                .withColumn(new ColumnDefinition<>(
                        Domain.INSTITUTION.ATTR_NAME,
                        new LocTextKey("sebserver.institution.list.column.name"),
                        null,
                        0,
                        entity -> entity.name,
                        null,
                        true))
                .withColumn(new ColumnDefinition<>(
                        Domain.INSTITUTION.ATTR_URL_SUFFIX,
                        new LocTextKey("sebserver.institution.list.column.urlSuffix"),
                        null,
                        0,
                        entity -> entity.urlSuffix,
                        null,
                        true))
                .withColumn(new ColumnDefinition<>(
                        Domain.INSTITUTION.ATTR_ACTIVE,
                        new LocTextKey("sebserver.institution.list.column.active"),
                        null,
                        0,
                        entity -> entity.active,
                        null,
                        true))
                .compose(content);

    }

}
