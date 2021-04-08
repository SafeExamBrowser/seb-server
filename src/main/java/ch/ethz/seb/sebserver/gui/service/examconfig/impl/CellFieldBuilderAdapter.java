/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.TitleOrientation;
import ch.ethz.seb.sebserver.gui.service.examconfig.ExamConfigurationService;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

interface CellFieldBuilderAdapter {

    CellFieldBuilderAdapter DUMMY_BUILDER_ADAPTER = new CellFieldBuilderAdapter() {
        @Override
        public void createCell(final ViewGridBuilder builder) {
        }

        @Override
        public String toString() {
            return "[DUMMY]";
        }
    };

    void createCell(ViewGridBuilder builder);

    default void balanceGrid(final CellFieldBuilderAdapter[][] grid, final int x, final int y) {
    }

    static CellFieldBuilderAdapter labelBuilder(
            final ConfigurationAttribute attribute,
            final Orientation orientation) {

        return new CellFieldBuilderAdapter() {

            private int span = 1;

            @Override
            public void createCell(final ViewGridBuilder builder) {

                final WidgetFactory widgetFactory = builder.examConfigurationService.getWidgetFactory();
                final Label label = widgetFactory.labelLocalized(
                        builder.parent,
                        new LocTextKey(ExamConfigurationService.ATTRIBUTE_LABEL_LOC_TEXT_PREFIX + attribute.name,
                                attribute.name),
                        true);
                final GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
                switch (orientation.title) {
                    case LEFT:
                    case RIGHT: {
                        label.setAlignment(SWT.LEFT);
                        gridData.verticalIndent = 5;
                        break;
                    }
                    case RIGHT_SPAN:
                    case LEFT_SPAN: {
                        label.setAlignment(SWT.LEFT);
                        gridData.horizontalSpan = (this.span > 1) ? this.span : orientation.width;
                        gridData.verticalIndent = 5;
                        break;
                    }
                    case TOP: {
                        gridData.horizontalSpan = orientation.width;
                        gridData.verticalAlignment = SWT.BOTTOM;
                        break;
                    }

                    default: {
                        label.setAlignment(SWT.LEFT);
                        break;
                    }
                }
                label.setLayoutData(gridData);
                label.pack();
            }

            @Override
            public void balanceGrid(final CellFieldBuilderAdapter[][] grid, final int x, final int y) {
                if (grid[y][x] != this) {
                    return;
                }
                if (orientation.title == TitleOrientation.LEFT_SPAN) {
                    int xpos = x - 1;
                    while (xpos >= 0 && grid[y][xpos] == null && this.span < orientation.width) {
                        grid[y][xpos] = this;
                        grid[y][xpos + 1] = DUMMY_BUILDER_ADAPTER;
                        this.span++;
                        xpos--;
                    }
                }
            }

            @Override
            public String toString() {
                return "[LABEL]";
            }
        };
    }

    static CellFieldBuilderAdapter passwordConfirmLabel(
            final ConfigurationAttribute attribute,
            final Orientation orientation) {

        return new CellFieldBuilderAdapter() {
            @Override
            public void createCell(final ViewGridBuilder builder) {
                final WidgetFactory widgetFactory = builder.examConfigurationService.getWidgetFactory();
                final Label label = widgetFactory.labelLocalized(
                        builder.parent,
                        new LocTextKey(
                                ExamConfigurationService.ATTRIBUTE_LABEL_LOC_TEXT_PREFIX + attribute.name
                                        + ".confirm"));
                final GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
                label.setAlignment(SWT.LEFT);
                gridData.verticalIndent = 20;
                label.setLayoutData(gridData);
            }

            @Override
            public String toString() {
                return "[PASSWORD CONFIRM LABEL]";
            }
        };
    }

    class GroupCellFieldBuilderAdapter implements CellFieldBuilderAdapter {

        final Collection<Orientation> orientationsOfGroup;

        int x = 100;
        int y = 100;
        int width = 1;
        int height = 1;

        GroupCellFieldBuilderAdapter(final Collection<Orientation> orientationsOfGroup) {
            this.orientationsOfGroup = orientationsOfGroup;

            for (final Orientation o : this.orientationsOfGroup) {
                final int xpos = o.xPosition - ((o.title == TitleOrientation.LEFT) ? 1 : 0);
                this.x = Math.min(xpos, this.x);
                final int ypos = o.yPosition - ((o.title == TitleOrientation.TOP) ? 1 : 0);
                this.y = Math.min(ypos, this.y);
                this.width = Math.max(this.width, o.xpos() + o.width());
                this.height = Math.max(this.height, o.ypos() + o.height());
            }

            this.width = this.width - this.x;
            this.height = this.height - this.y + 1;
        }

