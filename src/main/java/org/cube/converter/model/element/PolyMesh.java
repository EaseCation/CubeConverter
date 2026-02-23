package org.cube.converter.model.element;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public final class PolyMesh {
    private final boolean normalizedUvs;
    private final float[][] positions;  // [vertexCount][3] — x, y, z
    private final float[][] normals;    // [normalCount][3] — nx, ny, nz
    private final float[][] uvs;        // [uvCount][2] — u, v
    private final int[][][] polys;      // [polyCount][3 or 4][3] — each vertex: [posIndex, normalIndex, uvIndex]
}
