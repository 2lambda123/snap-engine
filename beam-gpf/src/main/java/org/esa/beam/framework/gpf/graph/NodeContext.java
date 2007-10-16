package org.esa.beam.framework.gpf.graph;

import com.bc.ceres.core.Assert;
import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.*;
import org.esa.beam.framework.gpf.internal.OperatorContext;
import org.esa.beam.framework.gpf.internal.OperatorImage;

import java.lang.reflect.Field;

/**
 * Default implementation for {@link org.esa.beam.framework.gpf.internal.OperatorContext}.
 */
class NodeContext {

    private final GraphContext graphContext;
    private final Node node;
    private Operator operator;
    private OperatorContext operatorContext;
    private int referenceCount;
    private Product targetProduct;

    public NodeContext(GraphContext graphContext, Node node) throws GraphException {
        this.graphContext = graphContext;
        this.node = node;
        initOperator();
        initOperatorContext();
    }

    public GraphContext getGraphContext() {
        return graphContext;
    }

    public Node getNode() {
        return node;
    }

    public Operator getOperator() {
        return operator;
    }

    public boolean isOutput() {
        return referenceCount == 0;
    }

    void incrementReferenceCount() {
        this.referenceCount++;
    }

    public void initTargetProduct() throws GraphException {
        try {
            targetProduct = operatorContext.getTargetProduct();
        } catch (OperatorException e) {
            throw new GraphException(e.getMessage(), e);
        }
    }

    public Product getTargetProduct() {
        Assert.notNull(targetProduct, "targetProduct");
        return targetProduct;
    }
    
    public OperatorImage getTargetImage(Band band) {
        return operatorContext.getTargetImage(band);
    }

    public boolean canComputeTileStack() {
        return operatorContext.canComputeTileStack();
    }

    public boolean isInitialized() {
        return operatorContext.isInitialized();
    }

    public void addSourceProduct(String id, Product sourceProduct) {
        operatorContext.addSourceProduct(id, sourceProduct);
    }

    public void setParameters(Xpp3Dom configuration) {
        operatorContext.setConfiguration(configuration);
    }

    public Product getSourceProduct(String id) {
        return operatorContext.getSourceProduct(id);
    }

    public Product[] getSourceProducts() {
        return operatorContext.getSourceProducts();
    }
    
    private void initOperator() throws GraphException {
        final OperatorSpiRegistry spiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        OperatorSpi operatorSpi = spiRegistry.getOperatorSpi(node.getOperatorName());
        if (operatorSpi == null) {
            throw new GraphException("SPI not found for operator '" + node.getOperatorName() + "'");
        }

        try {
            this.operator = operatorSpi.createOperator();
            this.operator.setLogger(graphContext.getLogger());
            // this.operator.setConfiguration(node.getConfiguration());
        } catch (OperatorException e) {
            throw new GraphException("Failed to create instance of operator '" + node.getOperatorName() + "'");
        }
    }

    private void initOperatorContext() {
        try {
            Field field = Operator.class.getDeclaredField("context");
            field.setAccessible(true);
            operatorContext = (OperatorContext) field.get(operator);
            field.setAccessible(false);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    public synchronized void dispose() {
        if (operatorContext != null && !operatorContext.isDisposed()) {
            operatorContext.dispose(); // disposes operator as well
            operatorContext = null;
            operator = null;
        }
        if (targetProduct != null) {
            targetProduct.dispose();
            targetProduct = null;
        }
    }
}
