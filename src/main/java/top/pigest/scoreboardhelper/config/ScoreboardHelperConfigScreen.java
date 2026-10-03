package top.pigest.scoreboardhelper.config;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import top.pigest.scoreboardhelper.gui.widget.PropertySliderWidget;
import top.pigest.scoreboardhelper.util.TranslationKeyType;

public class ScoreboardHelperConfigScreen extends Screen {
    private final @Nullable Screen parent;
    private final ScoreboardHelperConfig config;
    private static final int TITLE_Y = 30;

    public ScoreboardHelperConfigScreen(@Nullable Screen parent, ScoreboardHelperConfig config) {
        super(Component.translatable(getTranslationKey("title", TranslationKeyType.NORMAL)));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        int leftX = width / 2 - 10 - 200;
        int rightX = width / 2 + 10;
        addRenderableWidget(createBooleanPropertyButton(leftX, TITLE_Y + 20, 200, 20, config.scoreboardShown));
        addRenderableWidget(createBooleanPropertyButton(leftX, TITLE_Y + 20 + 30, 200, 20, config.sidebarScoreShown));
        addRenderableWidget(createEnumPropertyButton(leftX, TITLE_Y + 20 + 30 * 2, 200, 20, config.sidebarPosition, ScoreboardHelperConfig.ScoreboardSidebarPosition.values()));
        addRenderableWidget(createIntegerPropertySlider(leftX, TITLE_Y + 20 + 30 * 3, 200, 20, config.maxShowCount, 0, 100));
        addRenderableWidget(createIntegerPropertySlider(leftX, TITLE_Y + 20 + 30 * 4, 200, 20, config.sidebarYOffset, -100, 100));
        addRenderableWidget(createDoublePropertySlider(rightX, TITLE_Y + 20, 200, 20, config.sidebarBackgroundOpacity, 0.0, 1.0));
        addRenderableWidget(createDoublePropertySlider(rightX, TITLE_Y + 20 + 30, 200, 20, config.sidebarBackgroundTitleOpacity, 0.0, 1.0));
        addRenderableWidget(createDoublePropertySlider(rightX, TITLE_Y + 20 + 30 * 2, 200, 20, config.sidebarTextOpacity, 0.1, 1.0));
        addRenderableWidget(createDoublePropertySlider(rightX, TITLE_Y + 20 + 30 * 3, 200, 20, config.sidebarTitleTextOpacity, 0.1, 1.0));
        addRenderableWidget(createBooleanPropertyButton(rightX, TITLE_Y + 20 + 30 * 4, 200, 20, config.defaultTeamChat));

        addRenderableWidget(Button.builder(Component.translatable(getTranslationKey("reset", TranslationKeyType.NORMAL)), button -> {
            config.resetDefault();
            this.rebuildWidgets();
        }).bounds(rightX, height - 40, 200, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).bounds(leftX, height - 40, 200, 20).build());
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void removed() {
        config.save();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, TITLE_Y, 0xFFFFFFFF);
    }

    private static String getTranslationKey(String key, TranslationKeyType keyType) {
        return switch (keyType) {
            case NORMAL -> "options.scoreboard-helper." + key;
            case TOOLTIP -> "options.scoreboard-helper." + key + ".tooltip";
        };
    }

    private Button createBooleanPropertyButton(int x, int y, int width, int height, Property<Boolean> property) {
        Component text = Component.translatable(getTranslationKey(property.getKey(), TranslationKeyType.NORMAL));
        Component toolTip = Component.translatable(getTranslationKey(property.getKey(), TranslationKeyType.TOOLTIP));
        return Button.builder(CommonComponents.optionStatus(text, property.getValue()), button -> {
            boolean newValue = !property.getValue();
            button.setMessage(CommonComponents.optionStatus(text, newValue));
            property.setValue(newValue);
        }).tooltip(Tooltip.create(toolTip)).bounds(x, y, width, height).build();
    }

    @SafeVarargs
    private <T extends Enum<?>> CycleButton<T> createEnumPropertyButton(int x, int y, int width, int height, Property<T> property, T... values) {
        Component text = Component.translatable(getTranslationKey(property.getKey(), TranslationKeyType.NORMAL));
        Component tooltip = Component.translatable(getTranslationKey(property.getKey(), TranslationKeyType.TOOLTIP));
        CycleButton<T> button = CycleButton.<T>builder(value -> Component.translatable(getTranslationKey(property.getKey(), TranslationKeyType.NORMAL) + ".value." + value), property.getValue())
                .withValues(values)
                .create(x, y, width, height, text, (b, value) -> property.setValue(value));
        button.setTooltip(Tooltip.create(tooltip));
        return button;
    }

    private PropertySliderWidget<Integer> createIntegerPropertySlider(int x, int y, int width, int height, Property<Integer> property, int min, int max) {
        Component text = Component.translatable(getTranslationKey(property.getKey(), TranslationKeyType.NORMAL));
        Component tooltip = Component.translatable(getTranslationKey(property.getKey(), TranslationKeyType.TOOLTIP));
        double value = 1.0 * (property.getValue() - min) / (max - min);
        PropertySliderWidget<Integer> slider = new PropertySliderWidget<>(x, y, width, height, text, value, property, PropertySliderWidget.ValueTextGetter.getDefaultTextGetter(), PropertySliderWidget.PropertyValueApplier.getDefaultIntegerPropertyValueApplier(min, max));
        slider.setTooltip(Tooltip.create(tooltip));
        return slider;
    }

    private PropertySliderWidget<Double> createDoublePropertySlider(int x, int y, int width, int height, Property<Double> property, double min, double max) {
        Component text = Component.translatable(getTranslationKey(property.getKey(), TranslationKeyType.NORMAL));
        Component tooltip = Component.translatable(getTranslationKey(property.getKey(), TranslationKeyType.TOOLTIP));
        double value = (property.getValue() - min) / (max - min);
        PropertySliderWidget<Double> slider = new PropertySliderWidget<>(x, y, width, height, text, value, property, PropertySliderWidget.ValueTextGetter.getDefaultPercentTextGetter(), PropertySliderWidget.PropertyValueApplier.getDefaultDoublePropertyValueApplier(min, max));
        slider.setTooltip(Tooltip.create(tooltip));
        return slider;
    }
}
