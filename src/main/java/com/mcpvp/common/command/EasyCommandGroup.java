package com.mcpvp.common.command;

import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class EasyCommandGroup extends EasyCommand {

    private final List<EasyCommand> commands = new ArrayList<>();
    private EasyCommand defaultCommand;

    public EasyCommandGroup(String name) {
        super(name);
    }

    protected void addCommand(EasyCommand command, boolean setDefault) {
        if (setDefault) {
            this.defaultCommand = command;
        }
        this.commands.add(command);
    }

    protected void addCommand(EasyCommand command) {
        this.addCommand(command, false);
    }

    @Override
    public boolean onCommand(CommandSender sender, String alias, List<String> args) {
        // Use the default command, if present
        if (args.isEmpty()) {
            if (this.defaultCommand != null) {
                return this.defaultCommand.onCommand(sender, alias, args);
            } else {
                return false;
            }
        }

        // Match command based on args[0]
        Optional<EasyCommand> found = this.findCommand(args.get(0));

        return found.map(easyCommand ->
            easyCommand.onCommand(sender, alias, args.subList(1, args.size()))
        ).orElse(false);
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String alias, List<String> args) {
        if (args.size() == 1) {
            return this.commands.stream().map(EasyCommand::getName).toList();
        }

        // Match command based on args[0]
        Optional<EasyCommand> found = this.findCommand(args.get(0));

        if (found.isPresent()) {
            return found.get().getTabCompletions(sender, alias, args.subList(1, args.size()));
        }

        return super.getTabCompletions(sender, alias, args);
    }

    protected Optional<EasyCommand> findCommand(String arg) {
        return this.commands.stream()
            .sorted((c1, c2) -> c2.getName().length() - c1.getName().length())
            .filter(c -> c.getName().equalsIgnoreCase(arg))
            .findFirst();
    }

}
