package com.mcpvp.common.structure;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A utility class that makes it easy to add blocks to a structure. Building a structure is a two-step process:
 * <p>
 * 1. Queue up all actions and their respective location. Each location will be checked individually for violations.
 * 2. Execute all queued actions once all locations have been deemed valid.
 */
@RequiredArgsConstructor
public class StructureBuilder {

    private final StructureManager manager;
    /**
     * A queue of actions to be taken if the structure can be built.
     */
    private final Map<Block, Consumer<Block>> queued = new HashMap<>();
    /**
     * A list of StructureBlocks, populated once the structure is built.
     */
    @Getter
    private final List<StructureBlock> built = new ArrayList<>();
    /**
     * Any violations encountered while attempting to queue actions.
     */
    @Getter
    private final List<StructureViolation> violations = new ArrayList<>();

    /**
     * Attempts to edit the block. If this block has already been edited by this structure, an exception will
     * be thrown. If a {@link StructureViolation} occurs, it will be placed in {@link #getViolations()}.
     *
     * @param block  The block to edit.
     * @param placer A function which can mutate the block.
     * @throws IllegalArgumentException If this structure already has a block at the location.
     */
    public void setBlock(Block block, Consumer<Block> placer) {
        if (queued.containsKey(block)) {
            throw new IllegalArgumentException("Location already has a block in structure");
        }

        // Register any violations
        List<StructureViolation> violations = manager.check(block);
        this.violations.addAll(violations);

        // Queue the action anyway
        // The violations might be ignored
        queued.put(block, placer);
    }

    /**
     * Simplified version of {@link #setBlock(Block, Consumer)} which changes the material.
     *
     * @param block    The block to edit.
     * @param material The type to make the block.
     */
    public void setBlock(Block block, Material material) {
        setBlock(block, b -> b.setType(material));
    }

    /**
     * Builds the structure by executing all the queued changes. Ensure {@link #getViolations()} is empty before calling this.
     */
    public void complete() {
        if (!this.violations.isEmpty()) {
            throw new IllegalStateException("Attempted to place structure with violations");
        }

        queued.forEach((block, placer) -> {
            StructureBlock placed = new StructureBlock(block, block.getState());
            built.add(placed);
            placer.accept(block);
        });
    }

}
