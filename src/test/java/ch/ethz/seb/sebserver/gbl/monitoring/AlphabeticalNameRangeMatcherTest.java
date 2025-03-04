/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.monitoring;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AlphabeticalNameRangeMatcherTest {

    @Test
    public void test1() {
        final AlphabeticalNameRangeMatcher alphabeticalNameRangeMatcher = new AlphabeticalNameRangeMatcher();
        
        final String start1 = "A";
        final String end1 = "M";
        
        assertTrue(alphabeticalNameRangeMatcher.isInRange("A", start1, end1));
        assertTrue(alphabeticalNameRangeMatcher.isInRange("a", start1, end1));
        assertTrue(alphabeticalNameRangeMatcher.isInRange("aa", start1, end1));
        assertTrue(alphabeticalNameRangeMatcher.isInRange("Aa", start1, end1));

        assertTrue(alphabeticalNameRangeMatcher.isInRange("M", start1, end1));
        assertTrue(alphabeticalNameRangeMatcher.isInRange("m", start1, end1));
        assertTrue(alphabeticalNameRangeMatcher.isInRange("mm", start1, end1));
        assertTrue(alphabeticalNameRangeMatcher.isInRange("Mm", start1, end1));
        assertTrue(alphabeticalNameRangeMatcher.isInRange("Mz", start1, end1));

        assertFalse(alphabeticalNameRangeMatcher.isInRange("N", start1, end1));
        assertFalse(alphabeticalNameRangeMatcher.isInRange("n", start1, end1));
        assertFalse(alphabeticalNameRangeMatcher.isInRange("nn", start1, end1));
        assertFalse(alphabeticalNameRangeMatcher.isInRange("NM", start1, end1));
        
    }

    @Test
    public void test2() {
        final AlphabeticalNameRangeMatcher alphabeticalNameRangeMatcher = new AlphabeticalNameRangeMatcher();

        final String start1 = "Andi";
        final String end1 = "Marcel";
        
        // everything that do not start with the exact given start word do  not fit in the range
        assertFalse(alphabeticalNameRangeMatcher.isInRange("A", start1, end1));
        assertFalse(alphabeticalNameRangeMatcher.isInRange("a", start1, end1));
        assertFalse(alphabeticalNameRangeMatcher.isInRange("aa", start1, end1));
        assertFalse(alphabeticalNameRangeMatcher.isInRange("Aa", start1, end1));
        assertFalse(alphabeticalNameRangeMatcher.isInRange("An", start1, end1));
        assertFalse(alphabeticalNameRangeMatcher.isInRange("And", start1, end1));
        
        // everything that starts with the exact given start word fits in the range no mather what follows
        assertTrue(alphabeticalNameRangeMatcher.isInRange("Andi", start1, end1));
        assertTrue(alphabeticalNameRangeMatcher.isInRange("Andia", start1, end1));
        assertTrue(alphabeticalNameRangeMatcher.isInRange("Andi*", start1, end1));
        assertTrue(alphabeticalNameRangeMatcher.isInRange("AndiA", start1, end1));
        assertTrue(alphabeticalNameRangeMatcher.isInRange("Andi Soundso", start1, end1));
        
        // everything that is below the exact given end word fits in the range
        assertTrue(alphabeticalNameRangeMatcher.isInRange("M", start1, end1));
        assertTrue(alphabeticalNameRangeMatcher.isInRange("m", start1, end1));
        assertTrue(alphabeticalNameRangeMatcher.isInRange("ma", start1, end1));
        assertTrue(alphabeticalNameRangeMatcher.isInRange("Ma", start1, end1));
        assertTrue(alphabeticalNameRangeMatcher.isInRange("Mar", start1, end1));
        // NOTE: space is lower than "l" so this fits into range too
        assertTrue(alphabeticalNameRangeMatcher.isInRange("Marce ", start1, end1));
        
        // the exact given end word fits in the range
        assertTrue(alphabeticalNameRangeMatcher.isInRange("Marcel", start1, end1));
        // everything that starts with the exact given word fits in the range no mather what follows
        assertTrue(alphabeticalNameRangeMatcher.isInRange("Marcela", start1, end1));
        assertTrue(alphabeticalNameRangeMatcher.isInRange("Marcel ", start1, end1));
        assertTrue(alphabeticalNameRangeMatcher.isInRange("Marcel*", start1, end1));
        
        // everything that is higher than the exact given end word is not in range
        // NOTE: this is the first name that fits not in rage
        assertFalse(alphabeticalNameRangeMatcher.isInRange("Marcem", start1, end1));
        assertFalse(alphabeticalNameRangeMatcher.isInRange("mm", start1, end1));
        assertFalse(alphabeticalNameRangeMatcher.isInRange("Mm", start1, end1));
        assertFalse(alphabeticalNameRangeMatcher.isInRange("Mz", start1, end1));
        assertFalse(alphabeticalNameRangeMatcher.isInRange("N", start1, end1));
        assertFalse(alphabeticalNameRangeMatcher.isInRange("n", start1, end1));
        assertFalse(alphabeticalNameRangeMatcher.isInRange("nn", start1, end1));
        assertFalse(alphabeticalNameRangeMatcher.isInRange("NM", start1, end1));

        assertFalse(alphabeticalNameRangeMatcher.isInRange("Mbrcel", start1, end1));
    }

    @Test
    public void test3() {
        final AlphabeticalNameRangeMatcher alphabeticalNameRangeMatcher = new AlphabeticalNameRangeMatcher();

        final String start1 = "Maria";
        final String end1 = "Zorro";

        assertTrue(alphabeticalNameRangeMatcher.isInRange("Zorro", start1, end1));
        assertFalse(alphabeticalNameRangeMatcher.isInRange("Zurro", start1, end1));
        assertFalse(alphabeticalNameRangeMatcher.isInRange("Zzzz", start1, end1));
    }
}


