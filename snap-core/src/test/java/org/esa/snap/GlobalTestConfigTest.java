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
package org.esa.snap;

import org.esa.snap.core.util.SystemUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Properties;

import static org.junit.Assert.*;

public class GlobalTestConfigTest {

    private Properties _propertys;

    @Before
    public void setUp() throws Exception {
        _propertys = System.getProperties();
    }

    @After
    public void tearDown() throws Exception {
        System.setProperties(_propertys);
    }

    @Test
    public void testGetEnvisatTestDataDir() {
        File file = null;
        try {
            file = GlobalTestConfig.getSnapTestDataInputDirectory();
        } catch (SecurityException e) {
            fail("SecurityException not expected");
        }
        assertNotNull(file);

        System.setProperty(GlobalTestConfig.SNAP_TEST_DATA_INPUT_DIR_PROPERTY_NAME,
                SystemUtils.convertToLocalPath("C:/envi/test/data/"));
        try {
            file = GlobalTestConfig.getSnapTestDataInputDirectory();
        } catch (SecurityException e) {
            fail("SecurityException not expected");
        }
        assertEquals(new File(SystemUtils.convertToLocalPath("C:/envi/test/data/")), file);

        System.getProperties().remove(GlobalTestConfig.SNAP_TEST_DATA_INPUT_DIR_PROPERTY_NAME);
        try {
            file = GlobalTestConfig.getSnapTestDataInputDirectory();
        } catch (SecurityException e) {
            fail("SecurityException not expected");
        }
        final File defaultFile = new File(SystemUtils.getApplicationHomeDir(),
                SystemUtils.convertToLocalPath(
                        GlobalTestConfig.SNAP_TEST_DATA_INPUT_DIR_DEFAULT_PATH));
        assertEquals(defaultFile, file);
    }
}
