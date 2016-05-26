package org.esa.s3tbx.c2rcc.olci;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Marco Peters
 */
@Ignore
public class OlciProductSignatureTest {
    private static final String[] EXPECTED_REFLEC_BANDS = {
            "rwa_" + 1, "rwa_" + 2, "rwa_" + 3, "rwa_" + 4, "rwa_" + 5,
            "rwa_" + 6, "rwa_" + 7, "rwa_" + 8, "rwa_" + 9, "rwa_" + 10,
            "rwa_" + 12, "rwa_" + 16, "rwa_" + 17, "rwa_" + 18, "rwa_" + 21};
    private static final String[] EXPECTED_NORM_REFLEC_BANDS = {
            "rwn_" + 1, "rwn_" + 2, "rwn_" + 3, "rwn_" + 4, "rwn_" + 5,
            "rwn_" + 6, "rwn_" + 7, "rwn_" + 8, "rwn_" + 9, "rwn_" + 10,
            "rwn_" + 12, "rwn_" + 16, "rwn_" + 17, "rwn_" + 18, "rwn_" + 21};
    private static final String EXPECTED_IOP_APIG = "iop_apig";
    private static final String EXPECTED_IOP_ADET = "iop_adet";
    private static final String EXPECTED_IOP_AGELB = "iop_agelb";
    private static final String EXPECTED_IOP_BPART = "iop_bpart";
    private static final String EXPECTED_IOP_BWIT = "iop_bwit";
    private static final String EXPECTED_IOP_ADG = "iop_adg";
    private static final String EXPECTED_IOP_ATOT = "iop_atot";
    private static final String EXPECTED_IOP_BTOT = "iop_btot";
    private static final String EXPECTED_CONC_CHL = "conc_chl";
    private static final String EXPECTED_CONC_TSM = "conc_tsm";
    private static final String[] EXPECTED_KD_BANDS = {"kd489", "kdmin", "kd_z90max"};
    private static final String[] EXPECTED_OOS_BANDS = {"oos_rtosa", "oos_rwa"};
    private static final String[] EXPECTED_IOP_UNC_BANDS = {
            "unc_apig", "unc_adet", "unc_agelb", "unc_bpart",
            "unc_bwit", "unc_adg", "unc_atot", "unc_btot"};
    private static final String[] EXPECTED_KD_UNC_BANDS = {"unc_kd489", "unc_kdmin"};
    private static final String[] EXPECTED_RTOSA_GC_BANDS = {
            "rtosa_gc_" + 1, "rtosa_gc_" + 2, "rtosa_gc_" + 3, "rtosa_gc_" + 4, "rtosa_gc_" + 5,
            "rtosa_gc_" + 6, "rtosa_gc_" + 7, "rtosa_gc_" + 8, "rtosa_gc_" + 9, "rtosa_gc_" + 10,
            "rtosa_gc_" + 12, "rtosa_gc_" + 16, "rtosa_gc_" + 17, "rtosa_gc_" + 18, "rtosa_gc_" + 21};
    private static final String[] EXPECTED_RTOSA_GCAANN_BANDS = {
            "rtosagc_aann_" + 1, "rtosagc_aann_" + 2, "rtosagc_aann_" + 3, "rtosagc_aann_" + 4, "rtosagc_aann_" + 5,
            "rtosagc_aann_" + 6, "rtosagc_aann_" + 7, "rtosagc_aann_" + 8, "rtosagc_aann_" + 9, "rtosagc_aann_" + 10,
            "rtosagc_aann_" + 12, "rtosagc_aann_" + 16, "rtosagc_aann_" + 17, "rtosagc_aann_" + 18, "rtosagc_aann_" + 21};
    private static final String[] EXPECTED_RTOA_BANDS = {
            "rtoa_" + 1, "rtoa_" + 2, "rtoa_" + 3, "rtoa_" + 4, "rtoa_" + 5,
            "rtoa_" + 6, "rtoa_" + 7, "rtoa_" + 8, "rtoa_" + 9, "rtoa_" + 10,
            "rtoa_" + 11, "rtoa_" + 12, "rtoa_" + 13, "rtoa_" + 14, "rtoa_" + 15,
            "rtoa_" + 16, "rtoa_" + 17, "rtoa_" + 18, "rtoa_" + 19, "rtoa_" + 20, "rtoa_" + 21};
    private static final String[] EXPECTED_RPATH_BANDS = {
            "rpath_" + 1, "rpath_" + 2, "rpath_" + 3, "rpath_" + 4, "rpath_" + 5,
            "rpath_" + 6, "rpath_" + 7, "rpath_" + 8, "rpath_" + 9, "rpath_" + 10,
            "rpath_" + 12, "rpath_" + 16, "rpath_" + 17, "rpath_" + 18, "rpath_" + 21};
    private static final String[] EXPECTED_TDOWN_BANDS = {
            "tdown_" + 1, "tdown_" + 2, "tdown_" + 3, "tdown_" + 4, "tdown_" + 5,
            "tdown_" + 6, "tdown_" + 7, "tdown_" + 8, "tdown_" + 9, "tdown_" + 10,
            "tdown_" + 12, "tdown_" + 16, "tdown_" + 17, "tdown_" + 18, "tdown_" + 21};
    private static final String[] EXPECTED_TUP_BANDS = {
            "tup_" + 1, "tup_" + 2, "tup_" + 3, "tup_" + 4, "tup_" + 5,
            "tup_" + 6, "tup_" + 7, "tup_" + 8, "tup_" + 9, "tup_" + 10,
            "tup_" + 12, "tup_" + 16, "tup_" + 17, "tup_" + 18, "tup_" + 21};

