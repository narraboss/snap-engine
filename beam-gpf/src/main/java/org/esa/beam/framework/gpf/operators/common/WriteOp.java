package org.esa.beam.framework.gpf.operators.common;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;

import org.esa.beam.dataio.dimap.DimapProductWriter;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.DataNode;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.gpf.internal.OperatorExecutor;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.math.MathUtils;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OperatorMetadata(alias = "Write",
                  description = "Writes a product to disk.")
public class WriteOp extends Operator {

    @TargetProduct
    private Product targetProduct;
    @SourceProduct(alias = "input")
    private Product sourceProduct;

    @Parameter(description = "The output file to which the data product is written.")
    private File file;
    @Parameter(defaultValue = ProductIO.DEFAULT_FORMAT_NAME,
               description = "The name of the output file format.")
    private String formatName;
    @Parameter(defaultValue = "true",
               description = "If true, all output files are deleted when the write operation has failed.")
    private boolean deleteOutputOnFailure;
    @Parameter(defaultValue = "true",
               description = "If true, the write operation waits until all tiles of a single line are available before writing it.")
    private boolean writeCompleteTileLines;

    private ProductWriter productWriter;
    private List<Band> writableBands;
    private boolean productFileWritten;
    private Map<MultiLevelImage, List<Point>> notComputedTileIndexList;
    private boolean headerChanged;
    private ProductNodeChangeListener productNodeChangeListener;
    private Map<BandLine, Tile[]> lineCache;
    private Dimension tileSize;
    private int tileCountX;
    

    public WriteOp() {
    }

    public WriteOp(Product sourceProduct, File file, String formatName) {
        this(sourceProduct, file, formatName, true);
    }

    public WriteOp(Product sourceProduct, File file, String formatName, boolean deleteOutputOnFailure) {
        this.sourceProduct = sourceProduct;
        this.file = file;
        this.formatName = formatName;
        this.deleteOutputOnFailure = deleteOutputOnFailure;
    }

    @Override
    public void initialize() throws OperatorException {
        targetProduct = sourceProduct;
        productWriter = ProductIO.getProductWriter(formatName);
        if (productWriter == null) {
            throw new OperatorException("No product writer for the '" + formatName + "' format available");
        }
        productWriter.setIncrementalMode(false);
        targetProduct.setProductWriter(productWriter);
        final Band[] bands = targetProduct.getBands();
        writableBands = new ArrayList<Band>(bands.length);
        for (final Band band : bands) {
            if (productWriter.shouldWrite(band)) {
                writableBands.add(band);
            }
        }
        notComputedTileIndexList = new HashMap<MultiLevelImage, List<Point>>(writableBands.size());
        headerChanged = false;
        productNodeChangeListener = new ProductNodeChangeListener();
        
        tileSize = ImageManager.getPreferredTileSize(targetProduct);
        targetProduct.setPreferredTileSize(tileSize);
        tileCountX = MathUtils.ceilInt(targetProduct.getSceneRasterWidth() / (double) tileSize.width);
        lineCache = new HashMap<BandLine, Tile[]>();
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        if (!writableBands.contains(targetBand)) {
            return;
        }
        try {
            synchronized (productWriter) {
                if (!productFileWritten) {
                    productWriter.writeProductNodes(targetProduct, file);
                    productFileWritten = true;
                    // rewrite of product header is only supported for DIMAP
                    if (productWriter instanceof DimapProductWriter) {
                        targetProduct.addProductNodeListener(productNodeChangeListener);
                    }
                }
            }
            final Rectangle rect = targetTile.getRectangle();
            final Tile sourceTile = getSourceTile(targetBand, rect, pm);
            if (writeCompleteTileLines) {
                int tileX = MathUtils.floorInt(targetTile.getMinX() / (double) tileSize.width);
                int tileY = MathUtils.floorInt(targetTile.getMinY() / (double) tileSize.height);
                BandLine key = new BandLine(targetBand, tileY);
                Tile[] cacheLine = storeTileInLineCache(key, tileX, sourceTile);
                if (cacheLine != null) {
                    writeCacheLine(targetBand, cacheLine);
                }
            } else {
                final ProductData rawSamples = sourceTile.getRawSamples();
                synchronized (productWriter) {
                    productWriter.writeBandRasterData(targetBand, rect.x, rect.y, rect.width, rect.height, rawSamples, pm);
                }
            }
            updateComputedTileMap(targetBand, targetTile);
        } catch (Exception e) {
            if (deleteOutputOnFailure) {
                try {
                    productWriter.deleteOutput();
                    productFileWritten = false;
                } catch (IOException ignored) {
                }
            }
            if (e instanceof OperatorException) {
                throw (OperatorException) e;
            } else {
                throw new OperatorException(e);
            }
        }
    }
    
    private Tile[] storeTileInLineCache(BandLine key, int tileX, Tile tile) {
        synchronized (lineCache) {
            Tile[] lineOfTiles;
            if (lineCache.containsKey(key)) {
                lineOfTiles = lineCache.get(key);
            } else {
                lineOfTiles = new Tile[tileCountX];
                lineCache.put(key, lineOfTiles);
            }
            lineOfTiles[tileX] = tile;
            for (int i = 0; i < lineOfTiles.length; i++) {
                if (lineOfTiles[i] == null) {
                    return null;
                }
            }
            lineCache.remove(key);
            return lineOfTiles;
        }
    }
    
