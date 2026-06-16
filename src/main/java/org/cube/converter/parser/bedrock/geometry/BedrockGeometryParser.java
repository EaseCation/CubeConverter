package org.cube.converter.parser.bedrock.geometry;

import com.viaversion.viaversion.libs.gson.JsonArray;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import org.cube.converter.model.element.Cube;
import org.cube.converter.model.element.Locator;
import org.cube.converter.model.element.Parent;
import org.cube.converter.model.element.PolyMesh;
import org.cube.converter.model.impl.bedrock.BedrockGeometryModel;
import org.cube.converter.util.GsonUtil;
import org.cube.converter.util.element.Position2V;
import org.cube.converter.util.element.Position3V;
import org.cube.converter.util.element.UVMap;

import java.util.ArrayList;
import java.util.List;

// I am NOT cleaning this up lmao...
public class BedrockGeometryParser {
    public static List<BedrockGeometryModel> parse(String json) {
        return parse(GsonUtil.getGson().fromJson(json.trim(), JsonObject.class));
    }

    public static List<BedrockGeometryModel> parse(JsonObject json) {
        final List<BedrockGeometryModel> geometries = new ArrayList<>();

        if (json.has("minecraft:geometry")) {
            final JsonElement element = json.get("minecraft:geometry");
            if (!element.isJsonArray()) {
                return null;
            }

            final JsonArray array = element.getAsJsonArray();
            for (JsonElement element1 : array) {
                if (!element1.isJsonObject()) {
                    continue;
                }
                JsonObject object = element1.getAsJsonObject();

                final BedrockGeometryModel geometry = getGeometry(object, "minecraft:geometry", "texture_width", "texture_height");
                if (geometry == null) {
                    continue;
                }
                geometries.add(geometry);
            }
        }

        for (String elementName : json.keySet()) {
            JsonElement element = json.get(elementName);
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            if (!object.has("texturewidth")) {
                continue;
            }
            final BedrockGeometryModel geometry = getGeometry(object, elementName, "texturewidth", "textureheight");
            if (geometry == null) {
                continue;
            }
            geometries.add(geometry);
        }

        return geometries;
    }

    private static BedrockGeometryModel getGeometry(JsonObject geometry, String elementName, String textureWidthName, String textureHeightName) {
        JsonObject description = geometry.has("description") ? geometry.getAsJsonObject("description") : geometry;
        String identifier = description.has("identifier") ? description.get("identifier").getAsString() : elementName;

        if (!description.has(textureHeightName) || !description.has(textureWidthName)) {
            return null;
        }

        int textureWidth = description.get(textureWidthName).getAsInt();
        int textureHeight = description.get(textureHeightName).getAsInt();

        if (!geometry.has("bones")) {
            return null;
        }

        final List<Parent> bones = new ArrayList<>();
        for (JsonElement boneElement : geometry.getAsJsonArray("bones")) {
            final JsonObject boneObject = boneElement.getAsJsonObject();
            final String name = boneObject.get("name").getAsString();

            final Position3V parentPivot = new Position3V(boneObject.getAsJsonArray("pivot"));
            final Position3V boneRotation = new Position3V(boneObject.getAsJsonArray("rotation"));

            // parentPivot.setX(-parentPivot.getX());
            // boneRotation.setX(-boneRotation.getX());
            // boneRotation.setY(-boneRotation.getY());

            Parent bone = new Parent(name, parentPivot, boneRotation);

            if (boneObject.has("parent")) {
                bone.setParent(boneObject.get("parent").getAsString());
            }

            if (boneObject.has("poly_mesh")) {
                bone.setPolyMesh(parsePolyMesh(boneObject.getAsJsonObject("poly_mesh")));
            }

            if (boneObject.has("locators") && boneObject.get("locators").isJsonObject()) {
                JsonObject locatorsObject = boneObject.getAsJsonObject("locators");
                for (String locatorName : locatorsObject.keySet()) {
                    JsonElement locatorElement = locatorsObject.get(locatorName);
                    if (locatorElement == null) {
                        continue;
                    }
                    if (locatorElement.isJsonArray()) {
                        bone.getLocators().put(locatorName, new Locator(new Position3V(locatorElement.getAsJsonArray()), Position3V.zero(), false));
                    } else if (locatorElement.isJsonObject()) {
                        JsonObject locatorObject = locatorElement.getAsJsonObject();
                        if (locatorObject.has("offset") && locatorObject.get("offset").isJsonArray()) {
                            Position3V offset = new Position3V(locatorObject.getAsJsonArray("offset"));
                            Position3V rotation = locatorObject.has("rotation") && locatorObject.get("rotation").isJsonArray()
                                    ? new Position3V(locatorObject.getAsJsonArray("rotation"))
                                    : Position3V.zero();
                            boolean ignoreInheritedScale = locatorObject.has("ignore_inherited_scale")
                                    && locatorObject.get("ignore_inherited_scale").getAsBoolean();
                            bone.getLocators().put(locatorName, new Locator(offset, rotation, ignoreInheritedScale));
                        }
                    }
                }
            }

            if (!boneObject.has("cubes")) {
                bones.add(bone);
                continue;
            }

            int i = 0;
            final JsonArray cubeElements = boneObject.getAsJsonArray("cubes");
            for (JsonElement cubeElement : cubeElements) {
                JsonObject cubeObject = cubeElement.getAsJsonObject();
                final Position3V position = new Position3V(cubeObject.getAsJsonArray("origin"));
                final Position3V size = new Position3V(cubeObject.getAsJsonArray("size"));
                final Position3V pivot = new Position3V(cubeObject.getAsJsonArray("pivot"));
                final Position3V rotation = new Position3V(cubeObject.getAsJsonArray("rotation"));
                // pivot.setX(-pivot.getX());

                // rotation.setX(-rotation.getX());
                // rotation.setY(-rotation.getY());

                boolean mirror = false;
                if (cubeObject.has("mirror")) {
                    mirror = cubeObject.get("mirror").getAsBoolean();
                }

                Cube cube;
                if (cubeObject.get("uv") instanceof JsonArray) {
                    JsonArray array = cubeObject.getAsJsonArray("uv");
                    Float[] offset = new Float[] { array.get(0).getAsFloat(), array.get(1).getAsFloat() };
                    cube = new Cube(pivot, position, size, rotation, mirror, UVMap.fromBoxUV(size, offset, mirror));
                } else {
                    cube = new Cube(pivot, position, size, rotation, mirror, UVMap.fromPerfaceUV(cubeObject));
                }

                cube.setParent(bone.getParent());
                if (cubeObject.has("inflate")) {
                    cube.setInflate(cubeObject.get("inflate").getAsFloat());
                }

                bone.getCubes().put(i, cube);
                i++;
            }

            bones.add(bone);
        }

        final BedrockGeometryModel geometryModel = new BedrockGeometryModel(identifier, new Position2V(textureWidth, textureHeight));
        geometryModel.getParents().addAll(bones);
        return geometryModel;
    }

