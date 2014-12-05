package org.esa.beam.dataio;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Random;

/**
 * @author Marco Peters
 */
class ExpectedGeoCoding {

    @JsonProperty()
    private ExpectedGeoCoordinate[] coordinates;
    @JsonProperty()
    private Double reverseAccuracy;


    ExpectedGeoCoding() {
        reverseAccuracy = -1.0;
    }

    ExpectedGeoCoding(Product product, Random random) {
        this();
        final ArrayList<Point2D> pointList = ExpectedPixel.createPointList(product, random);
        final GeoCoding geoCoding = product.getGeoCoding();
        coordinates = new ExpectedGeoCoordinate[pointList.size()];
        for (int i = 0; i < pointList.size(); i++) {
            Point2D point = pointList.get(i);
            final float x = (float) point.getX();
            final float y = (float) point.getY();
            final GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(x, y), null);
            final PixelPos pixelPos = geoCoding.getPixelPos(geoPos, null);
            double xAccuracy = Math.abs(x - pixelPos.x);
            double yAccuracy = Math.abs(y - pixelPos.y);
            double accuracy = Math.max(xAccuracy, yAccuracy);
            reverseAccuracy = Math.max(reverseAccuracy, accuracy);
            coordinates[i] = new ExpectedGeoCoordinate(x, y, geoPos.getLat(), geoPos.getLon());
        }
    }

    public ExpectedGeoCoordinate[] getCoordinates() {
        return coordinates;
    }

    Double getReverseAccuracy() {
        return reverseAccuracy;
    }

}
