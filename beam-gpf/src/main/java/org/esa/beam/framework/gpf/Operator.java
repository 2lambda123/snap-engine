package org.esa.beam.framework.gpf;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.internal.OperatorContext;

import java.awt.Rectangle;
import java.util.Map;
import java.util.logging.Logger;


/**
 * The abstract base class for all operators intended to be extended by clients.
 * <p>The following methods are intended to be implemented or overidden:
 * <ld>
 * <li>{@link #initialize()}: must be implemented in order to initialise the operator and create the target
 * product.</li>
 * <li>{@link #computeTile(org.esa.beam.framework.datamodel.Band, Tile,com.bc.ceres.core.ProgressMonitor) computeTile()}: implemented to compute the tile
 * for a single band.</li>
 * <li>{@link #computeTileStack(java.util.Map computeTileStack()}: implemented to compute the tiles
 * for multiple bands.</li>
 * <li>{@link #dispose()}: can be overidden in order to free all resources previously allocated by the operator.</li>
 * <li>{@link #getConfigurationConverter()}: can be overidden in order to return a suitable converter for an operator's configuration.</li>
 * </ld>
 * </p>
 * <p>Generally, only one {@code computeTile} method needs to be implemented. It depends on the type of algorithm which
 * of both operations is most advantageous to implement:
 * <ol>
 * <li>If bands can be computed independently of each other, then it is
 * beneficial to implement the {@code computeTile()} method. This is the case for sub-sampling, map-projections,
 * band arithmetic, band filtering and statistic analyses.</li>
 * <li>{@code computeTileStack()} should be overriden in cases where the bands of a product cannot be computed independently, e.g.
 * because they are a simultaneous output. This is often the case for algorithms based on neural network, cluster analyses,
 * model inversion methods or spectral unmixing.</li>
 * </ol>
 * </p>
 * <p>The framework execute either the {@code computeTile()} or the {@code computeTileStack()} method
 * based on the current use case or request.
 * If tiles for single bands are requested, e.g. for image display, it will always prefer an implementation of
 * the {@code computeTile()} method and call it.
 * If all tiles are requested at once, e.g. writing a product to disk, it will attempt to use the {@code computeTileStack()}
 * method. If the framework cannot use its preferred operation, it will use the one implemented by the operator.</p>
 * <p/>
 * <p>todo - Explain the role of operator annotations (nf - 15.10.2007)</p>
 * <p>todo - Explain the role of the SPI (nf - 15.10.2007)</p>
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @author Marco Zühlke
 * @see OperatorSpi
 * @see org.esa.beam.framework.gpf.annotations.OperatorMetadata
 * @see org.esa.beam.framework.gpf.annotations.Parameter
 * @see org.esa.beam.framework.gpf.annotations.TargetProduct
 * @see org.esa.beam.framework.gpf.annotations.SourceProduct
 * @see org.esa.beam.framework.gpf.annotations.SourceProducts
 * @since 4.1
 */
public abstract class Operator {

    final OperatorContext context;

    /**
     * Constructs a new operator.
     */
    protected Operator() {
        context = new OperatorContext(this);
    }

    /**
     * Initializes this operator and returns its one and only target product.
     * <p/>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @return the target product
     * @throws OperatorException if an error occurs during operator initialisation
     * @see #getTargetProduct()
     */
    public abstract Product initialize() throws OperatorException;

    /**
     * @deprecated use the version below
     */
    public void computeTile(Band targetBand, Tile targetTile) throws OperatorException {
        computeTile(targetBand, targetTile, ProgressMonitor.NULL);
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented"</p>.
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException If an error occurs during computation of the target raster.
     */
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        throw new RuntimeException("not implemented");
    }

