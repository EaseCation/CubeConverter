package org.cube.converter.parser.bedrock.geometry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

            if (boneObject.has("binding")) {
                if (!boneObject.get("binding").isJsonPrimitive() || !boneObject.getAsJsonPrimitive("binding").isString()) {
                    throw new IllegalArgumentException("Bone '" + name + "' binding must be a string MoLang expression");
                }
                bone.setBinding(boneObject.get("binding").getAsString());
            }

            if (boneObject.has("poly_mesh")) {
                bone.setPolyMesh(parsePolyMesh(boneObject.getAsJsonObject("poly_mesh")));
            }

            if (boneObject.has("texture_meshes") && boneObject.get("texture_meshes").isJsonArray()) {
                final PolyMesh textureMesh = parseTextureMeshes(
                        boneObject.getAsJsonArray("texture_meshes"), textureWidth, textureHeight);
                if (textureMesh != null) {
                    bone.setPolyMesh(mergePolyMeshes(bone.getPolyMesh(), textureMesh));
                }
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

    /**
     * Converts the texture-mesh base surfaces into a renderable mesh. Bedrock expands the alpha
     * channel into voxel side faces at runtime; the converter does not own texture bytes, so it
     * deliberately emits only the two correctly oriented textured surfaces. Emitting six faces
     * with the full texture UV rectangle would create the visible compressed-texture border on
     * every sword/bow when viewed from an angle.
     */
    private static PolyMesh parseTextureMeshes(JsonArray textureMeshes, int textureWidth, int textureHeight) {
        final List<float[]> positions = new ArrayList<>();
        final List<float[]> normals = new ArrayList<>();
        final List<float[]> uvs = new ArrayList<>();
        final List<int[][]> polygons = new ArrayList<>();
        for (JsonElement element : textureMeshes) {
            if (!element.isJsonObject()) {
                continue;
            }
            final JsonObject mesh = element.getAsJsonObject();
            final float[] localPivot = vector(mesh, "local_pivot", 0, 0, 0);
            final float[] position = vector(mesh, "position", 0, 0, 0);
            final float[] rotation = vector(mesh, "rotation", 0, 0, 0);
            final float[] scale = vector(mesh, "scale", 1, 1, 1);
            final float depth = mesh.has("use_pixel_depth") && !mesh.get("use_pixel_depth").getAsBoolean()
                    ? Math.max(textureWidth, textureHeight) / 16.0F : 1.0F;

            final int offset = positions.size();
            // Bedrock item texture meshes use the same orientation as geometry.item_sprite:
            // the texture rectangle spans X/Y and the optional pixel depth is along Z. The
            // local pivot is an origin inside that rectangle; apply it before rotation.
            final float[][] corners = {
                    {0, 0, 0}, {0, textureHeight, 0},
                    {textureWidth, textureHeight, 0}, {textureWidth, 0, 0},
                    {0, 0, depth}, {0, textureHeight, depth},
                    {textureWidth, textureHeight, depth}, {textureWidth, 0, depth}
            };
            for (float[] corner : corners) {
                final float[] transformed = transformTextureVertex(corner, scale, localPivot, rotation, position);
                positions.add(transformed);
            }

            // Normals are kept in Bedrock space and converted to Java space by GeometryUtil.
            final float[][] faceNormals = {
                    {0, 0, -1}, {0, 0, 1}
            };
            for (float[] normal : faceNormals) {
                normals.add(transformTextureNormal(normal, rotation));
            }
            final int[][] faceUvs = {
                    {0, 0}, {0, textureHeight},
                    {textureWidth, textureHeight}, {textureWidth, 0},
                    {0, 0}, {0, textureHeight},
                    {textureWidth, textureHeight}, {textureWidth, 0}
            };
            for (int[] uv : faceUvs) {
                uvs.add(new float[]{uv[0], uv[1]});
            }

            // Keep only the two textured surfaces. Bedrock voxelizes alpha into side faces;
            // duplicating the full texture on all four sides creates compressed edge sprites.
            final int uvStart = uvs.size() - 8;
            polygons.add(face(offset, normals.size() - 2, uvStart, 0, 3, 2, 1));
            polygons.add(face(offset, normals.size() - 1, uvStart + 4, 4, 5, 6, 7));
        }
        if (positions.isEmpty()) {
            return null;
        }
        return new PolyMesh(false,
                positions.toArray(float[][]::new), normals.toArray(float[][]::new),
                uvs.toArray(float[][]::new), polygons.toArray(int[][][]::new));
    }

    private static float[] vector(JsonObject object, String name, float x, float y, float z) {
        if (!object.has(name) || !object.get(name).isJsonArray()) {
            return new float[]{x, y, z};
        }
        final JsonArray values = object.getAsJsonArray(name);
        return new float[]{
                values.size() > 0 ? values.get(0).getAsFloat() : x,
                values.size() > 1 ? values.get(1).getAsFloat() : y,
                values.size() > 2 ? values.get(2).getAsFloat() : z
        };
    }

    private static float[] transformTextureVertex(float[] vertex, float[] scale, float[] localPivot,
                                                   float[] rotation, float[] position) {
        float x = (vertex[0] - localPivot[0]) * scale[0];
        float y = (vertex[1] - localPivot[1]) * scale[1];
        float z = (vertex[2] - localPivot[2]) * scale[2];
        final double rx = Math.toRadians(rotation[0]);
        final double ry = Math.toRadians(rotation[1]);
        final double rz = Math.toRadians(rotation[2]);
        double cos = Math.cos(rx), sin = Math.sin(rx);
        double nextY = y * cos - z * sin;
        double nextZ = y * sin + z * cos;
        y = (float) nextY;
        z = (float) nextZ;
        cos = Math.cos(ry); sin = Math.sin(ry);
        double nextX = x * cos + z * sin;
        nextZ = -x * sin + z * cos;
        x = (float) nextX;
        z = (float) nextZ;
        cos = Math.cos(rz); sin = Math.sin(rz);
        nextX = x * cos - y * sin;
        nextY = x * sin + y * cos;
        return new float[]{(float) nextX + position[0], (float) nextY + position[1], z + position[2]};
    }

    private static int[][] face(int offset, int normal, int uvOffset,
                                int a, int b, int c, int d) {
        return new int[][]{
                {offset + a, normal, uvOffset},
                {offset + b, normal, uvOffset + 1},
                {offset + c, normal, uvOffset + 2},
                {offset + d, normal, uvOffset + 3}
        };
    }

    private static float[] transformTextureNormal(float[] normal, float[] rotation) {
        return transformTextureVertex(normal, new float[]{1, 1, 1}, new float[]{0, 0, 0}, rotation,
                new float[]{0, 0, 0});
    }

    private static PolyMesh mergePolyMeshes(PolyMesh first, PolyMesh second) {
        if (first == null) {
            return second;
        }
        final List<float[]> positions = new ArrayList<>(List.of(first.getPositions()));
        final List<float[]> normals = new ArrayList<>(List.of(first.getNormals()));
        final List<float[]> uvs = new ArrayList<>(List.of(first.getUvs()));
        final List<int[][]> polygons = new ArrayList<>(List.of(first.getPolys()));
        final int positionOffset = positions.size();
        final int normalOffset = normals.size();
        final int uvOffset = uvs.size();
        positions.addAll(List.of(second.getPositions()));
        normals.addAll(List.of(second.getNormals()));
        uvs.addAll(List.of(second.getUvs()));
        for (int[][] polygon : second.getPolys()) {
            final int[][] copy = new int[polygon.length][3];
            for (int i = 0; i < polygon.length; i++) {
                copy[i][0] = polygon[i][0] + positionOffset;
                copy[i][1] = polygon[i][1] + normalOffset;
                copy[i][2] = polygon[i][2] + uvOffset;
            }
            polygons.add(copy);
        }
        return new PolyMesh(first.isNormalizedUvs(), positions.toArray(float[][]::new),
                normals.toArray(float[][]::new), uvs.toArray(float[][]::new),
                polygons.toArray(int[][][]::new));
    }
}
