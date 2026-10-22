#version 330
#extension GL_ARB_separate_shader_objects : require

#include <wheredidiputit:outline.glsl>

// One edge of the outline (ContainerHighlight): v runs across it (0.5 on the line), u along it in glow half widths,
// measured from the nearest corner: 0 at the corner, negative inside the edge (down to -1, then -1 along the middle),
// positive past the corner (up to 1 at the end of the round cap). The glow is a capsule round the edge, so it runs
// round each corner instead of being cut there. The solid line stops at the corner dot's edge, so they never overlap,
// which would show as a brighter spot wherever the outline isn't opaque (behind blocks, fading).
uniform sampler2D Sampler0;
uniform sampler2D DepthSampler;

layout(location = 0) in vec2 texCoord;
layout(location = 1) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

void main() {
    float across = abs(texCoord.y - 0.5) * 2.0;
    float beyond = max(texCoord.x, 0.0);
    float inside = max(-texCoord.x, 0.0);
    // Two or three edges' glows stack at a corner: near it each gives about half, so together they add up to about one.
    float toCorner = length(vec2(texCoord.x >= 0.0 ? beyond : inside, across));
    float glow = outline_glow(length(vec2(beyond, across))) * mix(0.45, 1.0, smoothstep(0.0, 1.0, toCorner));
    float core = texCoord.x > 0.0 ? 0.0 : outline_core(across) * (1.0 - outline_core(length(vec2(inside, across))));
    fragColor = outline_color(vertexColor, core, glow, outline_visibility(DepthSampler));
}
