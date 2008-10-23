package org.esa.beam.jai;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.draw.Figure;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.util.ImageUtils;
import org.esa.beam.util.IntMap;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.jai.JAIUtils;
import org.esa.beam.util.math.MathUtils;

import javax.media.jai.*;
import javax.media.jai.operator.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;

public class ImageManager {

    private static final String CACHE_INTERMEDIATE_TILES_PROPERTY = "beam.imageManager.cacheIntermediateTiles";

    private final static ImageManager INSTANCE = new ImageManager();
    private final Map<Object, MultiLevelSource> maskImageMap = new WeakHashMap<Object, MultiLevelSource>(101);

    public static ImageManager getInstance() {
        return INSTANCE;
    }

    public synchronized void dispose() {
        for (MultiLevelSource multiLevelSource : maskImageMap.values()) {
            multiLevelSource.reset();
        }
        maskImageMap.clear();
    }

    public MultiLevelModel createMultiLevelModel(ProductNode productNode) {
        final Scene scene = SceneFactory.createScene(productNode);
        if (scene == null) {
            return null;
        }
        final int w = scene.getRasterWidth();
        final int h = scene.getRasterHeight();

        final AffineTransform i2mTransform;
        if (productNode.getProduct() != null
                && productNode.getProduct().getGeoCoding() instanceof MapGeoCoding) {
            final MapGeoCoding mapGeoCoding = (MapGeoCoding) productNode.getProduct().getGeoCoding();
            final MapInfo mapInfo = mapGeoCoding.getMapInfo();
            i2mTransform = mapInfo.getPixelToMapTransform();
        } else {
            i2mTransform = new AffineTransform();
        }

        return new DefaultMultiLevelModel(i2mTransform, w, h);
    }

    public static ImageLayout createSingleBandedImageLayout(RasterDataNode rasterDataNode) {
        return createSingleBandedImageLayout(rasterDataNode,
                                             getDataBufferType(rasterDataNode.getDataType()));
    }

    public static ImageLayout createSingleBandedImageLayout(RasterDataNode rasterDataNode, int dataBufferType) {
        int width = rasterDataNode.getSceneRasterWidth();
        int height = rasterDataNode.getSceneRasterHeight();
        Dimension tileSize = getPreferredTileSize(rasterDataNode.getProduct());
        return createSingleBandedImageLayout(dataBufferType, width, height, tileSize.width, tileSize.height);
    }

    public static ImageLayout createSingleBandedImageLayout(int dataType,
                                                            int width,
                                                            int height,
                                                            int tileWidth,
                                                            int tileHeight) {
        SampleModel sampleModel = ImageUtils.createSingleBandedSampleModel(dataType, width, height);
        ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
        return createImageLayout(width, height, tileWidth, tileHeight, sampleModel, colorModel);
    }

    public static ImageLayout createSingleBandedImageLayout(int dataBufferType,
                                                            int sourceWidth,
                                                            int sourceHeight,
                                                            Dimension tileSize,
                                                            ResolutionLevel level) {
        if (sourceWidth < 0) {
            throw new IllegalArgumentException("sourceWidth");
        }
        if (sourceHeight < 0) {
            throw new IllegalArgumentException("sourceHeight");
        }
        Assert.notNull("level");
        int destWidth = (int) Math.floor(sourceWidth / level.getScale());
        int destHeight = (int) Math.floor(sourceHeight / level.getScale());
        SampleModel sampleModel = ImageUtils.createSingleBandedSampleModel(dataBufferType,
                                                                           destWidth,
                                                                           destHeight);
        ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
        tileSize = tileSize != null ? tileSize : JAIUtils.computePreferredTileSize(destWidth, destHeight, 1);
        return createImageLayout(destWidth, destHeight, tileSize.width, tileSize.height, sampleModel, colorModel);
    }

    private static ImageLayout createImageLayout(int width,
                                                 int height,
                                                 int tileWidth,
                                                 int tileHeight,
                                                 SampleModel sampleModel,
                                                 ColorModel colorModel) {
        return new ImageLayout(0, 0,
                               width,
                               height,
                               0, 0,
                               tileWidth,
                               tileHeight,
                               sampleModel,
                               colorModel);
    }

