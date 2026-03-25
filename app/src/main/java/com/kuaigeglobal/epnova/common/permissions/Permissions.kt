package com.kuaigeglobal.epnova.common.permissions

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.nhsd.permisson.PermissionMediator
import com.nhsd.permisson.PermissionX
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class Permissions private constructor(
    private val owner: LifecycleOwner
) : LifecycleEventObserver {

    init {
        owner.lifecycle.addObserver(this)
    }

    companion object {
        private val cache = HashMap<String, Permissions>()

        @JvmStatic
        private fun get(owner: LifecycleOwner): Permissions {
            return cache[owner.toString()] ?: Permissions(owner).also {
                if (owner is AppCompatActivity || owner is Fragment) {
                    cache[owner.toString()] = it
                }
            }
        }

        @JvmStatic
        fun of(activity: FragmentActivity) = get(activity)
        @JvmStatic
        fun of(fragment: Fragment) = get(fragment)
    }

    suspend fun request(
        explainText: String,
        vararg permissions: String
    ): Boolean = suspendCancellableCoroutine { cont ->

        val mediator: PermissionMediator =
            when (owner) {
                is FragmentActivity -> {
                    PermissionX.with(owner)
                }

                is Fragment -> {
                    PermissionX.with(owner)
                }

                else -> {
                    cont.resume(false)
                    return@suspendCancellableCoroutine
                }
            }

        mediator.permissions(*permissions)
            .onRequestRationale(explainText)
            .request { result ->
                cont.resume(result.isAllGranted)
            }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (source.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            source.lifecycle.removeObserver(this)
            cache.remove(source.toString())
        }
    }
}