        @Override
        public void createCell(final ViewGridBuilder builder) {
            final WidgetFactory widgetFactory = builder.examConfigurationService.getWidgetFactory();
            final Orientation o = this.orientationsOfGroup.stream().findFirst().orElse(null);
            final String groupKey = ViewGridBuilder.getGroupKey(o.groupId);
            final LocTextKey groupLabelKey = new LocTextKey(
                    ExamConfigurationService.GROUP_LABEL_LOC_TEXT_PREFIX +
                            groupKey,
                    groupKey);
            final LocTextKey groupTooltipKey = new LocTextKey(
                    ExamConfigurationService.GROUP_LABEL_LOC_TEXT_PREFIX +
                            groupKey +
                            ExamConfigurationService.TOOL_TIP_SUFFIX,
                    groupKey);

            final Group group = widgetFactory.groupLocalized(
                    builder.parent,
                    this.width,
                    groupLabelKey,
                    groupTooltipKey);
            group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, this.width, this.height));

            final ViewGridBuilder groupBuilder = new ViewGridBuilder(
                    group,
                    builder.viewContext,
                    this,
                    builder.examConfigurationService);

            for (final Orientation orientation : this.orientationsOfGroup) {
                final ConfigurationAttribute attribute = builder.viewContext.getAttribute(orientation.attributeId);
                groupBuilder.add(attribute);
            }
            groupBuilder.compose();
        }
    }

    class ExpandBarCellFieldBuilderAdapter implements CellFieldBuilderAdapter {

        final Map<String, Collection<Orientation>> orientationsOfExpandBar;

        int x = 100;
        int y = 100;
        int width = 1;
        int height = 1;

        ExpandBarCellFieldBuilderAdapter(final Collection<Orientation> orientationsOfExpandBar) {
            this.orientationsOfExpandBar = new HashMap<>();

            for (final Orientation o : orientationsOfExpandBar) {
                final String expandKey = ViewGridBuilder.getExpandKey(o.groupId);
                if (expandKey == null) {
                    continue;
                }

                this.orientationsOfExpandBar
                        .computeIfAbsent(expandKey, key -> new ArrayList<>())
                        .add(o);

                final int xpos = o.xPosition - ((o.title == TitleOrientation.LEFT) ? 1 : 0);
                this.x = Math.min(xpos, this.x);
                final int ypos = o.yPosition - ((o.title == TitleOrientation.TOP) ? 1 : 0);
                this.y = Math.min(ypos, this.y);
                this.width = Math.max(this.width, o.xpos() + o.width());
                this.height = Math.max(this.height, o.ypos() + o.height());
            }

            this.width = this.width - this.x;
            this.height = this.height - this.y + 2;
        }

        @Override
        public void createCell(final ViewGridBuilder builder) {
            final WidgetFactory widgetFactory = builder.examConfigurationService.getWidgetFactory();
            final LocTextKey expandTooltipText = this.orientationsOfExpandBar
                    .keySet()
                    .stream()
                    .findFirst()
                    .map(key -> {
                        final String expandGroupKey = ViewGridBuilder.getExpandGroupKey(key);
                        return new LocTextKey(
                                ExamConfigurationService.TOOL_TIP_SUFFIX + expandGroupKey,
                                expandGroupKey);
                    }).orElse(null);

            final ExpandBar expandBar = widgetFactory.expandBarLocalized(
                    builder.parent,
                    expandTooltipText,
                    true);
            expandBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, this.width, this.height + 2));

            for (final Map.Entry<String, Collection<Orientation>> entry : this.orientationsOfExpandBar.entrySet()) {

                final String expandItemKey = ViewGridBuilder.getExpandItemKey(entry.getKey());
                final Collection<Orientation> value = entry.getValue();
                final LocTextKey labelKey = new LocTextKey(
                        ExamConfigurationService.GROUP_LABEL_LOC_TEXT_PREFIX + expandItemKey,
                        expandItemKey);

                final ExpandItem expandItem = widgetFactory.expandItemLocalized(
                        expandBar,
                        this.width,
                        labelKey);

                expandItem.setHeight(this.height * 23);
                final Composite body = (Composite) expandItem.getControl();
                final ViewGridBuilder expandBuilder = new ViewGridBuilder(
                        body,
                        builder.viewContext,
                        this,
                        builder.examConfigurationService);

                for (final Orientation orientation : value) {
                    final ConfigurationAttribute attribute = builder.viewContext.getAttribute(orientation.attributeId);
                    expandBuilder.add(attribute);
                }
                expandBuilder.compose();
            }
        }
    }

}