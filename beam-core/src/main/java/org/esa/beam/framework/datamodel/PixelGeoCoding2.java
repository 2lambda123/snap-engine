/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.framework.datamodel;

import com.bc.jexp.ParseException;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.math.DistanceCalculator;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.util.math.SinusoidalDistanceCalculator;

import javax.media.jai.PlanarImage;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.geom.Dimension2D;
import java.awt.image.Raster;

/**
 * This is an experimental pixel geo-coding, which solves some problems of {@link PixelGeoCoding},
 * but may bring-up others.
 *
 * @author Ralf Quast
 */
class PixelGeoCoding2 extends AbstractGeoCoding implements BasicPixelGeoCoding {

    private static final String SYSPROP_PIXEL_GEO_CODING_FRACTION_ACCURACY = "beam.pixelGeoCoding.fractionAccuracy";

    private final Band latBand;
    private final Band lonBand;
    private final String maskExpression;

    private final int rasterW;
    private final int rasterH;
    private final boolean fractionAccuracy = Boolean.getBoolean(SYSPROP_PIXEL_GEO_CODING_FRACTION_ACCURACY);
    private final double pixelDiagonalSquared;

    private transient PixelPosEstimator pixelPosEstimator;
    private transient final PixelFinder pixelFinder;

    private transient PlanarImage lonImage;
    private transient PlanarImage latImage;
    private transient PlanarImage maskImage;

    public interface PixelFinder {

        void findPixelPos(GeoPos geoPos, PixelPos pixelPos);
    }

    /**
     * Constructs a new pixel-based geo-coding.
     * <p/>
     * <i>Use with care: In contrast to the other constructor this one loads the data not until first access to
     * {@link #getPixelPos(org.esa.beam.framework.datamodel.GeoPos, org.esa.beam.framework.datamodel.PixelPos)} or {@link #getGeoPos(org.esa.beam.framework.datamodel.PixelPos, org.esa.beam.framework.datamodel.GeoPos)}. </i>
     *
     * @param latBand        the band providing the latitudes
     * @param lonBand        the band providing the longitudes
     * @param maskExpression the expression defining a valid-pixel mask, may be {@code null}
     */
    public PixelGeoCoding2(final Band latBand, final Band lonBand, String maskExpression) {
        Guardian.assertNotNull("latBand", latBand);
        Guardian.assertNotNull("lonBand", lonBand);
        final Product product = latBand.getProduct();

        if (product == null) {
            throw new IllegalArgumentException("latBand.getProduct() == null");
        }
        if (lonBand.getProduct() == null) {
            throw new IllegalArgumentException("lonBand.getProduct() == null");
        }
        // Note that if two bands are of the same product, they also have the same raster size
        if (product != lonBand.getProduct()) {
            throw new IllegalArgumentException("latBand.getProduct() != lonBand.getProduct()");
        }
        if (product.getSceneRasterWidth() < 2 || product.getSceneRasterHeight() < 2) {
            throw new IllegalArgumentException(
                    "latBand.getProduct().getSceneRasterWidth() < 2 || latBand.getProduct().getSceneRasterHeight() < 2");
        }

        this.latBand = latBand;
        this.lonBand = lonBand;

        this.rasterW = latBand.getSceneRasterWidth();
        this.rasterH = latBand.getSceneRasterHeight();

        try {
            lonImage = (PlanarImage) lonBand.getGeophysicalImage().getImage(0);
        } catch (ClassCastException e) {
            lonImage = lonBand.getGeophysicalImage();
        }
        try {
            latImage = (PlanarImage) latBand.getGeophysicalImage().getImage(0);
        } catch (ClassCastException e) {
            latImage = latBand.getGeophysicalImage();
        }

        this.maskImage = null;
        if (maskExpression != null) {
            maskExpression = maskExpression.trim();
            if (maskExpression.length() > 0) {
                final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
                for (int i = 0; i < maskGroup.getNodeCount(); i++) {
                    final Mask mask = maskGroup.get(i);
                    if (mask.getImageType() == Mask.BandMathsType.INSTANCE) {
                        if (maskExpression.equals(Mask.BandMathsType.getExpression(mask))) {
                            maskImage = mask.getSourceImage();
                            break;
                        }
                    }
                }
                if (maskImage == null) {
                    maskImage = (PlanarImage) ImageManager.getInstance().getMaskImage(maskExpression,
                                                                                      lonBand.getProduct()).getImage(0);
                }
            } else {
                maskExpression = null;
                // TODO - ensure that tile layout of lat and lon images is used
                maskImage = ConstantDescriptor.create((float) lonImage.getWidth(),
                                                      (float) lonImage.getHeight(),
                                                      new Byte[]{1}, null);
            }
        } else {
            maskExpression = null;
                // TODO - ensure that tile layout of lat and lon images is used
            maskImage = ConstantDescriptor.create((float) lonImage.getWidth(),
                                                  (float) lonImage.getHeight(),
                                                  new Byte[]{1}, null);
        }
        this.maskExpression = maskExpression;

        final PixelDimensionEstimator pixelDimensionEstimator = new SimplePixelDimensionEstimator();
        final Dimension2D pixelDimension = pixelDimensionEstimator.getPixelDimension(lonImage,
                                                                                     latImage,
                                                                                     maskImage);
        final double pixelSizeX = pixelDimension.getWidth();
        final double pixelSizeY = pixelDimension.getHeight();
        pixelDiagonalSquared = pixelSizeX * pixelSizeX + pixelSizeY * pixelSizeY;

        pixelPosEstimator = new PixelPosEstimator(lonImage, latImage, maskImage, 0.5);
        pixelFinder = new DefaultPixelFinder(lonImage, latImage, maskImage);
    }

