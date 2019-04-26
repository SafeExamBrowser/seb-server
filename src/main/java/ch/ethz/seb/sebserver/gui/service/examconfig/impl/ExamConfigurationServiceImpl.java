/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import java.util.Collection;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValue;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.examconfig.ExamConfigurationService;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputFieldBuilder;
import ch.ethz.seb.sebserver.gui.service.examconfig.ValueChangeListener;
import ch.ethz.seb.sebserver.gui.service.examconfig.ValueChangeRule;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestService;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetConfigAttributes;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetOrientations;

@Lazy
@Service
@GuiProfile
public class ExamConfigurationServiceImpl implements ExamConfigurationService, ValueChangeListener {

    private final RestService restService;
    private final JSONMapper jsonMapper;

    private final Collection<InputFieldBuilder> inputFieldBuilderMapping;
    private final Collection<ValueChangeRule> valueChangeRules;

    protected ExamConfigurationServiceImpl(
            final RestService restService,
            final JSONMapper jsonMapper,
            final Collection<InputFieldBuilder> inputFieldBuilder,
            final Collection<ValueChangeRule> valueChangeRules) {

        this.restService = restService;
        this.jsonMapper = jsonMapper;
        this.inputFieldBuilderMapping = Utils.immutableCollectionOf(inputFieldBuilder);
        this.valueChangeRules = Utils.immutableCollectionOf(valueChangeRules);
    }

    @Override
    public AttributeMapping getAttributes(final String template) {
        final List<ConfigurationAttribute> attributes = this.restService
                .getBuilder(GetConfigAttributes.class)
                .call()
                .getOrThrow();

        final List<Orientation> orientations = this.restService
                .getBuilder(GetOrientations.class)
                .withQueryParam(Domain.ORIENTATION.ATTR_TEMPLATE_ID, template)
                .call()
                .getOrThrow();

        return new AttributeMapping(template, attributes, orientations);
    }

    @Override
    public ViewContext createViewContext(
            final AttributeMapping attributeMapping,
            final Composite parent,
            final String name,
            final String configurationId,
            final int columns,
            final int rows) {

        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ViewContext initInputFieldValues(final ViewContext viewContext) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void valueChanged(
            final ViewContext context,
            final ConfigurationAttribute attribute,
            final String value,
            final int listIndex) {

        // TODO Auto-generated method stub

    }

    @Override
    public void tableChanged(final ConfigurationTableValue tableValue) {
        // TODO Auto-generated method stub

    }

}
