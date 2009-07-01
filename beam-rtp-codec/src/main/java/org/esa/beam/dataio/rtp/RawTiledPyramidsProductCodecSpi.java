package org.esa.beam.dataio.rtp;

import com.thoughtworks.xstream.XStream;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.util.Locale;

public class RawTiledPyramidsProductCodecSpi implements ProductReaderPlugIn, ProductWriterPlugIn {
    public static final String HEADER_NAME = "product.xml";
    public static final String FORMAT_NAME = "RAW-TILED-PYRAMIDS";
    public static final String FORMAT_DESCRIPTION = "Raw, tiled pyramids product (experimental)";
    static final String[] NO_FILE_EXTENSIONS = new String[0];

    public DecodeQualification getDecodeQualification(Object input) {
        final File headerFile = getHeaderFile(input);
        return headerFile.isFile() ? DecodeQualification.INTENDED : DecodeQualification.UNABLE;
    }

    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class};
    }

    public Class[] getOutputTypes() {
        return getInputTypes();
    }

    public ProductReader createReaderInstance() {
        return new RawTiledPyramidsProductReader(this);
    }

    public ProductWriter createWriterInstance() {
        return new RawTiledPyramidsProductWriter(this);
    }

    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME};
    }

    public String[] getDefaultFileExtensions() {
        return NO_FILE_EXTENSIONS;
    }

    public String getDescription(Locale locale) {
        return FORMAT_DESCRIPTION;
    }

    public BeamFileFilter getProductFileFilter() {
        return new RawTiledPyramidsProductFileFilter();
    }

    static boolean isProductDir(File dir) {
        return new File(dir, HEADER_NAME).isFile();
    }

    static File getHeaderFile(Object input) {
        final File file = new File(input.toString());
        if (HEADER_NAME.equals(file.getName())) {
            return file;
        }
        return new File(file, HEADER_NAME);
    }

    static XStream createXStream() {
        final XStream xStream = new XStream();
        xStream.setClassLoader(RawTiledPyramidsProductCodecSpi.class.getClassLoader());
        xStream.processAnnotations(new Class[]{ProductDescriptor.class, BandDescriptor.class});
        return xStream;
    }

}
