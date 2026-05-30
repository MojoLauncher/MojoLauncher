package net.kdt.pojavlaunch.utils

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

object GsonJsonUtils {
    /**
     * Safely converts a JsonElement into a JsonObject.
     * @param element the input JsonElement
     * @return the JsonObject if:
     * the JsonElement is not null
     * the JsonElement is not Json null
     * the JsonElement is a JsonObjet
     * null otherwise
     */
    fun getJsonObjectSafe(element: JsonElement?): JsonObject? {
        if (element == null) return null
        if (element.isJsonNull() || !element.isJsonObject()) return null
        return element.getAsJsonObject()
    }

    /**
     * Safely gets a JsonElement from a JsonObject
     * @param jsonObject the input JsonObject
     * @param memberName the member name of the JsonElement
     * @return the JsonElement if:
     * the input JsonObject is not null
     * the input JsonObject contains an element with the specified memberName
     * the JsonElement is not Json null
     * null otherwise
     */
    fun getElementSafe(jsonObject: JsonObject?, memberName: String?): JsonElement? {
        if (jsonObject == null) return null
        if (!jsonObject.has(memberName)) return null
        val element = jsonObject.get(memberName)
        if (element.isJsonNull()) return null
        return element
    }

    /**
     * Safely gets a JsonObject from a JsonObject
     * @param jsonObject the input JsonObject
     * @param memberName the member name of the output JsonObject
     * @return the output JsonObject if:
     * the input JsonObject is not null
     * the input JsonObject contains an element with the specified memberName
     * the output JsonObject is not Json null
     * the output JsonObject is a JsonObjet
     * null otherwise
     */
    fun getJsonObjectSafe(jsonObject: JsonObject?, memberName: String?): JsonObject? {
        return getJsonObjectSafe(getElementSafe(jsonObject, memberName))
    }

    /**
     * Safely gets a JsonArray from a JsonObject
     * @param jsonObject the input JsonObject
     * @param memberName the member name of the JsonArray
     * @return the JsonArray if:
     * the input JsonObject is not null
     * the input JsonObject contains an element with the specified memberName
     * the JsonArray is not Json null
     * the JsonArray is a JsonArray
     * null otherwise
     */
    fun getJsonArraySafe(jsonObject: JsonObject?, memberName: String?): JsonArray? {
        val jsonElement = getElementSafe(jsonObject, memberName)
        if (jsonElement == null || !jsonElement.isJsonArray()) return null
        return jsonElement.getAsJsonArray()
    }

    /**
     * Safely gets an int from a JsonObject
     * @param jsonObject the input JsonObject
     * @param memberName the member name of the int
     * @param onNullValue the value that will be returned if any of the checks fail
     * @return the int if:
     * the input JsonObject is not null
     * the input JsonObject contains an element with the specified memberName
     * the int is not Json null
     * the int is an actual integer
     * onNullValue otherwise
     */
    fun getIntSafe(jsonObject: JsonObject?, memberName: String?, onNullValue: Int): Int {
        val jsonElement = getElementSafe(jsonObject, memberName)
        if (jsonElement == null || !jsonElement.isJsonPrimitive()) return onNullValue
        try {
            return jsonElement.getAsInt()
        } catch (e: ClassCastException) {
            return onNullValue
        }
    }

    /**
     * Safely gets a String from a JsonObject
     * @param jsonObject the input JsonObject
     * @param memberName the member name of the int
     * @return the String if:
     * the input JsonObject is not null
     * the input JsonObject contains an element with the specified memberName
     * the String is not a Json null
     * the String is an actual String
     * null otherwise
     */
    fun getStringSafe(jsonObject: JsonObject?, memberName: String?): String? {
        val jsonElement = getElementSafe(jsonObject, memberName)
        if (jsonElement == null || !jsonElement.isJsonPrimitive()) return null
        try {
            return jsonElement.getAsString()
        } catch (e: ClassCastException) {
            return null
        }
    }
}
