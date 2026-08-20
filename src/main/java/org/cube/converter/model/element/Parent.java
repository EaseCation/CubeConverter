package org.cube.converter.model.element;

import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.cube.converter.util.element.Position3V;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Parent {
    private String parent = "";
    private String binding = "";

    private final String name;
    private final Position3V pivot;
    private final Position3V rotation;

    private final Map<Integer, Cube> cubes = new HashMap<>();
    private final Map<String, Locator> locators = new HashMap<>();
    private PolyMesh polyMesh = null;

    @Override
    public Parent clone() {
        final Parent parent = new Parent(name, pivot.clone(), rotation.clone());
        for (final Map.Entry<Integer, Cube> entry : this.cubes.entrySet()) {
            parent.cubes.put(entry.getKey(), entry.getValue().clone());
        }
        for (final Map.Entry<String, Locator> entry : this.locators.entrySet()) {
            parent.locators.put(entry.getKey(), entry.getValue().clone());
        }
        parent.setParent(this.parent);
        parent.setBinding(this.binding);
        parent.setPolyMesh(this.polyMesh);

        return parent;
    }
}
