package com.bc.ceres.glevel;

import com.bc.ceres.glevel.support.DefaultMultiLevelSource;

import java.awt.image.RenderedImage;

/**
 * A source for images at a given resolution level.
 * The resolution level, an integer number ranging from zero (the highest resolution)
 * to {@link MultiLevelModel#getLevelCount()}-1 (the lowest resolution) is computed from a given
 * scaling factor and vice versa.
 *
 * @author Norman Fomferra
 * @author Marco Z�hlke
 * @version $revision$ $date$
 */
public interface MultiLevelSource {
    MultiLevelSource NULL = DefaultMultiLevelSource.NULL;

    /**
     * Gets the layout model for the multi-resolution image supported by this {@code LevelImageSource}.
     * @return the multi-resolution image model.
     */
    MultiLevelModel getModel();

    /**
     * Gets the scaled image for the given resolution level.
     *
     * @param level The resolution level.
     * @return The scaled image, must be in the range 0 to {@link MultiLevelModel#getLevelCount()}-1.
     * @see #getModel()
     */
    RenderedImage getLevelImage(int level);

    /**
     * <p>Provides a hint that the level images provided so far will no longer be accessed from a
     * reference in user space.</p>
     *
     * <p>Therefore implementations of this method might also dispose any cached level images
     * that have been provided so far.</p>
     *
     * <p>After calling this method, a call to {@link #getLevelImage(int)}} for the same level may 
     * return a new level image instance.</p>
     *
     * <p>This method is particularly useful if properties have changed that affect the appearance of the
     * returned images at all levels, e.g. after a new color palette has been assigned or the
     * contrast range has changed.</p>
     */
    void reset();
}
