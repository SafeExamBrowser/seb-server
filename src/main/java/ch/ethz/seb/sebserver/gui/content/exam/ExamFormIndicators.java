/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.content.exam;

import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.swt.widgets.Composite;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.Threshold;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.content.action.ActionDefinition;
import ch.ethz.seb.sebserver.gui.service.ResourceService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.PageContext.AttributeKeys;
import ch.ethz.seb.sebserver.gui.service.page.PageService;
import ch.ethz.seb.sebserver.gui.service.page.PageService.PageActionBuilder;
import ch.ethz.seb.sebserver.gui.service.page.TemplateComposer;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.DeleteIndicator;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam.GetIndicatorPage;
import ch.ethz.seb.sebserver.gui.table.ColumnDefinition;
import ch.ethz.seb.sebserver.gui.table.EntityTable;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

@Lazy
@Component
@GuiProfile
public class ExamFormIndicators implements TemplateComposer {

    private final static LocTextKey INDICATOR_LIST_TITLE_KEY =
            new LocTextKey("sebserver.exam.indicator.list.title");
    private final static LocTextKey INDICATOR_LIST_TITLE_TOOLTIP_KEY =
            new LocTextKey("sebserver.exam.indicator.list.title" + Constants.TOOLTIP_TEXT_KEY_SUFFIX);
    private final static LocTextKey INDICATOR_TYPE_COLUMN_KEY =
            new LocTextKey("sebserver.exam.indicator.list.column.type");
    private final static LocTextKey INDICATOR_NAME_COLUMN_KEY =
            new LocTextKey("sebserver.exam.indicator.list.column.name");
    private final static LocTextKey INDICATOR_THRESHOLD_COLUMN_KEY =
            new LocTextKey("sebserver.exam.indicator.list.column.thresholds");
    private final static LocTextKey INDICATOR_EMPTY_SELECTION_TEXT_KEY =
            new LocTextKey("sebserver.exam.indicator.list.pleaseSelect");
    private static final LocTextKey INDICATOR_EMPTY_LIST_MESSAGE =
            new LocTextKey("sebserver.exam.indicator.list.empty");

    private final PageService pageService;
    private final ResourceService resourceService;
    private final WidgetFactory widgetFactory;
    private final RestService restService;

    public ExamFormIndicators(final PageService pageService) {
        this.pageService = pageService;
        this.resourceService = pageService.getResourceService();
        this.widgetFactory = pageService.getWidgetFactory();
        this.restService = pageService.getRestService();
    }

