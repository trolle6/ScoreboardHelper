package top.pigest.scoreboardhelper.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import top.pigest.scoreboardhelper.util.export.ScoreboardExportScreen;

import java.util.Collections;
import java.util.List;

public class ScoreboardListWidget extends ContainerObjectSelectionList<ScoreboardListWidget.Entry> {
    private final ScoreboardExportScreen parent;

    public ScoreboardListWidget(Minecraft minecraft, ScoreboardExportScreen parent) {
        super(minecraft, parent.width, parent.height - 32 - 80, 32, 25);
        this.parent = parent;
        for (ScoreboardExportScreen.RecordEntry entry : parent.getRecordEntries()) {
            this.addEntry(new Entry(entry));
        }
    }

    @Override
    public int getRowWidth() {
        return 340;
    }

    public class Entry extends ContainerObjectSelectionList.Entry<Entry> {
        private final ScoreboardExportScreen.RecordEntry entry;
        private final Button deleteButton;
        private final Button forwardButton;
        private final Button backwardButton;

        Entry(ScoreboardExportScreen.RecordEntry entry) {
            this.entry = entry;
            List<ScoreboardExportScreen.RecordEntry> recordEntries = parent.getRecordEntries();
            this.deleteButton = Button.builder(Component.translatable("options.scoreboard-helper.export.delete"), button -> {
                recordEntries.remove(entry);
                parent.refresh();
            }).size(60, 20).build();
            this.forwardButton = Button.builder(Component.literal("↑"), button -> {
                int index = recordEntries.indexOf(entry);
                if (index > 0) {
                    Collections.swap(recordEntries, index, index - 1);
                    parent.refresh();
                }
            }).size(20, 20).build();
            this.backwardButton = Button.builder(Component.literal("↓"), button -> {
                int index = recordEntries.indexOf(entry);
                if (index >= 0 && index < recordEntries.size() - 1) {
                    Collections.swap(recordEntries, index, index + 1);
                    parent.refresh();
                }
            }).size(20, 20).build();
            int index = recordEntries.indexOf(entry);
            this.forwardButton.active = index > 0;
            this.backwardButton.active = index < recordEntries.size() - 1;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.deleteButton, this.forwardButton, this.backwardButton);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.deleteButton, this.forwardButton, this.backwardButton);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            int x = getContentX();
            int y = getContentY();
            graphics.text(minecraft.font, entry.getDisplayName(), x, getContentYMiddle() - minecraft.font.lineHeight / 2, 0xFFFFFFFF, false);
            int buttonX = getContentRight() - 60 - 5 - 20 - 5 - 20;
            this.deleteButton.setPosition(buttonX, y);
            this.deleteButton.extractRenderState(graphics, mouseX, mouseY, delta);
            this.forwardButton.setPosition(buttonX + 60 + 5, y);
            this.forwardButton.extractRenderState(graphics, mouseX, mouseY, delta);
            this.backwardButton.setPosition(buttonX + 60 + 5 + 20 + 5, y);
            this.backwardButton.extractRenderState(graphics, mouseX, mouseY, delta);
        }
    }
}
