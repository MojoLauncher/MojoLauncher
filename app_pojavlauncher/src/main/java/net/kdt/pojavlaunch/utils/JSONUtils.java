package net.kdt.pojavlaunch.utils;

import net.kdt.pojavlaunch.Tools;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class JSONUtils {
    public static String[] insertJSONValueList(String[] args, Map<String, String> keyValueMap) {
        for (int i = 0; i < args.length; i++) {
            args[i] = insertSingleJSONValue(args[i], keyValueMap);
        }
        return args;
    }
    
    public static String insertSingleJSONValue(String value, Map<String, String> keyValueMap) {
        String valueInserted = value;
        for (Map.Entry<String, String> keyValue : keyValueMap.entrySet()) {
            valueInserted = valueInserted.replace("${" + keyValue.getKey() + "}", keyValue.getValue() == null ? "" : keyValue.getValue());
        }
        return valueInserted;
    }

    public static void writeToFile(File file, Object target) throws IOException {
        try(FileWriter fileWriter = new FileWriter(file)) {
            Tools.GLOBAL_GSON.toJson(target, fileWriter);
        }
    }

    public static <T> T readFromFile(File file, Class<T> clazs) throws IOException {
        try(FileReader fileReader = new FileReader(file)) {
            return Tools.GLOBAL_GSON.fromJson(fileReader, clazs);
        }
    }
}
