package com.kuaigeglobal.epnova.widget

import android.graphics.Color
import android.view.View
import android.widget.GridLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.widget.testing.WidgetTestActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TagControlsRegressionTest {
    @Test
    fun flow_repeatedMeasureKeepsExactHeightAndRemeasuresChangedChild() = withFixture { root ->
        val flow = root.findViewById<TagLayout>(R.id.regression_tag_flow)
        flow.setTags("A", "B")
        measure(flow, 1000, 200)
        flow.forceLayout()
        measure(flow, 1000, 200)
        assertEquals(200, flow.measuredHeight)
        val child = flow.toTagView(flow.getChildAt(0)).tagText
        child.text = "A much longer label"
        val oldWidth = child.measuredWidth
        measure(flow, 1000, 200)
        assertTrue(child.measuredWidth > oldWidth)
    }

    @Test
    fun flow_lazyCacheDoesNotMeasureHiddenTagsAndKeepsStyle() = withFixture { root ->
        val flow = root.findViewById<TagLayout>(R.id.regression_tag_flow)
        flow.setTags("A", "B", "C")
        measure(flow, 1000, 200)
        val text = flow.toTagView(flow.getChildAt(0)).tagText
        val size = text.textSize
        flow.setTags("D")
        var shown = -1
        flow.setOnTagsLayoutListener { _, count, _ -> shown = count }
        measure(flow, 1000, 200)
        assertEquals(1, shown)
        assertEquals(View.GONE, flow.getChildAt(1).visibility)
        assertEquals(0, flow.getChildAt(1).width)
        assertEquals(Color.BLACK, text.currentTextColor)
        assertEquals(size, text.textSize, 0f)
    }

    @Test
    fun flow_selectionIsIdempotentAndDeletionUsesLivePosition() = withFixture { root ->
        val flow = root.findViewById<TagLayout>(R.id.regression_tag_flow)
        flow.setTags(listOf("A", "B", "C")) // fixed-size input must not be mutated
        repeat(3) { flow.selectTagPosition(0) }
        flow.selectTagPosition(1)
        assertEquals(listOf(0, 1), flow.selectedTagPositions)
        repeat(3) { flow.unSelectTagPosition(0) }
        flow.selectTagPosition(2)
        assertEquals(listOf(1, 2), flow.selectedTagPositions)
        var clicked = -1
        flow.setOnItemClickListener { _, position -> clicked = position; true }
        flow.removeTag("A")
        flow.getChildAt(1).performClick()
        assertEquals(1, clicked)
        assertEquals(listOf("B", "C"), flow.tags)
        flow.selectTagPosition(100)
        flow.setTags(*emptyArray<String>())
        assertTrue(flow.tags.isEmpty())
        assertTrue(flow.selectedTagPositions.isEmpty())
    }

    @Test
    fun flow_maxLinesClearsPreviouslyLaidOutBounds() = withFixture { root ->
        val flow = root.findViewById<TagLayout>(R.id.regression_tag_flow)
        flow.setTags("AAAAAA", "BBBBBB", "CCCCCC")
        flow.setMaxLines(3)
        for (index in 0 until flow.childCount) {
            flow.getChildAt(index).layoutParams.width = 80
        }
        measure(flow, 100, 300)
        assertTrue(flow.getChildAt(1).height > 0)
        flow.setMaxLines(1)
        measure(flow, 100, 300)
        assertEquals(0, flow.getChildAt(1).height)
        flow.setMaxLines(0)
        measure(flow, 100, 300)
        assertEquals(0, flow.tagLineCount())
    }

    @Test
    fun flow_stateRestoreRebuildsDataBeforeSelection() = withFixture { root ->
        val original = root.findViewById<TagLayout>(R.id.regression_tag_flow)
        original.setTags("A", "B")
        original.selectTagPosition(1)
        val saved = original.onSaveInstanceState()
        original.setTags("X")
        original.onRestoreInstanceState(saved)
        assertEquals(listOf("A", "B"), original.tags)
        assertEquals(listOf(1), original.selectedTagPositions)
    }

    @Test
    fun flow_noCacheLimitBackfillsAfterRemovalAndSameDataKeepsSelection() = withFixture { root ->
        val flow = root.findViewById<TagLayout>(R.id.regression_tag_no_cache)
        flow.setTags("A", "B", "C")
        assertEquals(2, flow.childCount)
        flow.selectTagPosition(1)
        flow.setTags("A", "B", "C")
        assertEquals(listOf(1), flow.selectedTagPositions)
        flow.removeTag("A")
        assertEquals(2, flow.childCount)
        assertEquals("C", flow.toTagView(flow.getChildAt(1)).tagText.text.toString())
        assertEquals(listOf(0), flow.selectedTagPositions)
        flow.tags.clear()
        assertEquals(listOf("B", "C"), flow.tags)
    }

    @Test
    fun selectionLimitReductionReconcilesExistingSelections() = withFixture { root ->
        val flow = root.findViewById<TagLayout>(R.id.regression_tag_flow)
        val grid = root.findViewById<TagGridLayout>(R.id.regression_tag_grid)
        flow.setTags("A", "B")
        grid.setTags("A", "B")
        flow.selectTagPositions(0, 1)
        grid.selectTagPositions(0, 1)
        flow.setMaximumSelectionCount(1)
        grid.setMaximumSelectionCount(1)
        assertEquals(listOf(0), flow.selectedTagPositions)
        assertEquals(listOf(0), grid.selectedTagPositions)
        flow.setMaximumSelectionCount(0)
        grid.setMaximumSelectionCount(0)
        flow.getChildAt(0).performClick()
        grid.getChildAt(0).performClick()
        assertTrue(flow.selectedTagPositions.isEmpty())
        assertTrue(grid.selectedTagPositions.isEmpty())
    }

    @Test
    fun grid_reusesUnchangedDataAndExcludesPlaceholdersFromSelection() = withFixture { root ->
        val grid = root.findViewById<TagGridLayout>(R.id.regression_tag_grid)
        grid.setTags("A", "B", "C", "D")
        val first = grid.getChildAt(0)
        val firstParams = first.layoutParams as GridLayout.LayoutParams
        val secondParams = grid.getChildAt(1).layoutParams as GridLayout.LayoutParams
        val nextRowParams = grid.getChildAt(3).layoutParams as GridLayout.LayoutParams
        assertTrue(secondParams.leftMargin > firstParams.leftMargin)
        assertTrue(nextRowParams.topMargin > firstParams.topMargin)
        grid.setTags("A", "B", "C", "D")
        assertSame(first, grid.getChildAt(0))
        grid.tags.clear()
        assertEquals(4, grid.tags.size)
        grid.selectTagPositions(listOf(0, 0, 1, 2, 4, 5, -1, 100))
        assertEquals(listOf(0, 1), grid.selectedTagPositions)
        grid.selectTagPositions(null as List<Int>?)
        repeat(3) { grid.selectTagPosition(0) }
        grid.selectTagPosition(1)
        assertEquals(listOf(0, 1), grid.selectedTagPositions)
        grid.setTags("New")
        grid.getChildAt(0).performClick()
        assertEquals(listOf(0), grid.selectedTagPositions)
    }

    @Test
    fun grid_singleEmptySelectionClearsReferenceAndProgrammaticConstructorWorks() = withFixture { root ->
        val grid = TagGridLayout(root.context)
        grid.setTagSelectMode(TagGridLayout.SELECT_MODE_SINGLE)
        grid.setTags("A", "B")
        grid.selectTagPosition(0)
        assertNotNull(grid.currentSelected)
        grid.selectTagPositions(emptyList())
        assertNull(grid.currentSelected)
        grid.selectTagPosition(100)
        grid.setTags(null as List<String>?)
        assertEquals(0, grid.childCount)
    }

    @Test
    fun recycler_xmlSelectionLimitAndCapacityUseDataCount() = withFixture { root ->
        val tags = root.findViewById<TagRecycleView>(R.id.regression_tag_recycler)
        tags.setTags("A", "B", "C", "D")
        tags.addTags(listOf("E", "F")) // RecyclerView has not laid out any children yet
        tags.add("G")
        assertEquals(listOf("A", "B", "C", "D"), tags.tags)
        tags.tags.clear()
        assertEquals(4, tags.tags.size)
        val recycler = prepareRecycler(tags)
        var rejected = 0
        tags.setOnSelectOutRangeListener { _, _ -> rejected++ }
        recycler.findViewHolderForAdapterPosition(0)!!.itemView.performClick()
        recycler.findViewHolderForAdapterPosition(1)!!.itemView.performClick()
        assertEquals(1, rejected)
        assertTrue(recycler.findViewHolderForAdapterPosition(0)!!.itemView.isSelected)
        assertFalse(recycler.findViewHolderForAdapterPosition(1)!!.itemView.isSelected)
    }

    @Test
    fun recycler_unboundDeleteIsIgnoredAndDuplicateDeleteUsesPosition() = withFixture { root ->
        val tags = root.findViewById<TagRecycleView>(R.id.regression_tag_recycler)
        tags.setTags("A", "A", "B")
        val recycler = prepareRecycler(tags)
        val adapter = recycler.adapter!!
        adapter.createViewHolder(recycler, 0).itemView.findViewById<View>(R.id.delete).performClick()
        assertEquals(3, tags.tags.size)
        recycler.findViewHolderForAdapterPosition(1)!!.itemView.performClick()
        recycler.findViewHolderForAdapterPosition(1)!!.itemView.findViewById<View>(R.id.delete).performClick()
        measure(recycler, 1000, 1000)
        assertEquals(listOf("A", "B"), tags.tags)
        assertFalse(recycler.findViewHolderForAdapterPosition(0)!!.itemView.isSelected)
        assertFalse(recycler.findViewHolderForAdapterPosition(1)!!.itemView.isSelected)
    }

    @Test
    fun recycler_singleSelectionSurvivesRebindAndDataReplacementClearsIt() = withFixture { root ->
        val tags = root.findViewById<TagRecycleView>(R.id.regression_tag_single)
        tags.setTags("A", "B")
        val recycler = prepareRecycler(tags)
        val holder = recycler.findViewHolderForAdapterPosition(0)!!
        holder.itemView.performClick()
        assertTrue(holder.itemView.isSelected)
        @Suppress("UNCHECKED_CAST")
        val adapter = recycler.adapter as RecyclerView.Adapter<RecyclerView.ViewHolder>
        adapter.onBindViewHolder(holder, 1)
        assertFalse(holder.itemView.isSelected)
        adapter.onBindViewHolder(holder, 0)
        assertTrue(holder.itemView.isSelected)
        val second = recycler.findViewHolderForAdapterPosition(1)!!
        second.itemView.performClick()
        assertFalse(holder.itemView.isSelected)
        assertTrue(second.itemView.isSelected)
        tags.setTags("X", "Y")
        measure(recycler, 1000, 1000)
        assertFalse(recycler.findViewHolderForAdapterPosition(0)!!.itemView.isSelected)
    }

    @Test
    fun recycler_partialUpdatesRefreshPreviousAndNewLastRowSpacing() = withFixture { root ->
        val tags = root.findViewById<TagRecycleView>(R.id.regression_tag_single)
        tags.setTags("A", "B", "C")
        val recycler = prepareRecycler(tags)
        val manager = recycler.layoutManager!!
        val previousLast = recycler.findViewHolderForAdapterPosition(2)!!.itemView
        val bottomSpace = manager.getBottomDecorationHeight(previousLast)
        assertTrue(bottomSpace > 0)
        tags.add("D")
        measure(recycler, 1000, 1000)
        assertEquals(0, manager.getBottomDecorationHeight(
            recycler.findViewHolderForAdapterPosition(2)!!.itemView))
        assertEquals(bottomSpace, manager.getBottomDecorationHeight(
            recycler.findViewHolderForAdapterPosition(3)!!.itemView))
        tags.remove("D")
        measure(recycler, 1000, 1000)
        assertEquals(bottomSpace, manager.getBottomDecorationHeight(
            recycler.findViewHolderForAdapterPosition(2)!!.itemView))
    }

    @Test
    fun recycler_callbackMayReplaceDataWithoutSelectingStaleItem() = withFixture { root ->
        val tags = root.findViewById<TagRecycleView>(R.id.regression_tag_recycler)
        tags.setTags("A", "B")
        val recycler = prepareRecycler(tags)
        var selectionEvents = 0
        tags.setOnSelectChangeListener { _, _, _ -> selectionEvents++ }
        tags.setOnItemClickListener { _, _ -> tags.setTags("X"); false }
        recycler.findViewHolderForAdapterPosition(0)!!.itemView.performClick()
        assertEquals(0, selectionEvents)
        assertEquals(listOf("X"), tags.tags)
        tags.clear()
        assertEquals(0, recycler.adapter!!.itemCount)
    }

    private fun prepareRecycler(tags: TagRecycleView): RecyclerView =
        (tags.getChildAt(0) as RecyclerView).also {
            it.itemAnimator = null
            measure(it, 1000, 1000)
        }

    private fun measure(view: View, width: Int, height: Int) {
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, width, height)
    }

    private fun withFixture(block: (View) -> Unit) {
        ActivityScenario.launch(WidgetTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.layoutInflater.inflate(R.layout.view_tag_regression, activity.container, false)
                block(root)
            }
        }
    }
}
