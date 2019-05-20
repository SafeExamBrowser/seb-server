/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gui.service.examconfig.ExamConfigurationService;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputFieldBuilder;
import ch.ethz.seb.sebserver.gui.service.examconfig.impl.CellFieldBuilderAdapter.GroupCellFieldBuilderAdapter;

public class ViewGridBuilder {

    private static final Logger log = LoggerFactory.getLogger(ViewGridBuilder.class);

    final ExamConfigurationService examConfigurationService;
    final Composite parent;
    final ViewContext viewContext;

    private final CellFieldBuilderAdapter[][] grid;
    private final GroupCellFieldBuilderAdapter groupBuilderAdapter;
    private final Set<String> registeredGroups;

    ViewGridBuilder(
            final Composite parent,
            final ViewContext viewContext,
            final ExamConfigurationService examConfigurationService) {

        this.examConfigurationService = examConfigurationService;
        this.parent = parent;
        this.viewContext = viewContext;
        this.grid = new CellFieldBuilderAdapter[viewContext.getRows()][viewContext.getColumns()];
        this.groupBuilderAdapter = null;
        this.registeredGroups = new HashSet<>();
    }

    ViewGridBuilder(
            final Composite parent,
            final ViewContext viewContext,
            final GroupCellFieldBuilderAdapter groupBuilderAdapter,
            final ExamConfigurationService examConfigurationService) {

        this.examConfigurationService = examConfigurationService;
        this.parent = parent;
        this.viewContext = viewContext;
        this.groupBuilderAdapter = groupBuilderAdapter;
        this.grid = new CellFieldBuilderAdapter[groupBuilderAdapter.height - 1][groupBuilderAdapter.width];
        this.registeredGroups = null;
    }

    ViewGridBuilder add(final ConfigurationAttribute attribute) {
        if (log.isDebugEnabled()) {
            log.debug("Add SEB Configuration Attribute: " + attribute);
        }

        // ignore nested attributes here
        if (attribute.parentId != null) {
            return this;
        }

        final Orientation orientation = this.viewContext
                .getOrientation(attribute.id);

        // create group if this is not a group builder
        if (this.groupBuilderAdapter == null && StringUtils.isNotBlank(orientation.groupId)) {
            if (this.registeredGroups.contains(orientation.groupId)) {
                return this;
            }

            final GroupCellFieldBuilderAdapter groupBuilder =
                    new GroupCellFieldBuilderAdapter(this.viewContext.getOrientationsOfGroup(attribute));

            fillDummy(groupBuilder.x, groupBuilder.y, groupBuilder.width, groupBuilder.height);
            this.grid[groupBuilder.y][groupBuilder.x] = groupBuilder;
            this.registeredGroups.add(orientation.groupId);
            return this;
        }

        // create single input field with label
        final int xpos = orientation.xpos() + ((this.groupBuilderAdapter != null) ? -this.groupBuilderAdapter.x : 0);
        final int ypos = orientation.ypos() + ((this.groupBuilderAdapter != null) ? -this.groupBuilderAdapter.y : 0);

        if (orientation.width > 1 || orientation.height > 1) {
            fillDummy(xpos, ypos, orientation.width, orientation.height);
        }

        final InputFieldBuilder inputFieldBuilder = this.examConfigurationService.getInputFieldBuilder(
                attribute,
                orientation);

        this.grid[ypos][xpos] = CellFieldBuilderAdapter.fieldBuilderAdapter(
                inputFieldBuilder,
                attribute);

        try {
            switch (orientation.title) {
                case RIGHT:
                case RIGHT_SPAN: {
                    this.grid[ypos][xpos + 1] = CellFieldBuilderAdapter.labelBuilder(
                            attribute,
                            orientation);
                    break;
                }
                case LEFT:
                case LEFT_SPAN: {
                    this.grid[ypos][xpos - 1] = CellFieldBuilderAdapter.labelBuilder(
                            attribute,
                            orientation);
                    // special case for password, also add confirm label
                    if (attribute.type == AttributeType.PASSWORD_FIELD) {
                        this.grid[ypos + 1][xpos - 1] = CellFieldBuilderAdapter.passwordConfirmLabel(
                                attribute,
                                orientation);
                    }
                    break;
                }
//                case LEFT_SPAN: {
//                    int spanxpos = xpos - orientation.width;
//                    if (spanxpos < 0) {
//                        spanxpos = 0;
//                    }
//                    fillDummy(spanxpos, ypos, orientation.width, 1);
//                    this.grid[ypos][spanxpos] = CellFieldBuilderAdapter.labelBuilder(
//                            attribute,
//                            orientation);
//                    break;
//                }
                case TOP: {
                    fillDummy(xpos, ypos - 1, orientation.width, 1);
                    this.grid[ypos - 1][xpos] = CellFieldBuilderAdapter.labelBuilder(
                            attribute,
                            orientation);
                    break;
                }
                default: {
                }
            }
        } catch (final ArrayIndexOutOfBoundsException e) {
            log.error("Failed to set title as configured in: {} for attribute: {}", orientation, attribute, e);
        }

        return this;
    }

    void compose() {
        if (log.isDebugEnabled()) {
            log.debug("Compose grid view: \n" + gridToString());
        }

        // balance grid (optimize span and grab empty spaces for labels where applicable)
        for (int y = 0; y < this.grid.length; y++) {
            for (int x = 0; x < this.grid[y].length; x++) {
                if (this.grid[y][x] != null) {
                    this.grid[y][x].balanceGrid(this.grid, x, y);
                }
            }
        }

        for (int y = 0; y < this.grid.length; y++) {
            for (int x = 0; x < this.grid[y].length; x++) {
                if (this.grid[y][x] == null) {
                    final Label empty = new Label(this.parent, SWT.LEFT);
                    final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
                    gridData.verticalIndent = 8;
                    empty.setLayoutData(gridData);
                    empty.setText("" /* "empty " + x + " " + y */);
                } else {
                    this.grid[y][x].createCell(this);
                }
            }
        }
    }

    private void fillDummy(final int x, final int y, final int width, final int height) {
        final int upperBoundX = x + width;
        final int upperBoundY = y + height;
        for (int _y = y; _y < upperBoundY; _y++) {
            for (int _x = x; _x < upperBoundX; _x++) {
                this.grid[_y][_x] = CellFieldBuilderAdapter.dummyBuilderAdapter();
            }
        }
    }

    private String gridToString() {
        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < this.grid.length; i++) {
            if (sb.length() > 0) {
                sb.append(",\n");
            }
            sb.append(Arrays.toString(this.grid[i]));
        }
        return sb.toString();
    }

}
