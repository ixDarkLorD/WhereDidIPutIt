#version 330

#moj_import <coolcatlib:vfx.glsl>

// The outline round a found container (ContainerHighlight): each edge is a beam, a solid line down its middle that
// fades to its sides; the halo pass draws wider, fainter beams with the same shader. u runs along a beam, v across it.
// Unlike CoolCatLib's own shader it doesn't vanish behind blocks, it only dims there, so the container can be found
// through walls.
uniform sampler2D Sampler0;
uniform sampler2D DepthSampler;

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float across = abs(texCoord.y - 0.5) * 2.0;
    float core = 1.0 - smoothstep(0.35, 0.6, across);
    float glow = (1.0 - across) * (1.0 - across) * 0.6;
    float light = max(core, glow);
    float inFront = vfx_depth_fade(texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r, gl_FragCoord.z);
    light *= mix(0.75, 1.0, inFront);
    float alpha = clamp(light, 0.0, 1.0) * vertexColor.a;
    if (alpha < 0.003) discard;
    fragColor = vec4(mix(vertexColor.rgb, vec3(1.0), core * 0.25), alpha);
}
