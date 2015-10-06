package org.esa.snap.core.gpf.descriptor;

import org.esa.snap.framework.datamodel.Product;

/**
 * Target product element metadata.
 *
 * @author Norman Fomferra
 * @since BEAM 5
 */
public interface TargetProductDescriptor extends DataElementDescriptor {
    /**
     * @return The target product type.
     * Defaults to {@link org.esa.snap.framework.datamodel.Product}.
     */
    Class<? extends Product> getDataType();
}
