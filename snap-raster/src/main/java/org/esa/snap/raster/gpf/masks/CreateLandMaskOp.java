/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.raster.gpf.masks;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.dem.dataio.DEMFactory;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.TileGeoreferencing;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The CreateLandMask operator.
 */
@OperatorMetadata(alias = "Land-Sea-Mask",
        category = "Raster/Masks",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2015 by Array Systems Computing Inc.",
        description = "Creates a bitmask defining land vs ocean.")
public class CreateLandMaskOp extends Operator {

    @SourceProduct(alias = "source", canBeMultisize = false)
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct = null;

    @Parameter(description = "The list of source bands.", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames = null;

    @Parameter(label = "Mask the Land", defaultValue = "true")
    private Boolean landMask = true;

    @Parameter(label = "Use SRTM 3sec", defaultValue = "true")
    private Boolean useSRTM = true;

    @Parameter(label = "Vector", defaultValue = "")
    private String geometry = "";

    @Parameter(label = "Invert Vector", defaultValue = "false")
    private Boolean invertGeometry = false;

    @Parameter(label = "Bypass", defaultValue = "false")
    private Boolean byPass = false;

    private ElevationModel dem = null;
    private final static int landThreshold = -10;
    private final static int seaThreshold = -10;

    private final static String tmpVirtBandName = "_tmpVirtBand";

    @Override
    public void initialize() throws OperatorException {
        try {

            targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                    sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

            ProductUtils.copyProductNodes(sourceProduct, targetProduct);

            addSelectedBands();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Add the user selected bands to target product.
     *
     * @throws OperatorException The exceptions.
     */
    private void addSelectedBands() throws OperatorException {

        final Band[] sourceBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames, false);
        for (Band srcBand : sourceBands) {

            if (geometry != null && !geometry.isEmpty() && !byPass) {
                String expression = geometry + " ? " + srcBand.getName() + " : " + srcBand.getNoDataValue();
                if (invertGeometry) {
                    expression = "!" + expression;
                }
                final VirtualBand virtBand = new VirtualBand(srcBand.getName() + tmpVirtBandName,
                        srcBand.getDataType(),
                        srcBand.getRasterWidth(), srcBand.getRasterHeight(),
                        expression);
                virtBand.setUnit(srcBand.getUnit());
                virtBand.setDescription(srcBand.getDescription());
                sourceProduct.addBand(virtBand);

                final Band targetBand = ProductUtils.copyBand(virtBand.getName(), sourceProduct, targetProduct, false);
                targetBand.setName(srcBand.getName());
                targetBand.setSourceImage(virtBand.getSourceImage());
            } else {
                final Band targetBand = ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct, false);
                if (byPass) {
                    targetBand.setSourceImage(srcBand.getSourceImage());
                }
            }
        }
    }

    public void dispose() {
        if (geometry != null && !geometry.isEmpty() && !byPass) {
            final Band[] sourceBands = sourceProduct.getBands();
            for (Band srcBand : sourceBands) {
                if (srcBand.getName().contains(tmpVirtBandName)) {
                    sourceProduct.removeBand(srcBand);
                }
            }
        }
    }

    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed (same for all rasters in <code>targetRasters</code>).
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        try {
            if (dem == null) {
                createDEM();
            }

            final TileData[] trgTiles = getTargetTiles(targetTiles, targetRectangle, sourceProduct);

            final int minX = targetRectangle.x;
            final int minY = targetRectangle.y;
            final int maxX = targetRectangle.x + targetRectangle.width;
            final int maxY = targetRectangle.y + targetRectangle.height;
            boolean valid;

            final TileIndex srcTileIndex = new TileIndex(trgTiles[0].srcTile);
            final TileIndex trgTileIndex = new TileIndex(trgTiles[0].targetTile);
            final TileGeoreferencing tileGeoRef = new TileGeoreferencing(targetProduct, minX, minY, maxX - minX, maxY - minY);

            final float demNoDataValue = dem.getDescriptor().getNoDataValue();
            final double[][] localDEM = new double[maxY - minY + 2][maxX - minX + 2];
            DEMFactory.getLocalDEM(
                    dem, demNoDataValue, null, tileGeoRef, minX, minY, maxX - minX, maxY - minY, null, true, localDEM);

            for (int y = minY; y < maxY; ++y) {
                srcTileIndex.calculateStride(y);
                trgTileIndex.calculateStride(y);
                final int yy = y - minY;
                for (int x = minX; x < maxX; ++x) {
                    final int trgIndex = trgTileIndex.getIndex(x);
                    final double elev = localDEM[yy][x - minX];

                    if (landMask) {
                        if (useSRTM)
                            valid = elev == demNoDataValue;
                        else
                            valid = elev < seaThreshold;
                    } else {
                        if (useSRTM)
                            valid = elev != demNoDataValue;
                        else
                            valid = elev > landThreshold;
                    }

                    if (valid) {
                        final int srcIndex = srcTileIndex.getIndex(x);
                        for (TileData tileData : trgTiles) {
                            tileData.tileDataBuffer.setElemDoubleAt(trgIndex,
                                    tileData.srcDataBuffer.getElemDoubleAt(srcIndex));
                        }
                    } else {
                        for (TileData tileData : trgTiles) {
                            tileData.tileDataBuffer.setElemDoubleAt(trgIndex, tileData.noDataValue);
                        }
                    }
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private synchronized void createDEM() throws IOException {
        if (dem != null) return;

        if (useSRTM) {
            dem = DEMFactory.createElevationModel("SRTM 3Sec", ResamplingFactory.NEAREST_NEIGHBOUR_NAME);
        } else {
            dem = DEMFactory.createElevationModel("ACE2_5Min", ResamplingFactory.NEAREST_NEIGHBOUR_NAME);
        }
    }

    private TileData[] getTargetTiles(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle,
                                      final Product srcProduct) {
        final List<TileData> trgTileList = new ArrayList<>();
        final Set<Band> keySet = targetTiles.keySet();
        for (Band targetBand : keySet) {

            final TileData td = new TileData();
            td.targetTile = targetTiles.get(targetBand);
            td.srcTile = getSourceTile(srcProduct.getBand(targetBand.getName()), targetRectangle);
            td.tileDataBuffer = td.targetTile.getDataBuffer();
            td.srcDataBuffer = td.srcTile.getDataBuffer();
            td.noDataValue = targetBand.getNoDataValue();
            trgTileList.add(td);
        }
        return trgTileList.toArray(new TileData[trgTileList.size()]);
    }

    private static class TileData {
        Tile targetTile = null;
        Tile srcTile = null;
        ProductData tileDataBuffer = null;
        ProductData srcDataBuffer = null;
        double noDataValue = 0;
    }

    /**
     * Operator SPI.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(CreateLandMaskOp.class);
        }
    }
}
