package net.kdt.pojavlaunch.value;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.kdt.pojavlaunch.JVersionList;

import java.lang.reflect.Type;

public class ArgumentsAdapter implements JsonDeserializer<JVersionList.Arguments> {
    @Override
    public JVersionList.Arguments deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        JVersionList.Arguments args = new JVersionList.Arguments();
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if(jsonObject.has("jvm")) args.jvm = deserializeInternal(jsonObject.get("jvm"), context);
        if(jsonObject.has("game")) args.game = deserializeInternal(jsonObject.get("game"), context);
        return args;
    }

    private Object[] deserializeInternal(JsonElement jsonElement, JsonDeserializationContext context) {
        JsonArray array = jsonElement.getAsJsonArray();
        Object[] ret = new Object[array.size()];
        for(int i = 0; i < array.size(); i++){
            JsonElement item = array.get(i);
            ret[i] = item.isJsonObject() ? context.deserialize(item, JVersionList.Arguments.ArgValue.class) : context.deserialize(item, String.class);
        }
        return ret;
    }
}
