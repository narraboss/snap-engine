package org.esa.snap.core.dataio.cache;

import org.junit.Before;
import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SlabCacheTest {

    private DataStorage storage;

    @Before
    public void setUp() {
        storage = mock(DataStorage.class);
    }

    @Test
    public void testGet_oneSlab_created() {
        final SlabCache slabCache = new SlabCache(100, 200, 10, 10, storage);

        final Slab[] slabs = slabCache.get(2, 3, 3, 3);
        assertEquals(1, slabs.length);

        final Rectangle region = slabs[0].getRegion();
        assertEquals(0, region.x);
        assertEquals(0, region.y);
        assertEquals(10, region.width);
        assertEquals(10, region.height);

        assertTrue(slabs[0].getLastAccess() > -1);

        verify(storage, times(1)).readRasterData(eq(0), eq(0), eq(10), eq(10), anyObject());
        verifyNoMoreInteractions(storage);
    }

    @Test
    public void testGet_oneSlab_fromCached() {
        final SlabCache slabCache = new SlabCache(100, 200, 10, 10, storage);

        final Slab[] slabs = slabCache.get(2, 3, 3, 3);
        assertEquals(1, slabs.length);
        final long lastAccess = slabs[0].getLastAccess();
        assertTrue(lastAccess > -1);
        final Rectangle region = slabs[0].getRegion();
        assertEquals(0, region.x);
        assertEquals(0, region.y);
        assertEquals(10, region.width);
        assertEquals(10, region.height);

        final Slab[] slabsSecond = slabCache.get(2, 3, 3, 3);
        assertEquals(1, slabsSecond.length);
        assertSame(slabs[0], slabsSecond[0]);
        assertTrue(slabsSecond[0].getLastAccess() > -1);
        assertTrue(slabsSecond[0].getLastAccess() >= lastAccess);

        verify(storage, times(1)).readRasterData(eq(0), eq(0), eq(10), eq(10), anyObject());
        verifyNoMoreInteractions(storage);
    }

    @Test
    public void testGet_twoSlabs_horizontal() {
        final SlabCache slabCache = new SlabCache(90, 190, 10, 9, storage);

        final Slab[] slabs = slabCache.get(8, 3, 4, 3);
        assertEquals(2, slabs.length);

        Rectangle region = slabs[0].getRegion();
        assertEquals(0, region.x);
        assertEquals(0, region.y);
        assertEquals(10, region.width);
        assertEquals(9, region.height);
        assertTrue(slabs[0].getLastAccess() > -1);

        region = slabs[1].getRegion();
        assertEquals(10, region.x);
        assertEquals(0, region.y);
        assertEquals(10, region.width);
        assertEquals(9, region.height);
        assertTrue(slabs[1].getLastAccess() > -1);

        verify(storage, times(1)).readRasterData(eq(0), eq(0), eq(10), eq(9), anyObject());
        verify(storage, times(1)).readRasterData(eq(10), eq(0), eq(10), eq(9), anyObject());
        verifyNoMoreInteractions(storage);
    }

    @Test
    public void testGet_twoSlabs_vertical() {
        final SlabCache slabCache = new SlabCache(80, 180, 9, 8, storage);

        final Slab[] slabs = slabCache.get(2, 6, 4, 4);
        assertEquals(2, slabs.length);

        Rectangle region = slabs[0].getRegion();
        assertEquals(0, region.x);
        assertEquals(0, region.y);
        assertEquals(9, region.width);
        assertEquals(8, region.height);
        assertTrue(slabs[0].getLastAccess() > -1);

        region = slabs[1].getRegion();
        assertEquals(0, region.x);
        assertEquals(8, region.y);
        assertEquals(9, region.width);
        assertEquals(8, region.height);
        assertTrue(slabs[1].getLastAccess() > -1);

        verify(storage, times(1)).readRasterData(eq(0), eq(0), eq(9), eq(8), anyObject());
        verify(storage, times(1)).readRasterData(eq(0), eq(8), eq(9), eq(8), anyObject());
        verifyNoMoreInteractions(storage);
    }

    @Test
    public void testGet_fourSlabs_intersectingCenterPoint() {
        final SlabCache slabCache = new SlabCache(50, 140, 10, 20, storage);

        final Slab[] slabs = slabCache.get(28, 38, 10, 10);
        assertEquals(4, slabs.length);

        Rectangle region = slabs[0].getRegion();
        assertEquals(20, region.x);
        assertEquals(20, region.y);
        assertEquals(10, region.width);
        assertEquals(20, region.height);
        assertTrue(slabs[0].getLastAccess() > -1);

        region = slabs[1].getRegion();
        assertEquals(20, region.x);
        assertEquals(40, region.y);
        assertEquals(10, region.width);
        assertEquals(20, region.height);
        assertTrue(slabs[1].getLastAccess() > -1);

        region = slabs[2].getRegion();
        assertEquals(30, region.x);
        assertEquals(20, region.y);
        assertEquals(10, region.width);
        assertEquals(20, region.height);
        assertTrue(slabs[2].getLastAccess() > -1);

        region = slabs[3].getRegion();
        assertEquals(30, region.x);
        assertEquals(40, region.y);
        assertEquals(10, region.width);
        assertEquals(20, region.height);
        assertTrue(slabs[3].getLastAccess() > -1);

        verify(storage, times(1)).readRasterData(eq(20), eq(20), eq(10), eq(20), anyObject());
        verify(storage, times(1)).readRasterData(eq(20), eq(40), eq(10), eq(20), anyObject());
        verify(storage, times(1)).readRasterData(eq(30), eq(20), eq(10), eq(20), anyObject());
        verify(storage, times(1)).readRasterData(eq(30), eq(40), eq(10), eq(20), anyObject());
        verifyNoMoreInteractions(storage);
    }
}
