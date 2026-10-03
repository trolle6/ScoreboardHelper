package top.pigest.scoreboardhelper;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.TeamColor;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import top.pigest.scoreboardhelper.command.SBHelperCommand;
import top.pigest.scoreboardhelper.config.ScoreboardHelperConfig;
import top.pigest.scoreboardhelper.config.ScoreboardHelperConfigScreen;
import top.pigest.scoreboardhelper.util.Constants;
import top.pigest.scoreboardhelper.util.export.ScoreboardExportScreen;

import java.util.Optional;

public class ScoreboardHelper implements ClientModInitializer {
    public static final String MOD_ID = "scoreboard-helper";

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    private static final KeyMapping keyBindingPageUp = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.scoreboard-helper.pageUp",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UP,
            CATEGORY
    ));

    private static final KeyMapping keyBindingPageDown = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.scoreboard-helper.pageDown",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_DOWN,
            CATEGORY
    ));

    private static final KeyMapping keyBindingSwitchDisplay = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.scoreboard-helper.switchDisplay",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    ));
    private static final KeyMapping keyBindingOpenConfig = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.scoreboard-helper.openConfig",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    ));
    private static final KeyMapping keyBindingExportScoreboard = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.scoreboard-helper.exportScoreboard",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_BACKSLASH,
            CATEGORY
    ));

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> SBHelperCommand.register(dispatcher));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Constants.CD_SWITCH_DISPLAY = Constants.CD_SWITCH_DISPLAY > 0 ? Constants.CD_SWITCH_DISPLAY - 1 : 0;
            Constants.CD_EXPORT = Constants.CD_EXPORT > 0 ? Constants.CD_EXPORT - 1 : 0;
            if (keyBindingPageDown.isDown()) {
                Objective objective = getSidebarObjective(client);
                if (objective != null) {
                    long count = objective.getScoreboard().listPlayerScores(objective).stream().filter(score -> !score.isHidden()).count();
                    if (count > Constants.PAGE + ScoreboardHelperConfig.INSTANCE.maxShowCount.getValue()) {
                        Constants.PAGE++;
                    }
                }
            }
            if (keyBindingPageUp.isDown()) {
                Constants.PAGE = Constants.PAGE > 0 ? Constants.PAGE - 1 : 0;
            }
            if (keyBindingSwitchDisplay.isDown() && Constants.CD_SWITCH_DISPLAY == 0) {
                ScoreboardHelperConfig.INSTANCE.scoreboardShown.setValue(!ScoreboardHelperConfig.INSTANCE.scoreboardShown.getValue());
                Constants.CD_SWITCH_DISPLAY = 5;
            }
            if (keyBindingOpenConfig.isDown()) {
                client.gui.setScreen(new ScoreboardHelperConfigScreen(client.gui.screen(), ScoreboardHelperConfig.INSTANCE));
            }
            if (keyBindingExportScoreboard.isDown() && Constants.CD_EXPORT == 0) {
                ScoreboardExportScreen.INSTANCE.setParent(client.gui.screen());
                client.gui.setScreen(ScoreboardExportScreen.INSTANCE);
                Constants.CD_EXPORT = 5;
            }
        });
    }

    public static @Nullable Objective getSidebarObjective(Minecraft client) {
        if (client.level == null || client.player == null) {
            return null;
        }
        Scoreboard scoreboard = client.level.getScoreboard();
        PlayerTeam team = scoreboard.getPlayersTeam(client.player.getScoreboardName());
        if (team != null) {
            Optional<TeamColor> color = team.getColor();
            if (color.isPresent()) {
                Objective teamObjective = scoreboard.getDisplayObjective(color.get().displaySlot());
                if (teamObjective != null) {
                    return teamObjective;
                }
            }
        }
        return scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
    }
}
