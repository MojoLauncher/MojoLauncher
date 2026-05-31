package net.kdt.pojavlaunch.utils

import net.kdt.pojavlaunch.Tools
import java.io.*

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

    @JvmStatic
    fun <T> readFromStream(stream: InputStream, clazs: Class<T>): T? {
        InputStreamReader(stream).use { reader ->
            return Tools.GLOBAL_GSON.fromJson(reader, clazs)
        }
    }
}
