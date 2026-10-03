package top.pigest.scoreboardhelper.util;

import net.minecraft.network.chat.Component;

public record SidebarEntry(Component name, Component score, int scoreWidth) {
}
