package org.cube.converter.parser.bedrock.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.cube.converter.data.bedrock.BedrockAttachableData;
import org.cube.converter.data.bedrock.BedrockEntityData;
import org.cube.converter.util.GsonUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.cube.converter.util.GsonUtil.*;

@RequiredArgsConstructor
public class BedrockDataParser {
    public static BedrockEntityData parseEntity(final String json) {
        JsonElement element = GsonUtil.getGson().fromJson(json.trim(), JsonElement.class);
        if (!element.isJsonObject()) // Well this can happen sometimes a json can only have ["en_US"], or something like that, I have no idea.
            return null;

        return parseEntity(element.getAsJsonObject());
    }

    public static BedrockAttachableData parseAttachable(final String json) {
        return parseAttachable(json, "<inline attachable>");
    }

    public static BedrockAttachableData parseAttachable(final String json, final String sourceName) {
        JsonElement element = GsonUtil.getGson().fromJson(json.trim(), JsonElement.class);
        if (!element.isJsonObject()) // Well this can happen sometimes a json can only have ["en_US"], or something like that, I have no idea.
            return null;

        try {
            return parseAttachable(element.getAsJsonObject());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid attachable in " + sourceName + ": " + exception.getMessage(), exception);
        }
    }

    private static BedrockAttachableData parseAttachable(final JsonObject json) {
        return (BedrockAttachableData) parse(json, "minecraft:attachable", true);
    }

    private static BedrockEntityData parseEntity(final JsonObject json) {
        return parse(json, "minecraft:client_entity", false);
    }

    private static BedrockEntityData parse(final JsonObject json, final String identifierName, boolean attachable) {
        if (!json.has(identifierName)) {
            return null;
        }

        final JsonObject description = json.getAsJsonObject(identifierName).getAsJsonObject("description");
        final String identifier = description.getAsJsonPrimitive("identifier").getAsString();
        final Map<String, String> materials = objectToMap(description.getAsJsonObject("materials"));
        final Map<String, String> textures = objectToMap(description.getAsJsonObject("textures"));
        final Map<String, String> geometries = objectToMap(description.getAsJsonObject("geometry"));
        final Map<String, String> animations = objectToMap(description.getAsJsonObject("animations"));
        final Map<String, String> particleEffects = objectToMap(description.getAsJsonObject("particle_effects"));
        final Map<String, String> soundEffects = objectToMap(description.getAsJsonObject("sound_effects"));
        final List<BedrockEntityData.RenderController> controllers = BedrockEntityData.RenderController.parse(description.getAsJsonArray("render_controllers"));
        final BedrockEntityData.Scripts scripts;
        if (description.has("scripts")) {
            scripts = BedrockEntityData.Scripts.parse(description.getAsJsonObject("scripts"));
        } else {
            scripts = BedrockEntityData.Scripts.emptyScript();
        }

        final Map<String, String> itemConditions = attachable ? parseItemConditions(description) : Map.of();

        return attachable ? new BedrockAttachableData(identifier, scripts, controllers, materials, animations, textures, geometries, particleEffects, itemConditions, soundEffects) :
                new BedrockEntityData(identifier, scripts, controllers, materials, animations, textures, geometries, particleEffects, soundEffects);
    }

    private static Map<String, String> parseItemConditions(final JsonObject description) {
        final JsonElement itemElement = description.get("item");
        if (itemElement == null) {
            return Map.of();
        }
        if (!itemElement.isJsonObject()) {
            throw new IllegalArgumentException("minecraft:attachable.description.item must be an object");
        }

        final LinkedHashMap<String, String> conditions = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : itemElement.getAsJsonObject().entrySet()) {
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("minecraft:attachable.description.item." + entry.getKey() + " must be a string MoLang expression");
            }
            conditions.put(entry.getKey(), entry.getValue().getAsString());
        }
        return conditions;
    }
}
