package com.kdt.pickafile

import java.io.File

abstract class FileSelectedListener {
    abstract fun onFileSelected(file: File?, path: String?)
    fun onFileLongClick(file: File?, path: String?) {}
}
