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

package org.esa.beam.dataio.rtp;

import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;

import static org.esa.beam.dataio.rtp.RawTiledPyramidsProductCodecSpi.FORMAT_DESCRIPTION;
import static org.esa.beam.dataio.rtp.RawTiledPyramidsProductCodecSpi.FORMAT_NAME;
import static org.esa.beam.dataio.rtp.RawTiledPyramidsProductCodecSpi.HEADER_NAME;
import static org.esa.beam.dataio.rtp.RawTiledPyramidsProductCodecSpi.NO_FILE_EXTENSIONS;
import static org.esa.beam.dataio.rtp.RawTiledPyramidsProductCodecSpi.isProductDir;


class RawTiledPyramidsProductFileFilter extends BeamFileFilter {
    public RawTiledPyramidsProductFileFilter() {
        super(FORMAT_NAME, NO_FILE_EXTENSIONS, FORMAT_DESCRIPTION);
    }

    @Override
    public boolean accept(File file) {
        if (isProductDir(file.getParentFile())) {
            return file.getName().equals(HEADER_NAME);
        }
        return file.isDirectory();
    }

    @Override
    public boolean isCompoundDocument(File dir) {
        return isProductDir(dir);
    }

    @Override
    public FileSelectionMode getFileSelectionMode() {
        return FileSelectionMode.FILES_AND_DIRECTORIES;
    }
}
