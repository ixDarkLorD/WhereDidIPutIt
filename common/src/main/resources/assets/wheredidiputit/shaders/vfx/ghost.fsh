#version 330

#moj_import <coolcatlib:vfx.glsl>

// The ghost of a found container (ContainerHighlight, ContainerGhost): a picture of it, drawn opaque by the game's own
// renderers, laid over it at one opacity and only where something stands in front of it. The picture's quad is at the
// container's nearest point, so where the container is in plain sight it is behind the quad: nothing is drawn there
// and the real block shows.
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
