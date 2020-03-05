/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import java.util.Collection;
import java.util.NoSuchElementException;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.examconfig.InputFieldBuilder;

@Lazy
@Service
@GuiProfile
public class InputFieldBuilderSupplier {

    private final Collection<InputFieldBuilder> inputFieldBuilder;

    protected InputFieldBuilderSupplier(final Collection<InputFieldBuilder> inputFieldBuilder) {
        this.inputFieldBuilder = inputFieldBuilder;
        inputFieldBuilder
                .forEach(builder -> builder.init(this));
    }

    public InputFieldBuilder getInputFieldBuilder(
            final ConfigurationAttribute attribute,
            final Orientation orientation) {

        return this.inputFieldBuilder
                .stream()
                .filter(b -> b.builderFor(attribute, orientation))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No InputFieldBuilder found for : " + attribute.type));
    }

}
