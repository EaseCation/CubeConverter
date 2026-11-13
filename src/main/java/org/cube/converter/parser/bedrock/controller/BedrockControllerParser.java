package org.cube.converter.parser.bedrock.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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

            final Map<String, String> materialsMap = new LinkedHashMap<>();
            if (object.has("materials")) {
                for (final JsonElement element : object.getAsJsonArray("materials")) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    materialsMap.putAll(objectToMap(element.getAsJsonObject()));
                }
            }

            final Map<String, String> colorExpressions = new LinkedHashMap<>();
            final JsonObject color = object.getAsJsonObject("color");
            if (color != null) {
                for (String component : List.of("r", "g", "b", "a")) {
                    final JsonElement value = color.get(component);
                    if (value != null && value.isJsonPrimitive()) {
                        colorExpressions.put(component, value.getAsString());
                    }
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
                lightColorMultiplier = parseFloatOrDefault(object.get("light_color_multiplier"), 1.0f);
            }

            final BedrockRenderController.UvAnim uvAnim = parseUvAnim(object.get("uv_anim"));

            final JsonObject arrays = object.getAsJsonObject("arrays");
            if (arrays == null) {
                list.add(new BedrockRenderController(identifier, materialsMap, geometryExpression,
                        textureExpressions, List.of(), List.of(), List.of(), partVisibility,
                        colorExpressions, ignoreLighting, lightColorMultiplier, uvAnim));
                continue;
            }

            final List<BedrockRenderController.Array> textures = BedrockRenderController.Array.parse(arrays.getAsJsonObject("textures"));
            final List<BedrockRenderController.Array> geometries = BedrockRenderController.Array.parse(arrays.getAsJsonObject("geometries"));
            final List<BedrockRenderController.Array> materials = BedrockRenderController.Array.parse(arrays.getAsJsonObject("materials"));

            list.add(new BedrockRenderController(identifier, materialsMap, geometryExpression,
                    textureExpressions, materials, textures, geometries, partVisibility,
                    colorExpressions, ignoreLighting, lightColorMultiplier, uvAnim));
        }

        return list;
    }

    // uv_anim: {"offset": [u, v], "scale": [u, v]}; each entry is a number or a MoLang expression.
    // A missing offset defaults to 0 and a missing scale to 1, matching Bedrock.
    private static BedrockRenderController.UvAnim parseUvAnim(final JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        final JsonObject object = element.getAsJsonObject();
        final String[] offset = parsePair(object.get("offset"), "0");
        final String[] scale = parsePair(object.get("scale"), "1");
        return new BedrockRenderController.UvAnim(offset[0], offset[1], scale[0], scale[1]);
    }

    private static String[] parsePair(final JsonElement element, final String fallback) {
        final String[] pair = {fallback, fallback};
        if (element == null || !element.isJsonArray()) {
            return pair;
        }
        final JsonArray array = element.getAsJsonArray();
        for (int i = 0; i < 2 && i < array.size(); i++) {
            final JsonElement value = array.get(i);
            if (value.isJsonPrimitive()) {
                pair[i] = value.getAsString();
            }
        }
        return pair;
    }

    // light_color_multiplier (and similar fields) may be a MoLang expression such as
    // "query.is_baby ? 1.6 : 1" rather than a constant. We cannot evaluate MoLang at parse time
    // (no entity context here), so fall back to the default instead of crashing the whole pack parse.
    private static float parseFloatOrDefault(final JsonElement element, final float fallback) {
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        final JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isNumber()) {
            return primitive.getAsFloat();
        }
        if (primitive.isString()) {
            try {
                return Float.parseFloat(primitive.getAsString().trim());
            } catch (final NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static List<String> arrayToList(JsonArray array) {
        List<String> list = new ArrayList<>();

        for (JsonElement element : array) {
            list.add(element.getAsString());
        }

        return list;
    }
}
