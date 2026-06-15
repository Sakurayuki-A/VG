这是 Vibe Coding 项目最重要的东西。

---

VG Framework Development Specification

1. Framework Goal
VG 是一个基于 Vulkan 的 Java GUI Framework。
核心目标：
高性能
可扩展
API 简洁
组件化
适用于：
Desktop GUI
Overlay GUI
Game GUI
Cheat GUI
Tool GUI

---

2. Directory Structure

所有代码必须遵守以下目录结构。

com.vg
│
├── core
│   ├── VG.java
│   ├── VGConfig.java
│   ├── Anchor.java
│   ├── RenderContext.java
│   └── MouseContext.java
│
├── color
│   └── VGColor.java
│
├── component
│   ├── VGCard.java
│   ├── VGButton.java
│   ├── VGText.java
│   ├── VGTooltip.java
│   └── VGPanel.java
│
├── layout
│   ├── RowLayout.java
│   ├── ColumnLayout.java
│   └── StackLayout.java
│
├── event
│   ├── HoverEvent.java
│   ├── ClickEvent.java
│   └── DragEvent.java
│
├── animation
│   ├── Animation.java
│   ├── Easing.java
│   └── Animator.java
│
├── input
│   ├── MouseInput.java
│   └── KeyboardInput.java
│
├── renderer
│   ├── VulkanRenderer.java
│   ├── PipelineManager.java
│   ├── BufferManager.java
│   └── CommandManager.java
│
├── shader
│   ├── GradientShader.java
│   ├── ShadowShader.java
│   └── BlurShader.java
│
└── util
    ├── MathUtil.java
    └── VGTimer.java


---

3. API Entry Rule
所有绘制 API 必须统一从：

VG.xxx()
进入。
正确：
VG.rect(...)
VG.roundRect(...)
VG.gradientRect(...)
VG.shadow(...)

错误：
new RectRenderer()
new ShadowRenderer()
new GradientRenderer()

Renderer 属于内部实现。
不能暴露给用户。

---

4. Component Rule
组件负责：
状态
布局
交互

组件不负责：

Vulkan命令
Pipeline
Buffer
Shader管理

例如：
VGButton
负责：
hover
click
animation
tooltip
不负责：
vkCmdDraw

---

5. Rendering Rule
所有组件：
render()
只负责渲染自己。

例如：
button.render();
card.render();
tooltip.render();

组件之间不能互相管理生命周期。
禁止：
button.renderTooltip();

应该：
button.render();
if(button.hovered()) {
    tooltip.render();
}


---

6. Interaction Rule
未来所有交互统一建立在：
contains(x,y)
之上。

例如：
button.contains(mouseX,mouseY)

派生：
Hover
Click
Drag
Tooltip
Focus
Select

禁止直接实现：
button.isHover()

而没有：
contains()
基础判断。

---

7. Anchor Rule
唯一合法锚点：
Anchor
包含：

TOP_LEFT
TOP_CENTER
TOP_RIGHT

CENTER_LEFT
CENTER
CENTER_RIGHT

BOTTOM_LEFT
BOTTOM_CENTER
BOTTOM_RIGHT

VGAnchor：

@Deprecated
仅保留兼容。
禁止新增功能。

---

8. Layout Rule
布局优先。
禁止：
x = 584
y = 317
大量硬编码。
优先：
Anchor
Layout
Percentage
例如：
W * 0.5f
H * 0.5f
或者：
ctx.anchor(Anchor.CENTER)

---

9. Animation Rule
所有动画必须进入：
animation/

禁止：
buttonAlpha += 0.1f;
散落在组件内部。

统一：
Animator
Animation
Easing
管理。

---

10. Input Rule
鼠标输入统一入口：
MouseContext
例如：

ctx.mouse().x()
ctx.mouse().y()

ctx.mouse().leftPressed()

禁止：
GLFW.glfwGetCursorPos(...)
出现在业务组件。

---

11. Shader Rule
Shader 只负责视觉。
包括：
Shadow
Glow
Blur
Bloom
Gradient

Shader 不负责：
Hover
Click
Tooltip
Drag

---

12. Future Development Priority
当前开发顺序：
P0
交互系统
实现：
contains()
MouseContext
Hover
Click
Drag
Tooltip
VGButton

---

P1
文本系统
实现：
Font
Text
TextLayout

---

P2
动画系统
实现：
Animator
Easing
Transition

---

P3
视觉增强
实现：
Glow
Blur
Bloom

---

13. AI Coding Rule
AI 修改代码前必须遵守：
不重复造轮子
如果已有：
Anchor
禁止生成：
VGAnchor2
AnchorUtil
PositionAnchor

---

不新增平行 API

如果已有：

VGCard

禁止生成：

CardWidget
UICard
CardComponent

---

不绕过框架
禁止：
GLFW
Vulkan
LWJGL

直接出现在业务代码。

必须经过：
VG
RenderContext
MouseContext
封装层。

---

14. Component Responsibility Rule
组件只负责：
状态
几何信息
渲染
例如：
button.render();

button.contains(x,y);

button.x();
button.y();

button.width();
button.height();

组件不负责：
业务逻辑
显示时机
隐藏时机
动画时机
延迟逻辑

禁止：
button.showTooltip();

button.hideTooltip();

button.setTooltipDelay(500);

button.playHoverAnimation();
这些属于业务层行为。

15. Capability vs Policy Rule
VG 采用：
Capability（能力）
Policy（策略）
分离设计。
Capability
框架提供能力。

例如：
button.hovered();

button.clicked();

button.contains(x,y);

tooltip.render();

tooltip.text(...);

tooltip.position(...);

animator.value();
这些都是能力。
Policy
业务决定策略。

例如：

if(button.hoveredFor(500)) {
    tooltip.render();
}

if(button.clicked()) {
    popup.render();
}

tooltip.position(
    mouse.x() + 10,
    mouse.y() + 10
);

tooltip.alpha(
    fadeAnimation.value()
);

这些属于策略。

VG 不提供：

tooltip.showAfter(500);

tooltip.followMouse();

tooltip.fadeIn();

tooltip.fadeOut();

tooltip.autoPosition();

因为这些会导致：
API爆炸
组件耦合
行为不可预测

VG 只提供：
Button
Card
Tooltip
Menu
Dialog
Animator
MouseContext

而：
什么时候显示
什么时候隐藏
显示什么内容
播放什么动画
显示多久
全部由业务层控制。