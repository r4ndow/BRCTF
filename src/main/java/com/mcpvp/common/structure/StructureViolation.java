package com.mcpvp.common.structure;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class StructureViolation {

    /**
     * A unique key to identify this type of structure violation, e.g. `STRUCTURE_ALREADY_PRESENT`
     */
    private final String key;

    /**
     * A message that can be shown to the user.
     */
    private final String message;

}