    public static int getDataBufferType(int productDataType) {
        switch (productDataType) {
            case ProductData.TYPE_INT8:
            case ProductData.TYPE_UINT8:
                return DataBuffer.TYPE_BYTE;
            case ProductData.TYPE_INT16:
                return DataBuffer.TYPE_SHORT;
            case ProductData.TYPE_UINT16:
                return DataBuffer.TYPE_USHORT;
            case ProductData.TYPE_INT32:
            case ProductData.TYPE_UINT32:
                return DataBuffer.TYPE_INT;
            case ProductData.TYPE_FLOAT32:
                return DataBuffer.TYPE_FLOAT;
            case ProductData.TYPE_FLOAT64:
                return DataBuffer.TYPE_DOUBLE;
            default:
                throw new IllegalArgumentException("productDataType");
        }
    }

    public static int getProductDataType(int dataBufferType) {
        switch (dataBufferType) {
            case DataBuffer.TYPE_BYTE:
                return ProductData.TYPE_UINT8;
            case DataBuffer.TYPE_SHORT:
                return ProductData.TYPE_INT16;
            case DataBuffer.TYPE_USHORT:
                return ProductData.TYPE_UINT16;
            case DataBuffer.TYPE_INT:
                return ProductData.TYPE_INT32;
            case DataBuffer.TYPE_FLOAT:
                return ProductData.TYPE_FLOAT32;
            case DataBuffer.TYPE_DOUBLE:
                return ProductData.TYPE_FLOAT64;
            default:
                throw new IllegalArgumentException("dataBufferType");
        }
    }

    public static Dimension getPreferredTileSize(Product product) {
        Dimension tileSize;
        final Dimension preferredTileSize = product.getPreferredTileSize();
        if (preferredTileSize != null) {
            tileSize = preferredTileSize;
        } else {
            tileSize = JAIUtils.computePreferredTileSize(product.getSceneRasterWidth(),
                                                         product.getSceneRasterHeight(), 1);
        }
        return tileSize;
    }

    public PlanarImage createColoredBandImage(RasterDataNode[] rasterDataNodes,
                                              ImageInfo imageInfo,
                                              int level) {
        Assert.notNull(rasterDataNodes,
                       "rasterDataNodes");
        Assert.state(rasterDataNodes.length == 1
                || rasterDataNodes.length == 3
                || rasterDataNodes.length == 4,
                     "invalid number of bands");

        prepareImageInfos(rasterDataNodes, ProgressMonitor.NULL);
        if (rasterDataNodes.length == 1) {
            return createColored1BandImage(rasterDataNodes[0], imageInfo, level);
        } else {
            return createColored3BandImage(rasterDataNodes, imageInfo, level);
        }
    }

    private PlanarImage createColored1BandImage(RasterDataNode raster, ImageInfo imageInfo, int level) {
        Assert.notNull(raster, "raster");
        Assert.notNull(imageInfo, "imageInfo");
        PlanarImage sourceImage = getSourceImage(raster, level);
        PlanarImage validMaskImage = getValidMaskImage(raster, level);
        PlanarImage image = createByteIndexedImage(raster, sourceImage, imageInfo);
        image = createMatchCdfImage(image, imageInfo.getHistogramMatching(), new Stx[]{raster.getStx()});
        image = createLookupRgbImage(raster, image, validMaskImage, imageInfo);
        return image;
    }

    private PlanarImage createColored3BandImage(RasterDataNode[] rasters, ImageInfo rgbImageInfo, int level) {
        PlanarImage[] images = new PlanarImage[rasters.length];
        PlanarImage[] validMaskImages = new PlanarImage[rasters.length];
        Stx[] stxs = new Stx[rasters.length];
        for (int i = 0; i < rasters.length; i++) {
            RasterDataNode raster = rasters[i];
            stxs[i] = raster.getStx();
            PlanarImage sourceImage = getSourceImage(raster, level);
            images[i] = createByteIndexedImage(raster,
                                               sourceImage,
                                               rgbImageInfo.getRgbChannelDef().getMinDisplaySample(i),
                                               rgbImageInfo.getRgbChannelDef().getMaxDisplaySample(i),
                                               rgbImageInfo.getRgbChannelDef().getGamma(i));
            validMaskImages[i] = getValidMaskImage(raster, level);
        }
        // todo - correctly handle no-data color (nf, 10.10.2008)
        return createMergeRgbaOp(images, validMaskImages, rgbImageInfo.getHistogramMatching(), stxs);
    }

