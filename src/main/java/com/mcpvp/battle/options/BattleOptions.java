package com.mcpvp.battle.options;

import com.mcpvp.battle.BattlePlugin;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Delegate;

import java.io.IOException;
import java.util.function.Consumer;

@Getter
@AllArgsConstructor
public class BattleOptions {

    /**
     * Most options can be read straight from the input file, so we use delegate to make those available.
     */
    @Delegate
    private final BattleOptionsInput input;
    private final BattlePlugin plugin;
    private final BattleOptionsLoader loader;

    public BattleOptions(BattlePlugin plugin, BattleOptionsLoader loader) throws IOException {
        this.plugin = plugin;
        this.loader = loader;
        this.input = loader.read();
    }

    /**
     * Edit the live options object while also persisting the changes to the file.
     *
     * @param consumer Edits the input.
     * @throws IOException If the new value cannot be saved.
     */
    public void edit(Consumer<BattleOptionsInput> consumer) throws IOException {
        consumer.accept(this.input);
        this.loader.save(this.input);
    }

}