    @Override
    public void compose(final PageContext pageContext) {
        final Composite content = pageContext.getParent();
        final EntityKey entityKey = pageContext.getEntityKey();
        final boolean editable = BooleanUtils.toBoolean(
                pageContext.getAttribute(ExamForm.ATTR_EDITABLE));

        // List of Indicators
        this.widgetFactory.addFormSubContextHeader(
                content,
                INDICATOR_LIST_TITLE_KEY,
                INDICATOR_LIST_TITLE_TOOLTIP_KEY);

        final PageActionBuilder actionBuilder = this.pageService.pageActionBuilder(pageContext
                .clearEntityKeys()
                .removeAttribute(AttributeKeys.IMPORT_FROM_QUIZ_DATA));

        final EntityTable<Indicator> indicatorTable =
                this.pageService.entityTableBuilder(this.restService.getRestCall(GetIndicatorPage.class))
                        .withRestCallAdapter(builder -> builder.withQueryParam(
                                Indicator.FILTER_ATTR_EXAM_ID,
                                entityKey.modelId))
                        .withEmptyMessage(INDICATOR_EMPTY_LIST_MESSAGE)
                        .withMarkup()
                        .withPaging(100)
                        .hideNavigation()
                        .withColumn(new ColumnDefinition<>(
                                Domain.INDICATOR.ATTR_NAME,
                                INDICATOR_NAME_COLUMN_KEY,
                                Indicator::getName)
                                        .widthProportion(2))
                        .withColumn(new ColumnDefinition<>(
                                Domain.INDICATOR.ATTR_TYPE,
                                INDICATOR_TYPE_COLUMN_KEY,
                                this::indicatorTypeName)
                                        .widthProportion(1))
                        .withColumn(new ColumnDefinition<Indicator>(
                                Domain.THRESHOLD.REFERENCE_NAME,
                                INDICATOR_THRESHOLD_COLUMN_KEY,
                                i -> thresholdsValue(i.thresholds, i.type))
                                        .asMarkup()
                                        .widthProportion(4))
                        .withDefaultActionIf(
                                () -> editable,
                                () -> actionBuilder
                                        .newAction(ActionDefinition.EXAM_INDICATOR_MODIFY_FROM_LIST)
                                        .withParentEntityKey(entityKey)
                                        .create())

                        .withSelectionListener(this.pageService.getSelectionPublisher(
                                pageContext,
                                ActionDefinition.EXAM_INDICATOR_MODIFY_FROM_LIST,
                                ActionDefinition.EXAM_INDICATOR_DELETE_FROM_LIST))

                        .compose(pageContext.copyOf(content));

        actionBuilder

                .newAction(ActionDefinition.EXAM_INDICATOR_MODIFY_FROM_LIST)
                .withParentEntityKey(entityKey)
                .withSelect(
                        indicatorTable::getMultiSelection,
                        PageAction::applySingleSelectionAsEntityKey,
                        INDICATOR_EMPTY_SELECTION_TEXT_KEY)
                .publishIf(() -> editable && indicatorTable.hasAnyContent(), false)

                .newAction(ActionDefinition.EXAM_INDICATOR_DELETE_FROM_LIST)
                .withEntityKey(entityKey)
                .withSelect(
                        indicatorTable::getMultiSelection,
                        this::deleteSelectedIndicator,
                        INDICATOR_EMPTY_SELECTION_TEXT_KEY)
                .publishIf(() -> editable && indicatorTable.hasAnyContent(), false)

                .newAction(ActionDefinition.EXAM_INDICATOR_NEW)
                .withParentEntityKey(entityKey)
                .publishIf(() -> editable);

    }

    private PageAction deleteSelectedIndicator(final PageAction action) {
        final EntityKey indicatorKey = action.getSingleSelection();
        this.resourceService.getRestService()
                .getBuilder(DeleteIndicator.class)
                .withURIVariable(API.PARAM_MODEL_ID, indicatorKey.modelId)
                .call();
        return action;
    }

    private String indicatorTypeName(final Indicator indicator) {
        if (indicator.type == null) {
            return Constants.EMPTY_NOTE;
        }

        return this.resourceService.getI18nSupport()
                .getText(ResourceService.EXAM_INDICATOR_TYPE_PREFIX + indicator.type.name());
    }

    static String thresholdsValue(
            final List<Threshold> thresholds,
            final IndicatorType indicatorType) {

        if (thresholds.isEmpty()) {
            return Constants.EMPTY_NOTE;
        }

        final StringBuilder builder = thresholds
                .stream()
                .reduce(
                        new StringBuilder(),
                        (sb, threshold) -> sb
                                .append("<span style='padding: 2px 5px 2px 5px; background-color: #")
                                .append(threshold.color)
                                .append("; ")
                                .append((Utils.darkColorContrast(Utils.parseRGB(threshold.color)))
                                        ? "color: #4a4a4a; "
                                        : "color: #FFFFFF;")
                                .append("'>")
                                .append(Indicator.getDisplayValue(indicatorType, threshold.value))
                                .append(" (")
                                .append(threshold.color)
                                .append(")")
                                .append("</span>")
                                .append(" | "),
                        StringBuilder::append);
        builder.delete(builder.length() - 3, builder.length() - 1);
        return builder.toString();
    }
}
