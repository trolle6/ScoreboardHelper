package top.pigest.scoreboardhelper.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.pigest.scoreboardhelper.config.ScoreboardHelperConfig;
import top.pigest.scoreboardhelper.util.Constants;
import top.pigest.scoreboardhelper.util.SidebarEntry;

import java.util.Comparator;
import java.util.List;

@Mixin(Hud.class)
public abstract class HudMixin {
    @Shadow @Final private static Comparator<PlayerScoreEntry> SCORE_DISPLAY_ORDER;

    @Shadow @Final private Minecraft minecraft;

    @Shadow public abstract Font getFont();

    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void scoreboardHelper$displaySidebar(GuiGraphicsExtractor graphics, Objective objective, CallbackInfo ci) {
        ci.cancel();
        ScoreboardHelperConfig config = ScoreboardHelperConfig.INSTANCE;
        if (!config.scoreboardShown.getValue()) {
            return;
        }

        Font font = this.getFont();
        boolean showScore = config.sidebarScoreShown.getValue();
        Scoreboard scoreboard = objective.getScoreboard();
        NumberFormat objectiveScoreFormat = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);

        List<SidebarEntry> entries = scoreboard.listPlayerScores(objective)
                .stream()
                .filter(score -> !score.isHidden())
                .sorted(SCORE_DISPLAY_ORDER)
                .skip(Constants.PAGE)
                .limit(config.maxShowCount.getValue())
                .map(score -> {
                    PlayerTeam team = scoreboard.getPlayersTeam(score.owner());
                    Component name = PlayerTeam.formatNameForTeam(team, score.ownerName());
                    Component scoreText = showScore ? score.formatValue(objectiveScoreFormat) : Component.empty();
                    return new SidebarEntry(name, scoreText, showScore ? font.width(scoreText) : 0);
                })
                .toList();

        Component title = objective.getDisplayName();
        int titleWidth = font.width(title);
        int spacerWidth = font.width(": ");
        int width = titleWidth;
        for (SidebarEntry entry : entries) {
            width = Math.max(width, font.width(entry.name()) + (entry.scoreWidth() > 0 ? spacerWidth + entry.scoreWidth() : 0));
        }

        int count = entries.size();
        int lineHeight = font.lineHeight;
        int bottom = switch (config.sidebarPosition.getValue()) {
            case LEFT_LOWER_CORNER, RIGHT_LOWER_CORNER -> graphics.guiHeight() - 3;
            case LEFT_UPPER_CORNER, RIGHT_UPPER_CORNER -> 3 + lineHeight * (count + 1);
            case LEFT, RIGHT -> graphics.guiHeight() / 2 + count * lineHeight / 3;
        };
        bottom += config.sidebarYOffset.getValue();

        int left;
        int right;
        switch (config.sidebarPosition.getValue()) {
            case LEFT, LEFT_LOWER_CORNER, LEFT_UPPER_CORNER -> {
                left = 3;
                right = 3 + width + 2;
            }
            default -> {
                left = graphics.guiWidth() - width - 3;
                right = graphics.guiWidth() - 3 + 2;
            }
        }

        int backgroundColor = this.minecraft.options.getBackgroundColor(config.sidebarBackgroundOpacity.getValue().floatValue());
        int headerBackgroundColor = this.minecraft.options.getBackgroundColor(config.sidebarBackgroundTitleOpacity.getValue().floatValue());
        int titleColor = scoreboardHelper$whiteWithOpacity(config.sidebarTitleTextOpacity.getValue());
        int textColor = scoreboardHelper$whiteWithOpacity(config.sidebarTextOpacity.getValue());

        int headerY = bottom - count * lineHeight;
        graphics.fill(left - 2, headerY - lineHeight - 1, right, headerY - 1, headerBackgroundColor);
        graphics.fill(left - 2, headerY - 1, right, bottom, backgroundColor);
        graphics.text(font, title, left + width / 2 - titleWidth / 2, headerY - lineHeight, titleColor, false);

        for (int i = 0; i < count; i++) {
            SidebarEntry entry = entries.get(i);
            int y = bottom - (count - i) * lineHeight;
            graphics.text(font, entry.name(), left, y, textColor, false);
            if (entry.scoreWidth() > 0) {
                graphics.text(font, entry.score(), right - entry.scoreWidth(), y, textColor, false);
            }
        }
    }

    @Unique
    private static int scoreboardHelper$whiteWithOpacity(double opacity) {
        int alpha = (int) Math.round(Math.clamp(opacity, 0.0, 1.0) * 255);
        return (alpha << 24) | 0xFFFFFF;
    }
}
