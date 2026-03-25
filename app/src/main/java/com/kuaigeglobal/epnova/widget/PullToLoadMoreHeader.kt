package com.kuaigeglobal.epnova.widget

import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import com.kuaigeglobal.epnova.R
import com.shizhefei.view.coolrefreshview.CoolRefreshView
import com.shizhefei.view.coolrefreshview.PullHeader
import com.shizhefei.view.coolrefreshview.PullHeader.DefaultConfig


class PullToLoadMoreHeader : PullHeader {
    private var mProgressBar: View? = null

    override fun createHeaderView(refreshView: CoolRefreshView): View {
        val headView: View = LayoutInflater.from(refreshView.context).inflate(R.layout.pull_to_load_more_header, refreshView, false)
        mProgressBar = headView.findViewById(R.id.pull_to_refresh_progress)
        return headView
    }

    /**
     * 开始拖动
     */
    override fun onPullBegin(refreshView: CoolRefreshView?) {
        isDownArrow = true
        mProgressBar?.isVisible = true
    }

    /**
     * 位置变化
     *
     * @param refreshView
     * @param status          状态 /没有任何操作
     * public final static byte PULL_STATUS_INIT = 1;
     * //开始下拉
     * public final static byte PULL_STATUS_TOUCH_MOVE = 2;
     * //回到原始位置
     * public final static byte PULL_STATUS_RESET = 3;
     * //刷新中
     * public final static byte PULL_STATUS_REFRESHING = 4;
     * //刷新完成
     * public final static byte PULL_STATUS_COMPLETE = 5;
     * @param dy              下拉事件的位移
     * @param currentDistance 当前位移的距离
     */
    override fun onPositionChange(refreshView: CoolRefreshView?, status: Int, dy: Int, currentDistance: Int) {
    }

    private var isDownArrow = false

    /**
     * 刷新中
     */
    override fun onRefreshing(refreshView: CoolRefreshView?) {
        mProgressBar?.isVisible = true
    }

    /**
     * 没有刷新的释放回去
     */
    override fun onReset(refreshView: CoolRefreshView?, pullRelease: Boolean) {
        mProgressBar?.isVisible = false
    }

    /**
     * 设置刷新完成，并且释放回去
     */
    override fun onPullRefreshComplete(frame: CoolRefreshView?) {
        mProgressBar?.isVisible = false
    }

    override fun getConfig(): PullHeader.Config {
        return config
    }

    //使用默认的配置
    private val config = DefaultConfig()
}