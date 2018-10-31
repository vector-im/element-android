package im.vector.riotredesign.core.epoxy

import android.support.annotation.IdRes
import android.support.annotation.LayoutRes
import android.view.View
import com.airbnb.epoxy.EpoxyModel
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class KotlinModel(
        @LayoutRes private val layoutRes: Int
) : EpoxyModel<View>() {

    private var view: View? = null
    private var onBindCallback: (() -> Unit)? = null

    abstract fun bind()

    override fun bind(view: View) {
        this.view = view
        onBindCallback?.invoke()
        bind()
    }

    override fun unbind(view: View) {
        this.view = null
    }

    fun onBind(lambda: (() -> Unit)?): KotlinModel {
        onBindCallback = lambda
        return this
    }

    override fun getDefaultLayout() = layoutRes

    protected fun <V : View> bind(@IdRes id: Int) = object : ReadOnlyProperty<KotlinModel, V> {
        override fun getValue(thisRef: KotlinModel, property: KProperty<*>): V {
            // This is not efficient because it looks up the view by id every time (it loses
            // the pattern of a "holder" to cache that look up). But it is simple to use and could
            // be optimized with a map
            @Suppress("UNCHECKED_CAST")
            return view?.findViewById(id) as V?
                    ?: throw IllegalStateException("View ID $id for '${property.name}' not found.")
        }
    }
}