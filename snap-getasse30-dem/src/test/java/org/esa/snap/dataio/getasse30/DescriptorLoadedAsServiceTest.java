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
package org.esa.snap.dataio.getasse30;

import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class DescriptorLoadedAsServiceTest {

    @Test
    public void testDescriptorIsLoaded() {
        ElevationModelRegistry registry = ElevationModelRegistry.getInstance();
        ElevationModelDescriptor descriptor = registry.getDescriptor("GETASSE30");

        assertNotNull(descriptor);
    }
}
