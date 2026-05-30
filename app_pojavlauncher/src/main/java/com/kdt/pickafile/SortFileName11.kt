package com.kdt.pickafile

import java.io.File

class SortFileName : Comparator<File?> {
    override fun compare(f1: File?, f2: File?): Int {
        if (f1 == null || f2 == null) return 0
        return f1.name.compareTo(f2.name, ignoreCase = true)
    }
}
