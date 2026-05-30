package net.kdt.pojavlaunch.utils

import java.util.AbstractList

/**
 * Provide a "mostly immutable" view to a "mother" list, by reference.
 * The difference from List.sublist() is:
 * - the ability to apply a FILTER on listGeneration
 * - "immutability", you can't add elements to the list from here, but it is backed by the real list.
 * @param <E>
</E> */
class FilteredSubList<E>(motherList: Array<E?>, filter: BasicPredicate<E?>) : AbstractList<E?>(),
    MutableList<E?> {
    private val mArrayList: ArrayList<E?>

    init {
        mArrayList = ArrayList<E?>()
        refresh(motherList, filter)
    }

    fun refresh(motherArray: Array<E?>, filter: BasicPredicate<E?>) {
        if (!mArrayList.isEmpty()) mArrayList.clear()

        for (item in motherArray) {
            if (filter.test(item)) {
                mArrayList.add(item)
            }
        }
        // Should we trim ?
        mArrayList.trimToSize()
    }

    override val size: Int
        get() = mArrayList.size

    override fun iterator(): MutableIterator<E?> {
        return mArrayList.iterator()
    }

    override fun remove(element: E?): Boolean {
        return mArrayList.remove(element)
    }

    override fun removeAll(elements: Collection<E?>): Boolean {
        return mArrayList.removeAll(elements)
    }

    override fun retainAll(elements: Collection<E?>): Boolean {
        return mArrayList.retainAll(elements)
    }

    override fun clear() {
        mArrayList.clear()
    }

    override fun get(index: Int): E? {
        return mArrayList.get(index)
    }

    override fun removeAt(index: Int): E? {
        return mArrayList.removeAt(index)
    }

    override fun listIterator(): MutableListIterator<E?> {
        return mArrayList.listIterator()
    }

    override fun listIterator(index: Int): MutableListIterator<E?> {
        return mArrayList.listIterator(index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E?> {
        return mArrayList.subList(fromIndex, toIndex)
    }


    // Predicate is API 24+, so micro backport
    fun interface BasicPredicate<E> {
        fun test(item: E?): Boolean
    }
}
