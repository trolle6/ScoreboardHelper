package top.pigest.scoreboardhelper.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import top.pigest.scoreboardhelper.config.ScoreboardHelperConfig;

public class SBHelperCommand {
    private static final SimpleCommandExceptionType INVALID_COUNT_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("commands.sbhelper.invalidCount"));

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommands.literal("sbhelper")
                .then(ClientCommands.literal("maxDisplayCount")
                        .then(ClientCommands.argument("count", IntegerArgumentType.integer())
                                .executes(context -> executeMaxDisplayCount(context.getSource(), IntegerArgumentType.getInteger(context, "count")))))
        );
    }

    private static int executeMaxDisplayCount(FabricClientCommandSource source, int count) throws CommandSyntaxException {
        if (count < 0) {
            throw INVALID_COUNT_EXCEPTION.create();
        }
        ScoreboardHelperConfig.INSTANCE.maxShowCount.setValue(count);
        ScoreboardHelperConfig.INSTANCE.save();
        source.sendFeedback(Component.translatable("commands.sbhelper.success.setMaxCount", count));
        return count;
    }
}
