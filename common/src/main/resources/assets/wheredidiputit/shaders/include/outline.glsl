#version 330

#moj_import <coolcatlib:vfx.glsl>

// The look of the found container's outline (ContainerHighlight), shared by its edges (vfx/outline_line) and its
// corners (vfx/outline_dot). Distances are in units of the glow's half width: 0 on the line, 1 where the glow ends.

// How much of the glow's half width the solid line takes; ContainerHighlight's widths keep the same ratio.
const float OUTLINE_CORE = 0.25;

// The solid line: opaque, with a soft edge one tenth of the glow wide, so overlapping pieces don't brighten.
float outline_core(float dist) {
    return 1.0 - smoothstep(OUTLINE_CORE - 0.05, OUTLINE_CORE + 0.05, dist);
}

// The glow around it: smooth all the way out, and zero where the shape ends.
float outline_glow(float dist) {
    float t = clamp((dist - OUTLINE_CORE) / (1.0 - OUTLINE_CORE), 0.0, 1.0);
    return 0.45 * (1.0 - t) * (1.0 - t) * (1.0 - t * t);
}

// Shown fully in front of blocks and dimmed behind them, so the container can be found through walls.
float outline_visibility(sampler2D depthSampler) {
    return mix(0.7, 1.0, vfx_depth_fade(texelFetch(depthSampler, ivec2(gl_FragCoord.xy), 0).r, gl_FragCoord.z));
}

vec4 outline_color(vec4 vertexColor, float core, float glow, float visibility) {
    float alpha = max(core, glow) * vertexColor.a * visibility;
    if (alpha < 0.003) discard;
    // The line a little paler than its glow, so it reads as a bright edge.
    return vec4(mix(vertexColor.rgb, vec3(1.0), core * 0.3), alpha);
}
