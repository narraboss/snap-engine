/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.binning.operator;

import com.bc.ceres.binding.BindingException;
import org.esa.beam.binning.BinManager;
import org.esa.beam.binning.CompositingType;
import org.esa.beam.binning.PlanetaryGrid;
import org.esa.beam.binning.VariableContext;
import org.esa.beam.binning.aggregators.AggregatorAverage;
import org.esa.beam.binning.aggregators.AggregatorMinMax;
import org.esa.beam.binning.aggregators.AggregatorOnMaxSet;
import org.esa.beam.binning.support.PlateCarreeGrid;
import org.esa.beam.binning.support.SEAGrid;
import org.esa.beam.util.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.*;

public class BinningConfigTest {

    private static BinningConfig config;

    @BeforeClass
    public static void initBinningConfig() throws IOException, BindingException {
        config = loadConfig(BinningConfigTest.class.getResourceAsStream("BinningConfigTest.xml"));
    }

    @Test
    public void testPlanetaryGrid() {
        BinningConfig localConfig = new BinningConfig();
        PlanetaryGrid grid = localConfig.createPlanetaryGrid();
        assertEquals(2160, grid.getNumRows());
        assertEquals(SEAGrid.class, grid.getClass());

        localConfig.setPlanetaryGrid("org.esa.beam.binning.support.PlateCarreeGrid");
        localConfig.setNumRows(2000);
        grid = localConfig.createPlanetaryGrid();
        assertEquals(2000, grid.getNumRows());
        assertEquals(PlateCarreeGrid.class, grid.getClass());

        grid = config.createPlanetaryGrid();
        assertEquals(4320, grid.getNumRows());
        assertEquals(SEAGrid.class, grid.getClass());
    }

    @Test
    public void testCompositingType() throws Exception {
        assertEquals(CompositingType.MOSAICKING, config.getCompositingType());
    }

    @Test
    public void testResultingVariableContext() {
        VariableContext variableContext = config.createVariableContext();

        assertEquals(6, variableContext.getVariableCount());

        assertEquals(0, variableContext.getVariableIndex("ndvi"));
        assertEquals(1, variableContext.getVariableIndex("tsm"));
        assertEquals(2, variableContext.getVariableIndex("reflec_3"));
        assertEquals(3, variableContext.getVariableIndex("reflec_7"));
        assertEquals(4, variableContext.getVariableIndex("reflec_8"));
        assertEquals(-1, variableContext.getVariableIndex("reflec_6"));
        assertEquals(-1, variableContext.getVariableIndex("reflec_10"));

        assertEquals("!l2_flags.INVALID && l2_flags.WATER", variableContext.getValidMaskExpression());

        assertEquals("ndvi", variableContext.getVariableName(0));
        assertEquals("(reflec_10 - reflec_6) / (reflec_10 + reflec_6)", variableContext.getVariableExpression(0));

        assertEquals("reflec_7", variableContext.getVariableName(3));
        assertEquals(null, variableContext.getVariableExpression(3));
    }

    @Test
    public void testResultingBinManager() {
        BinManager binManager = config.createBinningContext(null, null, null).getBinManager();
        assertEquals(3, binManager.getAggregatorCount());

        assertEquals(AggregatorAverage.class, binManager.getAggregator(0).getClass());
        assertArrayEquals(new String[]{"tsm_mean", "tsm_sigma"}, binManager.getAggregator(0).getOutputFeatureNames());

        assertEquals(AggregatorOnMaxSet.class, binManager.getAggregator(1).getClass());
        assertArrayEquals(new String[]{"ndvi_max", "ndvi_mjd", "reflec_3", "reflec_7", "reflec_8"},
                          binManager.getAggregator(1).getOutputFeatureNames());

        assertEquals(AggregatorMinMax.class, binManager.getAggregator(2).getClass());
        assertArrayEquals(new String[]{"chl_min", "chl_max"}, binManager.getAggregator(2).getOutputFeatureNames());

        assertArrayEquals(new String[]{"tsm_mean", "tsm_sigma",
                "ndvi_max", "ndvi_mjd", "reflec_3", "reflec_7", "reflec_8",
                "chl_min", "chl_max"
        }, binManager.getResultFeatureNames());
        assertFalse(binManager.hasPostProcessor());
    }

    @Test
    public void testXmlGeneration() throws BindingException {
        final String xml = config.toXml();
        //System.out.println("xml = \n" + xml);
        final BinningConfig configCopy = BinningConfig.fromXml(xml);

        assertEquals(config.getNumRows(), configCopy.getNumRows());
        assertEquals(config.getCompositingType(), configCopy.getCompositingType());
        assertEquals(config.getSupersampling(), configCopy.getSupersampling());
        assertEquals(config.getMaskExpr(), configCopy.getMaskExpr());
        assertArrayEquals(config.getVariableConfigs(), configCopy.getVariableConfigs());
        assertArrayEquals(config.getAggregatorConfigs(), configCopy.getAggregatorConfigs());
    }

    @Test
    public void testNumRows() {
        assertEquals(4320, config.getNumRows());
    }

    static BinningConfig loadConfig(InputStream is) throws IOException, BindingException {
        try (InputStreamReader reader = new InputStreamReader(is)) {
            return BinningConfig.fromXml(FileUtils.readText(reader));
        }
    }

}
