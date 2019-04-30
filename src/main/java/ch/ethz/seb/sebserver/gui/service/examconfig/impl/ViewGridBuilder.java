/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gui.service.examconfig.ExamConfigurationService;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputField;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputFieldBuilder;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

public class ViewGridBuilder {

    private final ExamConfigurationService examConfigurationService;
    private final Composite parent;
    private final ViewContext viewContext;
    private final CellFieldBuilderAdapter[][] grid;
    private final Set<String> registeredGroups;

    ViewGridBuilder(
            final Composite parent,
            final ViewContext viewContext,
            final ExamConfigurationService examConfigurationService) {

        this.examConfigurationService = examConfigurationService;
        this.parent = parent;
        this.viewContext = viewContext;
        this.grid = new CellFieldBuilderAdapter[viewContext.rows][viewContext.columns];
        this.registeredGroups = new HashSet<>();

        fillDummy(0, 0, viewContext.columns, viewContext.rows);
    }

    ViewGridBuilder add(final ConfigurationAttribute attribute) {
        // ignore nested attributes here
        if (attribute.parentId != null) {
            return this;
        }

        final Orientation orientation = this.viewContext.attributeMapping
                .getOrientation(attribute.id);

        // create group builder
        if (StringUtils.isNotBlank(orientation.groupId)) {
            if (this.registeredGroups.contains(orientation.groupId)) {
                return this;
            }

            final GroupCellFieldBuilderAdapter groupBuilder =
                    new GroupCellFieldBuilderAdapter(this, attribute);

            fillDummy(groupBuilder.x, groupBuilder.y, groupBuilder.width, groupBuilder.height);
            this.grid[groupBuilder.y][groupBuilder.x] = groupBuilder;
            this.registeredGroups.add(orientation.groupId);
            return this;
        }

        // create single input field with label
        final int xpos = orientation.xpos();
        final int ypos = orientation.ypos();

        final InputFieldBuilder inputFieldBuilder = this.examConfigurationService.getInputFieldBuilder(
                attribute,
                orientation);

        final CellFieldBuilderAdapter fieldBuilderAdapter = fieldBuilderAdapter(
                inputFieldBuilder,
                attribute);

        switch (orientation.title) {
            case RIGHT: {
                this.grid[ypos][xpos] = fieldBuilderAdapter;
                this.grid[ypos][xpos + 1] = labelBuilder(attribute, orientation);
                break;
            }
            case LEFT: {
                this.grid[ypos][xpos] = labelBuilder(attribute, orientation);
                this.grid[ypos][xpos + 1] = fieldBuilderAdapter;
                break;
            }
            case TOP: {
                this.grid[ypos][xpos] = labelBuilder(attribute, orientation);
                this.grid[ypos + 1][xpos] = fieldBuilderAdapter;
            }
            default: {
                this.grid[ypos][xpos] = fieldBuilderAdapter;
            }
        }

        return this;
    }

    void compose() {
        for (int y = 0; y < this.grid.length; y++) {
            for (int x = 0; x < this.grid[y].length; x++) {
                if (this.grid[y][x] == null) {
                    final Label empty = new Label(this.parent, SWT.LEFT);
                    empty.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
                    empty.setText("");
                } else {
                    this.grid[y][x].createCell(this);
                }
            }
        }
    }

    private static interface CellFieldBuilderAdapter {

        void createCell(ViewGridBuilder builder);
    }

    private CellFieldBuilderAdapter dummyBuilderAdapter() {
        return new CellFieldBuilderAdapter() {
            @Override
            public void createCell(final ViewGridBuilder builder) {
            }
        };
    }

    private final CellFieldBuilderAdapter fieldBuilderAdapter(
            final InputFieldBuilder inputFieldBuilder,
            final ConfigurationAttribute attribute) {

        return new CellFieldBuilderAdapter() {
            @Override
            public void createCell(final ViewGridBuilder builder) {

                final InputField inputField = inputFieldBuilder.createInputField(
                        ViewGridBuilder.this.parent,
                        attribute,
                        ViewGridBuilder.this.viewContext);

//                final Orientation orientation =
//                        ViewGridBuilder.this.viewContext.attributeMapping.getOrientation(attribute.id);
//
//                //inputField.setSpan(orientation.width, orientation.height);
                ViewGridBuilder.this.viewContext.registerInputField(inputField);
            }
        };
    }