    @Override
    public Band getLatBand() {
        return latBand;
    }

    @Override
    public Band getLonBand() {
        return lonBand;
    }

    @Override
    public String getValidMask() {
        return maskExpression;
    }

    @Override
    public GeoCoding getPixelPosEstimator() {
        return null;
    }

    @Override
    public int getSearchRadius() {
        return 0;
    }

    /**
     * Returns <code>false</code> always.
     *
     * @return <code>false</code>
     */
    @Override
    public boolean isCrossingMeridianAt180() {
        return false;
    }

    /**
     * Checks whether or not this geo-coding can determine the pixel position from a geodetic position.
     *
     * @return <code>true</code>, if so
     */
    @Override
    public boolean canGetPixelPos() {
        return pixelPosEstimator.canGetPixelPos();
    }

    /**
     * Checks whether or not this geo-coding can determine the geodetic position from a pixel position.
     *
     * @return <code>true</code>, if so
     */
    @Override
    public boolean canGetGeoPos() {
        return true;
    }

    @Override
    public PixelPos getPixelPos(final GeoPos geoPos, PixelPos pixelPos) {
        if (pixelPos == null) {
            pixelPos = new PixelPos();
        }
        if (geoPos.isValid()) {
            pixelPosEstimator.getPixelPos(geoPos, pixelPos);

            if (pixelPos.isValid()) {
                pixelFinder.findPixelPos(geoPos, pixelPos);
            }
        } else {
            pixelPos.setInvalid();
        }
        return pixelPos;
    }

    /**
     * Returns the latitude and longitude value for a given pixel co-ordinate.
     *
     * @param pixelPos the pixel's co-ordinates given as x,y
     * @param geoPos   an instance of <code>GeoPos</code> to be used as retun value. If this parameter is
     *                 <code>null</code>, the method creates a new instance which it then returns.
     *
     * @return the geographical position as lat/lon.
     */
    @Override
    public GeoPos getGeoPos(final PixelPos pixelPos, GeoPos geoPos) {
        if (geoPos == null) {
            geoPos = new GeoPos();
        }
        geoPos.setInvalid();
        if (pixelPos.isValid() && pixelPosIsInsideRasterWH(pixelPos)) {
            int x0 = (int) Math.floor(pixelPos.getX());
            int y0 = (int) Math.floor(pixelPos.getY());
            final PlanarImage lonMaskImage = lonBand.getValidMaskImage();
            final PlanarImage latMaskImage = latBand.getValidMaskImage();
            final float lon0 = getSampleFloat(x0, y0, lonImage, lonMaskImage);
            final float lat0 = getSampleFloat(x0, y0, latImage, latMaskImage);

            if (lat0 >= -90.0f && lat0 <= 90.0f && lon0 >= -180.0f && lon0 <= 180.0f) {
                if (fractionAccuracy) {
                    if (x0 > 0 && pixelPos.x - x0 < 0.5f || x0 == rasterW - 1) {
                        x0 -= 1;
                    }
                    if (y0 > 0 && pixelPos.y - y0 < 0.5f || y0 == rasterH - 1) {
                        y0 -= 1;
                    }

                    final float wx = pixelPos.x - (x0 + 0.5f);
                    final float wy = pixelPos.y - (y0 + 0.5f);
                    final float lon = interpolate(x0, y0, wx, wy, lonImage, lonMaskImage, -180.0f, 180.0f, lon0);
                    final float lat = interpolate(x0, y0, wx, wy, latImage, latMaskImage, -90.0f, 90.0f, lat0);

                    geoPos.setLocation(lat, lon);
                } else {
                    geoPos.setLocation(lat0, lon0);
                }
            }
        }
        return geoPos;
    }

