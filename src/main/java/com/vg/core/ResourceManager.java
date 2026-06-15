package com.vg.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用资源管理器，用于跟踪和管理需要清理的资源。
 * 实现 AutoCloseable 接口，支持 try-with-resources 语法。
 */
public class ResourceManager implements AutoCloseable {
    
    private final List<AutoCloseable> resources = new ArrayList<>();
    private boolean closed = false;
    
    /**
     * 注册一个需要管理的资源
     * @param resource 实现了 AutoCloseable 接口的资源
     * @return 传入的资源，方便链式调用
     */
    public <T extends AutoCloseable> T register(T resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Cannot register null resource");
        }
        if (closed) {
            throw new IllegalStateException("ResourceManager is already closed");
        }
        resources.add(resource);
        return resource;
    }
    
    /**
     * 注销资源（不再由管理器负责清理）
     * @param resource 要注销的资源
     */
    public void unregister(AutoCloseable resource) {
        resources.remove(resource);
    }
    
    /**
     * 检查资源是否已被管理
     * @param resource 要检查的资源
     * @return true 如果资源已被管理
     */
    public boolean isRegistered(AutoCloseable resource) {
        return resources.contains(resource);
    }
    
    /**
     * 获取已注册资源的数量
     * @return 资源数量
     */
    public int getResourceCount() {
        return resources.size();
    }
    
    /**
     * 清理所有注册的资源
     * 按照注册的逆序清理（后注册的先清理）
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        
        // 逆序清理资源（LIFO - Last In First Out）
        Exception firstException = null;
        for (int i = resources.size() - 1; i >= 0; i--) {
            AutoCloseable resource = resources.get(i);
            try {
                if (resource != null) {
                    resource.close();
                }
            } catch (Exception e) {
                if (firstException == null) {
                    firstException = e;
                } else {
                    firstException.addSuppressed(e);
                }
                System.err.println("[ResourceManager] Failed to close resource: " + 
                    resource.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }
        
        resources.clear();
        closed = true;
        
        if (firstException != null) {
            throw new RuntimeException("Failed to close some resources", firstException);
        }
    }
    
    /**
     * 检查管理器是否已关闭
     * @return true 如果已关闭
     */
    public boolean isClosed() {
        return closed;
    }
}