    private static final String EXPECTED_QUALITY_FLAGS = "quality_flags";
    private static final String EXPECTED_L2_FLAGS = "l2_flags";

    @Test
    public void testProductSignature_Default() throws FactoryException, TransformException {

        C2rccOlciOperator operator = createDefaultOperator();

        Product targetProduct = operator.getTargetProduct();

        assertDefaultBands(targetProduct);
    }

    @Test
    public void testProductSignature_OnlyMandatory() throws FactoryException, TransformException {

        C2rccOlciOperator operator = createDefaultOperator();
        operator.setOutputRwa(false);
        operator.setOutputRwn(false);
        operator.setOutputKd(false);
        Product targetProduct = operator.getTargetProduct();

        assertMandatoryBands(targetProduct);
        assertEquals(12, targetProduct.getNumBands());
    }

    @Test
    public void testProductSignature_DefaultWithRtosa() throws FactoryException, TransformException {

        C2rccOlciOperator operator = createDefaultOperator();
        operator.setOutputRtosa(true);
        operator.setOutputRtosaGcAann(true);
        Product targetProduct = operator.getTargetProduct();

        assertDefaultBands(targetProduct);
        assertBands(targetProduct, EXPECTED_RTOSA_GC_BANDS);
        assertBands(targetProduct, EXPECTED_RTOSA_GCAANN_BANDS);
    }

    @Test
    public void testProductSignature_DefaultWithRtoa() throws FactoryException, TransformException {

        C2rccOlciOperator operator = createDefaultOperator();
        operator.setOutputRtoa(true);
        Product targetProduct = operator.getTargetProduct();

        assertDefaultBands(targetProduct);
        assertBands(targetProduct, EXPECTED_RTOA_BANDS);
    }

    @Test
    public void testProductSignature_DefaultWithOthers() throws FactoryException, TransformException {

        C2rccOlciOperator operator = createDefaultOperator();
        operator.setOutputRpath(true);
        operator.setOutputTdown(true);
        operator.setOutputTup(true);
        operator.setOutputOos(true);
        Product targetProduct = operator.getTargetProduct();

        assertDefaultBands(targetProduct);
        assertBands(targetProduct, EXPECTED_RPATH_BANDS);
        assertBands(targetProduct, EXPECTED_TDOWN_BANDS);
        assertBands(targetProduct, EXPECTED_TUP_BANDS);
        assertBands(targetProduct, EXPECTED_OOS_BANDS);
    }

    @Test
    public void testProductSignature_DefaultWithUncertainties() throws FactoryException, TransformException {

        C2rccOlciOperator operator = createDefaultOperator();
        operator.setOutputUncertainties(true);
        Product targetProduct = operator.getTargetProduct();

        assertDefaultBands(targetProduct);
        assertBands(targetProduct, EXPECTED_IOP_UNC_BANDS);
    }

