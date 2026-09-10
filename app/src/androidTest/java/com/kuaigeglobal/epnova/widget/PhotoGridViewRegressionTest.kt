package com.kuaigeglobal.epnova.widget

import android.net.Uri
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.GridLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.widget.testing.WidgetTestActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoGridViewRegressionTest {
    private val a = Uri.parse("file:///photo-grid-a.jpg")
    private val b = Uri.parse("file:///photo-grid-b.jpg")
    private val c = Uri.parse("file:///photo-grid-c.jpg")
    private val d = Uri.parse("file:///photo-grid-d.jpg")

    @Test
    fun snapshotAndBatchCallbacksIncludeClearButIgnoreUnchangedData() = withFixture { root ->
        val grid = root.auto()
        val events = mutableListOf<List<Uri>>()
        grid.setOnPhotosChangedListener { photos -> events.add(photos.toList()) }
        assertEquals(listOf(emptyList<Uri>()), events)
        val input = arrayListOf(a, b)
        grid.setPhotos(input)
        val first = grid.getChildAt(0)
        grid.setPhotos(listOf(a, b))
        assertSame(first, grid.getChildAt(0))
        input.clear()
        grid.photos.clear()
        assertEquals(listOf(a, b), grid.photos)
        grid.clearPhotos()
        grid.clearPhotos()
        assertEquals(listOf(emptyList(), listOf(a, b), emptyList()), events)
        assertEquals(0, grid.photoCount)
        assertEquals(1, grid.childCount)
        assertEquals(R.id.add_photo, grid.getChildAt(0).id)
    }

    @Test
    fun everyAddPathRespectsCapacityAndRestoresButtonAfterRemoval() = withFixture { root ->
        val grid = root.auto()
        grid.setPhotos(listOf(a, b, c, d, a))
        grid.addPhoto(b)
        grid.addPhotos(listOf(c, d))
        assertEquals(listOf(a, b, c, d), grid.photos)
        assertNull(grid.findViewById<View>(R.id.add_photo))
        grid.removePhotoAt(1)
        assertEquals(listOf(a, c, d), grid.photos)
        assertEquals(R.id.add_photo, grid.getChildAt(3).id)
        grid.addPhoto(b)
        assertEquals(listOf(a, c, d, b), grid.photos)
        assertEquals(grid.photos, (0 until grid.childCount).map { grid.getChildAt(it).tag })
    }

    @Test
    fun deletionUsesLivePositionAndIgnoresDetachedDuplicateViews() = withFixture { root ->
        val grid = root.auto()
        grid.setPhotos(listOf(a, a, b))
        val removed = grid.getChildAt(0)
        val duplicate = grid.getChildAt(1)
        removed.findViewById<View>(R.id.grid_item_delete_button).performClick()
        assertSame(duplicate, grid.getChildAt(0))
        assertEquals(listOf(a, b), grid.photos)
        removed.findViewById<View>(R.id.grid_item_delete_button).performClick()
        removed.performClick() // 已移除的预览入口也必须忽略。
        assertEquals(listOf(a, b), grid.photos)
        duplicate.findViewById<View>(R.id.grid_item_delete_button).performClick()
        assertEquals(listOf(b), grid.photos)
    }

    @Test
    fun reorderAndPartialReplacementReuseExistingImages() = withFixture { root ->
        val grid = root.auto()
        grid.setPhotos(listOf(a, b, c))
        val first = grid.getChildAt(0)
        val third = grid.getChildAt(2)
        val requested = arrayListOf(c, a, d)
        grid.setPhotos(requested)
        assertEquals(listOf(c, a, d), requested)
        assertSame(third, grid.getChildAt(0))
        assertSame(first, grid.getChildAt(1))
        assertEquals(grid.photos, (0 until grid.photoCount).map { grid.getChildAt(it).tag })
        assertEquals(R.id.add_photo, grid.getChildAt(grid.photoCount).id)
    }

    @Test
    fun invalidInputAndOutOfRangeRemovalDoNotConsumeCapacityOrNotify() = withFixture { root ->
        val grid = root.auto()
        var events = 0
        grid.setOnPhotosChangedListener { events++ }
        grid.setPhotos(null)
        grid.addPhotos(null)
        grid.addPhoto(null)
        grid.addPhoto(Uri.EMPTY)
        grid.removePhotoAt(-1)
        grid.removePhotoAt(100)
        assertEquals(1, events)
        grid.addPhotos(listOf(null, Uri.EMPTY, a, b))
        assertEquals(listOf(a, b), grid.photos)
        assertEquals(2, events)
    }

    @Test
    fun firstMeasureAndResizeUseCurrentWidthAndPadding() = withFixture { root ->
        val grid = root.auto()
        grid.setPadding(11, 7, 17, 13)
        val gap = root.dp(5)
        measure(grid, 400)
        val width = (400 - 28 - gap * 3) / 4
        assertEquals(width, grid.getChildAt(0).measuredWidth)
        assertEquals(width + 20, grid.measuredHeight)
        grid.setPhotos(listOf(a, b))
        measure(grid, 600)
        val resized = (600 - 28 - gap * 3) / 4
        assertEquals(resized, grid.getChildAt(0).measuredWidth)
        assertEquals(resized + 20, grid.measuredHeight)
    }

    @Test
    fun columnChangesAndDeletionReflowRows() = withFixture { root ->
        val grid = root.auto()
        grid.setPhotos(listOf(a, b, c, d))
        measure(grid, 400)
        grid.columnCount = 2
        measure(grid, 400)
        val width = (400 - root.dp(5)) / 2
        assertEquals(width * 2 + root.dp(5), grid.measuredHeight)
        grid.setPhotos(listOf(a)) // 一张图片 + 添加按钮，回到一行。
        measure(grid, 400)
        assertEquals(width, grid.measuredHeight)
        grid.columnCount = 0
        measure(grid, 400)
        assertEquals(1, grid.columnCount)
    }

    @Test
    fun rtlKeepsGapBetweenItemsAndDoesNotAddLeadingGap() = withFixture { root ->
        val grid = root.auto()
        grid.layoutDirection = View.LAYOUT_DIRECTION_RTL
        grid.setPhotos(listOf(a, b))
        measure(grid, 400)
        val first = grid.getChildAt(0)
        val second = grid.getChildAt(1)
        assertTrue(first.left > second.left)
        assertEquals(400, first.right)
        assertEquals(root.dp(5), first.left - second.right)
        assertEquals(0, (first.layoutParams as GridLayout.LayoutParams).marginStart)
    }

    @Test
    fun fixedDimensionsShowAddButtonInitiallyAndSizeImageToCell() = withFixture { root ->
        val grid = root.findViewById<PhotoGridView>(R.id.regression_photo_fixed)
        assertEquals(R.id.add_photo, grid.getChildAt(0).id)
        measure(grid, 400)
        assertEquals(root.dp(64), grid.getChildAt(0).measuredWidth)
        assertEquals(root.dp(48), grid.getChildAt(0).measuredHeight)
        grid.setPhotos(listOf(a, b))
        measure(grid, 400)
        assertEquals(3, grid.childCount) // autoHide=false，满额时保留入口。
        val image = grid.getChildAt(0).findViewById<View>(R.id.grid_item_picture)
        assertEquals(root.dp(64), image.measuredWidth)
        assertEquals(root.dp(48), image.measuredHeight)
        grid.addPhoto(c)
        assertEquals(2, grid.photoCount)
    }

    @Test
    fun zeroCapacityAndHiddenAddButtonRemainConsistent() = withFixture { root ->
        val zero = root.findViewById<PhotoGridView>(R.id.regression_photo_zero)
        zero.setPhotos(listOf(a))
        assertEquals(0, zero.photoCount)
        assertEquals(0, zero.childCount)
        val readonly = root.findViewById<PhotoGridView>(R.id.regression_photo_readonly)
        readonly.setPhotos(listOf(a, b))
        assertEquals(listOf(a, b), (0 until readonly.childCount).map { readonly.getChildAt(it).tag })
        readonly.removePhoto(a)
        assertEquals(listOf(b), readonly.photos)
        readonly.clearPhotos()
        assertEquals(0, readonly.childCount)
    }

    @Test
    fun wrappedContextAndUnboundedOrNarrowWidthAreSupported() = withFixture { root ->
        val grid = PhotoGridView(ContextThemeWrapper(root.context, android.R.style.Theme_Material_Light))
        grid.layoutTransition = null
        assertEquals(4, grid.columnCount)
        grid.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        assertTrue(grid.getChildAt(0).measuredWidth > 0)
        measure(grid, 1)
        assertEquals(0, grid.getChildAt(0).measuredWidth)
        assertTrue(grid.measuredHeight >= 0)
    }

    @Test
    fun urlReplacementSupportsReadonlyListsAndClearsOnEmptyInput() = withFixture { root ->
        val grid = root.auto()
        val urls = listOf(null, "", "  ", a.toString(), b.toString())
        grid.setPhotoUrls(urls)
        assertEquals(listOf(a, b), grid.photos)
        val first = grid.getChildAt(0)
        grid.setPhotoUrls(urls)
        assertSame(first, grid.getChildAt(0))
        grid.setPhotoUrls(null)
        assertEquals(0, grid.photoCount)
    }

    @Test
    fun activeTransitionsDoNotLoseAddButtonOrBreakReordering() {
        ActivityScenario.launch(WidgetTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.layoutInflater.inflate(R.layout.view_photo_grid_regression, activity.container, false)
                activity.container.addView(root)
                val grid = root.auto()
                // 保留真实 LayoutTransition，覆盖按钮尚未退场就立即删图的情况。
                grid.setPhotos(listOf(a, b, c))
                measure(grid, 400)
                val addButton = grid.findViewById<View>(R.id.add_photo)
                repeat(3) {
                    grid.setPhotos(listOf(a, b, c, d))
                    assertEquals(-1, grid.indexOfChild(addButton))
                    grid.removePhotoAt(3)
                    assertEquals(3, grid.indexOfChild(addButton))
                    grid.setPhotos(listOf(c, b, a))
                    assertEquals(listOf(c, b, a), (0 until grid.photoCount).map { grid.getChildAt(it).tag })
                    grid.clearPhotos()
                    assertEquals(0, grid.indexOfChild(addButton))
                }
            }
        }
    }

    private fun View.auto(): PhotoGridView = findViewById(R.id.regression_photo_auto)
    private fun View.dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun measure(view: View, width: Int) {
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        view.layout(0, 0, width, view.measuredHeight)
    }

    private fun withFixture(block: (View) -> Unit) {
        ActivityScenario.launch(WidgetTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.layoutInflater.inflate(R.layout.view_photo_grid_regression, activity.container, false)
                for (id in intArrayOf(R.id.regression_photo_auto, R.id.regression_photo_fixed,
                    R.id.regression_photo_zero, R.id.regression_photo_readonly)) {
                    root.findViewById<PhotoGridView>(id).layoutTransition = null
                }
                block(root)
            }
        }
    }
}
