package com.vg.vulkan;

import com.vg.core.ResourceManager;
import com.vg.core.VG;
import com.vg.core.VGConfig;
import com.vg.core.batch.VertexBatcher;
import com.vg.core.command.DrawCommand;
import com.vg.core.command.IconCommand;
import com.vg.core.command.PopClipCommand;
import com.vg.core.command.PushClipCommand;
import com.vg.core.command.ShadowCommand;
import com.vg.core.command.TextCommand;
import com.vg.core.font.VGFont;
import com.vg.core.icon.VGIconFont;
import com.vg.core.util.VKUtil;
import com.vg.window.VGWindow;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;

public class VKRenderer implements AutoCloseable {

    private final VGConfig config;
    private final VGWindow window;
    private final VG vg;
    
    // 通用资源管理器，自动管理所有 Vulkan 资源
    private final ResourceManager resourceManager = new ResourceManager();

    private VKInstance vkInstance;
    private VKSurface vkSurface;
    private VKDevice vkDevice;
    private VKSwapchain vkSwapchain;
    private VKRenderPass vkRenderPass;
    private VKFramebuffer vkFramebuffer;
    private VKCommandPool vkCommandPool;
    private VKSyncObjects vkSyncObjects;
    private VkCommandBuffer[] commandBuffers;

    private VKShaderModule vertexShader;
    private VKShaderModule fragmentShader;
    private VKPipeline vkPipeline;
    private VKDynamicVertexBuffer dynamicVertexBuffer;

    private VKShaderModule textVertexShader;
    private VKShaderModule textFragmentShader;
    private VKPipelineText textPipeline;
    private VKDynamicTextVertexBuffer textVertexBuffer;

    private VKPipelineText iconPipeline;
    private VKDynamicTextVertexBuffer iconVertexBuffer;

    // SDF 阴影渲染
    private VKShaderModule shadowVertexShader;
    private VKShaderModule shadowFragmentShader;
    private VKPipelineShadow shadowPipeline;
    private VKDynamicShadowVertexBuffer shadowVertexBuffer;

    private int msaaSamples = VK_SAMPLE_COUNT_4_BIT;
    private long[] msaaColorImages;
    private long[] msaaImageViews;
    private long[] msaaMemories;
    private int msaaImageCount = 0;

    private static final int MAX_FRAMES_IN_FLIGHT = 2;
    private int currentFrame = 0;
    private boolean framebufferResized = false;
    private boolean running = false;

    // ── 性能统计 ──
    private int lastFrameDrawCalls = 0;
    private int lastFrameVertices = 0;
    private int lastFrameBatches = 0;
    private int lastFrameTriangles = 0;
    private float fps = 0f;
    private long lastFpsTime = 0L;
    private int fpsFrameCount = 0;
    private float fpsAccumulator = 0f;
    // 已保存的帧统计（在 recordCommandBuffer 之后更新，给 getStats 读取）
    private int savedDrawCalls = 0;
    private int savedVertices = 0;
    private int savedBatches = 0;
    private int savedTriangles = 0;

    // ── 裁剪栈 ──
    private record ClipRect(float x, float y, float w, float h) {}
    private final Deque<ClipRect> clipStack = new ArrayDeque<>();

    private void pushClip(float x, float y, float w, float h) {
        if (!clipStack.isEmpty()) {
            ClipRect p = clipStack.peek();
            float nx = Math.max(p.x, x);
            float ny = Math.max(p.y, y);
            float nw = Math.min(p.x + p.w, x + w) - nx;
            float nh = Math.min(p.y + p.h, y + h) - ny;
            clipStack.push(new ClipRect(nx, ny, Math.max(0, nw), Math.max(0, nh)));
        } else {
            clipStack.push(new ClipRect(x, y, Math.max(0, w), Math.max(0, h)));
        }
    }