    private static PlanarImage createByteIndexedImage(RasterDataNode raster,
                                                      PlanarImage sourceImage,
                                                      ImageInfo imageInfo) {
        ColorPaletteDef colorPaletteDef = imageInfo.getColorPaletteDef();
        final double minSample = colorPaletteDef.getMinDisplaySample();
        final double maxSample = colorPaletteDef.getMaxDisplaySample();

        final IndexCoding indexCoding = (raster instanceof Band) ? ((Band) raster).getIndexCoding() : null;
        if (indexCoding != null) {
            final IntMap sampleColorIndexMap = new IntMap((int) minSample - 1, 4098);
            final ColorPaletteDef.Point[] points = colorPaletteDef.getPoints();
            for (int colorIndex = 0; colorIndex < points.length; colorIndex++) {
                sampleColorIndexMap.putValue((int) points[colorIndex].getSample(), colorIndex);
            }
            final int undefinedIndex = colorPaletteDef.getNumPoints();
            return createIndexedImage(sourceImage, sampleColorIndexMap, undefinedIndex);
        } else {
            return createByteIndexedImage(raster, sourceImage, minSample, maxSample, 1.0);
        }
    }

    private static PlanarImage createByteIndexedImage(RasterDataNode raster,
                                                      PlanarImage sourceImage,
                                                      double minSample,
                                                      double maxSample,
                                                      double gamma) {
        double newMin = raster.scaleInverse(minSample);
        double newMax = raster.scaleInverse(maxSample);
        PlanarImage image = createRescaleOp(sourceImage,
                                            255.0 / (newMax - newMin),
                                            255.0 * newMin / (newMin - newMax));
        // todo - make sure this is not needed, e.g. does "format" auto-clamp?? (nf, 10.2008)
        // image = createClampOp(image, 0, 255);
        image = createByteFormatOp(image);
        if (gamma != 0.0 && gamma != 1.0) {
            byte[] gammaCurve = MathUtils.createGammaCurve(gamma, new byte[256]);
            LookupTableJAI lookupTable = new LookupTableJAI(gammaCurve);
            image = LookupDescriptor.create(image, lookupTable, createDefaultRenderingHints());
        }
        return image;
    }

    private static PlanarImage createClampOp(RenderedImage image, int min, int max) {
        return ClampDescriptor.create(image,
                                             new double[]{min},
                                             new double[]{max},
                                             createDefaultRenderingHints());
    }

    private static RenderingHints createDefaultRenderingHints() {
        boolean cacheIntermediateTiles = Boolean.getBoolean(CACHE_INTERMEDIATE_TILES_PROPERTY);
        if (!cacheIntermediateTiles) {
            return new RenderingHints(JAI.KEY_TILE_CACHE, null);
        }
        return new RenderingHints(null);
    }

    private static PlanarImage createIndexedImage(RenderedImage sourceImage, IntMap intMap, int undefinedIndex) {
        if (sourceImage.getSampleModel().getNumBands() != 1) {
            throw new IllegalArgumentException();
        }
        final int[][] ranges = intMap.getRanges();
        final int keyMin = ranges[0][0];
        final int keyMax = ranges[0][1];
        final int valueMin = ranges[1][0];
        final int valueMax = ranges[1][1];
        final int keyRange = 1 + keyMax - keyMin;
        final int valueRange = 1 + valueMax - valueMin;
        if (keyRange > Short.MAX_VALUE) {
            throw new IllegalArgumentException("intMap: keyRange > Short.MAX_VALUE");
        }
        LookupTableJAI lookup;
        if (valueRange <= 256) {
            final byte[] table = new byte[keyRange + 2];
            for (int i = 1; i < table.length - 1; i++) {
                final int value = intMap.getValue(keyMin + i - 1);
                table[i] = (byte) (value != IntMap.NULL ? value : undefinedIndex);
            }
            table[0] = (byte) undefinedIndex;
            table[table.length - 1] = (byte) undefinedIndex;
            lookup = new LookupTableJAI(table, keyMin - 1);
        } else if (valueRange <= 65536) {
            final short[] table = new short[keyRange + 2];
            for (int i = 1; i < table.length; i++) {
                final int value = intMap.getValue(keyMin + i - 1);
                table[i] = (short) (value != IntMap.NULL ? value : undefinedIndex);
            }
            table[0] = (short) undefinedIndex;
            table[table.length - 1] = (short) undefinedIndex;
            lookup = new LookupTableJAI(table, keyMin - 1, valueRange > 32767);
        } else {
            final int[] table = new int[keyRange + 2];
            for (int i = 1; i < table.length; i++) {
                final int value = intMap.getValue(keyMin + i - 1);
                table[i] = value != IntMap.NULL ? value : undefinedIndex;
            }
            table[0] = undefinedIndex;
            table[table.length - 1] = undefinedIndex;
            lookup = new LookupTableJAI(table, keyMin - 1);
        }
        RenderingHints hints = createDefaultRenderingHints();
        sourceImage = ClampDescriptor.create(sourceImage,
                                             new double[]{keyMin - 1},
                                             new double[]{keyMax + 1},
                                             hints);
        return LookupDescriptor.create(sourceImage, lookup, hints);
    }

