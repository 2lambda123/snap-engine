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

package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import org.junit.Test;

public class CharacterConverterTest extends AbstractConverterTest {

    private CharacterConverter converter;

    @Override
    public Converter getConverter() {
        if (converter == null) {
            converter = new CharacterConverter();
        }
        return converter;
    }


    @Test
    public void testConverter() throws ConversionException {
        testValueType(Character.class);

        testParseSuccess('5', "5");
        testParseSuccess('B', "B");
        testParseSuccess(null, "");

        testParseFailed("Ballamann!");

        testFormatSuccess("B", 'B');
        testFormatSuccess("5", '5');
        testFormatSuccess("", null);

        assertNullCorrectlyHandled();
    }

}
