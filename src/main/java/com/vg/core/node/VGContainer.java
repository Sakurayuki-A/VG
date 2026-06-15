package com.vg.core.node;

import com.vg.core.input.MouseContext;

import java.util.ArrayList;
import java.util.List;

public class VGContainer extends VGNode {

    protected final List<VGNode> children = new ArrayList<>();

    // ── Children Management ──

    public VGContainer add(VGNode node) {
        if (node.parent != null) node.parent.remove(node);
        children.add(node);
        node.parent = this;
        return this;
    }

    public VGContainer remove(VGNode node) {
        if (children.remove(node)) {
            node.parent = null;
        }
        return this;
    }

    public void clear() {
        for (var child : children) {
            child.parent = null;
        }
        children.clear();
    }

    public List<VGNode> children() {
        return children;
    }

    // ── Lifecycle ──

    @Override
    public void update(float dt, MouseContext mouse) {
        for (VGNode child : children) {
            if (!child.visible) continue;
            child.update(dt, mouse);
        }
    }

    @Override
    public void render() {
        for (VGNode child : children) {
            if (!child.visible) continue;
            child.render();
        }
    }
}