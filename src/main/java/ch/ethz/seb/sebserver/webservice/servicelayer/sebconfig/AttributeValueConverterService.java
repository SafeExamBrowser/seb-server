/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;

/** Interface of a SEB Exam Configuration XML conversion service */
public interface AttributeValueConverterService {

    /** Use this to get a AttributeValueConverter for a given ConfigurationAttribute.
     *
     * @param attribute The ConfigurationAttribute instance
     * @return a AttributeValueConverter for a given ConfigurationAttribute */
    AttributeValueConverter getAttributeValueConverter(ConfigurationAttribute attribute);

    /** Use this to get a AttributeValueConverter for a given AttributeType.
     *
     * @param attributeType The ConfigurationAttribute type
     * @return a AttributeValueConverter for a given AttributeType */
    AttributeValueConverter getAttributeValueConverter(final AttributeType attributeType);

    /** Get Use this to get a AttributeValueConverter for a given ConfigurationAttribute.
     *
     * @param attributeName the name of the attribute
     * @return a AttributeValueConverter for a given ConfigurationAttribute */
    AttributeValueConverter getAttributeValueConverter(String attributeName);

}
