import org.cube.converter.data.bedrock.BedrockAttachableData;
import org.cube.converter.model.element.Parent;
import org.cube.converter.model.impl.bedrock.BedrockGeometryModel;
import org.cube.converter.parser.bedrock.data.BedrockDataParser;
import org.cube.converter.parser.bedrock.geometry.BedrockGeometryParser;
import org.junit.jupiter.api.Test;

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
    void convertsTextureMeshesToRenderablePolyMesh() {
        List<BedrockGeometryModel> geometries = BedrockGeometryParser.parse("""
                {"minecraft:geometry":[{
                  "description":{"identifier":"geometry.sprite","texture_width":16,"texture_height":16},
                  "bones":[{"name":"rightitem","pivot":[0,0,0],"rotation":[0,0,0],
                    "texture_meshes":[{"texture":"default","local_pivot":[6,0,6],
                      "position":[2,1,-1],"rotation":[0,0,0]}]}]
                }]}
                """);

        Parent bone = geometries.getFirst().getParents().getFirst();
        assertNotNull(bone.getPolyMesh());
        assertEquals(8, bone.getPolyMesh().getPositions().length);
        assertEquals(6, bone.getPolyMesh().getPolys().length);
        assertEquals(4, bone.getPolyMesh().getUvs().length);

        float[][] positions = bone.getPolyMesh().getPositions();
        assertArrayEquals(new float[]{-4.0F, 1.0F, -7.0F}, positions[0]);
        assertArrayEquals(new float[]{12.0F, 1.0F, -7.0F}, positions[3]);
    }
}
