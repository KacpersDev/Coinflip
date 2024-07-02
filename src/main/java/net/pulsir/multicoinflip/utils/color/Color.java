package net.pulsir.multicoinflip.utils.color;

import org.bukkit.ChatColor;

public class Color {

    @SuppressWarnings("ALL")
    public static String translate(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
