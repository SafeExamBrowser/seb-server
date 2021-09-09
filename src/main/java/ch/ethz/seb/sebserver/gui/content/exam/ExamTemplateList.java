/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.exam;

import java.util.function.BooleanSupplier;
import java.util.function.Function;

import org.eclipse.swt.widgets.Composite;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamTemplate;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetExamTemplatePage;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser.GrantCheck;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition.TableFilterAttribute;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.table.TableFilter.CriteriaType;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class ExamTemplateList implements TemplateComposer {

    public static final LocTextKey PAGE_TITLE_KEY =
            new LocTextKey("sebserver.examtemplate.list.title");
    public final static LocTextKey COLUMN_TITLE_INSTITUTION_KEY =
            new LocTextKey("sebserver.examtemplate.list.column.institution");
    public final static LocTextKey COLUMN_TITLE_NAME_KEY =
            new LocTextKey("sebserver.examtemplate.list.column.name");
    public final static LocTextKey COLUMN_TITLE_EXAM_TYPE_KEY =
            new LocTextKey("sebserver.examtemplate.list.column.examType");
    public final static LocTextKey COLUMN_TITLE_DEFAULT_KEY =
            new LocTextKey("sebserver.examtemplate.list.column.default");
    public final static LocTextKey EMPTY_LIST_TEXT_KEY =
            new LocTextKey("sebserver.examtemplate.list.empty");

    public final static LocTextKey COLUMN_TITLE_DEFAULT_TRUE_KEY =
            new LocTextKey("sebserver.examtemplate.list.column.default.true");
    public final static LocTextKey COLUMN_TITLE_DEFAULT_FALSE_KEY =
            new LocTextKey("sebserver.examtemplate.list.column.default.false");

    public static final LocTextKey NO_MODIFY_PRIVILEGE_ON_OTHER_INSTITUTION =
            new LocTextKey("sebserver.examtemplate.list.action.no.modify.privilege");
    public final static LocTextKey EMPTY_SELECTION_TEXT_KEY =
            new LocTextKey("sebserver.examtemplate.info.pleaseSelect");

    private final PageService pageService;
    private final ResourceService resourceService;
    private final int pageSize;

    private final TableFilterAttribute institutionFilter;
    private final TableFilterAttribute nameFilter =
            new TableFilterAttribute(CriteriaType.TEXT, ExamTemplate.FILTER_ATTR_NAME);
    private final TableFilterAttribute typeFilter;

    protected ExamTemplateList(
            final PageService pageService,
            @Value("${sebserver.gui.list.page.size:20}") final Integer pageSize) {

        this.pageService = pageService;
        this.resourceService = pageService.getResourceService();
        this.pageSize = pageSize;

        this.institutionFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                Entity.FILTER_ATTR_INSTITUTION,
                this.resourceService::institutionResource);

        this.typeFilter = new TableFilterAttribute(
                CriteriaType.SINGLE_SELECTION,
                ExamTemplate.FILTER_ATTR_EXAM_TYPE,
                this.resourceService::examTypeResources);
    }

    @Override
    public void compose(final PageContext pageContext) {
        final WidgetFactory widgetFactory = this.pageService.getWidgetFactory();
        final CurrentUser currentUser = this.resourceService.getCurrentUser();
        final RestService restService = this.resourceService.getRestService();
        final I18nSupport i18nSupport = this.pageService.getI18nSupport();

        // content page layout with title
        final Composite content = widgetFactory.defaultPageLayout(
                pageContext.getParent(),
                PAGE_TITLE_KEY);

        final PageActionBuilder actionBuilder = this.pageService
                .pageActionBuilder(pageContext.clearEntityKeys());

        final BooleanSupplier isSEBAdmin =
                () -> currentUser.get().hasRole(UserRole.SEB_SERVER_ADMIN);

        final Function<String, String> institutionNameFunction =
                this.resourceService.getInstitutionNameFunction();

        // table
        final EntityTable<ExamTemplate> table =
                this.pageService.entityTableBuilder(restService.getRestCall(GetExamTemplatePage.class))
                        .withEmptyMessage(EMPTY_LIST_TEXT_KEY)
                        .withPaging(this.pageSize)
                        .withDefaultSort(isSEBAdmin.getAsBoolean()
                                ? Domain.EXAM_TEMPLATE.ATTR_INSTITUTION_ID
                                : Domain.EXAM_TEMPLATE.ATTR_NAME)

                        .withColumnIf(
                                isSEBAdmin,
                                () -> new ColumnDefinition<ExamTemplate>(
                                        Domain.EXAM_TEMPLATE.ATTR_INSTITUTION_ID,
                                        COLUMN_TITLE_INSTITUTION_KEY,
                                        examTemplate -> institutionNameFunction
                                                .apply(String.valueOf(examTemplate.getInstitutionId())))
                                                        .withFilter(this.institutionFilter)
                                                        .sortable())

                        .withColumn(new ColumnDefinition<>(
                                Domain.EXAM_TEMPLATE.ATTR_NAME,
                                COLUMN_TITLE_NAME_KEY,
                                ExamTemplate::getName)
                                        .withFilter(this.nameFilter)
                                        .sortable())

                        .withColumn(new ColumnDefinition<ExamTemplate>(
                                Domain.EXAM_TEMPLATE.ATTR_EXAM_TYPE,
                                COLUMN_TITLE_EXAM_TYPE_KEY,
                                this.resourceService::localizedExamTypeName)
                                        .withFilter(this.typeFilter)
                                        .sortable())

                        .withColumn(new ColumnDefinition<ExamTemplate>(
                                Domain.EXAM_TEMPLATE.ATTR_INSTITUTIONAL_DEFAULT,
                                COLUMN_TITLE_DEFAULT_KEY,
                                et -> (et.institutionalDefault)
                                        ? i18nSupport.getText(COLUMN_TITLE_DEFAULT_TRUE_KEY)
                                        : i18nSupport.getText(COLUMN_TITLE_DEFAULT_FALSE_KEY)))

                        .withDefaultAction(actionBuilder
                                .newAction(ActionDefinition.EXAM_TEMPLATE_VIEW_FROM_LIST)
                                .create())

                        .withSelectionListener(this.pageService.getSelectionPublisher(
                                pageContext,
                                ActionDefinition.EXAM_TEMPLATE_VIEW_FROM_LIST,
                                ActionDefinition.EXAM_TEMPLATE_MODIFY_FROM_LIST))

                        .compose(pageContext.copyOf(content));

        final GrantCheck userGrant = currentUser.grantCheck(EntityType.EXAM_TEMPLATE);
        actionBuilder
                .newAction(ActionDefinition.EXAM_TEMPLATE_NEW)
                .publishIf(userGrant::iw)

                .newAction(ActionDefinition.EXAM_TEMPLATE_VIEW_FROM_LIST)
                .withSelect(table::getSelection, PageAction::applySingleSelectionAsEntityKey, EMPTY_SELECTION_TEXT_KEY)
                .publish(false)

                .newAction(ActionDefinition.EXAM_TEMPLATE_MODIFY_FROM_LIST)
                .withSelect(table::getSelection, PageAction::applySingleSelectionAsEntityKey, EMPTY_SELECTION_TEXT_KEY)
                .publishIf(() -> userGrant.im(), false);
    }

}
