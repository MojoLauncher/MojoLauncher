package net.kdt.pojavlaunch.utils

import net.kdt.pojavlaunch.Tools
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

object JSONUtils {
    fun insertJSONValueList(
        args: MutableList<String?>,
        keyValueMap: MutableMap<String?, String?>
    ): MutableList<String?> {
        for (i in args.indices) {
            args.set(i, insertSingleJSONValue(args.get(i)!!, keyValueMap))
        }
        return args
    }

    fun insertSingleJSONValue(value: String, keyValueMap: MutableMap<String?, String?>): String {
        var valueInserted = value
        for (keyValue in keyValueMap.entries) {
            valueInserted = valueInserted.replace(
                "\${" + keyValue.key + "}",
                (if (keyValue.value == null) "" else keyValue.value)!!
            )
        }
        return valueInserted
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeToFile(file: File?, target: Any?) {
        FileWriter(file).use { fileWriter ->
            Tools.GLOBAL_GSON.toJson(target, fileWriter)
        }
    }

    @Throws(IOException::class)
    fun <T> readFromFile(file: File?, clazs: Class<T>): T? {
        FileReader(file).use { fileReader ->
            return Tools.GLOBAL_GSON.fromJson(fileReader, clazs)
        }
    }
}
