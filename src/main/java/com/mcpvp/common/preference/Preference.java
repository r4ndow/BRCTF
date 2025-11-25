package com.mcpvp.common.preference;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor(staticName = "of")
public class Preference<T> {

    private final String key;
    private final TypeReference<T> type;

    public static <T> Preference<T> of(String key, Class<T> type) {
        return of(key, new TypeReference<>() {});
    }

}