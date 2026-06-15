#version 450

// 顶点输入 - 扩展格式包含阴影参数
layout(location = 0) in vec2 inPosition;     // NDC 坐标
layout(location = 1) in vec3 inColor;        // RGB
layout(location = 2) in float inAlpha;       // Alpha
layout(location = 3) in vec4 inShadowRect;   // 阴影矩形 (x, y, width, height) 像素坐标
layout(location = 4) in vec2 inBlurParams;   // 模糊参数 (spread, blur)

// 输出到片段着色器
layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec4 fragShadowRect;
layout(location = 2) out vec2 fragBlurParams;

void main() {
    gl_Position = vec4(inPosition, 0.0, 1.0);
    fragColor = vec4(inColor, inAlpha);
    fragShadowRect = inShadowRect;
    fragBlurParams = inBlurParams;
}
