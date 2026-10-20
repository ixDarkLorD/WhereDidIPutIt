#version 330

#moj_import <wheredidiputit:outline.glsl>

// One edge of the outline (ContainerHighlight): v runs across it (0.5 on the line). u is 1 along the middle and runs
// down to 0 at both ends over one glow half width, where the corner dot's round glow takes over, so the glows of the
// edges and corners blend into one instead of overlapping as squares. Near a corner u is also the distance to it, so
// the solid line stops exactly where the dot's begins: they never overlap, which would show as a brighter spot
// wherever the outline isn't opaque (behind blocks, fading).
uniform sampler2D Sampler0;
uniform sampler2D DepthSampler;

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float across = abs(texCoord.y - 0.5) * 2.0;
    float ramp = clamp(texCoord.x, 0.0, 1.0);
    float core = outline_core(across) * (1.0 - outline_core(length(vec2(ramp, across))));
    fragColor = outline_color(vertexColor, core, outline_glow(across) * ramp, outline_visibility(DepthSampler));
}
