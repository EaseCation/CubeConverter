import org.cube.converter.data.bedrock.controller.BedrockRenderController;
import org.cube.converter.parser.bedrock.controller.BedrockControllerParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RenderControllerUvAnimTest {
    @Test
    void keepsUvAnimExpressionsAndConstants() {
        var controller = BedrockControllerParser.parse("""
                {"render_controllers":{"controller.render.test":{
                  "geometry":"Geometry.default",
                  "textures":["Texture.default"],
                  "uv_anim":{"offset":["(math.mod(v.lo, 51) * 20 + 1) / 1024", 0.5],"scale":[0.017578125, "7 / 256"]}
                }}}
                """).getFirst();

        assertEquals(new BedrockRenderController.UvAnim(
                "(math.mod(v.lo, 51) * 20 + 1) / 1024", "0.5", "0.017578125", "7 / 256"), controller.uvAnim());
    }

    @Test
    void defaultsMissingComponentsAndOmitsAbsentUvAnim() {
        var controllers = BedrockControllerParser.parse("""
                {"render_controllers":{
                  "controller.render.partial":{"geometry":"Geometry.default","uv_anim":{"offset":["q.x"]}},
                  "controller.render.none":{"geometry":"Geometry.default"}
                }}
                """);

        assertEquals(new BedrockRenderController.UvAnim("q.x", "0", "1", "1"), controllers.get(0).uvAnim());
        assertNull(controllers.get(1).uvAnim());
    }
}
