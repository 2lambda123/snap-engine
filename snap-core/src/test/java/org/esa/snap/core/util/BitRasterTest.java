/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class BitRasterTest {

    @Test
    public void testAccessors() {
        final int width = 13;
        final int height = 5;
        final int size = width * height;

        BitRaster bitRaster = new BitRaster(width, height);

        assertEquals(width, bitRaster.getWidth());
        assertEquals(height, bitRaster.getHeight());

        for (int i = 0; i < size; i++) {
            assertFalse("i=" + i, bitRaster.isSet(i));
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bitRaster.set(x, y, true);
            }
        }

        for (int i = 0; i < size; i++) {
            assertTrue("i=" + i, bitRaster.isSet(i));
        }

        for (int i = 0; i < size; i++) {
            bitRaster.set(i, false);
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                assertFalse("x=" + x + ",y=" + y, bitRaster.isSet(x, y));
            }
        }

        testAccessors(bitRaster, 12, 4);
        testAccessors(bitRaster, 0, 0);
        testAccessors(bitRaster, 2, 1);
        testAccessors(bitRaster, 8, 2);
    }

    private void testAccessors(BitRaster bitRaster, int x, int y) {
        assertFalse(bitRaster.isSet(x, y));
        assertFalse(bitRaster.isSet(x + y * bitRaster.getWidth()));
        bitRaster.set(x, y, true);
        assertTrue(bitRaster.isSet(x, y));
        assertTrue(bitRaster.isSet(x + y * bitRaster.getWidth()));
        bitRaster.set(x, y, false);
        assertFalse(bitRaster.isSet(x, y));
        assertFalse(bitRaster.isSet(x + y * bitRaster.getWidth()));
    }
}