    private void applyClipScissor(VkCommandBuffer commandBuffer, MemoryStack stack) {
        try (MemoryStack localStack = MemoryStack.stackPush()) {
            VkRect2D.Buffer scissor = VkRect2D.calloc(1, localStack);
            if (!clipStack.isEmpty()) {
                ClipRect r = clipStack.peek();
                scissor.offset().set(Math.round(r.x), Math.round(r.y));
                scissor.extent().set(Math.max(0, Math.round(r.w)), Math.max(0, Math.round(r.h)));
            } else {
                scissor.offset().set(0, 0);
                scissor.extent().set(
                        vkSwapchain.getExtent().width(),
                        vkSwapchain.getExtent().height()
                );
            }
            vkCmdSetScissor(commandBuffer, 0, scissor);
        }
    }

    /**
     * 获取上一帧的性能统计数据快照
     */
    public PerformanceStats getStats() {
        return new PerformanceStats(fps, savedDrawCalls, savedVertices, savedBatches, savedTriangles);
    }

    public static class PerformanceStats {
        public final float fps;
        public final int drawCalls;
        public final int vertices;
        public final int batches;
        public final int triangles;

        public PerformanceStats(float fps, int drawCalls, int vertices, int batches, int triangles) {
            this.fps = fps;
            this.drawCalls = drawCalls;
            this.vertices = vertices;
            this.batches = batches;
            this.triangles = triangles;
        }

        @Override
        public String toString() {
            return String.format("FPS: %.1f | Draw Calls: %d | Vertices: %d | Batches: %d | Triangles: %d",
                    fps, drawCalls, vertices, batches, triangles);
        }
    }

    public VKRenderer(VGConfig config, VGWindow window, VG vg) {
        this.config = config;
        this.window = window;
        this.vg = vg;
    }

    public void init() {
        // 创建并注册所有资源到资源管理器
        vkInstance = resourceManager.register(new VKInstance(config, window.getRequiredVulkanExtensions()));

        long surfaceHandle = window.createVulkanSurface(vkInstance.address());
        vkSurface = resourceManager.register(new VKSurface(vkInstance.get(), surfaceHandle));

        vkDevice = resourceManager.register(new VKDevice(vkInstance.get(), vkSurface.get()));

        int maxSamples = vkDevice.getMaxSampleCount();
        msaaSamples = Math.min(msaaSamples, maxSamples);
        System.out.println("[VG] MSAA: " + msaaSamples + "x (max supported: " + maxSamples + "x)");

        vkSwapchain = resourceManager.register(new VKSwapchain(vkDevice, vkSurface.get(),
                window.getFramebufferWidth(), window.getFramebufferHeight()));

        createMsaaResources();

        vkRenderPass = resourceManager.register(new VKRenderPass(vkDevice.get(), vkSwapchain.getImageFormat(), msaaSamples));
        vkFramebuffer = resourceManager.register(new VKFramebuffer(vkDevice.get(), vkSwapchain, vkRenderPass, msaaImageViews));

        vkCommandPool = resourceManager.register(new VKCommandPool(vkDevice.get(), vkDevice.getGraphicsQueueFamily()));
        commandBuffers = vkCommandPool.allocateCommandBuffers(MAX_FRAMES_IN_FLIGHT);

        vkSyncObjects = resourceManager.register(new VKSyncObjects(vkDevice.get(), vkSwapchain.getImageCount(), MAX_FRAMES_IN_FLIGHT));

        vertexShader = resourceManager.register(new VKShaderModule(vkDevice.get(), "shaders/vert.spv"));
        fragmentShader = resourceManager.register(new VKShaderModule(vkDevice.get(), "shaders/frag.spv"));
        vkPipeline = resourceManager.register(new VKPipeline(vkDevice.get(), vkRenderPass, vertexShader, fragmentShader,
                window.getFramebufferWidth(), window.getFramebufferHeight(), msaaSamples));

        textVertexShader = resourceManager.register(new VKShaderModule(vkDevice.get(), "shaders/text_vert.spv"));
        textFragmentShader = resourceManager.register(new VKShaderModule(vkDevice.get(), "shaders/text_frag.spv"));

        dynamicVertexBuffer = resourceManager.register(new VKDynamicVertexBuffer(vkDevice.get(), vkDevice, 400000));
        textVertexBuffer = resourceManager.register(new VKDynamicTextVertexBuffer(vkDevice.get(), vkDevice, 65536));
        iconVertexBuffer = resourceManager.register(new VKDynamicTextVertexBuffer(vkDevice.get(), vkDevice, 65536));

        // 初始化 SDF 阴影
        shadowVertexShader = resourceManager.register(new VKShaderModule(vkDevice.get(), "shaders/shadow_vert.spv"));
        shadowFragmentShader = resourceManager.register(new VKShaderModule(vkDevice.get(), "shaders/shadow_frag.spv"));
        shadowVertexBuffer = resourceManager.register(new VKDynamicShadowVertexBuffer(vkDevice.get(), vkDevice, 16384));
        createShadowPipeline(window.getFramebufferWidth(), window.getFramebufferHeight());

        window.setResizeCallback((width, height) -> {
            framebufferResized = true;
        });

        System.out.println("[VG] Renderer initialized successfully");
    }

