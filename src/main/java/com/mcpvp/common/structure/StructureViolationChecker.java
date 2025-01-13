package com.mcpvp.common.structure;

import org.bukkit.block.Block;

import java.util.Optional;

/**
 * Represents a class that can check if a given block violates any known rules.
 */
public interface StructureViolationChecker {

    /**
     * Check a block for a violation.
     *
     * @param block The block to check.
     * @return An Optional violation.
     */
    public Optional<StructureViolation> check(Block block);

}
