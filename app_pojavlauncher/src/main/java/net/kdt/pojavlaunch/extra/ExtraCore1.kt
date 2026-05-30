package net.kdt.pojavlaunch.extra

import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.Volatile

/**
 * Class providing callback across all of a program
 * to allow easy thread safe implementations of UI update without context leak
 * It is also perfectly engineered to make it unpleasant to use.
 * 
 * This class uses a singleton pattern to simplify access to it
 */
class ExtraCore  // No unwanted instantiation
private constructor() {
    // Store the key-value pair
    private val mValueMap: MutableMap<String?, Any?> = ConcurrentHashMap<String?, Any?>()

    // Store what each ExtraListener listen to
    private val mListenerMap: MutableMap<String?, ConcurrentLinkedQueue<WeakReference<ExtraListener<*>?>>?> =
        ConcurrentHashMap<String?, ConcurrentLinkedQueue<WeakReference<ExtraListener<*>?>>?>()

    companion object {
        // Singleton instance
        @Volatile
        private var sExtraCoreSingleton: ExtraCore? = null

        private val instance: ExtraCore?
            // All public methods will pass through this one
            get() {
                if (sExtraCoreSingleton == null) {
                    synchronized(ExtraCore::class.java) {
                        if (sExtraCoreSingleton == null) {
                            sExtraCoreSingleton = ExtraCore()
                        }
                    }
                }
                return sExtraCoreSingleton
            }

        /**
         * Set the value associated to a key and trigger all listeners
         * @param key The key
         * @param value The value
         */
        @Suppress("UNCHECKED_CAST")
        fun setValue(key: String?, value: Any?) {
            if (value == null || key == null) return  // null values create an NPE on insertion


            instance!!.mValueMap.put(key, value)
            val extraListenerList: ConcurrentLinkedQueue<WeakReference<ExtraListener<*>?>>? =
                instance!!.mListenerMap.get(key)
            if (extraListenerList == null) return  //No listeners

            for (listenerRef in extraListenerList) {
                val listener = listenerRef.get()
                if (listener == null) {
                    extraListenerList.remove(listenerRef)
                    continue
                }

                //Notify the listener about a state change and remove it if asked for
                val castListener = listener as? ExtraListener<Any>
                if (castListener?.onValueSet(key, value) == true) {
                    removeExtraListenerFromValue(key, listener)
                }
            }
        }

        /** @return The value behind the key
         */
        fun getValue(key: String?): Any? {
            return instance!!.mValueMap.get(key)
        }

        /** @return The value behind the key, or the default value
         */
        fun getValue(key: String?, defaultValue: Any?): Any? {
            val value: Any? = instance!!.mValueMap.get(key)
            return if (value != null) value else defaultValue
        }

        fun consumeValue(key: String?): Any? {
            val value: Any? = instance!!.mValueMap.get(key)
            instance!!.mValueMap.remove(key)
            return value
        }

        /**
         * Link an ExtraListener to a value
         * @param key The value key to look for
         * @param listener The ExtraListener to link
         */
        fun addExtraListener(key: String?, listener: ExtraListener<*>?) {
            if (listener == null) return
            var listenerList: ConcurrentLinkedQueue<WeakReference<ExtraListener<*>?>>? =
                instance!!.mListenerMap.get(key)
            // Look for new sets
            if (listenerList == null) {
                listenerList = ConcurrentLinkedQueue<WeakReference<ExtraListener<*>?>>()
                instance!!.mListenerMap.put(key, listenerList)
            }

            // This is kinda naive, I should look for duplicates
            listenerList.add(WeakReference<ExtraListener<*>?>(listener))
        }

        /**
         * Unlink an ExtraListener from a value.
         * Unlink null references found along the way
         * @param key The value key to ignore now
         * @param listener The ExtraListener to unlink
         */
        fun removeExtraListenerFromValue(key: String?, listener: ExtraListener<*>?) {
            val listenerList: ConcurrentLinkedQueue<WeakReference<ExtraListener<*>?>>? =
                instance!!.mListenerMap.get(key)
            if (listenerList == null) return

            // Removes all occurrences of ExtraListener and all null references
            for (listenerWeakReference in listenerList) {
                val actualListener = listenerWeakReference.get()

                if (actualListener == null || actualListener === listener) {
                    listenerList.remove(listenerWeakReference)
                }
            }
        }

    }
}
