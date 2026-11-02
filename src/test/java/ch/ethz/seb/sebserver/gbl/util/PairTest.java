package ch.ethz.seb.sebserver.gbl.util;

import org.junit.Test;

import java.util.Objects;

public class PairTest {

    @Test
    public void testHash() {
        Pair<Long, Integer> p1 = new Pair<>(1L, 2);
        Pair<Long, Integer> p2 = new Pair<>(2L, 1);

        System.out.println("******* p1: " + p1.hashCode());
        System.out.println("******* p2: " + p2.hashCode());

    }
}
