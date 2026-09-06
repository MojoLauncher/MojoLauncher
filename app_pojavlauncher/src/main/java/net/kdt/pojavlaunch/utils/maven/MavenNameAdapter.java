package net.kdt.pojavlaunch.utils.maven;

import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class MavenNameAdapter extends TypeAdapter<MavenName> {
    @Override
    public void write(JsonWriter jsonWriter, MavenName mavenName) throws IOException {
        jsonWriter.value(mavenName.toString());
    }

    @Override
    public MavenName read(JsonReader reader) throws IOException {
        if(reader.peek() != JsonToken.STRING) throw new JsonSyntaxException("Expected String for maven name");
        return MavenName.parse(reader.nextString());
    }
}
