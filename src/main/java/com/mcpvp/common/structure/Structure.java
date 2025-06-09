package com.mcpvp.common.structure;

import com.mcpvp.common.EasyLifecycle;
import com.mcpvp.common.event.EasyListener;
import com.mcpvp.common.time.Duration;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A structure is a collection of blocks which can be placed and removed all at once.
 * Every instance should be only be placed once.
 */
@RequiredArgsConstructor
public abstract class Structure extends EasyLifecycle implements EasyListener {

    private final StructureManager manager;
    private final List<StructureBlock> blocks = new ArrayList<>();

    /**
     * Attempt to place the structure at the given center block.
     *
     * @param center The center block of the structure.
     * @return A list of structure violations. If this list is empty, the structure has been placed. Otherwise,
     * one of the violations caused the placing to be stopped, in which case nothing has been changed.
     */
    public List<StructureViolation> place(Block center) {
        StructureBuilder builder = new StructureBuilder(manager);
        build(center, builder);

        if (!builder.getViolations().isEmpty()) {
            return builder.getViolations();
        }

        // Actually builds the structure
        builder.complete();
        this.blocks.addAll(builder.getBuilt());
        manager.onBuild(this);
        attach((EasyListener) this);
        return Collections.emptyList();
    }

    /**
     * The abstract method for actually building this structure. The given builder should be used to adjust
     * blocks. Nothing will be changed if any violations are generated in {@link StructureBuilder#getViolations()}.
     *
     * @param center  The center of the structure.
     * @param builder A builder instance that can be used to adjust blocks.
     */
    protected abstract void build(Block center, StructureBuilder builder);

    /**
     * @return A list of all blocks impacted by this Structure.
     */
    public List<Block> getBlocks() {
        return blocks.stream().map(sb -> sb.getBlock()).toList();
    }

    /**
     * Remove the structure, restoring any changed blocks back to their original form.
     */
    public void remove() {
        this.shutdown();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        blocks.forEach(StructureBlock::restore);
        manager.onRemove(this);
    }

    /**
     * Queue this structure to be removed after the given amount of time. By attaching the task to this
     * structure, any premature removal will make sure `remove` is not called twice.
     *
     * @param duration The time to wait before removing this structure.
     */
    protected void removeAfter(Duration duration) {
        attach(Bukkit.getScheduler().runTaskLater(getPlugin(), this::remove, duration.ticks()));
    }

}
