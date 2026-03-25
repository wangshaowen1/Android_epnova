package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setPadding
import com.kuaigeglobal.epnova.common.ext.formatDuration
import com.kuaigeglobal.epnova.common.ext.updateLayoutParams
import com.kuaigeglobal.epnova.databinding.LayoutPlayerSeekbarBinding
import com.nhsd.basic.ext.dp

/**
 * 播放器进度条
 */
class PlayerSeekbar : FrameLayout {
    interface OnSeekListener {
        fun onDragEnd(position: Long)

        fun onDrag()
    }
    var currentPosition = 0L
        set(value) {
            if (field != value) {
                field = value
                update()
            }
        }
    var duration = 0L
        set(value) {
            if (value >= 0) {
                binding.tvDuration.text = value.formatDuration()
                field = value
                update()
            }
        }
    var onSeekListener: OnSeekListener? = null

    /**
     * 拖动状态
     */
    private var dragging = false
        set(value) {
            if (field != value) {
                requestDisallowInterceptTouchEvent(value)
                if (value) {
                    onSeekListener?.onDrag()
                    binding.times.visibility = View.VISIBLE
                    binding.progressBg.updateLayoutParams<ConstraintLayout.LayoutParams> { it.height = 4.dp }
                    binding.thumb.setPadding(7.dp)
                } else {
                    binding.times.visibility = View.GONE
                    // 【新增】拖动结束时，隐藏左右指示图标
                    binding.dragLeft.visibility = View.GONE
                    binding.dragRight.visibility = View.GONE
                    binding.progressBg.updateLayoutParams<ConstraintLayout.LayoutParams> { it.height = 2.dp }
                    binding.thumb.setPadding(12.dp)
                    onSeekListener?.onDragEnd(dragValue)
                    currentPosition = dragValue
                }
                field = value
            }
        }
    private var dragValue = 0L
        set(value) {
            if (field != value) {
                field = value
                update()
            }
        }

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs)
    }

    private val binding by lazy {
        LayoutPlayerSeekbarBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        binding.tvDuration.text = duration.formatDuration()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                onTouch(event)
                dragging = true
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                onTouch(event)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                onTouch(event)
                dragging = false
            }
        }
        return super.onTouchEvent(event)
    }

    private fun onTouch(event: MotionEvent) {
        dragValue = (((event.x - binding.progressBg.x) / binding.progressBg.width).coerceAtMost(1f)
            .coerceAtLeast(0f) * duration).toLong()
    }

    private fun update() {
        if (duration < 1) {
            binding.tvProgress.text = 0L.formatDuration()
            binding.tvDuration.text = binding.tvProgress.text
            binding.progress.updateLayoutParams<ConstraintLayout.LayoutParams> {
                it.horizontalBias = 0f
            }
            // 这种情况下也隐藏图标
            binding.dragLeft.visibility = View.GONE
            binding.dragRight.visibility = View.GONE
        } else {
            val position = if (dragging) dragValue else currentPosition
            binding.tvProgress.text = position.formatDuration()
            binding.progress.updateLayoutParams<ConstraintLayout.LayoutParams> {
                it.horizontalBias = (1f * position / duration).coerceAtMost(1f)
            }

            if (dragging) {
                when {
                    dragValue > currentPosition -> {
                        // 向右拖动（快进）
                        binding.dragRight.visibility = View.VISIBLE
                        binding.dragLeft.visibility = View.GONE
                    }
                    dragValue < currentPosition -> {
                        // 向左拖动（后退）
                        binding.dragRight.visibility = View.GONE
                        binding.dragLeft.visibility = View.VISIBLE
                    }
                    else -> {
                        // 回到了初始点
                        binding.dragRight.visibility = View.GONE
                        binding.dragLeft.visibility = View.GONE
                    }
                }
            }
        }
    }

}
