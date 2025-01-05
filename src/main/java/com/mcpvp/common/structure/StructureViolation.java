package com.mcpvp.common.structure;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class StructureViolation {

    /**
     * @return A unique key to identify this type of structure violation, e.g. `STRUCTURE_ALREADY_PRESENT`
     */
    private final String key;

    /**
     * @return A message that can be shown to the user.
     */
    private final String message;

}