    @Test
    public void testProductSignature_DefaultWithUncertaintiesAndKd() throws FactoryException, TransformException {

        C2rccOlciOperator operator = createDefaultOperator();
        operator.setOutputUncertainties(true);
        operator.setOutputKd(true);
        Product targetProduct = operator.getTargetProduct();

        assertDefaultBands(targetProduct);
        assertBands(targetProduct, EXPECTED_IOP_UNC_BANDS);
        assertBands(targetProduct, EXPECTED_KD_UNC_BANDS);
        assertBands(targetProduct, EXPECTED_KD_BANDS);
    }

    private void assertDefaultBands(Product targetProduct) {
        assertMandatoryBands(targetProduct);
        assertBands(targetProduct, EXPECTED_REFLEC_BANDS);
        assertBands(targetProduct, EXPECTED_NORM_REFLEC_BANDS);
        assertBands(targetProduct, EXPECTED_KD_BANDS);
    }

    private void assertMandatoryBands(Product targetProduct) {
        assertBands(targetProduct, EXPECTED_IOP_APIG);
        assertBands(targetProduct, EXPECTED_IOP_ADET);
        assertBands(targetProduct, EXPECTED_IOP_AGELB);
        assertBands(targetProduct, EXPECTED_IOP_BPART);
        assertBands(targetProduct, EXPECTED_IOP_BWIT);
        assertBands(targetProduct, EXPECTED_IOP_ADG);
        assertBands(targetProduct, EXPECTED_IOP_ATOT);
        assertBands(targetProduct, EXPECTED_IOP_BTOT);
        assertBands(targetProduct, EXPECTED_CONC_CHL);
        assertBands(targetProduct, EXPECTED_CONC_TSM);
        assertBands(targetProduct, EXPECTED_QUALITY_FLAGS);
        assertBands(targetProduct, EXPECTED_L2_FLAGS);
    }

    private void assertBands(Product targetProduct, String... expectedBands) {
        for (String expectedBand : expectedBands) {
            assertTrue("Expected band " + expectedBand + " in product", targetProduct.containsBand(expectedBand));
        }
    }

    private C2rccOlciOperator createDefaultOperator() throws FactoryException, TransformException {
        C2rccOlciOperator operator = new C2rccOlciOperator();
        operator.setParameterDefaultValues();
        operator.setSourceProduct(createOlciTestProduct());
        return operator;
    }

    private Product createOlciTestProduct() throws FactoryException, TransformException {
        Product product = new Product("test-meris", "t", 1, 1);
        for (int i = 1; i <= C2rccOlciOperator.BAND_COUNT; i++) {
            String expression = String.valueOf(i);
            product.addBand(String.format("Oa%02d_radiance", i), expression);
            product.addBand("solar_flux_band_" + i, expression);
        }

        product.addBand(C2rccOlciOperator.RASTER_NAME_ALTITUDE, "500");
        product.addBand(C2rccOlciOperator.RASTER_NAME_SUN_AZIMUTH, "42");
        product.addBand(C2rccOlciOperator.RASTER_NAME_SUN_ZENITH, "42");
        product.addBand(C2rccOlciOperator.RASTER_NAME_VIEWING_AZIMUTH, "42");
        product.addBand(C2rccOlciOperator.RASTER_NAME_VIEWING_ZENITH, "42");
        product.addBand(C2rccOlciOperator.RASTER_NAME_SEA_LEVEL_PRESSURE, "999");
        product.addBand(C2rccOlciOperator.RASTER_NAME_TOTAL_OZONE, "333");
        Band flagBand = product.addBand(C2rccOlciOperator.RASTER_NAME_QUALITY_FLAGS, ProductData.TYPE_INT8);
        FlagCoding l1FlagsCoding = new FlagCoding(C2rccOlciOperator.RASTER_NAME_QUALITY_FLAGS);
        product.getFlagCodingGroup().add(l1FlagsCoding);
        flagBand.setSampleCoding(l1FlagsCoding);

        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 1, 1, 10, 50, 1, 1));

        return product;
    }
}