/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content;

import java.util.function.Function;

import org.eclipse.swt.widgets.Composite;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.lmssetup.GetLmsSetups;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition.TableFilterAttribute;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.table.TableFilter.CriteriaType;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class LmsSetupList implements TemplateComposer {

    private final WidgetFactory widgetFactory;
    private final ResourceService resourceService;
    private final int pageSize;

    protected LmsSetupList(
            final WidgetFactory widgetFactory,
            final ResourceService resourceService,
            @Value("${sebserver.gui.list.page.size}") final Integer pageSize) {

        this.widgetFactory = widgetFactory;
        this.resourceService = resourceService;
        this.pageSize = (pageSize != null) ? pageSize : 20;
    }

    @Override
    public void compose(final PageContext pageContext) {
        final CurrentUser currentUser = this.resourceService.getCurrentUser();
        final RestService restService = this.resourceService.getRestService();
        final I18nSupport i18nSupport = this.widgetFactory.getI18nSupport();

        // content page layout with title
        final Composite content = this.widgetFactory.defaultPageLayout(
                pageContext.getParent(),
                new LocTextKey("sebserver.lmssetup.list.title"));

        final boolean isSEBAdmin = currentUser.get().hasRole(UserRole.SEB_SERVER_ADMIN);

        // table
        final EntityTable<LmsSetup> table =
                this.widgetFactory.entityTableBuilder(restService.getRestCall(GetLmsSetups.class))
                        .withEmptyMessage(new LocTextKey("sebserver.lmssetup.list.empty"))
                        .withPaging(this.pageSize)
                        .withColumnIf(() -> isSEBAdmin,
                                new ColumnDefinition<>(
                                        Domain.LMS_SETUP.ATTR_INSTITUTION_ID,
                                        new LocTextKey("sebserver.lmssetup.list.column.institution"),
                                        lmsSetupInstitutionNameFunction(this.resourceService),
                                        new TableFilterAttribute(
                                                CriteriaType.SINGLE_SELECTION,
                                                Domain.USER.ATTR_INSTITUTION_ID,
                                                this.resourceService::institutionResource),
                                        false))
                        .withColumn(new ColumnDefinition<>(
                                Domain.LMS_SETUP.ATTR_NAME,
                                new LocTextKey("sebserver.lmssetup.list.column.name"),
                                entity -> entity.name,
                                (isSEBAdmin)
                                        ? new TableFilterAttribute(CriteriaType.TEXT, Domain.LMS_SETUP.ATTR_NAME)
                                        : null,
                                true))
                        .withColumn(new ColumnDefinition<>(
                                Domain.LMS_SETUP.ATTR_LMS_TYPE,
                                new LocTextKey("sebserver.lmssetup.list.column.type"),
                                this::lmsSetupTypeName,
                                (isSEBAdmin)
                                        ? new TableFilterAttribute(
                                                CriteriaType.SINGLE_SELECTION,
                                                Domain.LMS_SETUP.ATTR_LMS_TYPE,
                                                this.resourceService::lmsTypeResources)
                                        : null,
                                false, true))
                        .withColumn(new ColumnDefinition<>(
                                Domain.LMS_SETUP.ATTR_ACTIVE,
                                new LocTextKey("sebserver.lmssetup.list.column.active"),
                                entity -> entity.active,
                                true))
                        .compose(content);

    }

    private String lmsSetupTypeName(final LmsSetup lmsSetup) {
        if (lmsSetup.lmsType == null) {
            return Constants.EMPTY_NOTE;
        }

        return this.resourceService.getI18nSupport()
                .getText("sebserver.lmssetup.type." + lmsSetup.lmsType.name());
    }

    private static Function<LmsSetup, String> lmsSetupInstitutionNameFunction(final ResourceService resourceService) {
        final Function<String, String> institutionNameFunction = resourceService.getInstitutionNameFunction();
        return lmsSetup -> institutionNameFunction.apply(String.valueOf(lmsSetup.institutionId));
    }

}
