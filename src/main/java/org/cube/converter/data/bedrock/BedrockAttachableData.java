package org.cube.converter.data.bedrock;

import lombok.Getter;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@ToString(callSuper = true)
public class BedrockAttachableData extends BedrockEntityData {
    private final LinkedHashMap<String, String> itemConditions;

    public BedrockAttachableData(String identifier, Scripts scripts, List<RenderController> controllers, Map<String, String> materials, Map<String, String> animations, Map<String, String> textures, Map<String, String> geometries, Map<String, String> particleEffects) {
        this(identifier, scripts, controllers, materials, animations, textures, geometries, particleEffects, Map.of());
    }

    public BedrockAttachableData(String identifier, Scripts scripts, List<RenderController> controllers, Map<String, String> materials, Map<String, String> animations, Map<String, String> textures, Map<String, String> geometries, Map<String, String> particleEffects, Map<String, String> itemConditions) {
        this(identifier, scripts, controllers, materials, animations, textures, geometries, particleEffects, itemConditions, Map.of());
    }

    public BedrockAttachableData(String identifier, Scripts scripts, List<RenderController> controllers,
                                 Map<String, String> materials, Map<String, String> animations,
                                 Map<String, String> textures, Map<String, String> geometries,
                                 Map<String, String> particleEffects, Map<String, String> itemConditions,
                                 Map<String, String> soundEffects) {
        super(identifier, scripts, controllers, materials, animations, textures, geometries, particleEffects, soundEffects);
        this.itemConditions = new LinkedHashMap<>(itemConditions);
    }
}
