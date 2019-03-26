/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.widget;

import static org.junit.Assert.assertEquals;

import org.eclipse.swt.graphics.RGB;
import org.junit.Test;

public class ColorSelectionTest {

    @Test
    public void testParseRGB() {
        String colorString = "FFFFFF";
        assertEquals(
                "RGB {255, 255, 255}",
                ColorSelection.parseRGB(colorString).toString());

        colorString = "FFaa34";
        assertEquals(
                "RGB {255, 170, 52}",
                ColorSelection.parseRGB(colorString).toString());
    }

    @Test
    public void testParseColorString() {
        final RGB color = new RGB(255, 255, 255);
        assertEquals("ffffff", ColorSelection.parseColorString(color));

    }

}
