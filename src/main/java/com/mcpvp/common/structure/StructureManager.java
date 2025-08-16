package com.mcpvp.common.structure;

import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages a list of placed structures to prevent collisions. Also enforces restrictions.
 */
public class StructureManager {

    private final List<StructureViolationChecker> checkers = new ArrayList<>();
    private final List<Structure> structures = new ArrayList<>();

    {
        checkers.add(block -> {
            if (structures.stream().anyMatch(s -> s.getBlocks().contains(block))) {
                return Optional.of(new StructureViolation("STRUCTURE_ALREADY_PRESENT", "Something is already placed here"));
            }
            return Optional.empty();
        });
    }

    /**
     * Checks the given block for any violations.
     *
     * @param block The block to check.
     * @return All found violations, or an empty list if nothing was found.
     */
    public List<StructureViolation> check(Block block) {
        List<StructureViolation> violations = new ArrayList<>();
        checkers.forEach(checker -> {
            checker.check(block).ifPresent(violations::add);
        });
        return violations;
    }

    public void onBuild(Structure structure) {
        structures.add(structure);
    }

    public void onRemove(Structure structure) {
        structures.remove(structure);
    }

    public void registerChecker(StructureViolationChecker checker) {
        checkers.add(checker);
    }

    public List<Structure> getStructures() {
        return new ArrayList<>(structures);
    }
}
