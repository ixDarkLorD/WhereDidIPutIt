#version 330

#moj_import <coolcatlib:vfx.glsl>

// The ghost of a found container (ContainerHighlight, ContainerGhost): its own block faces, each in its texture,
// drawn only where something stands in front of them. Where the container is in plain sight its faces (pushed out of
// the block a hair) are in front of everything, so nothing is drawn there and the real block shows.
uniform sampler2D Sampler0;
uniform sampler2D DepthSampler;

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 texel = texture(Sampler0, texCoord);
    float hidden = 1.0 - vfx_depth_fade(texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r, gl_FragCoord.z);
    float alpha = texel.a * vertexColor.a * hidden;
    if (alpha < 0.01) discard;
    fragColor = vec4(texel.rgb * vertexColor.rgb, alpha);
}