    public void run() {
        running = true;

        while (!window.shouldClose() && running) {
            window.pollEvents();

            int width = window.getFramebufferWidth();
            int height = window.getFramebufferHeight();

            if (width == 0 || height == 0) {
                continue;
            }

            if (framebufferResized) {
                recreateSwapchain();
                continue;
            }

            drawFrame();
        }

        vkDevice.waitIdle();
    }

    private void drawFrame() {
        int frameIndex = currentFrame % MAX_FRAMES_IN_FLIGHT;

        int fenceStatus = vkGetFenceStatus(vkDevice.get(), vkSyncObjects.getInFlightFence(frameIndex));

        if (fenceStatus == VK_NOT_READY) {
            return;
        }

        VKUtil.check(fenceStatus, "Failed to get fence status");

        vkResetFences(vkDevice.get(), vkSyncObjects.getInFlightFence(frameIndex));

        // 每帧执行渲染回调，重新生成绘图命令
        vg.executeRenderCallback();

        int imageIndex = vkSwapchain.acquireNextImage(vkSyncObjects.getImageAvailableSemaphore(frameIndex));

        if (imageIndex == -1) {
            recreateSwapchain();
            return;
        }

        // 重置性能计数器（在 executeRenderCallback 之后，避免回调读取到 0）
        lastFrameDrawCalls = 0;
        lastFrameVertices = 0;
        lastFrameBatches = 0;

        recordCommandBuffer(commandBuffers[frameIndex], imageIndex);

        // 保存帧统计快照（记录完毕后给下一帧的 getStats 读取）
        savedDrawCalls = lastFrameDrawCalls;
        savedVertices = lastFrameVertices;
        savedBatches = lastFrameBatches;
        savedTriangles = lastFrameVertices / 3;

        submitCommandBuffer(frameIndex, imageIndex);

        presentFrame(imageIndex);

        // ── FPS 统计 ──
        long now = System.nanoTime();
        if (lastFpsTime == 0) {
            lastFpsTime = now;
        }
        fpsFrameCount++;
        fpsAccumulator += (now - lastFpsTime) / 1e9f;
        lastFpsTime = now;
        if (fpsAccumulator >= 0.5f) { // 每 0.5s 更新一次 FPS
            fps = fpsFrameCount / fpsAccumulator;
            fpsFrameCount = 0;
            fpsAccumulator = 0f;
        }

        currentFrame++;
    }

