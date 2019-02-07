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
import ch.ethz.seb.sebserver.gbl.model.institution.Institution;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.page.action.InstitutionActions;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.institution.GetInstitutions;
import ch.ethz.seb.sebserver.gui.service.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.service.table.EntityTable;
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

        // title
        this.widgetFactory.labelLocalizedTitle(
                content,
                new LocTextKey("sebserver.institution.list.title"));

        // table
        final EntityTable<Institution> table =
                this.widgetFactory.entityTableBuilder(this.restService.getRestCall(GetInstitutions.class))
                        .withPaging(3)
                        .withColumn(new ColumnDefinition<>(
                                Domain.INSTITUTION.ATTR_NAME,
                                new LocTextKey("sebserver.institution.list.column.name"),
                                entity -> entity.name,
                                true))
                        .withColumn(new ColumnDefinition<>(
                                Domain.INSTITUTION.ATTR_URL_SUFFIX,
                                new LocTextKey("sebserver.institution.list.column.urlSuffix"),
                                entity -> entity.urlSuffix,
                                true))
                        .withColumn(new ColumnDefinition<>(
                                Domain.INSTITUTION.ATTR_ACTIVE,
                                new LocTextKey("sebserver.institution.list.column.active"),
                                entity -> entity.active,
                                true))
                        .compose(content);

        // propagate content actions to action-pane
        pageContext.createAction(ActionDefinition.INSTITUTION_NEW)
                .withExec(InstitutionActions::newInstitution)
                .publish()
                .createAction(ActionDefinition.INSTITUTION_MODIFY)
                .withExec(InstitutionActions.editInstitution(table))
                .publish();

    }

}
