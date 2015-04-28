/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.dataio.dem.ace2_5min;

import com.bc.io.FileDownloader;
import com.bc.io.FileUnpacker;
import org.esa.snap.framework.datamodel.GeoPos;
import org.esa.snap.framework.dataop.dem.AbstractElevationModelDescriptor;
import org.esa.snap.framework.dataop.dem.ElevationModel;
import org.esa.snap.framework.dataop.maptransf.Datum;
import org.esa.snap.framework.dataop.resamp.Resampling;

import java.awt.*;
import java.io.File;
import java.net.URL;

public class ACE2_5MinElevationModelDescriptor extends AbstractElevationModelDescriptor {

    private static final String NAME = "ACE2_5Min";
    private static final String DB_FILE_SUFFIX = "_5M.ACE2";
    private static final String ARCHIVE_URL_PATH = "http://nest.s3.amazonaws.com/data/5M_HEIGHTS.zip";
    public static final int NUM_X_TILES = 24;
    public static final int NUM_Y_TILES = 12;
    public static final int DEGREE_RES = 15;
    public static final int PIXEL_RES = 180;
    public static final int NO_DATA_VALUE = -500;
    public static final GeoPos RASTER_ORIGIN = new GeoPos(90.0f, 180.0f);
    public static final int RASTER_WIDTH = NUM_X_TILES * PIXEL_RES;
    public static final int RASTER_HEIGHT = NUM_Y_TILES * PIXEL_RES;
    private static final Datum DATUM = Datum.WGS_84;

    public ACE2_5MinElevationModelDescriptor() {
    }

    public String getName() {
        return NAME;
    }

    public Datum getDatum() {
        return DATUM;
    }

    public int getNumXTiles() {
        return NUM_X_TILES;
    }

    public int getNumYTiles() {
        return NUM_Y_TILES;
    }

    public float getNoDataValue() {
        return NO_DATA_VALUE;
    }

    @Override
    public int getRasterWidth() {
        return RASTER_WIDTH;
    }

    @Override
    public int getRasterHeight() {
        return RASTER_HEIGHT;
    }

    @Override
    public GeoPos getRasterOrigin() {
        return RASTER_ORIGIN;
    }

    @Override
    public int getDegreeRes() {
        return DEGREE_RES;
    }

    @Override
    public int getPixelRes() {
        return PIXEL_RES;
    }

    public boolean isDemInstalled() {
        return true;
    }

    public URL getDemArchiveUrl() {
        return null;
    }

    public ElevationModel createDem(Resampling resamplingMethod) {
        if (!isDemInstalled()) {
            installDemFiles(null);
        }
        return new ACE2_5MinElevationModel(this, resamplingMethod);
    }

    public String createTileFilename(int minLat, int minLon) {
        String latString = minLat < 0 ? Math.abs(minLat) + "S" : minLat + "N";
        while (latString.length() < 3) {
            latString = '0' + latString;
        }
        String lonString = minLon < 0 ? Math.abs(minLon) + "W" : minLon + "E";
        while (lonString.length() < 4) {
            lonString = '0' + lonString;
        }
        return latString + lonString + DB_FILE_SUFFIX;
    }

    @Override
    public synchronized boolean installDemFiles(Object uiComponent) {
        if (isDemInstalled()) {
            return true;
        }
        if (isInstallingDem()) {
            return true;
        }
        final Component parent = uiComponent instanceof Component ? (Component) uiComponent : null;

        final File demInstallDir = getDemInstallDir();
        if (!demInstallDir.exists()) {
            final boolean success = demInstallDir.mkdirs();
            if (!success) {
                return false;
            }
        }

        try {
            final File archiveFile = FileDownloader.downloadFile(getDemArchiveUrl(), demInstallDir, parent);
            FileUnpacker.unpackZip(archiveFile, demInstallDir, parent);
            archiveFile.delete();
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