    private void recordCommandBuffer(VkCommandBuffer commandBuffer, int imageIndex) {
        vkResetCommandBuffer(commandBuffer, 0);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            VKUtil.check(vkBeginCommandBuffer(commandBuffer, beginInfo), "Failed to begin command buffer");

            VkClearValue.Buffer clearValues = VkClearValue.calloc(1, stack);
            clearValues.get(0).color().float32(0, 0.1f);
            clearValues.get(0).color().float32(1, 0.15f);
            clearValues.get(0).color().float32(2, 0.25f);
            clearValues.get(0).color().float32(3, 1.0f);

            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(vkRenderPass.get())
                    .framebuffer(vkFramebuffer.get(imageIndex))
                    .renderArea(area -> area
                            .offset(offset -> offset.x(0).y(0))
                            .extent(vkSwapchain.getExtent()))
                    .pClearValues(clearValues);

            vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

            setViewport(commandBuffer);

            // 设置全屏 scissor（Vulkan 要求动态 scissor 必须在首次绘制前设置）
            var fullScissor = org.lwjgl.vulkan.VkRect2D.calloc(1, stack);
            fullScissor.offset().set(0, 0);
            fullScissor.extent().set(
                    vkSwapchain.getExtent().width(),
                    vkSwapchain.getExtent().height()
            );
            vkCmdSetScissor(commandBuffer, 0, fullScissor);

            List<DrawCommand> commands = vg.getDrawCommands();

            // 清空裁剪栈，默认为全屏
            clipStack.clear();

            // ── 管线分桶（每个裁剪段内按管线类型积累，裁剪边界时刷新） ──
            // 段内顺序：Shape → Text → Icon → Shadow（管线一致即可合并为 1 个 Batch）
            List<DrawCommand> shapeBucket = new ArrayList<>();
            List<DrawCommand> textBucket = new ArrayList<>();
            List<DrawCommand> iconBucket = new ArrayList<>();
            List<DrawCommand> shadowBucket = new ArrayList<>();

            long shapeBufferOffset = 0;
            long textBufferOffset = 0;

            for (DrawCommand cmd : commands) {
                // ── 裁剪边界：刷新当前段所有桶，再切换裁剪 ──
                if (cmd instanceof PushClipCommand pcc) {
                    shapeBufferOffset = flushShapeBatch(commandBuffer, shapeBucket, shapeBufferOffset);
                    shapeBucket.clear();
                    textBufferOffset = flushTextBatch(commandBuffer, textBucket, textBufferOffset);
                    textBucket.clear();
                    flushIconBatch(commandBuffer, iconBucket);
                    iconBucket.clear();
                    flushShadowBatch(commandBuffer, shadowBucket);
                    shadowBucket.clear();
                    pushClip(pcc.x, pcc.y, pcc.width, pcc.height);
                    applyClipScissor(commandBuffer, stack);
                    continue;
                }
                if (cmd instanceof PopClipCommand) {
                    shapeBufferOffset = flushShapeBatch(commandBuffer, shapeBucket, shapeBufferOffset);
                    shapeBucket.clear();
                    textBufferOffset = flushTextBatch(commandBuffer, textBucket, textBufferOffset);
                    textBucket.clear();
                    flushIconBatch(commandBuffer, iconBucket);
                    iconBucket.clear();
                    flushShadowBatch(commandBuffer, shadowBucket);
                    shadowBucket.clear();
                    clipStack.pop();
                    applyClipScissor(commandBuffer, stack);
                    continue;
                }

                // ── 按管线类型入桶 ──
                if (cmd instanceof TextCommand) {
                    textBucket.add(cmd);
                } else if (cmd instanceof IconCommand) {
                    iconBucket.add(cmd);
                } else if (cmd instanceof ShadowCommand) {
                    shadowBucket.add(cmd);
                } else {
                    // Rect / RoundRect / Line / GradientRect / GradientRoundRect / Circle / RawVertex
                    shapeBucket.add(cmd);
                }
            }

            // ── 刷新剩余缓存 ──
            flushShapeBatch(commandBuffer, shapeBucket, shapeBufferOffset);
            flushTextBatch(commandBuffer, textBucket, textBufferOffset);
            flushIconBatch(commandBuffer, iconBucket);
            flushShadowBatch(commandBuffer, shadowBucket);

            vkCmdEndRenderPass(commandBuffer);

            VKUtil.check(vkEndCommandBuffer(commandBuffer), "Failed to end command buffer");
        }
    }

    private void submitCommandBuffer(int frameIndex, int imageIndex) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var waitSemaphores = stack.longs(vkSyncObjects.getImageAvailableSemaphore(frameIndex));
            var waitStages = stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            var signalSemaphores = stack.longs(vkSyncObjects.getRenderFinishedSemaphore(imageIndex));
            var pCommandBuffers = stack.pointers(commandBuffers[frameIndex].address());

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .waitSemaphoreCount(1)
                    .pWaitSemaphores(waitSemaphores)
                    .pWaitDstStageMask(waitStages)
                    .pCommandBuffers(pCommandBuffers)
                    .pSignalSemaphores(signalSemaphores);

            int result = vkQueueSubmit(vkDevice.getGraphicsQueue(), submitInfo,
                    vkSyncObjects.getInFlightFence(frameIndex));
            VKUtil.check(result, "Failed to submit draw command buffer");
        }
    }

    private void presentFrame(int imageIndex) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var waitSemaphores = stack.longs(vkSyncObjects.getRenderFinishedSemaphore(imageIndex));
            var swapchains = stack.longs(vkSwapchain.get());
            var pImageIndices = stack.ints(imageIndex);

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                    .pWaitSemaphores(waitSemaphores)
                    .pSwapchains(swapchains)
                    .pImageIndices(pImageIndices);

            VkPresentInfoKHR.nwaitSemaphoreCount(presentInfo.address(), 1);
            VkPresentInfoKHR.nswapchainCount(presentInfo.address(), 1);

            int result = vkQueuePresentKHR(vkDevice.getPresentQueue(), presentInfo);

            if (result == VK_ERROR_OUT_OF_DATE_KHR || result == VK_SUBOPTIMAL_KHR || framebufferResized) {
                framebufferResized = false;
                recreateSwapchain();
            } else if (result != VK_SUCCESS) {
                VKUtil.check(result, "Failed to present");
            }
        }
    }

    private void setViewport(VkCommandBuffer commandBuffer) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
            
            viewport.get(0)
                    .x(0.0f)
                    .y(0.0f)
                    .width(vkSwapchain.getExtent().width())
                    .height(vkSwapchain.getExtent().height())
                    .minDepth(0.0f)
                    .maxDepth(1.0f);

            vkCmdSetViewport(
                    commandBuffer,
                    0,
                    viewport
            );
        }
    }

    private void recreateSwapchain() {
        int width = window.getFramebufferWidth();
        int height = window.getFramebufferHeight();

        while (width == 0 || height == 0) {
            window.pollEvents();
            if (window.shouldClose()) {
                return;
            }
            width = window.getFramebufferWidth();
            height = window.getFramebufferHeight();
        }

        vkDevice.waitIdle();

        destroyMsaaResources();

        if (vkFramebuffer != null) {
            resourceManager.unregister(vkFramebuffer);
            vkFramebuffer.close();
        }
        if (vkCommandPool != null) {
            vkCommandPool.freeCommandBuffers(commandBuffers);
        }
        if (vkSwapchain != null) {
            resourceManager.unregister(vkSwapchain);
            vkSwapchain.close();
        }
        if (vkPipeline != null) {
            resourceManager.unregister(vkPipeline);
            vkPipeline.close();
        }
        if (textPipeline != null) {
            resourceManager.unregister(textPipeline);
            textPipeline.close();
            textPipeline = null;
        }
        if (iconPipeline != null) {
            resourceManager.unregister(iconPipeline);
            iconPipeline.close();
            iconPipeline = null;
        }
        if (shadowPipeline != null) {
            resourceManager.unregister(shadowPipeline);
            shadowPipeline.close();
            shadowPipeline = null;
        }

        vkSwapchain = resourceManager.register(new VKSwapchain(vkDevice, vkSurface.get(), width, height));

        createMsaaResources();

        vkFramebuffer = resourceManager.register(new VKFramebuffer(vkDevice.get(), vkSwapchain, vkRenderPass, msaaImageViews));
        commandBuffers = vkCommandPool.allocateCommandBuffers(MAX_FRAMES_IN_FLIGHT);
        vkPipeline = resourceManager.register(new VKPipeline(vkDevice.get(), vkRenderPass, vertexShader, fragmentShader, width, height, msaaSamples));
        createTextPipeline(width, height);
        createIconPipeline(width, height);
        createShadowPipeline(width, height);

        framebufferResized = false;
        currentFrame = 0;

        System.out.println("[VG] Swapchain recreated: " + width + "x" + height);
    }

    public void stop() {
        running = false;
    }

    private void createMsaaResources() {
        int imageCount = vkSwapchain.getImageCount();
        msaaImageCount = imageCount;
        msaaColorImages = new long[imageCount];
        msaaImageViews = new long[imageCount];
        msaaMemories = new long[imageCount];

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                    .imageType(VK_IMAGE_TYPE_2D)
                    .format(vkSwapchain.getImageFormat())
                    .extent(extent -> extent
                            .width(vkSwapchain.getExtent().width())
                            .height(vkSwapchain.getExtent().height())
                            .depth(1))
                    .mipLevels(1)
                    .arrayLayers(1)
                    .samples(msaaSamples)
                    .tiling(VK_IMAGE_TILING_OPTIMAL)
                    .usage(VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            for (int i = 0; i < imageCount; i++) {
                LongBuffer pImage = stack.mallocLong(1);
                int result = vkCreateImage(vkDevice.get(), imageInfo, null, pImage);
                VKUtil.check(result, "Failed to create MSAA color image " + i);
                msaaColorImages[i] = pImage.get(0);

                VkMemoryRequirements memReqs = VkMemoryRequirements.calloc(stack);
                vkGetImageMemoryRequirements(vkDevice.get(), msaaColorImages[i], memReqs);

                int memTypeIndex = findMemoryType(memReqs.memoryTypeBits(),
                        VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

                VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                        .allocationSize(memReqs.size())
                        .memoryTypeIndex(memTypeIndex);

                LongBuffer pMemory = stack.mallocLong(1);
                result = vkAllocateMemory(vkDevice.get(), allocInfo, null, pMemory);
                VKUtil.check(result, "Failed to allocate MSAA memory " + i);
                long memory = pMemory.get(0);
                msaaMemories[i] = memory;

                result = vkBindImageMemory(vkDevice.get(), msaaColorImages[i], memory, 0);
                VKUtil.check(result, "Failed to bind MSAA image memory " + i);

                VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                        .image(msaaColorImages[i])
                        .viewType(VK_IMAGE_VIEW_TYPE_2D)
                        .format(vkSwapchain.getImageFormat())
                        .subresourceRange(range -> range
                                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                                .baseMipLevel(0)
                                .levelCount(1)
                                .baseArrayLayer(0)
                                .layerCount(1));

                LongBuffer pView = stack.mallocLong(1);
                result = vkCreateImageView(vkDevice.get(), viewInfo, null, pView);
                VKUtil.check(result, "Failed to create MSAA image view " + i);
                msaaImageViews[i] = pView.get(0);
            }
        }

        System.out.println("[VG] Created " + imageCount + " MSAA color images (" + msaaSamples + "x)");
    }

    private void destroyMsaaResources() {
        if (msaaImageViews != null) {
            for (long view : msaaImageViews) {
                if (view != 0) vkDestroyImageView(vkDevice.get(), view, null);
            }
            msaaImageViews = null;
        }
        if (msaaColorImages != null) {
            for (long img : msaaColorImages) {
                if (img != 0) vkDestroyImage(vkDevice.get(), img, null);
            }
            msaaColorImages = null;
        }
        if (msaaMemories != null) {
            for (long mem : msaaMemories) {
                if (mem != 0) vkFreeMemory(vkDevice.get(), mem, null);
            }
            msaaMemories = null;
        }
        msaaImageCount = 0;
    }

    private int findMemoryType(int typeFilter, int properties) {
        VkPhysicalDeviceMemoryProperties memProps = vkDevice.getMemoryProperties();
        for (int i = 0; i < memProps.memoryTypeCount(); i++) {
            if ((typeFilter & (1 << i)) != 0
                    && (memProps.memoryTypes(i).propertyFlags() & properties) == properties) {
                return i;
            }
        }
        throw new RuntimeException("Failed to find suitable memory type");
    }

    public void createTextPipeline(int width, int height) {
        if (textPipeline != null) {
            // 从资源管理器注销旧管线（它会自动关闭）
            resourceManager.unregister(textPipeline);
            textPipeline.close();
            textPipeline = null;
        }

        // 使用当前激活的字体纹理（优先于 defaultFontTexture）
        var fontTexture = vg.getCurrentFontTexture();
        if (fontTexture == null) {
            System.out.println("[VKRenderer] WARNING: No font texture available for text pipeline!");
            return;
        }

        System.out.println("[VKRenderer] Creating text pipeline with font texture...");
        textPipeline = resourceManager.register(new VKPipelineText(vkDevice.get(), vkRenderPass,
                textVertexShader, textFragmentShader,
                width, height,
                fontTexture.getImageView(), fontTexture.getSampler(),
                msaaSamples));

        // 初始化所有字体纹理的描述符集（管线重建后旧描述符集已销毁，重新分配）
        vg.forEachFontTexture((tex) -> {
            tex.initDescriptorSet(vkDevice.get(), textPipeline.getDescriptorPool(), textPipeline.getDescriptorSetLayout());
        });
    }

    public void createIconPipeline(int width, int height) {
        if (iconPipeline != null) {
            resourceManager.unregister(iconPipeline);
            iconPipeline.close();
            iconPipeline = null;
        }

        var iconTex = vg.getIconTexture();
        if (iconTex == null) {
            return;
        }

        iconPipeline = resourceManager.register(new VKPipelineText(vkDevice.get(), vkRenderPass,
                textVertexShader, textFragmentShader,
                width, height,
                iconTex.getImageView(), iconTex.getSampler(),
                msaaSamples));

        // 初始化图标纹理的描述符集
        iconTex.initDescriptorSet(vkDevice.get(), iconPipeline.getDescriptorPool(), iconPipeline.getDescriptorSetLayout());
    }

    public void createShadowPipeline(int width, int height) {
        if (shadowPipeline != null) {
            resourceManager.unregister(shadowPipeline);
            shadowPipeline.close();
            shadowPipeline = null;
        }

        shadowPipeline = resourceManager.register(new VKPipelineShadow(vkDevice.get(), vkRenderPass,
                shadowVertexShader, shadowFragmentShader,
                width, height, msaaSamples));
    }

    public VKDevice getDevice() {
        return vkDevice;
    }

    public VKSwapchain getSwapchain() {
        return vkSwapchain;
    }

    // ── 刷新形状桶（Rect / RoundRect / Line / Gradient 等） ──
    private long flushShapeBatch(VkCommandBuffer commandBuffer, List<DrawCommand> bucket, long offset) {
        if (bucket.isEmpty()) return offset;

        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, vkPipeline.get());
        lastFrameBatches++;

        float[] vertices = VertexBatcher.batch(bucket,
                window.getFramebufferWidth(), window.getFramebufferHeight());

        if (vertices.length > 0) {
            dynamicVertexBuffer.updateVertices(vertices, offset);

            long[] buffers = {dynamicVertexBuffer.get()};
            long[] vboffsets = {offset};
            vkCmdBindVertexBuffers(commandBuffer, 0, buffers, vboffsets);

            int vertexCount = vertices.length / 6;
            vkCmdDraw(commandBuffer, vertexCount, 1, 0, 0);
            lastFrameDrawCalls++;
            lastFrameVertices += vertexCount;
            return offset + vertices.length * 4L;
        }
        return offset;
    }

    // ── 刷新阴影桶 ──
    private void flushShadowBatch(VkCommandBuffer commandBuffer, List<DrawCommand> bucket) {
        if (bucket.isEmpty() || shadowPipeline == null) return;

        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, shadowPipeline.get());
        lastFrameBatches++;

        float[] shadowVertices = VertexBatcher.batchShadowsSDF(bucket,
                window.getFramebufferWidth(), window.getFramebufferHeight());

        if (shadowVertices.length > 0) {
            shadowVertexBuffer.updateVertices(shadowVertices);

            long[] buffers = {shadowVertexBuffer.get()};
            long[] offsets = {0};
            vkCmdBindVertexBuffers(commandBuffer, 0, buffers, offsets);

            int vertexCount = shadowVertices.length / 12;
            vkCmdDraw(commandBuffer, vertexCount, 1, 0, 0);
            lastFrameDrawCalls++;
            lastFrameVertices += vertexCount;
        }
    }

    // ── 渲染一组文字命令（按字体分组后一次提交） ──
    private long flushTextBatch(VkCommandBuffer commandBuffer, List<DrawCommand> texts, long textBufferOffset) {
        if (texts.isEmpty() || textPipeline == null) return textBufferOffset;

        Map<VGFont, List<DrawCommand>> fontGroups = new LinkedHashMap<>();
        for (DrawCommand cmd : texts) {
            if (cmd instanceof TextCommand t) {
                VGFont f = t.font != null ? t.font : vg.getCurrentFont();
                fontGroups.computeIfAbsent(f, k -> new ArrayList<>()).add(cmd);
            }
        }

        long currentOffset = textBufferOffset;
        boolean firstFont = true;
        for (Map.Entry<VGFont, List<DrawCommand>> entry : fontGroups.entrySet()) {
            VGFont font = entry.getKey();
            List<DrawCommand> cmds = entry.getValue();
            VKFontTexture fontTex = vg.getFontTexture(font);
            if (fontTex == null) continue;

            float[] textVertices = VertexBatcher.batchText(cmds,
                    window.getFramebufferWidth(), window.getFramebufferHeight(), font);
            if (textVertices.length == 0) continue;

            if (currentOffset + textVertices.length * 4L > textVertexBuffer.getSize()) continue;

            textVertexBuffer.updateVertices(textVertices, currentOffset);

            if (firstFont) {
                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, textPipeline.get());
                firstFont = false;
            }
            lastFrameBatches++;

            // ── 使用字体纹理自身的描述符集（预分配，非录制期间更新） ──
            long fontDescriptorSet = fontTex.getDescriptorSet();

            long[] buffers = {textVertexBuffer.get()};
            long[] offsets = {currentOffset};
            vkCmdBindVertexBuffers(commandBuffer, 0, buffers, offsets);

            long[] descriptorSets = {fontDescriptorSet};
            vkCmdBindDescriptorSets(commandBuffer,
                    VK_PIPELINE_BIND_POINT_GRAPHICS,
                    textPipeline.getLayout(), 0, descriptorSets, null);

            int vertexCount = textVertices.length / 7;
            vkCmdDraw(commandBuffer, vertexCount, 1, 0, 0);
            lastFrameDrawCalls++;
            lastFrameVertices += vertexCount;

            currentOffset += textVertices.length * 4L;
        }
        return currentOffset;
    }

    // ── 刷新图标桶 ──
    private void flushIconBatch(VkCommandBuffer commandBuffer, List<DrawCommand> icons) {
        if (icons.isEmpty() || iconPipeline == null) return;

        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, iconPipeline.get());
        lastFrameBatches++;

        VGIconFont iconFont = VG.getIconFont();
        float[] iconVertices = VertexBatcher.batchIcons(icons,
                window.getFramebufferWidth(), window.getFramebufferHeight(), iconFont);

        if (iconVertices.length > 0) {
            iconVertexBuffer.updateVertices(iconVertices);

            long[] buffers = {iconVertexBuffer.get()};
            long[] offsets = {0};
            vkCmdBindVertexBuffers(commandBuffer, 0, buffers, offsets);

            // 使用图标纹理自身的描述符集
            VKFontTexture iconTex = vg.getIconTexture();
            long iconDescriptorSet = iconTex != null ? iconTex.getDescriptorSet() : iconPipeline.getDescriptorSet();
            long[] descriptorSets = {iconDescriptorSet};
            vkCmdBindDescriptorSets(commandBuffer,
                    VK_PIPELINE_BIND_POINT_GRAPHICS,
                    iconPipeline.getLayout(), 0, descriptorSets, null);

            int vertexCount = iconVertices.length / 7;
            vkCmdDraw(commandBuffer, vertexCount, 1, 0, 0);
            lastFrameDrawCalls++;
            lastFrameVertices += vertexCount;
        }
    }

    @Override
    public void close() {
        if (vkDevice != null) {
            vkDevice.waitIdle();
        }

        destroyMsaaResources();

        resourceManager.close();
    }
}