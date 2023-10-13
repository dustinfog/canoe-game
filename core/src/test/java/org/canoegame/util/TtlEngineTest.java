package org.canoegame.util;

import junit.framework.TestCase;

public class TtlEngineTest extends TestCase {

    public void testAdd() throws InterruptedException {
        var engine = new TtlEngine<Integer>(1000, null);
        var node1 = engine.add(1);

        Thread.sleep(500);
        var node2 = engine.add(2);
        engine.add(2);
        assertFalse(node1.isExpired());

        Thread.sleep(500);
        engine.expire();
        engine.touch(node2);

        Thread.sleep(500);
        engine.expire();
        assertTrue(node1.isExpired());
        assertFalse(node2.isExpired());
        Thread.sleep(500);
        engine.expire();
        assertTrue(node2.isExpired());
    }
}