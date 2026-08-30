import org.cube.converter.data.bedrock.BedrockAttachableData;
import org.cube.converter.model.element.Parent;
import org.cube.converter.model.impl.bedrock.BedrockGeometryModel;
import org.cube.converter.parser.bedrock.data.BedrockDataParser;
import org.cube.converter.parser.bedrock.geometry.BedrockGeometryParser;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AttachableParsingTest {
    @Test
    void preservesItemConditionOrder() {
        BedrockAttachableData data = BedrockDataParser.parseAttachable("""
                {"minecraft:attachable":{"description":{
                  "identifier":"test:gun",
                  "item":{"test:first":"q.is_first","test:second":"q.is_second"}
                }}}
                """);

        assertEquals(List.of("test:first", "test:second"), List.copyOf(data.getItemConditions().keySet()));
        assertEquals("q.is_first", data.getItemConditions().get("test:first"));
    }

    @Test
    void missingItemMapIsEmpty() {
        BedrockAttachableData data = BedrockDataParser.parseAttachable("""
                {"minecraft:attachable":{"description":{"identifier":"test:gun"}}}
                """);

        assertTrue(data.getItemConditions().isEmpty());
    }

    @Test
    void reportsSourceAndFieldForInvalidItemMap() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> BedrockDataParser.parseAttachable("""
                        {"minecraft:attachable":{"description":{"identifier":"test:gun","item":[]}}}
                        """, "attachables/test.json"));

        assertTrue(exception.getMessage().contains("attachables/test.json"));
        assertTrue(exception.getMessage().contains("description.item"));
    }

    @Test
    void preservesBindingWhenCloningBone() {
        List<BedrockGeometryModel> geometries = BedrockGeometryParser.parse("""
                {"minecraft:geometry":[{
                  "description":{"identifier":"geometry.test","texture_width":16,"texture_height":16},
                  "bones":[{"name":"root","pivot":[0,0,0],"rotation":[0,0,0],
                    "binding":"q.item_slot_to_bone_name(c.item_slot)"}]
                }]}
                """);

        Parent bone = geometries.getFirst().getParents().getFirst();
        assertEquals("q.item_slot_to_bone_name(c.item_slot)", bone.getBinding());
        assertEquals(bone.getBinding(), bone.clone().getBinding());

        Parent differentBinding = new Parent(bone.getName(), bone.getPivot(), bone.getRotation());
        differentBinding.setBinding("rightitem");
        assertNotEquals(bone, differentBinding);
    }

    @Test
    void preservesTextureMeshesForRuntimeSpriteGeneration() {
        List<BedrockGeometryModel> geometries = BedrockGeometryParser.parse("""
                {"minecraft:geometry":[{
                  "description":{"identifier":"geometry.sprite","texture_width":16,"texture_height":16},
                  "bones":[{"name":"rightitem","pivot":[0,0,0],"rotation":[0,0,0],
                    "texture_meshes":[{"texture":"default","local_pivot":[6,0,6],
                      "position":[2,1,-1],"rotation":[0,0,0]}]}]
                }]}
                """);

        Parent bone = geometries.getFirst().getParents().getFirst();
        assertNull(bone.getPolyMesh());
        assertEquals(1, bone.getTextureMeshes().size());
        assertEquals("default", bone.getTextureMeshes().getFirst().getTexture());
        assertEquals(1.0F, bone.getTextureMeshes().getFirst().getDepth());
    }

    private static int[] positionIndices(int[][] polygon) {
        return Arrays.stream(polygon).mapToInt(vertex -> vertex[0]).toArray();
    }
}
