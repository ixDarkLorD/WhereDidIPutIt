#version 330

#moj_import <wheredidiputit:outline.glsl>

// The path line (Guidance): a ribbon facing the camera along the route, floating above the ground. v runs across it
// (0.5 on the line), u along it in blocks, counted back from the container (0 there, negative towards the player). The
// outline's thin bright line in its soft glow, with chevrons pointing to the container drifting along it.
uniform sampler2D Sampler0;
uniform sampler2D DepthSampler;

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

// Blocks between chevrons, and how fast they drift towards the container, in blocks a second.
const float CHEVRON_SPACING = 1.6;
const float CHEVRON_SPEED = 1.4;

void main() {
    float across = abs(texCoord.y - 0.5) * 2.0;
    // Each chevron's arms trail behind its tip, so it points the way.
    float phase = fract((texCoord.x - VfxTime.x * CHEVRON_SPEED) / CHEVRON_SPACING + across * 0.16);
    float chevron = smoothstep(0.0, 0.025, phase) * (1.0 - smoothstep(0.08, 0.13, phase)) * (1.0 - smoothstep(0.6, 0.85, across));
    float core = max(outline_core(across), chevron);
    float glow = outline_glow(across) * 0.8;
    // Behind blocks it stays, dimmer, so the way on can be seen round a corner.
    float visibility = mix(0.35, 1.0, vfx_depth_fade(texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r, gl_FragCoord.z));
    fragColor = outline_color(vertexColor, core, glow, visibility);
}
