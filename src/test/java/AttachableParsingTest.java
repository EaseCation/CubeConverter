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
}
