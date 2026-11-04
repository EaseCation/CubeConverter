package org.cube.converter.model.element;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cube.converter.util.element.Position3V;

/** Immutable description of one Bedrock texture mesh kept alongside its converted base mesh. */
@Getter
@RequiredArgsConstructor
public final class TextureMesh {
    private final String texture;
    private final Position3V localPivot;
    private final Position3V position;
    private final Position3V rotation;
    private final Position3V scale;
    private final float depth;

    public TextureMesh clone() {
        return new TextureMesh(texture, localPivot.clone(), position.clone(), rotation.clone(), scale.clone(), depth);
    }
}
