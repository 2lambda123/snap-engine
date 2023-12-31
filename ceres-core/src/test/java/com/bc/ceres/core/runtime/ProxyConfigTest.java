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

package com.bc.ceres.core.runtime;

import org.junit.Test;

import static org.junit.Assert.*;

public class ProxyConfigTest {

    @Test
    public void testDefaultValues() {
        ProxyConfig proxyConfig = new ProxyConfig();
        assertEquals("", proxyConfig.getHost());
        assertEquals(0, proxyConfig.getPort());
        assertFalse(proxyConfig.isAuthorizationUsed());
        assertEquals("", proxyConfig.getUsername());
        assertNotNull(proxyConfig.getPassword());
        assertEquals(0, proxyConfig.getPassword().length);
    }

    @Test
    public void testCryptDecrypt() {
        String s = ProxyConfig.scramble("An4nas?");
        assertNotNull(s);
        assertNotEquals("An4nas?", s);
        assertFalse(s.contains("An4nas?"));

        String t = ProxyConfig.descramble(s);
        assertEquals("An4nas?", t);
    }
}
