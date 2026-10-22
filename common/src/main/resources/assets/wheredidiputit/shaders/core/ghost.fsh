#version 330

#moj_import <minecraft:dynamictransforms.glsl>

// Found containers' ghosts (ContainerGhost) laid over the world: their picture, drawn opaque with the world's own
// camera, wherever the world is in front of them. Each is drawn a hair larger than its block, so where a container is in
// plain sight its ghost is just in front of it and nothing is drawn: the real block shows. ColorModulator holds the
// tint (rgb) and the opacity (a).
uniform sampler2D GhostSampler;
uniform sampler2D GhostDepthSampler;
uniform sampler2D DepthSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    ivec2 pixel = ivec2(gl_FragCoord.xy);
    // Depth is reversed (26.2): cleared to 0, nearer is larger. Where a ghost was drawn its depth is more than the
    // cleared 0; its colour's alpha can't tell, as the block pipelines don't all write alpha.
    float ghostDepth = texelFetch(GhostDepthSampler, pixel, 0).r;
    if (ghostDepth <= 0.0) discard;
    if (texelFetch(DepthSampler, pixel, 0).r <= ghostDepth) discard;
    vec3 ghost = texelFetch(GhostSampler, pixel, 0).rgb;
    fragColor = vec4(ghost * ColorModulator.rgb, ColorModulator.a);
}