    private CellFieldBuilderAdapter labelBuilder(
            final ConfigurationAttribute attribute,
            final Orientation orientation) {

        return new CellFieldBuilderAdapter() {
            @Override
            public void createCell(final ViewGridBuilder builder) {

                final WidgetFactory widgetFactory = builder.examConfigurationService.getWidgetFactory();
                final Label label = widgetFactory.labelLocalized(
                        ViewGridBuilder.this.parent,
                        new LocTextKey(ExamConfigurationService.ATTRIBUTE_LABEL_LOC_TEXT_PREFIX + attribute.name),
                        attribute.name);

                final GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
                switch (orientation.title) {
                    case LEFT:
                    case RIGHT: {
                        label.setAlignment(SWT.LEFT);
                        gridData.verticalIndent = 5;
                        break;
                    }
                    case TOP: {
                        label.setAlignment(SWT.BOTTOM);
                        break;
                    }

                    default: {
                        label.setAlignment(SWT.LEFT);
                    }
                }
                label.setLayoutData(gridData);
            }
        };
    }

    private static class GroupCellFieldBuilderAdapter implements CellFieldBuilderAdapter {

        final ViewGridBuilder builder;
        final ConfigurationAttribute attribute;
        final Collection<Orientation> orientationsOfGroup;

        int x = 0;
        final int y = 0;
        int width = 1;
        int height = 1;

        GroupCellFieldBuilderAdapter(
                final ViewGridBuilder builder,
                final ConfigurationAttribute attribute) {

            this.builder = builder;
            this.attribute = attribute;
            this.orientationsOfGroup =
                    builder.viewContext.attributeMapping.getOrientationsOfGroup(attribute);
            for (final Orientation o : this.orientationsOfGroup) {
                this.x = (this.x < o.xpos()) ? o.xpos() : this.x;
                this.x = (this.y < o.ypos()) ? o.ypos() : this.y;
                this.width = (this.width < o.xpos() + o.width()) ? o.xpos() + o.width() : this.width;
                this.height = (this.height < o.ypos() + o.height()) ? o.ypos() + o.height() : this.height;
            }

            this.width = this.width - this.x;
            this.height = this.height - this.y + 2;
        }

        @Override
        public void createCell(final ViewGridBuilder builder) {
            final Group group = new Group(builder.parent, SWT.NONE);
            group.setLayout(new GridLayout(this.width, true));
            group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, this.width, this.height));

            // TODO needs localization?
            final Orientation o = this.orientationsOfGroup.stream().findFirst().get();
            if (o != null) {
                group.setText(o.groupId);
            }

            for (final Orientation orientation : this.orientationsOfGroup) {
                final InputFieldBuilder inputComponentBuilder = builder.examConfigurationService
                        .getInputFieldBuilder(this.attribute, orientation);

                createSingleInputField(group, orientation, inputComponentBuilder);
            }
        }

        private void createSingleInputField(
                final Group group,
                final Orientation orientation,
                final InputFieldBuilder inputFieldBuilder) {

            final ConfigurationAttribute attr = this.builder.viewContext.attributeMapping
                    .getAttribute(orientation.attributeId);

            final InputField inputField = inputFieldBuilder.createInputField(
                    group,
                    attr,
                    this.builder.viewContext);

            final GridData gridData = new GridData(
                    SWT.FILL, SWT.FILL,
                    true, false,
                    orientation.width(), orientation.height());

            inputField.getControl().setLayoutData(gridData);
            inputField.getControl().setToolTipText(attr.name);
            this.builder.viewContext.registerInputField(inputField);
        }
    }

    private void fillDummy(final int x, final int y, final int width, final int height) {
        final int upperBoundX = x + width;
        final int upperBoundY = y + height;
        for (int _y = y; _y < upperBoundY; _y++) {
            for (int _x = x; _x < upperBoundX; _x++) {
                this.grid[_y][_x] = dummyBuilderAdapter();
            }
        }
    }

}