    /**
     * @deprecated use the version below
     */
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle) throws OperatorException {
        computeTileStack(targetTiles, targetRectangle, ProgressMonitor.NULL);
    }

    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented"</p>.
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed (same for all rasters in <code>targetRasters</code>).
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException if an error occurs during computation of the target rasters.
     */
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        throw new RuntimeException("not implemented");
    }

    /**
     * Releases the resources the operator has acquired during its lifetime.
     * The default implementation does nothing.
     */
    public void dispose() {
    }

    /**
     * Gets a suitable converter for this cperator's configuration.
     * This method is intended to be overridden by clients in order to provide
     * their own implementation of their configuration conversion.
     * The default implementation returns {@code this} if the derived operator
     * implements {@link org.esa.beam.framework.gpf.ParameterConverter}, or
     * {@code null} otherwise. In the latter case, a default configuration conversion
     * will be applied by the GPF.
     *
     * @return A suitable configuration converter or {@code null} for default configuration conversion.
     */
    public ParameterConverter getConfigurationConverter() {
        if (this instanceof ParameterConverter) {
            return (ParameterConverter) this;
        }
        return null;
    }

    /**
     * Gets the source products in the order they have been declared.
     *
     * @return the array source products
     */
    public final Product[] getSourceProducts() {
        return context.getSourceProducts();
    }

    /**
     * Gets the source product using the specified name.
     *
     * @param id the identifier
     * @return the source product, or {@code null} if not found
     * @see #getSourceProductId(Product)
     */
    public final Product getSourceProduct(String id) {
        Assert.notNull(id, "id");
        return context.getSourceProduct(id);
    }

    /**
     * Gets the identifier for the given source product.
     *
     * @param product The source product.
     * @return The identifier, or {@code null} if no such exists.
     * @see #getSourceProduct(String)
     */
    public final String getSourceProductId(Product product) {
        Assert.notNull(product, "product");
        return context.getSourceProductId(product);
    }

    /**
     * Adds a product to the list of source products.
     * One product instance can be registered with different identifiers, e.g. "source", "source1" and "input"
     * in consecutive calls.
     *
     * @param id      a product identifier
     * @param product the product to be added
     */
    public final void addSourceProduct(String id, Product product) {
        Assert.notNull(id, "id");
        Assert.notNull(product, "product");
        context.addSourceProduct(id, product);
    }

    /**
     * Gets the target product for the operator.
     * <p/>
     * <p>If a target product has not been set so far, calling this method will result in a
     * call to {@link #initialize()}.</p>
     *
     * @return The target product.
     * @throws OperatorException Thrown by {@link #initialize()},
     *                           if the target product has not yet been created.
     */
    public final Product getTargetProduct() throws OperatorException {
        return context.getTargetProduct();
    }

    /**
     * @deprecated use the version below
     */
    public final Tile getSourceTile(RasterDataNode rasterDataNode, Rectangle rectangle) throws OperatorException {
        return getSourceTile(rasterDataNode, rectangle, ProgressMonitor.NULL);
    }

    /**
     * Gets a {@link Tile} for a given band and rectangle.
     *
     * @param rasterDataNode the raster data node of a data product,
     *                       e.g. a {@link org.esa.beam.framework.datamodel.Band Band} or
     *                       {@link org.esa.beam.framework.datamodel.TiePointGrid TiePointGrid}.
     * @param rectangle      the raster rectangle in pixel coordinates
     * @param pm             The progress monitor passed into the
     *                       the {@link #computeTile(org.esa.beam.framework.datamodel.Band, Tile,com.bc.ceres.core.ProgressMonitor) computeTile} method or
     *                       the {@link #computeTileStack(java.util.Map, java.awt.Rectangle, com.bc.ceres.core.ProgressMonitor) computeTileStack}  method.
     * @return a tile.
     * @throws OperatorException if the tile request cannot be processed
     */
    public final Tile getSourceTile(RasterDataNode rasterDataNode, Rectangle rectangle, ProgressMonitor pm) throws OperatorException {
        return context.getSourceTile(rasterDataNode, rectangle, pm);
    }


    /**
     * Checks for cancelation of the current processing request. Throws an exception, if the
     * request has been canceled (e.g. by the user).
     *
     * @param pm The progress monitor passed into the
     *           the {@link #computeTile(org.esa.beam.framework.datamodel.Band, Tile,com.bc.ceres.core.ProgressMonitor) computeTile} method or
     *           the {@link #computeTileStack(java.util.Map, java.awt.Rectangle, com.bc.ceres.core.ProgressMonitor) computeTileStack}  method.
     * @throws OperatorException if the current processing request has been canceled (e.g. by the user).
     */
    protected final void checkForCancelation(ProgressMonitor pm) throws OperatorException {
        context.checkForCancelation(pm);
    }

    /**
     * @deprecated not required anymore
     */
    protected final ProgressMonitor createProgressMonitor() {
        return ProgressMonitor.NULL;
    }

    /**
     * Gets the logger whuich can be used to log information during initialisation and tile computation.
     *
     * @return The logger.
     */
    public final Logger getLogger() {
        return context.getLogger();
    }

    /**
     * Sets the logger which can be used to log information during initialisation and tile computation.
     *
     * @param logger The logger.
     */
    public final void setLogger(Logger logger) {
        Assert.notNull(logger, "logger");
        context.setLogger(logger);
    }

    /**
     * Gets the SPI which was used to create this operator.
     * If no operator has been explicitely set, the method will return an anonymous
     * SPI.
     *
     * @return The operator SPI.
     */
    public final OperatorSpi getSpi() {
        return context.getOperatorSpi();
    }

    /**
     * Sets the SPI which was used to create this operator.
     *
     * @param operatorSpi The operator SPI.
     */
    public final void setSpi(OperatorSpi operatorSpi) {
        Assert.notNull(operatorSpi, "operatorSpi");
        Assert.argument(operatorSpi.getOperatorClass().isAssignableFrom(getClass()), "operatorSpi");
        context.setOperatorSpi(operatorSpi);
    }
}