    private float interpolate(int x0, int y0, float wx, float wy, PlanarImage image, PlanarImage maskImage,
                              float min,
                              float max,
                              float defaultValue) {
        final int x1 = x0 + 1;
        final int y1 = y0 + 1;
        final float d00 = getSampleFloat(x0, y0, image, maskImage);
        if (d00 >= min && d00 <= max) {
            final float d10 = getSampleFloat(x1, y0, image, maskImage);
            if (d10 >= min && d10 <= max) {
                final float d01 = getSampleFloat(x0, y1, image, maskImage);
                if (d01 >= min && d01 <= max) {
                    final float d11 = getSampleFloat(x1, y1, image, maskImage);
                    if (d11 >= min && d11 <= max) {
                        return MathUtils.interpolate2D(wx, wy, d00, d10, d01, d11);
                    }
                }
            }
        }
        return defaultValue;
    }

    private boolean pixelPosIsInsideRasterWH(PixelPos pixelPos) {
        final float x = pixelPos.x;
        final float y = pixelPos.y;
        return x >= 0 && x < rasterW && y >= 0 && y < rasterH;
    }

    public int getRasterWidth() {
        return rasterW;
    }

    public int getRasterHeight() {
        return rasterH;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PixelGeoCoding2 that = (PixelGeoCoding2) o;

        if (!latBand.equals(that.latBand)) {
            return false;
        }
        if (!lonBand.equals(that.lonBand)) {
            return false;
        }
        if (maskExpression != null) {
            if (!maskExpression.equals(that.maskExpression)) {
                return false;
            }
        } else {
            if (that.maskExpression != null) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = latBand.hashCode();
        result = 31 * result + lonBand.hashCode();
        if (maskExpression != null) {
            result = 31 * result + maskExpression.hashCode();
        }
        return result;
    }

    @Override
    public synchronized void dispose() {
        pixelPosEstimator = null;
        lonImage = null;
        latImage = null;
    }

    private static float getSampleFloat(int pixelX, int pixelY, PlanarImage image, PlanarImage maskImage) {
        final int x = image.getMinX() + pixelX;
        final int y = image.getMinY() + pixelY;
        final int tileX = image.XToTileX(x);
        final int tileY = image.YToTileY(y);
        final Raster data = image.getTile(tileX, tileY);
        if (maskImage != null) {
            // tile coordinates of images and mask "shall" be equal, but they are not - TODO check why (mz 2013-11-08)
            final int maskTileX = maskImage.XToTileX(x);
            final int maskTileY = maskImage.YToTileY(y);
            final int maskValue = maskImage.getTile(maskTileX, maskTileY).getSample(x, y, 0);
            if (maskValue == 0) {
                return Float.NaN;
            }
        }
        return data.getSampleFloat(x, y, 0);
    }

    private static double getSampleDouble(int pixelX, int pixelY, PlanarImage image) {
        final int x = image.getMinX() + pixelX;
        final int y = image.getMinY() + pixelY;
        final int tileX = image.XToTileX(x);
        final int tileY = image.YToTileY(y);
        final Raster data = image.getTile(tileX, tileY);

        return data.getSampleDouble(x, y, 0);
    }

    private static int getSample(int pixelX, int pixelY, PlanarImage image) {
        final int x = image.getMinX() + pixelX;
        final int y = image.getMinY() + pixelY;
        final int tileX = image.XToTileX(x);
        final int tileY = image.YToTileY(y);
        final Raster data = image.getTile(tileX, tileY);

        return data.getSample(x, y, 0);
    }

    @Override
    public boolean transferGeoCoding(final Scene srcScene, final Scene destScene,
                                     final ProductSubsetDef subsetDef) {
        final Band srcLatBand = getLatBand();
        final Product destProduct = destScene.getProduct();
        Band latBand = destProduct.getBand(srcLatBand.getName());
        if (latBand == null) {
            latBand = GeoCodingFactory.createSubset(srcLatBand, destScene, subsetDef);
            destProduct.addBand(latBand);
        }
        final Band srcLonBand = getLonBand();
        Band lonBand = destProduct.getBand(srcLonBand.getName());
        if (lonBand == null) {
            lonBand = GeoCodingFactory.createSubset(srcLonBand, destScene, subsetDef);
            destProduct.addBand(lonBand);
        }
        String validMaskExpression = getValidMask();
        try {
            if (validMaskExpression != null) {
                GeoCodingFactory.copyReferencedRasters(validMaskExpression, srcScene, destScene, subsetDef);
            }
        } catch (ParseException ignored) {
            validMaskExpression = null;
        }
        destScene.setGeoCoding(new PixelGeoCoding2(latBand, lonBand, validMaskExpression));

        return true;
    }

    /**
     * Gets the datum, the reference point or surface against which {@link org.esa.beam.framework.datamodel.GeoPos} measurements are made.
     *
     * @return the datum
     */
    @Override
    public Datum getDatum() {
        return Datum.WGS_84;
    }

    private class DefaultPixelFinder implements PixelFinder {

        private final PlanarImage lonImage;
        private final PlanarImage latImage;
        private final PlanarImage maskImage;
        private final int maxSearchCycleCount = 30;
        private final int imageW;
        private final int imageH;

        private DefaultPixelFinder(PlanarImage lonImage, PlanarImage latImage, PlanarImage maskImage) {
            this.lonImage = lonImage;
            this.latImage = latImage;
            this.maskImage = maskImage;

            imageW = lonImage.getWidth();
            imageH = lonImage.getHeight();
        }

        @Override
        public void findPixelPos(GeoPos geoPos, PixelPos pixelPos) {
            final int searchRadius = 2 * maxSearchCycleCount;

            int x0 = (int) Math.floor(pixelPos.x);
            int y0 = (int) Math.floor(pixelPos.y);

            if (x0 + searchRadius >= 0 && x0 - searchRadius < imageW && y0 + searchRadius >= 0 && y0 - searchRadius < imageH) {
                if (x0 < 0) {
                    x0 = 0;
                } else if (x0 >= imageW) {
                    x0 = imageW - 1;
                }
                if (y0 < 0) {
                    y0 = 0;
                } else if (y0 >= imageH) {
                    y0 = imageH - 1;
                }

                int x1 = Math.max(x0 - searchRadius, 0);
                int y1 = Math.max(y0 - searchRadius, 0);
                int x2 = Math.min(x0 + searchRadius, imageW - 1);
                int y2 = Math.min(y0 + searchRadius, imageH - 1);

                final int rasterMinX = x1;
                final int rasterMinY = y1;
                @SuppressWarnings("UnnecessaryLocalVariable")
                final int rasterMaxX = x2;
                @SuppressWarnings("UnnecessaryLocalVariable")
                final int rasterMaxY = y2;

                final double lat0 = geoPos.lat;
                final double lon0 = geoPos.lon;
                final DistanceCalculator dc = new SinusoidalDistanceCalculator(lon0, lat0);

                double minDistance;
                if (getSample(x0, y0, maskImage) != 0) {
                    minDistance = dc.distance(getSampleDouble(x0, y0, lonImage), getSampleDouble(x0, y0, latImage));
                } else {
                    minDistance = Double.POSITIVE_INFINITY;
                }

                for (int i = 0; i < maxSearchCycleCount; i++) {
                    x1 = x0;
                    y1 = y0;

                    int minX = Math.max(x1 - 2, rasterMinX);
                    int minY = Math.max(y1 - 2, rasterMinY);
                    int maxX = Math.min(x1 + 2, rasterMaxX);
                    int maxY = Math.min(y1 + 2, rasterMaxY);

                    while (minX > rasterMinX) {
                        if (getSample(minX, y1, maskImage) != 0) {
                            break;
                        }
                        if (minX > rasterMinX) {
                            minX--;
                        }
                    }
                    while (maxX < rasterMaxX) {
                        if (getSample(maxX, y1, maskImage) != 0) {
                            break;
                        }
                        if (maxX < rasterMaxX) {
                            maxX++;
                        }
                    }

                    for (int y = minY; y <= maxY; y++) {
                        for (int x = minX; x <= maxX; x++) {
                            if (y != y0 || x != x0) {
                                if (getSample(x, y, maskImage) != 0) {
                                    final double lat = getSampleDouble(x, y, latImage);
                                    final double lon = getSampleDouble(x, y, lonImage);
                                    final double d = dc.distance(lon, lat);
                                    if (d < minDistance) {
                                        x1 = x;
                                        y1 = y;
                                        minDistance = d;
                                    }
                                }
                            }
                        }
                    }
                    if (x1 == x0 && y1 == y0) {
                        break;
                    }

                    x0 = x1;
                    y0 = y1;
                }
                if (minDistance < pixelDiagonalSquared) {
                    pixelPos.setLocation(x0 + 0.5f, y0 + 0.5f);
                } else {
                    pixelPos.setInvalid();
                }
            } else {
                pixelPos.setInvalid();
            }
        }
    }
}
