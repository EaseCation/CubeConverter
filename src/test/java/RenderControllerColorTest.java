import org.cube.converter.parser.bedrock.controller.BedrockControllerParser;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RenderControllerColorTest {
    @Test
    void preservesRuntimeColorExpressionsAndMaterialOrder() {
        var controllers = BedrockControllerParser.parse("""
                {"render_controllers":{"controller.render.test":{
                  "geometry":"Geometry.default",
                  "textures":["Texture.default"],
                  "materials":[{"body":"Material.default"},{"*":"Material.blend"}],
                  "color":{"r":"q.tint","g":1,"b":"0.5","a":"0.3"}
                }}}
                """);

        var controller = controllers.getFirst();
        assertEquals(Map.of("r", "q.tint", "g", "1", "b", "0.5", "a", "0.3"),
                controller.colorExpressions());
        assertEquals("body", controller.materialsMap().keySet().iterator().next());
    }
}
