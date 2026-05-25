package org.cube.converter.model.impl.bedrock;

import lombok.Getter;

import com.viaversion.viaversion.libs.gson.JsonObject;
import org.cube.converter.converter.FormatConverter;
import org.cube.converter.converter.enums.RotationType;
import org.cube.converter.model.GeneralModel;
import org.cube.converter.model.impl.java.JavaItemModel;
import org.cube.converter.parser.bedrock.geometry.BedrockGeometryParser;
import org.cube.converter.util.GsonUtil;
import org.cube.converter.util.element.Direction;
import org.cube.converter.util.element.Position2V;

import java.util.List;
import java.util.Map;

@Getter
public final class BedrockGeometryModel extends GeneralModel {
    private final String identifier;

    public BedrockGeometryModel(final String identifier, final Position2V textureSize) {
        super(textureSize);
        this.identifier = identifier;
    }

    @Override
    public JsonObject compile() {
        return null;
    }

    public JavaItemModel toJavaItemModel(Map<Direction, String> textureMap, RotationType type) {
        return FormatConverter.geometryToItemModel(textureMap, this, type);
    }

    public JavaItemModel toJavaItemModel(String texture, RotationType type) {
        return FormatConverter.geometryToItemModel(texture, this, type);
    }

    public List<JavaItemModel> toJavaMultiItemModel(String texture) {
        return FormatConverter.geometryToMultiItemModel(texture, this);
    }

    public static List<BedrockGeometryModel> fromJson(String json) {
        return fromJson(GsonUtil.getGson().fromJson(json.trim(), JsonObject.class));
    }

    public static List<BedrockGeometryModel> fromJson(JsonObject object) {
        return BedrockGeometryParser.parse(object);
    }
}