#version 330
#extension GL_ARB_separate_shader_objects : require

#include <wheredidiputit:outline.glsl>

// A corner of the outline (ContainerHighlight): a disc facing the camera with the solid line's round joint only. Its
// own glow showed as a circle at every corner (author, 2026-10-21); the edges' glows meet there instead.
uniform sampler2D Sampler0;
uniform sampler2D DepthSampler;

layout(location = 0) in vec2 texCoord;
layout(location = 1) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

void main() {
    float dist = length(texCoord * 2.0 - 1.0);
    fragColor = outline_color(vertexColor, outline_core(dist), 0.0, outline_visibility(DepthSampler));
}
