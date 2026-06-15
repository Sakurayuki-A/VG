# VG Framework

**Vulkan-accelerated vector graphics engine for Java.**  
Draw shapes, text, icons, shadows, and interactive UI components — all GPU-rendered through Vulkan.

---

## Features

| Category | Capabilities |
|----------|-------------|
| **Rendering** | Rect, RoundRect, Circle, Line, Gradient (4-corner), MSAA 4x |
| **Shadows** | SDF-based drop shadows with configurable spread, blur, color, offset |
| **Text** | Multi-font, multi-line with word wrap, ellipsis truncation, alignment |
| **Icons** | Material Symbols (1000+ icons), variable weight/fill/grade/size |
| **UI Components** | Button (hover/press), Input (focus/placeholder), Card (shadow/auto-size) |
| **Layout** | Spacing & Padding system, Auto Measure, Debug Overlay (bounds/baseline) |
| **Transform** | 2D translate/scale/rotate with matrix stack nesting |
| **Clipping** | Nested pushClip/popClip |
| **Pipeline** | Separate batch-optimized pipelines for Shape / Text / Icon / Shadow |

---

## Quick Start

### Requirements

- **Java 21+**
- **Vulkan-compatible GPU** (NVIDIA / AMD / Intel)
- Vulkan SDK *(optional, for validation layers)*

### Run the Showcase

```bash
git clone <repo-url>
cd VG
./gradlew.bat runVGShowcase
```

### Minimal Example

```java
VGConfig config = new VGConfig()
    .setWindowTitle("Hello VG")
    .setWindowWidth(800)
    .setWindowHeight(600);

try (VG vg = new VG(config)) {
    vg.init();
    vg.loadFont("fonts/arial.ttf", 15.0f);

    vg.setRenderCallback(ctx -> {
        // Background gradient
        VG.gradientRect(0, 0, VG.width(), VG.height(),
            0xFF1a1a2e, 0xFF16213e, 0xFF0f3460, 0xFF533483);

        // Shapes
        VG.roundRect(100, 100, 200, 60, 12, 0xFFe94560);
        VG.circle(400, 130, 30, 0xFF0f3460);
        VG.line(100, 200, 600, 200, 3, 0xFFe94560);

        // Shadow + Rect
        VG.shadow(100, 240, 200, 80, 4, 12, 0x80000000);
        VG.roundRect(100, 240, 200, 80, 8, 0xFF16213e);

        // Text
        VG.text("Hello VG!", 120, 280, 0xFFFFFFFF);
    });

    vg.run();
}
```

### Gradle Tasks

| Task | Description |
|------|-------------|
| `runVGShowcase` | Complete feature showcase |

---

## Project Structure

```
VG/
├── src/main/java/com/vg/
│   ├── core/          # Core API: VG, VGColor, VGFont, VGTextLayout, etc.
│   │   ├── batch/     # Vertex batcher for draw-call merging
│   │   ├── color/     # Color utilities
│   │   ├── command/   # Draw command types
│   │   ├── component/ # Button, Input, Card
│   │   ├── font/      # Font loading & metrics
│   │   ├── icon/      # Material Symbols icon font
│   │   ├── text/      # Text layout (wrap, align, ellipsis)
│   │   └── theme/     # Brand theme colors
│   ├── vulkan/        # Vulkan renderer & pipeline management
│   ├── window/        # GLFW window wrapper
│   └── sandbox/       # Showcase demos
├── src/main/resources/
│   ├── fonts/         # arial.ttf, MaterialSymbolsOutlined-Regular.ttf
│   └── shaders/       # GLSL sources & SPIR-V binaries
└── build.gradle.kts   # Java 21, LWJGL 3.3.4, Vulkan
```

---

## Prerequisites

- **Gradle 8.8** — Download `gradle-8.8-bin.zip` and place it in the project root.
