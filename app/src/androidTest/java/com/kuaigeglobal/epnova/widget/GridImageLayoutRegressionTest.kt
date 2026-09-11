package com.kuaigeglobal.epnova.widget

import android.graphics.Bitmap
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.widget.testing.WidgetTestActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class GridImageLayoutRegressionTest {
    private val a = "file:///grid-image-a.jpg"
    private val b = "file:///grid-image-b.jpg"
    private val c = "file:///grid-image-c.jpg"

    @Test fun nullAndEmptyDataClearViewsAndSnapshotsCannotMutateState() = withGrid { grid ->
        val input = arrayListOf(a, b)
        grid.setImages(input)
        val first = grid.getChildAt(0)
        grid.setImages(input.toList())
        assertSame(first, grid.getChildAt(0))
        input.clear()
        grid.images.clear()
        assertEquals(listOf(a, b), grid.images)
        grid.setImages(emptyList())
        assertEquals(0, grid.childCount)
        grid.setImages(listOf(a))
        grid.setImages(null)
        assertEquals(0, grid.imageCount)
        assertEquals(0, grid.childCount)
        first.performClick()
    }

    @Test fun invalidUrlsAreFilteredAndDuplicateOccurrencesRemainIndependent() = withGrid { grid ->
        grid.setImages(listOf(null, "", "  ", a, a, b))
        assertEquals(listOf(a, a, b), grid.images)
        val first = grid.getChildAt(0)
        val second = grid.getChildAt(1)
        val third = grid.getChildAt(2)
        assertNotSame(first, second)
        grid.setImages(listOf(b, a, a))
        assertSame(third, grid.getChildAt(0))
        assertSame(first, grid.getChildAt(1))
        assertSame(second, grid.getChildAt(2))
    }

    @Test fun clicksFollowReorderedPositionAndAllowReentrantClear() = withGrid { grid ->
        grid.setImages(listOf(a, b, c))
        val retained = grid.getChildAt(2)
        val removed = grid.getChildAt(1)
        grid.setImages(listOf(c, a))
        var clicks = 0
        grid.setOnImageClickListener { view, position, images ->
            clicks++
            assertSame(retained, view)
            assertEquals(0, position)
            assertEquals(listOf(c, a), images)
            images.clear()
            assertEquals(2, grid.imageCount)
            grid.clearImages()
        }
        removed.performClick()
        retained.performClick()
        retained.performClick()
        assertEquals(1, clicks)
        assertEquals(0, grid.childCount)
    }

    @Test fun rowLimitOnlyLimitsDisplayAndZeroRowsCanBeRestored() = withGrid { grid ->
        grid.columnCount = 2
        grid.maxRowCount = 1
        grid.setImages(listOf(a, b, c))
        assertEquals(2, grid.childCount)
        grid.setOnImageClickListener { _, _, images -> assertEquals(listOf(a, b, c), images) }
        grid.getChildAt(1).performClick()
        grid.maxRowCount = 0
        assertEquals(0, grid.childCount)
        assertEquals(3, grid.imageCount)
        grid.maxRowCount = 2
        assertEquals(3, grid.childCount)
        grid.columnCount = 0
        assertEquals(1, grid.columnCount)
        assertEquals(2, grid.childCount)
        grid.columnCount = Int.MAX_VALUE
        grid.maxRowCount = Int.MAX_VALUE
        assertEquals(3, grid.childCount)
    }

    @Test fun twoColumnsAndThreeRowsMeasureAndLayoutTheLastRow() = withGrid { grid ->
        grid.columnCount = 2
        grid.maxRowCount = 3
        grid.cellSize = 40
        grid.setSpacing(6, 8)
        grid.setPadding(10, 11, 12, 13)
        grid.setImages(List(6) { a })
        layout(grid, 300)
        assertEquals(160, grid.measuredHeight)
        assertEquals(10, grid.getChildAt(0).left)
        assertEquals(56, grid.getChildAt(1).left)
        assertEquals(107, grid.getChildAt(4).top)
        assertEquals(grid.height - 13, grid.getChildAt(5).bottom)
    }

    @Test fun originalModeKeepsFixedCellsWhenParentIsTooSmall() = withGrid { grid ->
        grid.columnCount = 3
        grid.cellSize = 93
        grid.setSpacing(6, 8)
        grid.setImages(List(6) { a })
        grid.measure(View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(50, View.MeasureSpec.EXACTLY))
        grid.layout(0, 0, 100, 50)
        assertEquals(93, grid.getChildAt(0).width)
        assertEquals(291, grid.getChildAt(2).right)
        assertEquals(194, grid.getChildAt(5).bottom)
        assertEquals(6, grid.getChildAt(1).left - grid.getChildAt(0).right)
        grid.sizeMode = GridImageLayout.SIZE_FIT_INSIDE
        grid.measure(View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(50, View.MeasureSpec.EXACTLY))
        grid.layout(0, 0, 100, 50)
        assertTrue(grid.getChildAt(2).right <= 100)
        assertTrue(grid.getChildAt(5).bottom <= 50)
    }

    @Test fun columnSpecificSizesRemainIndependentAndCanFallBackToCommonSize() = withGrid { grid ->
        grid.maxRowCount = 6
        grid.cellSize = 30
        grid.setColumnImageSizes(80, 60, 40)
        grid.setImages(List(6) { a })
        grid.columnCount = 1
        layout(grid, 400)
        assertEquals(80, grid.getChildAt(0).width)
        grid.columnCount = 2
        layout(grid, 400)
        assertEquals(60, grid.getChildAt(0).width)
        grid.columnCount = 3
        layout(grid, 400)
        assertEquals(40, grid.getChildAt(0).width)
        grid.setImages(listOf(a, b)) // 保留原来的“两张图使用两列尺寸”。
        layout(grid, 400)
        assertEquals(60, grid.getChildAt(0).width)
        grid.setColumnImageSizes(-1, -1, -1)
        layout(grid, 400)
        assertEquals(30, grid.getChildAt(0).width)
    }

    @Test fun fixedSingleSizeAndOriginalMinimumCropRemainAvailable() = withGrid { grid ->
        grid.setSingleImageSize(240, 120)
        grid.setImage(a, 1000, 2000)
        layout(grid, 100)
        assertEquals(240, grid.getChildAt(0).width)
        assertEquals(120, grid.getChildAt(0).height)
        assertEquals(ImageView.ScaleType.CENTER_CROP, (grid.getChildAt(0) as ImageView).scaleType)
        grid.setSingleImageSize(120, 240)
        layout(grid, 100)
        assertEquals(120, grid.getChildAt(0).width)
        assertEquals(240, grid.getChildAt(0).height)
        grid.setSingleImageSize(0, 0)
        grid.setSingleImageMinSize(180, 180)
        grid.setSingleImageMaxSize(300, 300)
        layout(grid, 400)
        assertEquals(180, grid.getChildAt(0).width)
        assertEquals(300, grid.getChildAt(0).height)
        assertEquals(ImageView.ScaleType.CENTER_CROP, (grid.getChildAt(0) as ImageView).scaleType)
    }

    @Test fun unknownSinglePlaceholderMatchesOriginalImageViewMeasurement() = withGrid { grid ->
        grid.setPadding(10, 5, 20, 15)
        grid.setSingleImageMinSize(180, 160)
        grid.setSingleImageMaxSize(300, 250)
        grid.setImages(listOf(a))
        val original = ColorFilterImageView(grid.context).apply {
            setImageResource(R.drawable.img_default_cover_1_1)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            minimumWidth = 180
            minimumHeight = 160
            maxWidth = 300
            maxHeight = 250
        }
        for (width in listOf(100, 600)) {
            val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.AT_MOST)
            original.measure(android.view.ViewGroup.getChildMeasureSpec(widthSpec, 30, -2),
                android.view.ViewGroup.getChildMeasureSpec(heightSpec, 20, -2))
            grid.measure(widthSpec, heightSpec) // 尚未加载，只对照占位阶段。
            assertEquals(original.measuredWidth, grid.getChildAt(0).measuredWidth)
            assertEquals(original.measuredHeight, grid.getChildAt(0).measuredHeight)
        }
    }

    @Test fun fitInsideKeepsFixedSingleAndPerColumnSizesAsUpperBounds() = withGrid { grid ->
        grid.sizeMode = GridImageLayout.SIZE_FIT_INSIDE
        grid.setSingleImageSize(120, 60)
        grid.setImage(a, 1000, 2000)
        layout(grid, 1000)
        assertEquals(120, grid.getChildAt(0).width)
        assertEquals(60, grid.getChildAt(0).height)
        layout(grid, 100)
        assertEquals(100, grid.getChildAt(0).width)
        assertEquals(50, grid.getChildAt(0).height)
        grid.setColumnImageSizes(80, 60, 40)
        grid.setImages(listOf(a, b, c))
        layout(grid, 1000)
        assertEquals(40, grid.getChildAt(0).width)
        grid.columnCount = 2
        layout(grid, 1000)
        assertEquals(60, grid.getChildAt(0).width)
    }

    @Test fun resizingPaddingAndAutoFixedSwitchUseFreshContentBounds() = withGrid { grid ->
        grid.columnCount = 3
        grid.setSpacing(6, 8)
        grid.setPadding(10, 5, 10, 5)
        grid.sizeMode = GridImageLayout.SIZE_AUTO_WIDTH
        grid.setImages(listOf(a, b, c))
        layout(grid, 332)
        assertEquals(100, grid.getChildAt(0).width)
        grid.cellSize = 40
        grid.sizeMode = GridImageLayout.SIZE_ORIGINAL
        layout(grid, 332)
        assertEquals(40, grid.getChildAt(0).width)
        grid.sizeMode = GridImageLayout.SIZE_AUTO_WIDTH
        layout(grid, 182)
        assertEquals(50, grid.getChildAt(0).width)
        grid.setPadding(20, 5, 30, 5)
        layout(grid, 182)
        assertEquals(40, grid.getChildAt(0).width)
    }

    @Test fun rtlMirrorsPositionsAndRetainsLogicalClickOrder() = withGrid { grid ->
        grid.columnCount = 2
        grid.cellSize = 40
        grid.setSpacing(6, 8)
        grid.setPadding(10, 0, 20, 0)
        grid.setImages(listOf(a, b, c))
        grid.layoutDirection = View.LAYOUT_DIRECTION_RTL
        layout(grid, 200)
        assertEquals(10, grid.getChildAt(0).left) // 默认保留旧控件从左排列。
        grid.setMirrorInRtl(true)
        layout(grid, 200)
        assertEquals(180, grid.getChildAt(0).right)
        assertEquals(134, grid.getChildAt(1).right)
        assertEquals(180, grid.getChildAt(2).right)
        grid.setOnImageClickListener { _, position, _ -> assertEquals(1, position) }
        grid.getChildAt(1).performClick()
    }

    @Test fun singleMetadataDoesNotLeakAcrossReplacementOrMultiImageModes() = withGrid { grid ->
        grid.setSingleImageMaxSize(200, 200)
        grid.setSingleImageMinSize(0, 0)
        grid.setImage(a, 400, 200)
        layout(grid, 300)
        assertEquals(200, grid.getChildAt(0).width)
        assertEquals(100, grid.getChildAt(0).height)
        val reused = grid.getChildAt(0)
        grid.cellSize = 40
        grid.setImages(listOf(a, b))
        layout(grid, 300)
        assertSame(reused, grid.getChildAt(0))
        assertEquals(ImageView.ScaleType.CENTER_CROP, (reused as ImageView).scaleType)
        assertEquals(40, reused.width)
        grid.setImages(listOf(a))
        layout(grid, 300)
        assertEquals(ImageView.ScaleType.FIT_CENTER, reused.scaleType)
        assertEquals(reused.width, reused.height) // 已清除原图尺寸，恢复方形占位图的 WRAP_CONTENT 测量。
        grid.setImage(a, 200, 400)
        layout(grid, 300)
        assertEquals(100, reused.width)
        assertEquals(200, reused.height)
    }

    @Test fun disabledPreviewStillAllowsCustomCallbackAndCanBeRestored() = withGrid { grid ->
        grid.setImages(listOf(a, b))
        val first = grid.getChildAt(0)
        grid.setPreviewEnabled(false)
        assertFalse(first.isClickable)
        var clicks = 0
        grid.setOnImageClickListener { _, _, _ -> clicks++ }
        assertTrue(first.isClickable)
        first.performClick()
        grid.setOnImageClickListener(null)
        assertFalse(first.isClickable)
        first.performClick()
        assertEquals(1, clicks)
        grid.setPreviewEnabled(true)
        assertFalse(first.isClickable) // null 仍保持关闭点击。
        grid.resetOnImageClickListener()
        assertTrue(first.isClickable)
        assertTrue(first.isFocusable)
    }

    @Test fun childViewSnapshotsAndInvalidPreviewPositionsAreSafe() = withGrid { grid ->
        grid.setImages(listOf(a, b))
        val snapshot = grid.childViews
        snapshot.clear()
        assertEquals(2, grid.childCount)
        grid.previewImage(-1)
        grid.previewImage(grid.imageCount)
        grid.previewImage(null, -1)
        grid.clearImages()
        grid.previewImage(0)
    }

    @Test fun tinyAndUnboundedMeasureSpecsStaySafe() = withGrid { grid ->
        grid.columnCount = 3
        grid.sizeMode = GridImageLayout.SIZE_FIT_INSIDE
        grid.setSpacing(6, 6)
        grid.setImages(List(9) { a })
        layout(grid, 3)
        assertEquals(0, grid.getChildAt(0).width)
        assertTrue(grid.getChildAt(2).right <= 3)
        grid.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        assertTrue(grid.measuredWidth > 0)
        assertTrue(grid.measuredHeight > 0)
        grid.setSpacing(-1, -1)
        layout(grid, 300)
        assertEquals(minOf(grid.cellSize, 100), grid.getChildAt(0).width)
    }

    @Test fun loadedSingleImageResolvesAspectRatioWithoutKnownDimensions() {
        val resized = CountDownLatch(1)
        var file: File? = null
        var grid: GridImageLayout? = null
        ActivityScenario.launch(WidgetTestActivity::class.java).use { scenario ->
            try {
                scenario.onActivity { activity ->
                    file = File.createTempFile("grid-image-wide-", ".png", activity.cacheDir)
                    val bitmap = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
                    try {
                        file!!.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    } finally {
                        bitmap.recycle()
                    }
                    grid = GridImageLayout(activity).apply {
                        sizeMode = GridImageLayout.SIZE_FIT_INSIDE
                        setSingleImageMaxSize(200, 200)
                        setImages(listOf(file!!.toURI().toString()))
                        getChildAt(0).addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                            if (v.width == 200 && v.height == 100) resized.countDown()
                        }
                    }
                    activity.container.addView(grid, FrameLayout.LayoutParams(300, FrameLayout.LayoutParams.WRAP_CONTENT))
                }
                assertTrue("本地图片加载后应按 2:1 比例重新测量", resized.await(5, TimeUnit.SECONDS))
                scenario.onActivity {
                    assertEquals(200, grid!!.getChildAt(0).width)
                    assertEquals(100, grid!!.getChildAt(0).height)
                }
            } finally {
                scenario.onActivity { grid?.clearImages() }
                file?.delete()
            }
        }
    }

    @Test fun xmlConfigurationAndProgrammaticConstructionBothInitializeDefaults() {
        ActivityScenario.launch(WidgetTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val grid = activity.layoutInflater.inflate(R.layout.view_grid_image_regression, activity.container, false) as GridImageLayout
                assertEquals(2, grid.columnCount)
                assertEquals(3, grid.maxRowCount)
                val dp = activity.resources.displayMetrics.density
                assertEquals((40 * dp).toInt(), grid.cellSize)
                grid.setImages(listOf(a, b, c))
                layout(grid, 600)
                assertEquals(grid.cellSize, grid.getChildAt(0).width)
                assertEquals((6.75f * dp).toInt(), grid.getChildAt(1).left - grid.getChildAt(0).right)
                assertFalse(grid.getChildAt(0).isClickable)
                grid.setImage(a, 1000, 2000)
                layout(grid, 600)
                assertEquals((72 * dp).toInt(), grid.getChildAt(0).width)
                assertEquals((48 * dp).toInt(), grid.getChildAt(0).height)
                val programmatic = GridImageLayout(activity)
                assertEquals(GridImageLayout.SIZE_ORIGINAL, programmatic.sizeMode)
                assertEquals(3, programmatic.columnCount)
                assertEquals(3, programmatic.maxRowCount)
                assertTrue(programmatic.cellSize > 0)
                grid.clearImages()
            }
        }
    }

    private fun layout(grid: GridImageLayout, width: Int) {
        grid.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        grid.layout(0, 0, grid.measuredWidth, grid.measuredHeight)
    }

    private fun withGrid(block: (GridImageLayout) -> Unit) {
        ActivityScenario.launch(WidgetTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val grid = GridImageLayout(activity)
                activity.container.addView(grid)
                try {
                    block(grid)
                } finally {
                    grid.clearImages()
                }
            }
        }
    }
}