    private static PlanarImage createMergeRgbaOp(PlanarImage[] sourceImages,
                                                 PlanarImage[] maskOpImages,
                                                 ImageInfo.HistogramMatching histogramMatching,
                                                 Stx[] stxs) {
        RenderingHints hints = createDefaultRenderingHints();

        if (histogramMatching == ImageInfo.HistogramMatching.None) {
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(sourceImages[0]);
            pb.addSource(sourceImages[1]);
            pb.addSource(sourceImages[2]);
            PlanarImage alpha = createMapOp(maskOpImages);
            if (alpha != null) {
                pb.addSource(alpha);
            }
            return JAI.create("bandmerge", pb, hints);
        } else {
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(sourceImages[0]);
            pb.addSource(sourceImages[1]);
            pb.addSource(sourceImages[2]);
            PlanarImage image = JAI.create("bandmerge", pb, hints);

            if (histogramMatching == ImageInfo.HistogramMatching.Equalize)  {
                image = createMatchCdfEqualizeImage(image, stxs);
            } else {
                image = createMatchCdfNormalizeImage(image, stxs);
            }

            PlanarImage alpha = createMapOp(maskOpImages);
            if (alpha != null) {
                pb = new ParameterBlock();
                pb.addSource(image);
                pb.addSource(alpha);
                image = JAI.create("bandmerge", pb, hints);
            }
            return image;
        }
    }

    private static PlanarImage createMapOp(PlanarImage[] maskOpImages) {
        RenderingHints hints = createDefaultRenderingHints();
        PlanarImage alpha = null;
        for (PlanarImage maskOpImage : maskOpImages) {
            if (maskOpImage != null) {
                if (alpha != null) {
                    alpha = MaxDescriptor.create(alpha, maskOpImage, hints);
                } else {
                    alpha = maskOpImage;
                }
            }
        }
        return alpha;
    }

    private static PlanarImage createLookupRgbImage(RasterDataNode rasterDataNode,
                                                    PlanarImage sourceImage,
                                                    PlanarImage maskImage, ImageInfo imageInfo) {
        Color[] palette;
        ColorPaletteDef colorPaletteDef = imageInfo.getColorPaletteDef();
        if (rasterDataNode instanceof Band && ((Band) rasterDataNode).getIndexCoding() != null) {
            Color[] origPalette = colorPaletteDef.getColors();
            palette = Arrays.copyOf(origPalette, origPalette.length + 1);
            palette[palette.length - 1] = imageInfo.getNoDataColor();
        } else {
            palette = colorPaletteDef.createColorPalette(rasterDataNode);
        }
        // todo - correctly handle no-data color (nf, 10.10.2008)
        final byte[][] lutData = new byte[3][palette.length];
        for (int i = 0; i < palette.length; i++) {
            lutData[0][i] = (byte) palette[i].getRed();
            lutData[1][i] = (byte) palette[i].getGreen();
            lutData[2][i] = (byte) palette[i].getBlue();
        }
        PlanarImage image = createLookupOp(sourceImage, lutData);
        if (maskImage != null) {
            // add mask image as alpha channel so that no-data becomes fully transparent
            image = BandMergeDescriptor.create(image, maskImage, createDefaultRenderingHints());
        }
        return image;
    }

    private PlanarImage createMatchCdfImage(PlanarImage sourceImage, ImageInfo.HistogramMatching histogramMatching, Stx[] stxs) {
        final boolean doEqualize = ImageInfo.HistogramMatching.Equalize == histogramMatching;
        final boolean doNormalize = ImageInfo.HistogramMatching.Normalize == histogramMatching;
        if (doEqualize || doNormalize) {
            if (doEqualize) {
                sourceImage = createMatchCdfEqualizeImage(sourceImage, stxs);
            } else {
                sourceImage = createMatchCdfNormalizeImage(sourceImage, stxs);
            }
        }
        return sourceImage;
    }

