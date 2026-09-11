package com.kuaigeglobal.epnova.widget;

import com.kuaigeglobal.epnova.widget.GridImageLayout.Geometry;

import org.junit.Test;
import java.util.Random;
import static org.junit.Assert.*;

public class GridImageGeometryTest {
    private static final int ANY = Geometry.UNBOUNDED;

    @Test public void originalGridPreservesFixedSizeAndConfiguredSpacing() {
        Geometry g = Geometry.fixedGrid(6, 2, 93, 6, 8);
        assertEquals(93, g.cellWidth);
        assertEquals(192, g.width);
        assertEquals(295, g.height);
        assertEquals(6, g.horizontalSpacing);
        assertEquals(8, g.verticalSpacing);
    }

    @Test public void autoWidthDoesNotShrinkConfiguredGapsOrUseHeight() {
        Geometry g = Geometry.autoWidthGrid(6, 3, 6, 8, 312, 93);
        assertEquals(100, g.cellWidth);
        assertEquals(208, g.height);
        g = Geometry.autoWidthGrid(3, 3, 6, 8, 3, 93);
        assertEquals(0, g.cellWidth);
        assertEquals(6, g.horizontalSpacing);
        assertEquals(12, g.width); // 保留间距，超出父容器的部分由父容器决定裁剪。
    }

    @Test public void originalSingleWithoutMaxUsesProvidedDimensionsRatherThan180Cap() {
        Geometry g = Geometry.originalSingle(640, 360, 180, 180, 0, 0, 93);
        assertEquals(640, g.width);
        assertEquals(360, g.height);
        assertTrue(g.crop);
    }

    @Test public void originalUnknownSingleUsesMinimumPlaceholderSize() {
        Geometry g = Geometry.originalSingle(0, 0, 180, 160, 0, 0, 93);
        assertEquals(180, g.width);
        assertEquals(160, g.height);
        assertFalse(g.crop);
    }

    @Test public void originalTallImageKeepsMinimumShortEdgeAndCrops() {
        Geometry g = Geometry.originalSingle(1000, 2000, 180, 180, 300, 300, 93);
        assertEquals(180, g.width);
        assertEquals(300, g.height);
        assertTrue(g.crop);
    }

    @Test public void originalSmallImageUpscalesOnlyBelowExistingThreshold() {
        Geometry g = Geometry.originalSingle(50, 25, 180, 180, 400, 400, 93);
        assertEquals(360, g.width);
        assertEquals(180, g.height);
        assertFalse(g.crop);
        g = Geometry.originalSingle(120, 100, 180, 180, 400, 400, 93);
        assertEquals(120, g.width);
        assertEquals(100, g.height);
    }

    @Test public void originalOneSidedMaxAndConflictingMinDoNotProduceZeroOrOversizedResults() {
        Geometry g = Geometry.originalSingle(600, 300, 0, 0, 300, 0, 93);
        assertEquals(300, g.width);
        assertEquals(150, g.height);
        g = Geometry.originalSingle(300, 600, 0, 0, 0, 300, 93);
        assertEquals(150, g.width);
        assertEquals(300, g.height);
        g = Geometry.originalSingle(1000, 2000, 180, 180, 100, 100, 93);
        assertEquals(100, g.width);
        assertEquals(100, g.height);
        assertTrue(g.crop);
    }

    @Test public void originalSmallImageWithZeroMinimumRemainsVisible() {
        Geometry g = Geometry.originalSingle(50, 25, 0, 0, 300, 300, 93);
        assertEquals(50, g.width);
        assertEquals(25, g.height);
        g = Geometry.originalSingle(25, 50, 0, 0, 300, 300, 93);
        assertEquals(25, g.width);
        assertEquals(50, g.height);
        g = Geometry.originalSingle(1, 10000, 0, 0, 300, 300, 93);
        assertEquals(1, g.width);
        assertEquals(300, g.height);
    }

    @Test public void fitInsideNeverEnlargesConfiguredCellsAndZeroRemainsZero() {
        Geometry g = Geometry.fitInsideGrid(3, 3, 40, 6, 8, 1000, ANY);
        assertEquals(40, g.cellWidth);
        assertEquals(132, g.width);
        g = Geometry.fitInsideGrid(3, 3, 0, 6, 8, 1000, ANY);
        assertEquals(0, g.cellWidth);
    }

    @Test public void twoColumnsAndThreeRowsIncludeLastRowAndCorrectSpacing() {
        Geometry g = Geometry.fitInsideGrid(6, 2, 93, 6, 8, 400, ANY);
        assertEquals(2, g.columns);
        assertEquals(3, g.rows);
        assertEquals(192, g.width);
        assertEquals(295, g.height);
    }

