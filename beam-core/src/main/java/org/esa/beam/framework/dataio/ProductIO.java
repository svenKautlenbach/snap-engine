/*
 * $Id: ProductIO.java,v 1.9 2007/04/17 10:03:50 marcop Exp $
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
package org.esa.beam.framework.dataio;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * The <code>ProductIO</code> class provides several utility methods concerning data I/O for remote sensing data
 * products.
 * <p/>
 * <p> For example, a product can be read in using a single method call:
 * <pre>
 *      Product product =  ProductIO.readProduct("test.prd", null);
 * </pre>
 * and written out in a similar way:
 * <pre>
 *      ProductIO.writeProduct(product, "HDF5", "test.h5", null);
 * </pre>
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision: 1.9 $ $Date: 2007/04/17 10:03:50 $
 */
public class ProductIO {

    /**
     * The name of the default product format.
     */
    public static final String DEFAULT_FORMAT_NAME = DimapProductConstants.DIMAP_FORMAT_NAME;

    /**
     * Gets a product reader for the given format name.
     *
     * @param formatName the product format name
     * @return a suitable product reader or <code>null</code> if none was found
     */
    public static ProductReader getProductReader(String formatName) {
        ProductIOPlugInManager registry = ProductIOPlugInManager.getInstance();
        Iterator it = registry.getReaderPlugIns(formatName);
        if (it.hasNext()) {
            ProductReaderPlugIn plugIn = (ProductReaderPlugIn) it.next();
            return plugIn.createReaderInstance();
        }
        return null;
    }

    /**
     * Gets an array of writer product file extensions for the given format name.
     *
     * @param formatName the format name
     * @return an array of extensions or null if the format does not exist
     */
    public static String[] getProducWritertExtensions(String formatName) {
        ProductIOPlugInManager registry = ProductIOPlugInManager.getInstance();
        Iterator it = registry.getWriterPlugIns(formatName);
        if (it.hasNext()) {
            ProductWriterPlugIn plugIn = (ProductWriterPlugIn) it.next();
            return plugIn.getDefaultFileExtensions();
        }
        return null;
    }

    /**
     * Gets a product writer for the given format name.
     *
     * @param formatName the product format name
     * @return a suitable product writer or <code>null</code> if none was found
     */
    public static ProductWriter getProductWriter(String formatName) {
        ProductIOPlugInManager registry = ProductIOPlugInManager.getInstance();
        Iterator it = registry.getWriterPlugIns(formatName);
        if (it.hasNext()) {
            ProductWriterPlugIn plugIn = (ProductWriterPlugIn) it.next();
            return plugIn.createWriterInstance();
        }
        return null;
    }

