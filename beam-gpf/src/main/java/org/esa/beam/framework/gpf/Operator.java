package org.esa.beam.framework.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;

import java.awt.Rectangle;
import java.util.Map;

/**
 * The {@code Operator} interface defines the signature for processing
 * algorithms. Every {@code Node} of a processing graph uses an
 * {@code Operator} instance to compute its target Product. Implement
 * this interface to create new processing algorithms.
 * <p>{@code Operator}s shall exclusively be created by their {@link OperatorSpi}.</p>
 * <p>Clients shall not implement or extend the interface <code>Operator</code> directly. Instead
 * they should derive from {@link org.esa.beam.framework.gpf.AbstractOperator}.</p>
 *
 * @author Norman Fomferra
 * @author Marco Zühlke
 * @author Maximilian Aulinger
 * @since 4.1
 */
public interface Operator {

    /**
     * Gets the service provider interface (SPI) which was used to create this operator.
     *
     * @return the service provider interface.
     */
    OperatorSpi getSpi();

    /**
     * Initializes this operator and returns its target product.
     * The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of raster data should be placed here.
     * <p/>
     * <p>Clients are adviced to use the progress monitor <code>pm</code> if the method may take
     * a while to finish. The progress monitor informs the framework about the progress being made.</p>
     *
     * @param context the operator's context
     * @param pm      a progress monitor. Can be used to signal progress.
     * @return the target product
     * @throws OperatorException if an error occurs during operator initialisation
     */
    Product initialize(OperatorContext context, ProgressMonitor pm) throws OperatorException;

    /**
     * Called by the framework in order to compute a target raster for the given band.
     *
     * @param targetBand   the target band
     * @param targetRaster the target raster associated with the band to be computed
     * @param pm           a progress monitor. Can be used to signal progress.
     * @throws OperatorException if an error occurs during computation of the target raster
     */
    void computeBand(Band targetBand, Raster targetRaster, ProgressMonitor pm) throws OperatorException;

    /**
     * Called by the framework in order to compute the target rasters for the given bands.
     *
     * @param targetRasters   the target bands and rasters to be computed
     * @param targetRectangle the target area in pixel coordinates to be computed (same for all rasters in <code>targetRasters</code>)
     * @param pm              a progress monitor. Can be used to signal progress.
     * @throws OperatorException if an error occurs during computation of the target rasters
     */
    void computeAllBands(Map<Band, Raster> targetRasters, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException;

    /**
     * Releases the resources the operator has acquired during its lifetime.
     */
    void dispose();
}