    @Test public void partialRowDoesNotAddAnExtraRow() {
        Geometry g = Geometry.fitInsideGrid(5, 3, 40, 6, 8, 300, ANY);
        assertEquals(2, g.rows);
        assertEquals(132, g.width);
        assertEquals(88, g.height);
    }

    @Test public void fitInsidePreservesConfiguredSizeAndHonorsHeightLimit() {
        Geometry g = Geometry.fitInsideGrid(6, 3, 100, 6, 8, 312, ANY);
        assertEquals(100, g.cellWidth);
        assertEquals(208, g.height);
        g = Geometry.fitInsideGrid(6, 3, 100, 6, 8, 312, 108);
        assertEquals(50, g.cellWidth);
        assertEquals(108, g.height);
    }

    @Test public void narrowAndZeroContainersNeverProduceNegativeDimensions() {
        Geometry g = Geometry.fitInsideGrid(9, 3, 93, 6, 6, 3, 2);
        assertEquals(0, g.cellWidth);
        assertTrue(g.width <= 3);
        assertTrue(g.height <= 2);
        g = Geometry.fitInsideGrid(9, 3, 0, 6, 6, 0, 0);
        assertEquals(0, g.width);
        assertEquals(0, g.height);
    }

    @Test public void unboundedFitInsideKeepsConfiguredSizeAndInvalidColumnsAreSafe() {
        Geometry g = Geometry.fitInsideGrid(2, 0, 93, 6, 6, ANY, ANY);
        assertEquals(1, g.columns);
        assertEquals(93, g.cellWidth);
        assertEquals(192, g.height);
        assertSame(Geometry.EMPTY, Geometry.fitInsideGrid(0, 0, 0, 0, 0, ANY, ANY));
    }

    @Test public void singleImageCanLimitOnlyOneDimension() {
        Geometry g = Geometry.fitInsideSingle(2, 300, 0, ANY, ANY, 180);
        assertEquals(300, g.width);
        assertEquals(150, g.height);
        g = Geometry.fitInsideSingle(0.5, 0, 300, ANY, ANY, 180);
        assertEquals(150, g.width);
        assertEquals(300, g.height);
    }

    @Test public void singleImageAlwaysFitsParentEvenWhenConfiguredLarger() {
        Geometry g = Geometry.fitInsideSingle(2, 400, 400, 100, 30, 180);
        assertEquals(60, g.width);
        assertEquals(30, g.height);
        g = Geometry.fitInsideSingle(2, 400, 400, 0, 30, 180);
        assertEquals(0, g.width);
        assertEquals(0, g.height);
    }

    @Test public void invalidRatioAndUnboundedSingleImageUseFiniteFallback() {
        for (double ratio : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            Geometry g = Geometry.fitInsideSingle(ratio, 0, 0, ANY, ANY, 180);
            assertEquals(180, g.width);
            assertEquals(180, g.height);
        }
        Geometry g = Geometry.fitInsideSingle(2, 0, 0, ANY, ANY, 180);
        assertEquals(180, g.width);
        assertEquals(90, g.height);
    }

    @Test public void extremeParametersDoNotOverflow() {
        Geometry g = Geometry.fitInsideGrid(Integer.MAX_VALUE, Integer.MAX_VALUE,
                Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, ANY, ANY);
        assertTrue(g.width >= 0 && g.width <= ANY);
        assertEquals(0, g.cellWidth);
        assertEquals(1, g.rows);
        assertEquals(ANY, Geometry.size(Long.MAX_VALUE));
        assertEquals(0, Geometry.size(Long.MIN_VALUE));
    }

    @Test public void variedGridConstraintsKeepAllCellBoundsInsideContent() {
        Random random = new Random(20260911);
        for (int iteration = 0; iteration < 2000; iteration++) {
            int count = 1 + random.nextInt(100);
            int width = random.nextInt(1000);
            int height = random.nextInt(1000);
            Geometry g = Geometry.fitInsideGrid(count, random.nextInt(20), random.nextInt(200),
                    random.nextInt(60), random.nextInt(60), width, height);
            assertTrue(g.width >= 0 && g.width <= width);
            assertTrue(g.height >= 0 && g.height <= height);
            assertEquals(g.cellWidth, g.cellHeight);
            for (int i = 0; i < count; i++) {
                int right = (i % g.columns) * (g.cellWidth + g.horizontalSpacing) + g.cellWidth;
                int bottom = (i / g.columns) * (g.cellHeight + g.verticalSpacing) + g.cellHeight;
                assertTrue(right <= g.width);
                assertTrue(bottom <= g.height);
            }
        }
    }
}
