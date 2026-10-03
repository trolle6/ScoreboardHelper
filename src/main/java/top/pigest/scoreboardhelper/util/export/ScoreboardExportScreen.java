package top.pigest.scoreboardhelper.util.export;

import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import top.pigest.scoreboardhelper.ScoreboardHelper;
import top.pigest.scoreboardhelper.gui.widget.ScoreboardListWidget;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ScoreboardExportScreen extends Screen {
    public static final ScoreboardExportScreen INSTANCE = new ScoreboardExportScreen(null);
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int TITLE_Y = 8;
    private @Nullable Screen parent;
    private final List<RecordEntry> entries = new ArrayList<>();
    private @Nullable ScoreboardListWidget scoreboardListWidget;

    public ScoreboardExportScreen(@Nullable Screen parent) {
        super(Component.translatable(getTranslationKey("title")));
        this.parent = parent;
    }

    public void setParent(@Nullable Screen parent) {
        this.parent = parent;
    }

    @Override
    protected void init() {
        double scroll = this.scoreboardListWidget != null ? this.scoreboardListWidget.scrollAmount() : 0;
        this.scoreboardListWidget = addRenderableWidget(new ScoreboardListWidget(minecraft, this));
        this.scoreboardListWidget.setScrollAmount(scroll);

        addRenderableWidget(Button.builder(Component.translatable(getTranslationKey("record")), button -> {
            if (!record()) {
                onClose();
            }
        }).bounds(width / 2 - 10 - 200, height - 40 - 30, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable(getTranslationKey("direct")), button -> {
            tryExport();
            onClose();
        }).bounds(width / 2 - 10 - 200, height - 40, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable(getTranslationKey("finish")), button -> {
            exportAll();
            onClose();
        }).bounds(width / 2 + 10, height - 40 - 30, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable(getTranslationKey("close")), button -> onClose())
                .bounds(width / 2 + 10, height - 40, 200, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, TITLE_Y, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    private static String getTranslationKey(String key) {
        return "options.scoreboard-helper.export." + key;
    }

    public void refresh() {
        this.rebuildWidgets();
    }

    private void sendMessage(Component message) {
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(message);
        }
    }

    private void sendError(String key) {
        sendMessage(Component.translatable(key).withStyle(ChatFormatting.RED));
    }

    private boolean record() {
        if (minecraft.player == null) {
            return false;
        }
        Objective objective = ScoreboardHelper.getSidebarObjective(minecraft);
        if (objective == null) {
            sendError("hint.scoreboard-helper.export.fail.inactive");
            return false;
        }
        RecordEntry entry = new RecordEntry(objective.getDisplayName());
        for (PlayerScoreEntry score : objective.getScoreboard().listPlayerScores(objective)) {
            entry.scores.add(new ScoreRecord(score.owner(), score.value()));
        }
        entries.removeIf(existing -> existing.displayName.equals(objective.getDisplayName()));
        entries.add(entry);
        this.refresh();
        return true;
    }

    private void tryExport() {
        if (minecraft.player == null) {
            return;
        }
        Objective objective = ScoreboardHelper.getSidebarObjective(minecraft);
        if (objective == null) {
            sendError("hint.scoreboard-helper.export.fail.inactive");
        } else {
            export(objective);
        }
    }

    private void export(Objective objective) {
        List<PlayerScoreEntry> scores = new ArrayList<>(objective.getScoreboard().listPlayerScores(objective));
        scores.sort(Comparator.comparingInt(PlayerScoreEntry::value).reversed().thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER));
        List<String> lines = new ArrayList<>();
        lines.add(Component.translatable(getTranslationKey("chart.player")).getString() + "," + Component.translatable(getTranslationKey("chart.score")).getString());
        for (PlayerScoreEntry score : scores) {
            lines.add(score.owner() + "," + score.value());
        }
        writeExport(objective.getName() + ".csv", lines);
    }

    private void exportAll() {
        if (minecraft.player == null) {
            return;
        }
        if (this.entries.isEmpty()) {
            sendError("hint.scoreboard-player.export.fail.no_entry");
            return;
        }
        Set<String> playerNames = new TreeSet<>();
        for (RecordEntry entry : entries) {
            for (ScoreRecord score : entry.scores) {
                playerNames.add(score.owner());
            }
        }
        List<String> lines = new ArrayList<>();
        StringBuilder head = new StringBuilder(Component.translatable(getTranslationKey("chart.player")).getString());
        for (RecordEntry entry : entries) {
            head.append(",").append(entry.displayName.getString());
        }
        lines.add(head.toString());
        for (String playerName : playerNames) {
            StringBuilder line = new StringBuilder(playerName);
            for (RecordEntry entry : entries) {
                line.append(",");
                entry.scores.stream()
                        .filter(score -> score.owner().equals(playerName))
                        .findFirst()
                        .ifPresent(score -> line.append(score.value()));
            }
            lines.add(line.toString());
        }
        writeExport("EXPORT-" + UUID.randomUUID() + ".csv", lines);
    }

    private void writeExport(String name, List<String> lines) {
        Path dir = FabricLoader.getInstance().getGameDir().resolve("scoreboard-exports");
        Path file = dir.resolve(name);
        try {
            Files.createDirectories(dir);
            Files.write(file, lines, StandardCharsets.UTF_8);
            Component link = Component.literal(name).setStyle(Style.EMPTY.withUnderlined(true).withClickEvent(new ClickEvent.OpenFile(file.toAbsolutePath())));
            sendMessage(Component.translatable("hint.scoreboard-helper.export.success", link));
        } catch (IOException e) {
            LOGGER.error("Couldn't export scoreboard to '{}'", file, e);
            sendError("hint.scoreboard-helper.export.fail.exception");
        }
    }

    public List<RecordEntry> getRecordEntries() {
        return entries;
    }

    public record ScoreRecord(String owner, int value) {
    }

    public static class RecordEntry {
        private final Component displayName;
        public final List<ScoreRecord> scores = new ArrayList<>();

        public RecordEntry(Component displayName) {
            this.displayName = displayName;
        }

        public Component getDisplayName() {
            return displayName;
        }
    }
}
