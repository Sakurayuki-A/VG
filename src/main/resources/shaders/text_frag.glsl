#version 450

layout(binding = 0) uniform sampler2D texSampler;

layout(location = 0) in vec2 fragTexCoord;
layout(location = 1) in vec3 fragColor;

layout(location = 0) out vec4 outColor;

void main() {
    float alpha = texture(texSampler, fragTexCoord).a;
    outColor = vec4(fragColor, alpha);
}