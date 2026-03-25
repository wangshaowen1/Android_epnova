package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout

class FixedFlingAppBarLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppBarLayout(context, attrs) {


    private class FixedFlingAppBarLayout : BaseBehavior<AppBarLayout>() {

        override fun onNestedPreFling(
            coordinatorLayout: CoordinatorLayout,
            child: AppBarLayout,
            target: View,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            tryCancelAppBarLayoutFling(child)
            return super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY)
        }

        private fun tryCancelAppBarLayoutFling(appBarLayout: AppBarLayout) {
            getFlingRunnable()?.let {
                appBarLayout.removeCallbacks(it)
            }
        }

        private fun getFlingRunnable(): Runnable? {
            return try {
                val superClass = this::class.java.superclass.superclass
                superClass.getDeclaredField("flingRunnable").let {
                    it.isAccessible = true
                    it.get(this) as? Runnable
                }
            } catch (e: Throwable) {
                null
            }
        }
    }

    override fun getBehavior(): CoordinatorLayout.Behavior<AppBarLayout> {
        return FixedFlingAppBarLayout()
    }
}
