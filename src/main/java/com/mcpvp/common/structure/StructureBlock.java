package com.mcpvp.common.structure;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * A single block within a structure, which will retain the original state of the block so that it can be restored.
 */
@Log4j2
@Getter
@RequiredArgsConstructor
public class StructureBlock {
    
    private final Block block;
    private final BlockState original;

    public void restore() {
        log.debug("Restoring " + block.getLocation() + " to " + original.getType());
        block.setType(original.getType());
        block.setData(original.getData().getData());
    }

}