    private void writeCacheLine(Band band, Tile[] cacheLine) throws IOException {
        Tile firstTile = cacheLine[0];
        int sceneWidth = targetProduct.getSceneRasterWidth();
        Rectangle lineBounds = new Rectangle(0, firstTile.getMinY(), sceneWidth, firstTile.getHeight());
        ProductData[] rawSampleOFLine = new ProductData[cacheLine.length];
        int[] tileWidth = new int[cacheLine.length];
        for (int tileX = 0; tileX < cacheLine.length; tileX++) {
            Tile tile = cacheLine[tileX];
            rawSampleOFLine[tileX] = tile.getRawSamples();
            tileWidth[tileX] = tile.getRectangle().width; 
        }
        ProductData sampleLine = ProductData.createInstance(rawSampleOFLine[0].getType(), sceneWidth);
        for (int y = lineBounds.y; y < lineBounds.y + lineBounds.height; y++) {
            int targetPos = 0;
            for (int tileX = 0; tileX < cacheLine.length; tileX++) {
                Object rawSamples = rawSampleOFLine[tileX].getElems();
                int width = tileWidth[tileX];
                int srcPos = (y - lineBounds.y) * width;
                System.arraycopy(rawSamples, srcPos, sampleLine.getElems(), targetPos, width);
                targetPos += width;

            }
            synchronized (productWriter) {
                productWriter.writeBandRasterData(band, 0, y, sceneWidth, 1, sampleLine, ProgressMonitor.NULL);
            }
        }
    }

    private void updateComputedTileMap(Band targetBand, Tile targetTile) throws IOException {
        synchronized (notComputedTileIndexList) {
            MultiLevelImage sourceImage = targetBand.getSourceImage();

            final List<Point> currentList = getTileList(sourceImage);
            currentList.remove(new Point(sourceImage.XToTileX(targetTile.getMinX()),
                                         sourceImage.YToTileY(targetTile.getMinY())));

            for (List<Point> points : notComputedTileIndexList.values()) {
                if (points.isEmpty()) {
                    targetProduct.removeProductNodeListener(productNodeChangeListener);
                } else { // not empty; there are more tiles to come
                    return;
                }
            }
            // If we get here all tiles are written
            if (headerChanged) {   // ask if we have to update the header
                getLogger().info("Product header changed. Overwriting " + file);
                productWriter.writeProductNodes(targetProduct, file);
            }
        }
    }


    private List<Point> getTileList(MultiLevelImage sourceImage) {
        List<Point> list = notComputedTileIndexList.get(sourceImage);
        if (list == null) {
            final int numXTiles = sourceImage.getNumXTiles();
            final int numYTiles = sourceImage.getNumYTiles();
            list = new ArrayList<Point>(numXTiles * numYTiles);
            for (int y = 0; y < numYTiles; y++) {
                for (int x = 0; x < numXTiles; x++) {
                    list.add(new Point(x, y));
                }
            }
            notComputedTileIndexList.put(sourceImage, list);
        }
        return list;
    }

    @Override
    public void dispose() {
        try {
            productWriter.close();
        } catch (IOException ignore) {
        }
        writableBands.clear();
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(WriteOp.class);
        }
    }

    public static void writeProduct(Product sourceProduct, File file, String formatName, ProgressMonitor pm) {
        writeProduct(sourceProduct, file, formatName, true, pm);
    }
    
    public static void writeProduct(Product sourceProduct, File file, String formatName, boolean deleteOutputOnFailure,
                                    ProgressMonitor pm) {

        final WriteOp writeOp = new WriteOp(sourceProduct, file, formatName, deleteOutputOnFailure);
        OperatorExecutor operatorExecutor = new OperatorExecutor(writeOp);
        try {
            operatorExecutor.execute(pm);
        } catch (OperatorException e) {
            if (deleteOutputOnFailure) {
                try {
                    writeOp.productWriter.deleteOutput();
                } catch (IOException ignored) {
                }
            }
            throw e;
        } finally {
            writeOp.logPerformanceAnalysis();
            writeOp.dispose();
        }
    }
    
    private static class BandLine {
        private final Band band;
        private final int tileY;
        
        private BandLine(Band band, int tileY) {
            this.band = band;
            this.tileY = tileY;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + band.hashCode();
            result = prime * result + tileY;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            BandLine other = (BandLine) obj;
            if (!band.equals(other.band)) {
                return false;
            }
            return tileY == other.tileY;
        }
        
        
    }
    
    private class ProductNodeChangeListener extends ProductNodeListenerAdapter {

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            if (!DataNode.PROPERTY_NAME_DATA.equalsIgnoreCase(event.getPropertyName())) {
                headerChanged = true;
            }
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            headerChanged = true;
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            headerChanged = true;
        }
    }
}
    