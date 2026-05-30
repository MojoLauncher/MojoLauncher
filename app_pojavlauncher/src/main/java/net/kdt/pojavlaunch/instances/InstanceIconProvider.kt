package net.kdt.pojavlaunch.instances

import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import net.ashmeet.hyperlauncher.R
import java.io.File
import java.util.Objects

object InstanceIconProvider {
    const val FALLBACK_ICON_NAME: String = "default"
    private val sIconCache: MutableMap<Int?, Drawable?> = HashMap<Int?, Drawable?>()
    private val sStaticIconCache: MutableMap<String?, Drawable?> = HashMap<String?, Drawable?>()
    private val sStaticIcons: MutableMap<String?, Int?> = HashMap<String?, Int?>()

    init {
        sStaticIcons.put("default", R.drawable.ic_package)
        sStaticIcons.put("fabric", R.drawable.ic_fabric)
        sStaticIcons.put("quilt", R.drawable.ic_quilt)
        sStaticIcons.put("forge", R.drawable.ic_forge)
        sStaticIcons.put("neoforge", R.drawable.ic_neoforge)
    }

    /**
     * Fetch an icon from the cache, or load it if it's not cached.
     * @param resources the Resources object, used for creating drawables
     * @param instance the instance
     * @return an icon drawable
     */
    @JvmStatic
    fun fetchIcon(resources: Resources, instance: DisplayInstance): Drawable {
        val identityHashCode = System.identityHashCode(instance)

        val cachedIcon = sIconCache.get(identityHashCode)
        if (cachedIcon != null) return cachedIcon

        val instanceIcon =
            fetchInstanceFileIcon(resources, identityHashCode, instance.instanceIconLocation)
        if (instanceIcon != null) return instanceIcon

        return fetchStaticIcon(resources, identityHashCode, instance.icon)
    }

    /**
     * Drop an icon from the icon cache. When dropped, it's Drawable will be re-read from the
     * instance icon file (or re-fetched from the static cache)
     * @param key the instance
     */
    @JvmStatic
    fun dropIcon(key: Instance) {
        sIconCache.remove(System.identityHashCode(key))
    }

    private fun fetchInstanceFileIcon(
        resources: Resources?,
        identityHash: Int,
        iconLocation: File
    ): Drawable? {
        if (!iconLocation.isFile() || !iconLocation.canRead()) return null
        val iconBitmap = BitmapFactory.decodeFile(iconLocation.getAbsolutePath())
        if (iconBitmap == null) return null
        val iconDrawable: Drawable = BitmapDrawable(resources, iconBitmap)
        sIconCache.put(identityHash, iconDrawable)
        return iconDrawable
    }

    private fun fetchStaticIcon(resources: Resources, identityHash: Int, icon: String?): Drawable {
        var staticIcon = sStaticIconCache.get(icon)
        if (staticIcon == null) {
            if (icon != null) staticIcon = getStaticIcon(resources, icon)
            if (staticIcon == null) staticIcon = fetchFallbackIcon(resources)
            sStaticIconCache.put(icon, staticIcon)
        }
        sIconCache.put(identityHash, staticIcon)
        return staticIcon
    }

    private fun fetchFallbackIcon(resources: Resources): Drawable {
        var fallbackIcon = sStaticIconCache.get(FALLBACK_ICON_NAME)
        if (fallbackIcon == null) {
            fallbackIcon =
                Objects.requireNonNull<Drawable?>(getStaticIcon(resources, FALLBACK_ICON_NAME))
            sStaticIconCache.put(FALLBACK_ICON_NAME, fallbackIcon)
        }
        return fallbackIcon
    }

    private fun getStaticIcon(resources: Resources, icon: String): Drawable? {
        val staticIconResource = getStaticIconResource(icon)
        if (staticIconResource == -1) return null
        return ResourcesCompat.getDrawable(resources, staticIconResource, null)
    }

    private fun getStaticIconResource(icon: String?): Int {
        val iconResource = sStaticIcons.get(icon)
        if (iconResource == null) return -1
        return iconResource
    }

    /**
     * Check whether the icon under the specified name is a static icon available in the provider.
     * @param name static icon name to check
     * @return whether the icon is available or not
     */
    fun hasStaticIcon(name: String?): Boolean {
        return sStaticIcons.containsKey(name)
    }
}