    private static PolyMesh parsePolyMesh(JsonObject polyMeshObj) {
        boolean normalizedUvs = polyMeshObj.has("normalized_uvs")
                && polyMeshObj.get("normalized_uvs").getAsBoolean();

        final JsonArray positionsArray = polyMeshObj.getAsJsonArray("positions");
        float[][] positions = new float[positionsArray.size()][3];
        for (int i = 0; i < positionsArray.size(); i++) {
            JsonArray pos = positionsArray.get(i).getAsJsonArray();
            positions[i][0] = pos.get(0).getAsFloat();
            positions[i][1] = pos.get(1).getAsFloat();
            positions[i][2] = pos.get(2).getAsFloat();
        }

        final JsonArray normalsArray = polyMeshObj.getAsJsonArray("normals");
        float[][] normals = new float[normalsArray.size()][3];
        for (int i = 0; i < normalsArray.size(); i++) {
            JsonArray n = normalsArray.get(i).getAsJsonArray();
            normals[i][0] = n.get(0).getAsFloat();
            normals[i][1] = n.get(1).getAsFloat();
            normals[i][2] = n.get(2).getAsFloat();
        }

        final JsonArray uvsArray = polyMeshObj.getAsJsonArray("uvs");
        float[][] uvs = new float[uvsArray.size()][2];
        for (int i = 0; i < uvsArray.size(); i++) {
            JsonArray uv = uvsArray.get(i).getAsJsonArray();
            uvs[i][0] = uv.get(0).getAsFloat();
            uvs[i][1] = uv.get(1).getAsFloat();
        }

        final JsonArray polysArray = polyMeshObj.getAsJsonArray("polys");
        int[][][] polys = new int[polysArray.size()][][];
        for (int i = 0; i < polysArray.size(); i++) {
            JsonArray poly = polysArray.get(i).getAsJsonArray();
            polys[i] = new int[poly.size()][3];
            for (int j = 0; j < poly.size(); j++) {
                JsonArray vertex = poly.get(j).getAsJsonArray();
                polys[i][j][0] = vertex.get(0).getAsInt();
                polys[i][j][1] = vertex.get(1).getAsInt();
                polys[i][j][2] = vertex.get(2).getAsInt();
            }
        }

        return new PolyMesh(normalizedUvs, positions, normals, uvs, polys);
    }
}
