package com.kuaigeglobal.epnova.common.ext

import androidx.viewbinding.ViewBinding
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.item.AppItemViewModel

/**
 * @deprecated 此方法已过时，不推荐使用。基于 position 的缓存机制存在以下问题：
 * 1. 当列表发生插入/删除操作时，position 会变化导致缓存错乱
 * 2. ViewModel 未正确更新，相同位置数据变化时会创建全新实例
 * 3. 缺少生命周期管理，可能造成内存泄漏
 *
 * 请改用基于稳定键(key)的新方法：
 * ```
 * fun <VM : BaseUpdatableItemViewModel<Item>, Item> ViewBinding.getVm(
 *     key: Any, // 使用数据唯一ID作为键
 *     item: Item,
 *     vmType: Class<VM>,
 *     initCallback: ((vm: VM) -> Unit)? = null
 * ): VM
 * ```
 *
 * @param position 列表位置（不稳定标识）
 * @param item 数据项
 * @param vmType ViewModel类类型
 * @param initCallback 初始化回调
 * @return ViewModel实例
 */
@Deprecated(
    message = "This position-based caching is deprecated. Use key-based caching instead.",
    replaceWith = ReplaceWith(
        expression = "getVm(key = item.id, item = item, vmType = vmType, initCallback = initCallback)",
        imports = ["com.nhsd.basic.ext.ItemViewModelExtensions.Kt"]
    )
)
fun <VM : AppItemViewModel<Item>, Item> ViewBinding.getVm(
    position: Int,
    item: Item?,
    vmType: Class<VM>,
    initCallback: ((vm: VM) -> Unit)? = null
): VM {
    var vm = getVm<VM>(position)
    if (vm == null || vm.shouldUpdate(item)) {
        val c = vmType.getConstructor((item as Any)::class.java)
        vm = c.newInstance(item) as VM
        vm.data.set(item)
        initCallback?.invoke(vm)
        putVm(position, vm)
    }
    return vm
}

fun <VM : AppItemViewModel<*>> ViewBinding.getVm(position: Int): VM? {
    val map = root.getTag(R.id.vm_key) as? MutableMap<Int, AppItemViewModel<*>>
    return map?.get(position) as? VM
}

fun <VM : AppItemViewModel<*>> ViewBinding.putVm(position: Int, vm: VM) {
    val existing = root.getTag(R.id.vm_key)
    val map = if (existing is MutableMap<*, *>) {
        existing as MutableMap<Int, AppItemViewModel<*>>
    } else {
        mutableMapOf<Int, AppItemViewModel<*>>().also {
            root.setTag(R.id.vm_key, it)
        }
    }
    map[position] = vm
}

fun AppItemViewModel<*>?.shouldUpdate(item: Any?): Boolean {
    return this?.data?.get() != item
}


private val VM_CACHE_KEY = R.id.vm_key  // 在ids.xml中定义

// 添加最大缓存限制
private const val MAX_CACHE_SIZE = 300

/**
 * 获取具有LRU特性的缓存Map
 */
fun ViewBinding.getCacheMap(): LinkedHashMap<Any?, AppItemViewModel<*>> {
    val tag = root.getTag(VM_CACHE_KEY)
    return if (tag is LinkedHashMap<*, *>) {
        @Suppress("UNCHECKED_CAST")
        tag as LinkedHashMap<Any?, AppItemViewModel<*>>
    } else {
        // 创建具有LRU特性的LinkedHashMap
        val newMap = object : LinkedHashMap<Any?, AppItemViewModel<*>>(
            MAX_CACHE_SIZE, 0.75f, true // accessOrder=true 表示按访问顺序排序
        ) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Any?, AppItemViewModel<*>>): Boolean {
                // 当大小超过限制时移除最久未使用的条目
                return size > MAX_CACHE_SIZE
            }
        }
        root.setTag(VM_CACHE_KEY, newMap)
        newMap
    }
}

/**
 * 获取或创建ViewModel (使用稳定键)
 */
inline fun <reified VM : AppItemViewModel<Item>, Item> ViewBinding.getVm(
    key: Any?,
    item: Item?,
    noinline viewModelFactory: (() -> VM)? = null,
    noinline initCallback: ((vm: VM) -> Unit)? = null
): VM {
    val cacheMap = getCacheMap()
    var vm = cacheMap[key] as? VM

    return when {
        vm == null -> {
            // 创建新实例
            vm = if (viewModelFactory != null) {
                viewModelFactory()
            } else {
                requireNotNull(item) { "Item must not be null when using reflection-based creation" }
                createViewModelInstance<VM, Item>(item)
            }
            initCallback?.invoke(vm)
            cacheMap[key] = vm
            vm
        }

        vm.shouldUpdate(item) -> {
            // 更新现有实例数据
            vm.updateData(item)
            vm
        }

        else -> vm
    }
}

/**
 * 创建ViewModel实例 (使用reified参数简化)
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified VM : AppItemViewModel<Item>, Item> ViewBinding.createViewModelInstance(
    item: Item?
): VM {
    return try {
        // 尝试通过构造函数创建
        VM::class.java.getDeclaredConstructor(item!!::class.java).newInstance(item)
    } catch (e: NoSuchMethodException) {
        // 回退到无参数构造函数 + setData
        VM::class.java.getDeclaredConstructor().newInstance().apply {
            updateData(item)
        }
    } as VM
}


/**
 * 清理指定键的缓存
 */
fun ViewBinding.clearVmCacheForKey(key: Any) {
    (getCacheMap() as? MutableMap<Any, *>)?.remove(key)
}

/**
 * 清理所有缓存
 */
fun ViewBinding.clearAllVmCache() {
    root.getTag(VM_CACHE_KEY)?.let {
        (it as? MutableMap<*, *>)?.clear()
    }
    root.setTag(VM_CACHE_KEY, null)
}

