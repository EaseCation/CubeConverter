package org.cube.converter.model.element;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cube.converter.util.element.Position3V;

@AllArgsConstructor
@Getter
public class Locator {
    private final Position3V offset;
    private final Position3V rotation;
    private final boolean ignoreInheritedScale;

    @Override
    public Locator clone() {
        return new Locator(this.offset.clone(), this.rotation.clone(), this.ignoreInheritedScale);
    }
}