    private static PlanarImage createMatchCdfEqualizeImage(PlanarImage sourceImage, Stx[] stxs) {

        Assert.notNull(sourceImage, "sourceImage");
        Assert.notNull(stxs, "stxs");
        int numBands = sourceImage.getSampleModel().getNumBands();
        Assert.argument(stxs.length == numBands, "stxs");

        final Histogram histogram = createHistogram(sourceImage, stxs);

        // Create an equalization CDF.
        float[][] eqCDF = new float[numBands][];
        for (int b = 0; b < numBands; b++) {
            int binCount = histogram.getNumBins(b);
            eqCDF[b] = new float[binCount];
            for (int i = 0; i < binCount; i++) {
                eqCDF[b][i] = (float) (i + 1) / (float) binCount;
            }
        }
        return MatchCDFDescriptor.create(sourceImage, eqCDF, createDefaultRenderingHints());
    }

    private static Histogram createHistogram(PlanarImage sourceImage, Stx[] stxs) {
        final Histogram histogram = createHistogram(stxs);
        sourceImage.setProperty("histogram", histogram);
        if (sourceImage instanceof RenderedOp) {
            RenderedOp renderedOp = (RenderedOp) sourceImage;
            renderedOp.getRendering().setProperty("histogram", histogram);
        }
        return histogram;
    }

    private static Histogram createHistogram(Stx[] stxs) {
        Histogram histogram = new Histogram(stxs[0].getHistogramBinCount(), 0, 256, stxs.length);
        for (int i = 0; i < stxs.length; i++) {
            System.arraycopy(stxs[i].getHistogramBins(), 0, histogram.getBins(i), 0, stxs[0].getHistogramBinCount());
        }
        return histogram;
    }

    private static RenderedOp createMatchCdfNormalizeImage(PlanarImage sourceImage, Stx[] stxs) {
        final double dev = 256.0;
        int numBands = sourceImage.getSampleModel().getNumBands();
        final double[] means = new double[numBands];
        Arrays.fill(means, 0.5 * dev);
        final double[] stdDevs = new double[numBands];
        Arrays.fill(stdDevs, 0.25 * dev);
        return createHistogramNormalizedImage(sourceImage, stxs, means, stdDevs);
    }

    private static RenderedOp createHistogramNormalizedImage(PlanarImage sourceImage, Stx[] stxs, double[] mean, double[] stdDev) {
        int numBands = sourceImage.getSampleModel().getNumBands();
        Assert.argument(numBands == mean.length, "length of mean must be equal to number of bands in the image");
        Assert.argument(numBands == stdDev.length, "length of stdDev must be equal to number of bands in the image");

        final Histogram histogram = createHistogram(sourceImage, stxs);

        float[][] normCDF = new float[numBands][];
        for (int b = 0; b < numBands; b++) {
            int binCount = histogram.getNumBins(b);
            normCDF[b] = new float[binCount];
            double mu = mean[b];
            double twoSigmaSquared = 2.0 * stdDev[b] * stdDev[b];
            normCDF[b][0] = (float) Math.exp(-mu * mu / twoSigmaSquared);
            for (int i = 1; i < binCount; i++) {
                double deviation = i - mu;
                normCDF[b][i] = normCDF[b][i - 1] +
                        (float) Math.exp(-deviation * deviation / twoSigmaSquared);
            }
        }

        for (int b = 0; b < numBands; b++) {
            int binCount = histogram.getNumBins(b);
            double CDFnormLast = normCDF[b][binCount - 1];
            for (int i = 0; i < binCount; i++) {
                normCDF[b][i] /= CDFnormLast;
            }
        }

        return MatchCDFDescriptor.create(sourceImage, normCDF, createDefaultRenderingHints());
    }


    public PlanarImage getSourceImage(RasterDataNode rasterDataNode, int level) {
        RenderedImage sourceImage = rasterDataNode.getSourceImage();
        return getLevelImage(sourceImage, level);
    }


    public PlanarImage getValidMaskImage(final RasterDataNode rasterDataNode, int level) {
        if (rasterDataNode.isValidMaskUsed()) {
            return getLevelImage(rasterDataNode.getValidMaskImage(), level);
        }
        return null;
    }

    public PlanarImage getGeophysicalImage(RasterDataNode rasterDataNode, int level) {
        RenderedImage levelZeroImage = rasterDataNode.getGeophysicalImage();
        return getLevelImage(levelZeroImage, level);
    }

