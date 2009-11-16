/*
 * $Id: $
 *
 * Copyright (C) 2008 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation. This program is distributed in the hope it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.esa.beam.framework.gpf.operators.common;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.GlobalTestConfig;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.Tile.Pos;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.gpf.graph.Graph;
import org.esa.beam.framework.gpf.graph.GraphContext;
import org.esa.beam.framework.gpf.graph.GraphIO;
import org.esa.beam.framework.gpf.graph.GraphProcessor;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.File;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogManager;

/**
 * Created by marcoz.
 *
 * @author marcoz
 * @version $Revision$ $Date$
 */
public class WriteOpTest extends TestCase {
    private static final int RASTER_SIZE = 4;
    private MockFillerOp.Spi fillOpSpi = new MockFillerOp.Spi();
    private WriteOp.Spi writeSpi = new WriteOp.Spi();
    private File outputFile;

    @Override
    protected void setUp() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(fillOpSpi);
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(writeSpi);
        outputFile = GlobalTestConfig.getBeamTestDataOutputFile("DIMAP/writtenProduct.dim");
        outputFile.getParentFile().mkdirs();
        outputFile.createNewFile();
    }

    @Override
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(fillOpSpi);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(writeSpi);
        File parentFile = outputFile.getParentFile();
        SystemUtils.deleteFileTree(parentFile);
    }

    public void testWrite() throws Exception {
        String graphOpXml = "<graph id=\"myOneNodeGraph\">\n"
                + "  <version>1.0</version>\n"
                + "  <header>\n"
                + "    <target refid=\"node2\" />\n"
                + "  </header>\n"
                + "  <node id=\"node1\">\n"
                + "    <operator>MockFiller</operator>\n"
                + "  </node>\n"
                + "  <node id=\"node2\">\n"
                + "    <operator>Write</operator>\n"
                + "    <sources>\n"
                + "      <input refid=\"node1\"/>\n"
                + "    </sources>\n"
                + "    <parameters>\n"
                + "       <file>" + outputFile.getAbsolutePath() + "</file>\n"
                + "       <deleteOutputOnFailure>false</deleteOutputOnFailure>\n"
                + "    </parameters>\n"
                + "  </node>\n"
                + "</graph>";
        StringReader reader = new StringReader(graphOpXml);
        Graph graph = GraphIO.read(reader);

        GraphProcessor processor = new GraphProcessor();
        GraphContext graphContext = processor.createGraphContext(graph, ProgressMonitor.NULL);
        processor.executeGraphContext(graphContext, ProgressMonitor.NULL);
        Product[] outputProducts = graphContext.getOutputProducts();
        outputProducts[0].dispose();

        Product productOnDisk = ProductIO.readProduct(outputFile, null);
        assertNotNull(productOnDisk);
        final ProductNodeGroup<Pin> pinProductNodeGroup = productOnDisk.getPinGroup();
        assertEquals(4, pinProductNodeGroup.getNodeCount());     // one for each tile, we have 4 tiles
        assertEquals("writtenProduct", productOnDisk.getName());
        assertEquals(1, productOnDisk.getNumBands());
        Band band1 = productOnDisk.getBandAt(0);
        assertEquals("Op1A", band1.getName());
        assertEquals(RASTER_SIZE, band1.getSceneRasterWidth());
        band1.loadRasterData();
        assertEquals(42, band1.getPixelInt(0, 0));
        productOnDisk.dispose();
    }

    @OperatorMetadata(alias = "MockFiller")
    public static class MockFillerOp extends Operator {

        @TargetProduct
        private Product targetProduct;

        @Override
        public void initialize() {
            targetProduct = new Product("Op1Name", "Op1Type", RASTER_SIZE, RASTER_SIZE);
            targetProduct.addBand(new Band("Op1A", ProductData.TYPE_INT8, RASTER_SIZE, RASTER_SIZE));
            targetProduct.setPreferredTileSize(2, 2);
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) {
            //System.out.println(getClass().getName() + ":");
            //System.out.println("  band = " + band);
            //System.out.println("  targetTile = " + targetTile.getRectangle());
            for (Pos pos : targetTile) {
                targetTile.setSample(pos.x, pos.y, 42);
            }
            final int minX = targetTile.getMinX();
            final int minY = targetTile.getMinY();
            final PlacemarkSymbol symbol = PinDescriptor.INSTANCE.createDefaultSymbol();
            band.getProduct().getPinGroup().add(new Pin(band.getName() + minX + "," + minY,
                                                        "label", "descr",
                                                        new PixelPos(minX, minY), null,
                                                        symbol));
        }

        public static class Spi extends OperatorSpi {

            public Spi() {
                super(MockFillerOp.class);
            }
        }
    }

}