    /**
     * Reads the data product specified by the given file.
     * <p>The returned product will be associated with a reader capable of decoding the file (also
     * see {@link org.esa.beam.framework.datamodel.Product#getProductReader() Product.productReader}).
     * If more than one appropriate reader exists in the registry, the returned product will be
     * associated with the reader which is the most preferred according to the product format names
     * supplied as last argument. If no reader capable of decoding the file is capable of handling
     * any of these product formats, the returned product will be associated with the first reader
     * found in the registry which is capable of decoding the file.</p>
     * <p/>
     * <p>The method does not automatically load band raster data, so
     * {@link org.esa.beam.framework.datamodel.Band#getRasterData() Band.rasterData} will always be null
     * for all bands in the product returned by this method.</p>
     * <p> The given subset info can be used to specify spatial and spectral portions of the original
     * proudct. If the subset is omitted, the complete product is read in.
     *
     * @param file        the data product file
     * @param subsetDef   the optional spectral and spatial subset, can be <code>null</code> in order to
     *                    accept all data in the original data product.
     * @param formatNames a list of product format names defining the preference, if more than one reader
     *                    found in the registry is capable of decoding the file.
     * @return a data model as an in-memory representation of the given product file or <code>null</code>,
     *         if no appropriate reader was found for the given product file
     * @throws IOException if an I/O error occurs
     * @see #readProduct(String, ProductSubsetDef)
     * @see #readProduct(URL, ProductSubsetDef)
     * @since 4.0
     */
    public static Product readProduct(File file, ProductSubsetDef subsetDef, String[] formatNames) throws IOException {
        Guardian.assertNotNull("file", file);

        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + file.getPath());
        }

        final ProductIOPlugInManager registry = ProductIOPlugInManager.getInstance();

        for (String formatName : formatNames) {
            final Iterator it = registry.getReaderPlugIns(formatName);

            ProductReaderPlugIn selectedPlugIn = null;
            while (it.hasNext()) {
                ProductReaderPlugIn plugIn = (ProductReaderPlugIn) it.next();
                DecodeQualification decodeQualification = plugIn.getDecodeQualification(file);
                if (decodeQualification == DecodeQualification.INTENDED) {
                    selectedPlugIn = plugIn;
                    break;
                } else if (decodeQualification == DecodeQualification.SUITABLE) {
                    selectedPlugIn = plugIn;
                }
            }
            if (selectedPlugIn != null) {
                ProductReader productReader = selectedPlugIn.createReaderInstance();
                if (productReader != null) {
                    return productReader.readProductNodes(file, subsetDef);
                }
            }
        }

        return readProduct(file, subsetDef);
    }

    /**
     * Reads the data product specified by the given file path.
     * <p>The product returned will be associated with the reader appropriate for the given
     * file format (see also {@link org.esa.beam.framework.datamodel.Product#getProductReader() Product.productReader}).</p>
     * <p>The method does not automatically read band data, thus
     * {@link org.esa.beam.framework.datamodel.Band#getRasterData() Band.rasterData} will always be null
     * for all bands in the product returned by this method.</p>
     * <p> The given subset info can be used to specify spatial and spectral portions of the original proudct. If the
     * subset is omitted, the complete product is read in.
     *
     * @param filePath  the data product file path
     * @param subsetDef the optional spectral and spatial subset, can be <code>null</code> in order to accept all data
     *                  in the original data product.
     * @return a data model as an in-memory representation of the given product file or <code>null</code> if no
     *         appropriate reader was found for the given product file
     * @throws IOException if an I/O error occurs
     * @see #readProduct(File, ProductSubsetDef)
     * @see #readProduct(URL, ProductSubsetDef)
     */
    public static Product readProduct(String filePath, ProductSubsetDef subsetDef) throws IOException {
        return readProduct(new File(filePath), subsetDef);
    }

    /**
     * Reads the data product specified by the given file.
     * <p>The product returned will be associated with the reader appropriate for the given
     * file format (see also {@link org.esa.beam.framework.datamodel.Product#getProductReader() Product.productReader}).</p>
     * <p>The method does not automatically read band data, thus
     * {@link org.esa.beam.framework.datamodel.Band#getRasterData() Band.rasterData} will always be null
     * for all bands in the product returned by this method.</p>
     * <p> The given subset info can be used to specify spatial and spectral portions of the original proudct. If the
     * subset is omitted, the complete product is read in.
     *
     * @param file      the data product file
     * @param subsetDef the optional spectral and spatial subset, can be <code>null</code> in order to accept all data
     *                  in the original data product.
     * @return a data model as an in-memory representation of the given product file or <code>null</code> if no
     *         appropriate reader was found for the given product file
     * @throws IOException if an I/O error occurs
     * @see #readProduct(String, ProductSubsetDef)
     * @see #readProduct(URL, ProductSubsetDef)
     */
    public static Product readProduct(File file, ProductSubsetDef subsetDef) throws IOException {
        Guardian.assertNotNull("file", file);
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + file.getPath());
        }
        final ProductReader productReader = getProductReaderForFile(file);
        if (productReader != null) {
            return productReader.readProductNodes(file, subsetDef);
        }
        return null;
    }

    /**
     * Returns a product reader instance for the given file if any registered product reader can decode the given file.
     *
     * @param file the file to decode.
     * @return a product reader for the given file or <code>null</code> if the file cannot be decoded.
     */
    public static ProductReader getProductReaderForFile(File file) {
        ProductIOPlugInManager registry = ProductIOPlugInManager.getInstance();
        Iterator it = registry.getAllReaderPlugIns();
        ProductReaderPlugIn selectedPlugIn = null;
        while (it.hasNext()) {
            ProductReaderPlugIn plugIn = (ProductReaderPlugIn) it.next();
            DecodeQualification decodeQualification = plugIn.getDecodeQualification(file);
            if (decodeQualification == DecodeQualification.INTENDED) {
                selectedPlugIn = plugIn;
                break;
            } else if (decodeQualification == DecodeQualification.SUITABLE) {
                selectedPlugIn = plugIn;
            }
        }
        if (selectedPlugIn != null) {
            return selectedPlugIn.createReaderInstance();
        }
        return null;
    }

    /**
     * Reads the data product specified by the given URL.
     * <p>The product returned will be associated with the reader appropriate for the given
     * file format (see also {@link org.esa.beam.framework.datamodel.Product#getProductReader() Product.productReader}).</p>
     * <p>The method does not automatically read band data, thus
     * {@link org.esa.beam.framework.datamodel.Band#getRasterData() Band.rasterData} will always be null
     * for all bands in the product returned by this method.</p>
     * <p/>
     * <p><b>WARNING:</b> Only URLs representing files in the local filesystem are currently supported.
     * <p/>
     * <p> The given subset info can be used to specify spatial and spectral portions of the original proudct. If the
     * subset is omitted, the complete product is read in.
     *
     * @param url       the data product's URL
     * @param subsetDef the optional spectral and spatial subset, can be <code>null</code> in order to accept all data
     *                  in the original data product.
     * @return a data model as an in-memory representation of the given product file or <code>null</code> if no
     *         appropriate reader was found for the given product file
     * @throws IOException if an I/O error occurs
     * @see #readProduct(String, ProductSubsetDef)
     * @see #readProduct(File, ProductSubsetDef)
     */
    public static Product readProduct(URL url, ProductSubsetDef subsetDef) throws IOException {
        Debug.trace("WARNING: general URLs are currently not supported by the ProductIO.readProductNodes method");
        try {
            return readProduct(new File(url.toURI()), subsetDef);
        } catch (URISyntaxException e) {
            IOException ioe = new IOException("URL not valid [" + url + "]");
            ioe.initCause(e);
            throw ioe;
        }
    }

    /**
     * Writes a product with the specified format to the given file path.
     * <p>The method also writes all band data to the file. Therefore the band data must either
     * <ld>
     * <li>be completely loaded ({@link org.esa.beam.framework.datamodel.Band#getRasterData() Band.rasterData} is not null)</li>
     * <li>or the product must be associated with a product reader ({@link org.esa.beam.framework.datamodel.Product#getProductReader() Product.productReader} is not null) so that unloaded data can be reloaded.</li>
     * </ld>.
     *
     * @param product    the product, must not be <code>null</code>
     * @param filePath   the file path
     * @param formatName the name of a supported product format, e.g. "HDF5". If <code>null</code>, the default format
     *                   "BEAM-DIMAP" will be used
     * @throws IOException if an IOException occurs
     */
    public static void writeProduct(Product product,
                                    String filePath,
                                    String formatName) throws IOException {
        writeProduct(product, new File(filePath), formatName, false, ProgressMonitor.NULL);
    }

    /**
     * Writes a product with the specified format to the given file path.
     * <p>The method also writes all band data to the file. Therefore the band data must either
     * <ld>
     * <li>be completely loaded ({@link org.esa.beam.framework.datamodel.Band#getRasterData() Band.rasterData} is not null)</li>
     * <li>or the product must be associated with a product reader ({@link org.esa.beam.framework.datamodel.Product#getProductReader() Product.productReader} is not null) so that unloaded data can be reloaded.</li>
     * </ld>.
     *
     * @param product    the product, must not be <code>null</code>
     * @param filePath   the file path
     * @param formatName the name of a supported product format, e.g. "HDF5". If <code>null</code>, the default format
     *                   "BEAM-DIMAP" will be used
     * @param pm         a monitor to inform the user about progress
     * @throws IOException if an IOException occurs
     */
    public static void writeProduct(Product product,
                                    String filePath,
                                    String formatName,
                                    ProgressMonitor pm) throws IOException {
        writeProduct(product, new File(filePath), formatName, false, pm);
    }

    /**
     * Writes a product with the specified format to the given file.
     * <p>The method also writes all band data to the file. Therefore the band data must either
     * <ld>
     * <li>be completely loaded ({@link org.esa.beam.framework.datamodel.Band#getRasterData() Band.rasterData} is not null)</li>
     * <li>or the product must be associated with a product reader ({@link org.esa.beam.framework.datamodel.Product#getProductReader() Product.productReader} is not null) so that unloaded data can be reloaded.</li>
     * </ld>.
     *
     * @param product     the product, must not be <code>null</code>
     * @param file        the product file , must not be <code>null</code>
     * @param formatName  the name of a supported product format, e.g. "HDF5". If <code>null</code>, the default format
     *                    "BEAM-DIMAP" will be used
     * @param incremental switch the product writer in incremental mode or not.
     * @throws IOException if an IOException occurs
     */
    public static void writeProduct(Product product,
                                    File file,
                                    String formatName,
                                    boolean incremental) throws IOException {
        writeProduct(product, file, formatName, incremental, ProgressMonitor.NULL);
    }

    /**
     * Writes a product with the specified format to the given file.
     * <p>The method also writes all band data to the file. Therefore the band data must either
     * <ld>
     * <li>be completely loaded ({@link org.esa.beam.framework.datamodel.Band#getRasterData() Band.rasterData} is not null)</li>
     * <li>or the product must be associated with a product reader ({@link org.esa.beam.framework.datamodel.Product#getProductReader() Product.productReader} is not null) so that unloaded data can be reloaded.</li>
     * </ld>.
     *
     * @param product     the product, must not be <code>null</code>
     * @param file        the product file , must not be <code>null</code>
     * @param formatName  the name of a supported product format, e.g. "HDF5". If <code>null</code>, the default format
     *                    "BEAM-DIMAP" will be used
     * @param incremental switch the product writer in incremental mode or not.
     * @param pm          a monitor to inform the user about progress
     * @throws IOException if an IOException occurs
     */
    public static void writeProduct(Product product,
                                    File file,
                                    String formatName,
                                    boolean incremental,
                                    ProgressMonitor pm) throws IOException {
        Guardian.assertNotNull("product", product);
        Guardian.assertNotNull("file", file);
        if (formatName == null) {
            formatName = DEFAULT_FORMAT_NAME;
        }
        ProductWriter productWriter = getProductWriter(formatName);
        if (productWriter == null) {
            throw new ProductIOException("no product writer for the '" + formatName + "' format available");
        }
        productWriter.setIncrementalMode(incremental);

        ProductWriter productWriterOld = product.getProductWriter();
        product.setProductWriter(productWriter);

        IOException ioException = null;
        try {
            productWriter.writeProductNodes(product, file);
            writeAllBands(product, pm);
        } catch (IOException e) {
            ioException = e;
        } finally {
            try {
                product.closeProductWriter();
            } catch (IOException e) {
                if (ioException == null) {
                    ioException = e;
                }
            }
            product.setProductWriter(productWriterOld);
        }

        if (ioException != null) {
            throw ioException;
        }
    }

    /**
     * This implementation helper methods writes all bands of the given product using the specified product writer. If a
     * band is entirely loaded its data is written out immediately, if not, a band's data raster is written out
     * line-by-line without producing any memory overhead.
     */
    private static void writeAllBands(Product product, ProgressMonitor pm) throws IOException {
        ProductWriter productWriter = product.getProductWriter();

        // for correct progress indication we need to collect
        // all bands which shall be written to the output
        ArrayList<Band> bandsToWrite = new ArrayList<Band>();
        for (int i = 0; i < product.getNumBands(); i++) {
            Band band = product.getBandAt(i);
            if (productWriter.shouldWrite(band)) {
                bandsToWrite.add(band);
            }
        }

        if (bandsToWrite.size() > 0) {
            pm.beginTask("Writing bands of product '" + product.getName() + "'...", bandsToWrite.size());
            try {
                for (int i = 0; i < bandsToWrite.size(); i++) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    Band band = bandsToWrite.get(i);
                    band.writeRasterDataFully(new SubProgressMonitor(pm, 1));
                }
            } finally {
                pm.done();
            }
        }
    }

    /**
     * Constructor. Private, in order to prevent instantiation.
     */
    private ProductIO() {
    }
}