    public MultiLevelSource getMultiLevelSource(RenderedImage levelZeroImage) {
        MultiLevelSource multiLevelSource;
        if (levelZeroImage instanceof MultiLevelSource) {
            multiLevelSource = (MultiLevelSource) levelZeroImage;
        } else {
            // todo - New image instance is created here. Maintain/keep it? 
            // This will happen e.g. for all bands created by GPF operators. (nf, 19.09.2008)
            final int levelCount = DefaultMultiLevelModel.getLevelCount(levelZeroImage.getWidth(), levelZeroImage.getHeight());
            multiLevelSource = new DefaultMultiLevelSource(levelZeroImage,
                                                           levelCount,
                                                           Interpolation.getInstance(Interpolation.INTERP_NEAREST));
            System.out.println("IMAGING 4.5: " +
                    "Warning: Created an (inefficient) instance of DefaultMultiLevelSource. " +
                    "Source image is a " + levelZeroImage.getClass());
        }
        return multiLevelSource;
    }

    private PlanarImage getLevelImage(RenderedImage levelZeroImage, int level) {
        final MultiLevelSource multiLevelSource = getMultiLevelSource(levelZeroImage);
        RenderedImage image = multiLevelSource.getImage(level);
        return PlanarImage.wrapRenderedImage(image);
    }

    public SingleBandedOpImage getMaskImage(final Product product, final String expression, int level) {
        final Object key = new MaskKey(product, expression);
        synchronized (maskImageMap) {
            MultiLevelSource mrMulti = maskImageMap.get(key);
            if (mrMulti == null) {
                mrMulti = new AbstractMultiLevelSource(createMultiLevelModel(product)) {

                    @Override
                    public RenderedImage createImage(int level) {
                        return VirtualBandOpImage.createMaskOpImage(product, expression, ResolutionLevel.create(getModel(), level));
                    }
                };
                maskImageMap.put(key, mrMulti);
            }
            // Note: cast is ok, because interface of MultiLevelSource requires to return same type
            return (SingleBandedOpImage) mrMulti.getImage(level);
        }
    }

    public ImageInfo getImageInfo(RasterDataNode[] rasters) {
        Assert.notNull(rasters, "rasters");
        Assert.argument(rasters.length == 1 || rasters.length == 3, "rasters.length == 1 || rasters.length == 3");
        if (rasters.length == 1) {
            Assert.state(rasters[0].getImageInfo() != null, "rasters[0].getImageInfo()");
            return rasters[0].getImageInfo();
        } else {
            final RGBChannelDef rgbChannelDef = new RGBChannelDef();
            for (int i = 0; i < rasters.length; i++) {
                RasterDataNode raster = rasters[i];
                Assert.state(rasters[i].getImageInfo() != null, "rasters[i].getImageInfo()");
                ImageInfo imageInfo = raster.getImageInfo();
                rgbChannelDef.setSourceName(i, raster.getName());
                rgbChannelDef.setMinDisplaySample(i, imageInfo.getColorPaletteDef().getMinDisplaySample());
                rgbChannelDef.setMaxDisplaySample(i, imageInfo.getColorPaletteDef().getMaxDisplaySample());
            }
            return new ImageInfo(rgbChannelDef);
        }
    }

    public void prepareImageInfos(RasterDataNode[] rasterDataNodes, ProgressMonitor pm) {
        int numTaskSteps = 0;
        for (RasterDataNode raster : rasterDataNodes) {
            numTaskSteps += raster.getImageInfo() == null ? 1 : 0;
        }

        pm.beginTask("Computing image statistics", numTaskSteps);
        try {
            for (final RasterDataNode raster : rasterDataNodes) {
                final ImageInfo imageInfo = raster.getImageInfo();
                if (imageInfo == null) {
                    raster.getImageInfo(SubProgressMonitor.create(pm, 1));
                }
            }
        } finally {
            pm.done();
        }
    }


    public int getStatisticsLevel(RasterDataNode raster, int levelCount) {
        final long imageSize = (long) raster.getSceneRasterWidth() * raster.getSceneRasterHeight();
        final int statisticsLevel;
        if (imageSize <= DefaultMultiLevelModel.MAX_PIXEL_COUNT) {
            statisticsLevel = 0;
        } else {
            statisticsLevel = levelCount - 1;
        }
        return statisticsLevel;
    }

