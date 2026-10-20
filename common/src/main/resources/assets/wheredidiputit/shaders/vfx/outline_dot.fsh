#version 330

#moj_import <wheredidiputit:outline.glsl>

// A corner of the outline (ContainerHighlight): a disc facing the camera, the same line and glow as the edges but
// round, so corners are rounded and their glow is even.
uniform sampler2D Sampler0;
uniform sampler2D DepthSampler;

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float dist = length(texCoord * 2.0 - 1.0);
    fragColor = outline_color(vertexColor, outline_core(dist), outline_glow(dist), outline_visibility(DepthSampler));
}
