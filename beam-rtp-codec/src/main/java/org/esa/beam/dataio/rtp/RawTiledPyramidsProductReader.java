package org.esa.beam.dataio.rtp;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.glevel.TiledFileMultiLevelSource;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class RawTiledPyramidsProductReader extends AbstractProductReader {
    public RawTiledPyramidsProductReader(RawTiledPyramidsProductCodecSpi spi) {
        super(spi);
    }

    protected Product readProductNodesImpl() throws IOException {

        final File headerFile = RawTiledPyramidsProductCodecSpi.getHeaderFile(getInput());
        final XStream xStream = RawTiledPyramidsProductCodecSpi.createXStream();
        final FileReader reader = new FileReader(headerFile);
        try {
            final ProductDescriptor productDescriptor = new ProductDescriptor();
            xStream.fromXML(reader, productDescriptor);

            final Product product = new Product(productDescriptor.getName(), productDescriptor.getType(), productDescriptor.getWidth(), productDescriptor.getHeight());
            product.setDescription(productDescriptor.getDescription());

            final BandDescriptor[] bandDescriptors = productDescriptor.getBandDescriptors();
            for (BandDescriptor bandDescriptor : bandDescriptors) {
                final String expression = bandDescriptor.getExpression();
                final Band band;
                if (expression != null && !expression.trim().isEmpty()) {
                    band = new VirtualBand(bandDescriptor.getName(),
                                           ProductData.getType(bandDescriptor.getDataType()),
                                           product.getSceneRasterWidth(),
                                           product.getSceneRasterHeight(), expression);
                } else {
                    band = new Band(bandDescriptor.getName(),
                                    ProductData.getType(bandDescriptor.getDataType()),
                                    product.getSceneRasterWidth(),
                                    product.getSceneRasterHeight());
                    band.setSourceImage(new DefaultMultiLevelImage(TiledFileMultiLevelSource.create(new File(headerFile.getParent(), bandDescriptor.getName()))));
                }
                band.setDescription(bandDescriptor.getDescription());
                band.setScalingFactor(bandDescriptor.getScalingFactor());
                band.setScalingOffset(bandDescriptor.getScalingOffset());
                product.addBand(band);
            }

            return product;
        } catch (XStreamException e) {
            throw new IOException("Failed to read product header.", e);
        } finally {
            reader.close();
        }
    }

    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {
        throw new IllegalStateException("should not come here!");
    }

}
