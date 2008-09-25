package org.esa.beam.glevel;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;

import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ImageManager;

import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;

public class BandImageMultiLevelSource extends AbstractMultiLevelSource {

    private final RasterDataNode[] rasterDataNodes;
    private ImageInfo imageInfo;

    public static BandImageMultiLevelSource create(RasterDataNode rasterDataNode, ProgressMonitor pm) {
        return create(new RasterDataNode[]{rasterDataNode}, pm);
    }

    public static BandImageMultiLevelSource create(RasterDataNode[] rasterDataNodes, ProgressMonitor pm) {
        RasterDataNode rdn = rasterDataNodes[0];
        MultiLevelModel model;
        if (rdn.getSourceImage() instanceof MultiLevelSource) {
            MultiLevelSource multiLevelSource = (MultiLevelSource) rdn.getSourceImage();
            model = multiLevelSource.getModel();
        } else {
            final int w = rdn.getSceneRasterWidth();
            final int h = rdn.getSceneRasterHeight();
            model = new DefaultMultiLevelModel(new AffineTransform(), w, h);
        }
        ImageManager.getInstance().prepareImageInfos(rasterDataNodes, pm);
        return new BandImageMultiLevelSource(model, rasterDataNodes);
    }

    public static BandImageMultiLevelSource create(RasterDataNode rasterDataNode,
                                          AffineTransform i2mTransform, ProgressMonitor pm) {
        return create(new RasterDataNode[]{rasterDataNode}, i2mTransform, pm);
    }

    public static BandImageMultiLevelSource create(RasterDataNode[] rasterDataNodes,
                                          AffineTransform i2mTransform, ProgressMonitor pm) {
        return create(rasterDataNodes, i2mTransform,
                      DefaultMultiLevelModel.getLevelCount(rasterDataNodes[0].getSceneRasterWidth(),
                                                           rasterDataNodes[0].getSceneRasterHeight()), pm);
    }

    private static BandImageMultiLevelSource create(RasterDataNode[] rasterDataNodes,
                                           AffineTransform i2mTransform,
                                           int levelCount,
                                           ProgressMonitor pm) {
        Assert.notNull(rasterDataNodes);
        Assert.argument(rasterDataNodes.length > 0);
        final int w = rasterDataNodes[0].getSceneRasterWidth();
        final int h = rasterDataNodes[0].getSceneRasterHeight();
        MultiLevelModel model = new DefaultMultiLevelModel(levelCount, i2mTransform, w, h);
        ImageManager.getInstance().prepareImageInfos(rasterDataNodes, pm);
        return new BandImageMultiLevelSource(model, rasterDataNodes);
    }

    public BandImageMultiLevelSource(MultiLevelModel model, RasterDataNode[] rasterDataNodes) {
        super(model);
        this.rasterDataNodes = rasterDataNodes.clone();
        imageInfo = ImageManager.getInstance().getImageInfo(rasterDataNodes);
    }

    public void setImageInfo(ImageInfo imageInfo) {
        this.imageInfo = imageInfo;
    }
    
    public ImageInfo getImageInfo() {
        return imageInfo;
    }

    @Override
    public RenderedImage createImage(int level) {
        return ImageManager.getInstance().createColoredBandImage(rasterDataNodes, imageInfo, level);
    }
}