    public PlanarImage createColoredMaskImage(Product product, String expression, Color color, boolean invertMask, int level) {
        PlanarImage image = getMaskImage(product, expression, level);
        return createColoredMaskImage(color, image, invertMask);
    }

    public static PlanarImage createColoredMaskImage(Color color, PlanarImage alphaImage, boolean invertAlpha) {
        return createColoredMaskImage(color, invertAlpha ? InvertDescriptor.create(alphaImage, null) : alphaImage);
    }

    public static PlanarImage createColoredMaskImage(Color color, PlanarImage alphaImage) {
        final ImageLayout imageLayout = new ImageLayout();
        imageLayout.setTileWidth(alphaImage.getTileWidth());
        imageLayout.setTileHeight(alphaImage.getTileHeight());
        final RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout);
        final RenderedOp colorImage =
                ConstantDescriptor.create(
                        (float) alphaImage.getWidth(),
                        (float) alphaImage.getHeight(),
                        new Byte[]{
                                (byte) color.getRed(),
                                (byte) color.getGreen(),
                                (byte) color.getBlue(),
                        }, hints);
        return BandMergeDescriptor.create(colorImage, alphaImage, hints);
    }

    /**
     * Creates a colored ROI image for the given band.
     *
     * @param rasterDataNode the band
     * @param color          the color
     * @param level          the level
     * @return the image, or null if the band has no valid ROI definition
     */
    public PlanarImage createColoredRoiImage(RasterDataNode rasterDataNode, Color color, int level) {
        final PlanarImage roi = createRoiMaskImage(rasterDataNode, level);
        if (roi == null) {
            return null;
        }
        return createColoredMaskImage(color, roi, false);
    }

    /**
     * Creates a ROI for the given band.
     *
     * @param rasterDataNode the band
     * @param level          the level
     * @return the ROI, or null if the band has no valid ROI definition
     */
    public PlanarImage createRoiMaskImage(final RasterDataNode rasterDataNode, int level) {
        final ROIDefinition roiDefinition = rasterDataNode.getROIDefinition();
        if (roiDefinition == null) {
            return null;
        }

        ArrayList<PlanarImage> rois = new ArrayList<PlanarImage>(4);

        // Step 1:  insert ROI pixels determined by bitmask expression
        String bitmaskExpr = roiDefinition.getBitmaskExpr();
        if (!StringUtils.isNullOrEmpty(bitmaskExpr) && roiDefinition.isBitmaskEnabled()) {
            rois.add(getMaskImage(rasterDataNode.getProduct(), bitmaskExpr, level));
        }

        // Step 2:  insert ROI pixels within value range
        if (roiDefinition.isValueRangeEnabled()) {
            String rangeExpr = rasterDataNode.getName() + " >= " + roiDefinition.getValueRangeMin() + " && "
                    + rasterDataNode.getName() + " <= " + roiDefinition.getValueRangeMax();
            rois.add(getMaskImage(rasterDataNode.getProduct(), rangeExpr, level));
        }

        // Step 3:  insert ROI pixels for pins
        final MultiLevelModel multiLevelModel = createMultiLevelModel(rasterDataNode);
        if (roiDefinition.isPinUseEnabled() && rasterDataNode.getProduct().getPinGroup().getNodeCount() > 0) {

            final Object key = new MaskKey(rasterDataNode.getProduct(), rasterDataNode.getName() + "_RoiPlacemarks");
            MultiLevelSource placemarkMaskMLS;
            synchronized (maskImageMap) {
                placemarkMaskMLS = maskImageMap.get(key);
                if (placemarkMaskMLS == null) {
                    placemarkMaskMLS = new AbstractMultiLevelSource(multiLevelModel) {

                        @Override
                        public RenderedImage createImage(int level) {
                            return new PlacemarkMaskOpImage(rasterDataNode.getProduct(),
                                                            PinDescriptor.INSTANCE, 3,
                                                            rasterDataNode.getSceneRasterWidth(),
                                                            rasterDataNode.getSceneRasterHeight(),
                                                            ResolutionLevel.create(getModel(), level));
                        }
                    };
                    maskImageMap.put(key, placemarkMaskMLS);
                }
            }
            rois.add((PlanarImage) placemarkMaskMLS.getImage(level));
        }

        // Step 4:  insert ROI pixels within shape
        Figure roiShapeFigure = roiDefinition.getShapeFigure();
        if (roiDefinition.isShapeEnabled() && roiShapeFigure != null) {

            final Object key = new MaskKey(rasterDataNode.getProduct(), rasterDataNode.getName() + "_RoiShapes");
            MultiLevelSource shapeMaskMLS;
            synchronized (maskImageMap) {
                shapeMaskMLS = maskImageMap.get(key);
                if (shapeMaskMLS == null) {
                    final Shape roiShape = roiShapeFigure.getShape();
                    shapeMaskMLS = new AbstractMultiLevelSource(multiLevelModel) {

                        @Override
                        public RenderedImage createImage(int level) {
                            return new ShapeMaskOpImage(roiShape,
                                                        rasterDataNode.getSceneRasterWidth(),
                                                        rasterDataNode.getSceneRasterHeight(),
                                                        ResolutionLevel.create(getModel(), level));
                        }
                    };
                    maskImageMap.put(key, shapeMaskMLS);
                }
            }
            rois.add((PlanarImage) shapeMaskMLS.getImage(level));
        }

        if (rois.size() == 0) {
            // todo - null is returned whenever a shape is converted into a ROI for any but the first time
            // todo - may be this problem is due to concurrency issues (nf, 08.2008)
            return null;
        }

        PlanarImage roi = rois.get(0);

        // Step 5: combine ROIs
        for (int i = 1; i < rois.size(); i++) {
            PlanarImage roi2 = rois.get(i);
            if (roiDefinition.isOrCombined()) {
                roi = MaxDescriptor.create(roi, roi2, null);
            } else {
                roi = MinDescriptor.create(roi, roi2, null);
            }
        }

        // Step 6:  invert ROI pixels
        if (roiDefinition.isInverted()) {
            roi = InvertDescriptor.create(roi, null);
        }

        return roi;
    }

    public static PlanarImage createFormatOp(RenderedImage image, int dataType) {
        if (image.getSampleModel().getDataType() == dataType) {
            return PlanarImage.wrapRenderedImage(image);
        }
        return FormatDescriptor.create(image,
                                       dataType,
                                       createDefaultRenderingHints());
    }

    private static PlanarImage createRescaleOp(RenderedImage src, double factor, double offset) {
        if (factor == 1.0 && offset == 0.0) {
            return PlanarImage.wrapRenderedImage(src);
        }
        return RescaleDescriptor.create(src,
                                        new double[]{factor},
                                        new double[]{offset},
                                        createDefaultRenderingHints());
    }

    public static PlanarImage createRescaleOp(RenderedImage src, int dataType, double factor, double offset, boolean log10Scaled) {
        PlanarImage image = createFormatOp(src, dataType);
        if (log10Scaled) {
            image = createRescaleOp(image, Math.log(10) * factor, Math.log(10) * offset);
            image = createExpOp(image);
        } else {
            image = createRescaleOp(image, factor, offset);
        }
        return image;
    }

    private static PlanarImage createExpOp(RenderedImage image) {
        return ExpDescriptor.create(image, createDefaultRenderingHints());
    }

    private static PlanarImage createLookupOp(RenderedImage src, byte[][] lookupTable) {
        LookupTableJAI lookup = new LookupTableJAI(lookupTable);
        return LookupDescriptor.create(src, lookup, createDefaultRenderingHints());
    }

    private static PlanarImage createByteFormatOp(RenderedImage src) {
        ColorModel cm = ImageUtils.create8BitGreyscaleColorModel();
        SampleModel sm = cm.createCompatibleSampleModel(src.getTileWidth(), src.getTileHeight());
        ImageLayout layout = new ImageLayout(src);
        layout.setColorModel(cm);
        layout.setSampleModel(sm);

        RenderingHints rh = createDefaultRenderingHints();
        rh.put(JAI.KEY_IMAGE_LAYOUT, layout);
        return FormatDescriptor.create(src, DataBuffer.TYPE_BYTE, rh);
    }

    private static class MaskKey {
        final WeakReference<Product> product;
        final String expression;

        private MaskKey(Product product, String expression) {
            Assert.notNull(product, "product");
            Assert.notNull(expression, "expression");
            this.product = new WeakReference<Product>(product);
            this.expression = expression;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null) {
                return false;
            }
            if (getClass() != o.getClass()) {
                return false;
            }
            MaskKey key = (MaskKey) o;
            return product.get() == key.product.get() && expression.equals(key.expression);

        }

        @Override
        public int hashCode() {
            int result;
            result = product.get().hashCode();
            result = 31 * result + expression.hashCode();
            return result;
        }
    }
}

