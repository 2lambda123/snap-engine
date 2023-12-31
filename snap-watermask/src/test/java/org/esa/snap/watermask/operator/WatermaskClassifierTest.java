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

package org.esa.snap.watermask.operator;

import org.junit.Before;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class WatermaskClassifierTest {

    private WatermaskClassifier gcClassifier;

    @Before
    public void setUp() throws Exception {
        gcClassifier = new WatermaskClassifier(1000);
    }

    @Test
    public void testGetWatermaskSampleAboveSixtyGC() {
        assertEquals(WatermaskClassifier.WATER_VALUE, gcClassifier.getWaterMaskSample(70.860277f, 29.205115f));
        assertEquals(WatermaskClassifier.WATER_VALUE, gcClassifier.getWaterMaskSample(70.853971f, 29.210610f));

        assertEquals(WatermaskClassifier.LAND_VALUE, gcClassifier.getWaterMaskSample(72.791664f, 105.28333f));
        assertEquals(WatermaskClassifier.LAND_VALUE, gcClassifier.getWaterMaskSample(72.794586f, 105.27786f));

        assertEquals(WatermaskClassifier.WATER_VALUE, gcClassifier.getWaterMaskSample(80.19444f, 25.963888f));
        assertEquals(WatermaskClassifier.LAND_VALUE, gcClassifier.getWaterMaskSample(80.14856f, 25.95601f));

        assertEquals(WatermaskClassifier.WATER_VALUE, gcClassifier.getWaterMaskSample(80.18703f, 26.04707f));
        assertEquals(WatermaskClassifier.LAND_VALUE, gcClassifier.getWaterMaskSample(80.176834f, 26.054949f));
    }

    @Test
    public void testIsWaterCenter() throws Exception {

        assertFalse(gcClassifier.isWater(22.663505f, 113.904132f));
        assertTrue(gcClassifier.isWater(22.544217f, 113.731145f));

        assertFalse(gcClassifier.isWater(49.68f, 0.581f));
        assertTrue(gcClassifier.isWater(49.434505f, 0.156014f));

        assertTrue(gcClassifier.isWater(49.33615f, -0.0096f));
        assertFalse(gcClassifier.isWater(49.310228f, 0.017876f));

        assertFalse(gcClassifier.isWater(46.5f, 0.5f));

        assertTrue(gcClassifier.isWater(5.01f, 0.01f));
        assertTrue(gcClassifier.isWater(5.95f, 0.93f));
        assertTrue(gcClassifier.isWater(5.04f, 0.95f));

        assertTrue(gcClassifier.isWater(5.5f, 0.5f));
        assertFalse(gcClassifier.isWater(5.88f, 0.24f));

        assertTrue(gcClassifier.isWater(43.322360f, 4.157f));
        assertTrue(gcClassifier.isWater(  43.468057f,     3.888443f));

        assertFalse(gcClassifier.isWater(46.003362f, -84.481173f));
        assertTrue(gcClassifier.isWater(45.967423f, -84.477179f));

        assertTrue(gcClassifier.isWater(53.5f, 5.92f));
        assertFalse(gcClassifier.isWater(53.458760f, 5.801733f));

        assertTrue(gcClassifier.isWater(-4.347463f, 11.443256f));
        assertFalse(gcClassifier.isWater(-4.2652f, 11.49324f));
    }

    @Test
    public void testImgFileCreation() {
        // north-west

        assertEquals("w002n51.img", WatermaskUtils.createImgFileName(51.007f, -1.30f));
        assertNotEquals("w001n51.img", WatermaskUtils.createImgFileName(51.007f, -1.30f));

        assertEquals("w002n48.img", WatermaskUtils.createImgFileName(48.007f, -1.83f));
        assertNotEquals("w001n48.img", WatermaskUtils.createImgFileName(48.007f, -1.83f));

        // north-east

        assertEquals("e000n51.img", WatermaskUtils.createImgFileName(51.007f, 0.30f));
        assertNotEquals("e001n51.img", WatermaskUtils.createImgFileName(51.007f, 0.30f));

        assertEquals("e000n49.img", WatermaskUtils.createImgFileName(49.993961334228516f, 0.006230226717889309f));
        assertNotEquals("w001n49.img", WatermaskUtils.createImgFileName(51.007f, 0.30f));

        assertEquals("e001n51.img", WatermaskUtils.createImgFileName(51.007f, 1.30f));
        assertNotEquals("e000n51.img", WatermaskUtils.createImgFileName(51.007f, 1.30f));

        assertEquals("e000n45.img", WatermaskUtils.createImgFileName(45.001f, 0.005f));
        assertNotEquals("w000n45.img", WatermaskUtils.createImgFileName(45.001f, 0.005f));

        assertEquals("e111n30.img", WatermaskUtils.createImgFileName(30.27f, 111.581f));
        assertNotEquals("e111n30.img", WatermaskUtils.createImgFileName(29.01f, 112.01f));

        // south-west

        assertEquals("w001s01.img", WatermaskUtils.createImgFileName(-0.01f, -0.30f));
        assertNotEquals("w000s01.img", WatermaskUtils.createImgFileName(-0.01f, -0.30f));

        assertEquals("w002s02.img", WatermaskUtils.createImgFileName(-1.01f, -1.30f));
        assertNotEquals("w001s01.img", WatermaskUtils.createImgFileName(-1.01f, -1.30f));

        // south-east

        assertEquals("e000s01.img", WatermaskUtils.createImgFileName(-0.01f, 0.30f));
        assertNotEquals("e000s00.img", WatermaskUtils.createImgFileName(-0.01f, 0.30f));

        assertEquals("e001s01.img", WatermaskUtils.createImgFileName(-0.01f, 1.30f));
        assertNotEquals("e001s00.img", WatermaskUtils.createImgFileName(-0.01f, 1.30f));
    }

    @Test
    public void testGetResource() {
        URL resource = getClass().getResource("image.properties");
        assertNotNull(resource);
    }
}