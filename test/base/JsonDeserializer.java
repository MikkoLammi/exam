package base;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import play.Logger;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class JsonDeserializer {

    private static GsonBuilder gsonBuilder = new GsonBuilder();
    static {
        gsonBuilder.registerTypeAdapter(Date.class, new DateDeserializer());
    }
    private static Gson gson = gsonBuilder.create();

    private static class DateDeserializer implements com.google.gson.JsonDeserializer<Date> {

        @Override
        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            try {
                return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(json.getAsString());
            } catch (ParseException e) {
                try {
                    return new Date(json.getAsLong());
                } catch (RuntimeException e2) {
                    Logger.warn("Failed to parse date " + json.getAsString());
                }
            }
            return null;
        }
    }

    public static <T> T deserialize(Class<T> model, JsonNode node) {
        return gson.fromJson(node.toString(), model);
    }
}