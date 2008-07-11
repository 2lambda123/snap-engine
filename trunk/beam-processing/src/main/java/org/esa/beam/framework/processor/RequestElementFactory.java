/*
 * $Id: RequestElementFactory.java,v 1.2 2007/04/13 14:33:02 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.processor;

import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;

import java.io.File;

/**
 * The factory for the elements of a processing request. A <code>RequestElementFactory</code> allows a particular
 * processor implementation to influence the process of generating request elements with a <code>RequestLoader</code>.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see RequestLoader
 * @see Request
 */
public interface RequestElementFactory {
    /**
     * Creates a new reference to an input product for the current processing request.
     *
     * @param file       the input product's file, must not be <code>null</code>
     * @param fileFormat the file format, can be <code>null</code> if not known
     * @param typeId     the product type identifier, can be <code>null</code> if not known
     * @throws IllegalArgumentException       if <code>url</code> is <code>null</code>
     * @throws RequestElementFactoryException if the element could not be created
     */
    ProductRef createInputProductRef(File file, String fileFormat, String typeId)
            throws RequestElementFactoryException;

    /**
     * Creates a new reference to an output product for the current processing request.
     *
     * @param file       the output product's file, must not be <code>null</code>
     * @param fileFormat the file format, can be <code>null</code> if not known
     * @param typeId     the product type identifier, can be <code>null</code> if not known
     * @throws IllegalArgumentException       if <code>url</code> is <code>null</code>
     * @throws RequestElementFactoryException if the element could not be created
     */
    ProductRef createOutputProductRef(File file, String fileFormat, String typeId)
            throws RequestElementFactoryException;

    /**
     * Creates a new processing parameter for the current processing request.
     *
     * @param name  the parameter name, must not be <code>null</code> or empty
     * @param value the parameter value, can be <code>null</code> if yet not known
     * @throws IllegalArgumentException       if <code>name</code> is <code>null</code> or empty
     * @throws RequestElementFactoryException if the parameter could not be created or is invalid
     */
    Parameter createParameter(String name, String value)
            throws RequestElementFactoryException;

    /**
     * Creates a parameter for the default input product path - which is the current user's home directory.
     */
    Parameter createDefaultInputProductParameter();

    /**
     * Creates an output product parameter set to the default path.
     */
    Parameter createDefaultOutputProductParameter();

    /**
     * Creates a default logging pattern parameter set to the prefix passed in.
     *
     * @param prefix the default setting for the logging pattern
     * @return a logging pattern Parameter conforming the system settings
     */
    Parameter createDefaultLogPatternParameter(String prefix);

    /**
     * Creates a logging to output product parameter set to true.
     *
     * @return the created logging to output product parameter.
     */
    Parameter createLogToOutputParameter(String value) throws ParamValidateException;
}
