package com.mcpvp.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class JsonFile<T> {

    private final File file;
    private final ObjectMapper objectMapper;
    private final TypeReference<T> type;
    private final Supplier<T> defaults;

    public void save(T data) throws IOException {
        if (!this.file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            this.file.createNewFile();
        }

        this.objectMapper.writeValue(this.file, data);
    }

    public T read() throws IOException {
        if (!this.file.exists()) {
            T data = this.defaults.get();
            this.save(data);
            return data;
        }

        return this.objectMapper.readValue(this.file, this.type);
    }

}
