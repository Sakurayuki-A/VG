#version 450

// 从顶点着色器接收
layout(location = 0) in vec4 fragColor;
layout(location = 1) in vec4 fragShadowRect;  // (rectX, rectY, rectW, rectH) 像素坐标
layout(location = 2) in vec2 fragBlurParams;   // (spread, blur)

// 输出
layout(location = 0) out vec4 outColor;

// 计算点到矩形的有向距离（SDF）
// 返回值：负数表示在矩形内部，正数表示在矩形外部
float sdRectangle(vec2 p, vec2 center, vec2 halfSize) {
    vec2 d = abs(p - center) - halfSize;
    return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0);
}

void main() {
    // 提取阴影参数
    float shadowX = fragShadowRect.x;
    float shadowY = fragShadowRect.y;
    float shadowW = fragShadowRect.z;
    float shadowH = fragShadowRect.w;
    
    float spread = fragBlurParams.x;
    float blur = fragBlurParams.y;
    
    // 使用 gl_FragCoord 获取当前像素坐标（屏幕空间）
    vec2 pixelPos = gl_FragCoord.xy;
    
    // 矩形中心和半尺寸
    vec2 center = vec2(shadowX + shadowW * 0.5, shadowY + shadowH * 0.5);
    vec2 halfSize = vec2(shadowW * 0.5, shadowH * 0.5);
    
    // 计算当前像素到矩形边界的距离
    float dist = sdRectangle(pixelPos, center, halfSize);
    
    // 应用 spread（向外扩展实心区域）
    dist -= spread;
    
    // 基于距离计算 alpha
    // dist < 0: 在内部，alpha = 1.0
    // dist > blur: 在模糊区域外，alpha = 0.0
    // 0 <= dist <= blur: 平滑过渡
    float alpha = 1.0 - smoothstep(0.0, blur, dist);
    
    // 应用颜色和最终 alpha
    outColor = vec4(fragColor.rgb, fragColor.a * alpha);
}
