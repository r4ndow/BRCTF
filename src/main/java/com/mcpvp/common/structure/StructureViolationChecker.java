package com.mcpvp.common.structure;

import java.util.Optional;

import org.bukkit.block.Block;

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
