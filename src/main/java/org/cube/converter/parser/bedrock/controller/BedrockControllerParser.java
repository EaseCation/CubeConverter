package org.cube.converter.parser.bedrock.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.cube.converter.data.bedrock.controller.BedrockRenderController;
import org.cube.converter.util.GsonUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.cube.converter.util.GsonUtil.objectToMap;

public class BedrockControllerParser {
    public static List<BedrockRenderController> parse(final String json) {
        return parse(GsonUtil.getGson().fromJson(json.trim(), JsonObject.class));
    }

    public static List<BedrockRenderController> parse(final JsonObject json) {
        if (!json.has("render_controllers")) {
            return new ArrayList<>();
        }

        final List<BedrockRenderController> list = new ArrayList<>();
        final JsonObject controllers = json.getAsJsonObject("render_controllers");
        for (final String identifier : controllers.keySet()) {
            if (!controllers.get(identifier).isJsonObject() || !identifier.startsWith("controller.render")) {
                continue;
            }

            final JsonObject object = controllers.getAsJsonObject(identifier);

            List<String> textureExpressions = new ArrayList<>();
            if (object.has("textures")) {
                textureExpressions = arrayToList(object.getAsJsonArray("textures"));
            }

            String geometryExpression = "";
            if (object.has("geometry")) {
                JsonElement element = object.get("geometry");
                if (element.isJsonPrimitive()) {
                    geometryExpression = element.getAsString();
                }
            }

            final Map<String, String> materialsMap = new HashMap<>();
            if (object.has("materials")) {
                for (final JsonElement element : object.getAsJsonArray("materials")) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    materialsMap.putAll(objectToMap(element.getAsJsonObject()));
                }
            }

            // Parse part_visibility
            final Map<String, String> partVisibility = new LinkedHashMap<>();
            if (object.has("part_visibility")) {
                final JsonArray pvArray = object.getAsJsonArray("part_visibility");
                for (final JsonElement elem : pvArray) {
                    if (elem.isJsonObject()) {
                        final JsonObject obj = elem.getAsJsonObject();
                        for (final String boneName : obj.keySet()) {
                            partVisibility.put(boneName, obj.get(boneName).getAsString());
                        }
                    }
                }
            }

            // Parse lighting properties
            boolean ignoreLighting = false;
            float lightColorMultiplier = 1.0f;
            if (object.has("ignore_lighting")) {
                ignoreLighting = object.get("ignore_lighting").getAsBoolean();
            }
            if (object.has("light_color_multiplier")) {
                lightColorMultiplier = object.get("light_color_multiplier").getAsFloat();
            }

            final JsonObject arrays = object.getAsJsonObject("arrays");
            if (arrays == null) {
                list.add(new BedrockRenderController(identifier, materialsMap, geometryExpression, textureExpressions, List.of(), List.of(), List.of(), partVisibility, ignoreLighting, lightColorMultiplier));
                continue;
            }

            final List<BedrockRenderController.Array> textures = BedrockRenderController.Array.parse(arrays.getAsJsonObject("textures"));
            final List<BedrockRenderController.Array> geometries = BedrockRenderController.Array.parse(arrays.getAsJsonObject("geometries"));
            final List<BedrockRenderController.Array> materials = BedrockRenderController.Array.parse(arrays.getAsJsonObject("materials"));

            list.add(new BedrockRenderController(identifier, materialsMap, geometryExpression, textureExpressions, materials, textures, geometries, partVisibility, ignoreLighting, lightColorMultiplier));
        }

        return list;
    }

    private static List<String> arrayToList(JsonArray array) {
        List<String> list = new ArrayList<>();

        for (JsonElement element : array) {
            list.add(element.getAsString());
        }

        return list;
    }
}
