package net.pulsir.multicoinflip.command;

import net.pulsir.multicoinflip.MultiCoinFlip;
import net.pulsir.multicoinflip.coinflip.CoinFlip;
import net.pulsir.multicoinflip.utils.color.Color;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CoinFlipCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) return false;
        if (args.length == 0) {
            Inventory inventory = Bukkit.createInventory(player, MultiCoinFlip.getInstance().getConfiguration()
                    .getConfiguration().getInt("inventory.size"), Color.translate(MultiCoinFlip.getInstance().getConfiguration()
                    .getConfiguration().getString("inventory.title")));

            for (CoinFlip coinFlip : MultiCoinFlip.getInstance().getCoinFlipManager().getCoinFlipMap().values()) {
                ItemStack itemStack = new ItemStack(Material.valueOf(MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getString("inventory.item.item")));
                ItemMeta meta = itemStack.getItemMeta();
                meta.setDisplayName(Color.translate(Objects.requireNonNull(MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getString("inventory.item.name"))
                        .replace("{creator}", coinFlip.getAuthorName())));
                meta.getPersistentDataContainer().set(MultiCoinFlip.getInstance().getItemKey(), PersistentDataType.STRING, coinFlip.getAuthor().toString());
                List<String> lore = new ArrayList<>();
                for (final String lines : MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getStringList("inventory.item.lore")) {
                    lore.add(Color.translate(lines).replace("{creator}", coinFlip.getAuthorName()).replace("{amount}", String.valueOf(coinFlip.getAmount())));
                }

                meta.setLore(lore);
                itemStack.setItemMeta(meta);

                inventory.addItem(itemStack);
            }

            if (MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getBoolean("inventory.overlay")) {
                for (int i = 0; i < inventory.getSize(); i++) {
                    if (inventory.getItem(i) == null) {
                        inventory.setItem(i, new ItemStack(Material.valueOf(MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getString("inventory.overlay-item"))));
                    }
                }
            }

            player.openInventory(inventory);
        } else if (args[0].equalsIgnoreCase("create")) {
            if (args.length == 1) {
                usage(player);
            } else {
                double amount = Double.parseDouble(args[1]);

                if (!MultiCoinFlip.getEcon().has(player, amount)) {
                    player.sendMessage(Color.translate(Objects.requireNonNull(MultiCoinFlip.getInstance().getLanguage()
                            .getConfiguration().getString("coinflip.insufficient-balance"))
                            .replace("{balance}", String.valueOf(MultiCoinFlip.getEcon().getBalance(player)))
                            .replace("{needed}", String.valueOf(amount))));
                    return false;
                }

                if (MultiCoinFlip.getInstance().getCoinFlipManager().getCoinFlipMap().get(player.getUniqueId()) != null) {
                    player.sendMessage(Color.translate(MultiCoinFlip.getInstance().getLanguage()
                            .getConfiguration().getString("coinflip.on-going")));
                    return false;
                }

                MultiCoinFlip.getInstance().getCoinFlipManager().getCoinFlipMap().put(player.getUniqueId(),
                        new CoinFlip(player.getName(), null, player.getUniqueId(), null, amount));
                MultiCoinFlip.getEcon().withdrawPlayer(player, amount);

                String message = player.getUniqueId() + "<splitter>" + player.getName() + "<splitter>" + amount;

                MultiCoinFlip.getInstance().getRedisManager().publish("coinflip_inventory", message);
                player.sendMessage(Color.translate(MultiCoinFlip.getInstance().getLanguage()
                        .getConfiguration().getString("coinflip.created")));
            }
        } else if (args[0].equalsIgnoreCase("cancel")) {
            if (MultiCoinFlip.getInstance().getCoinFlipManager().getCoinFlipMap().get(player.getUniqueId()) == null) {
                player.sendMessage(Color.translate(MultiCoinFlip.getInstance().getLanguage()
                        .getConfiguration().getString("coinflip.not-on-going")));
                return false;
            }

            double amount = MultiCoinFlip.getInstance().getCoinFlipManager().getCoinFlipMap().get(player.getUniqueId()).getAmount();
            MultiCoinFlip.getInstance().getCoinFlipManager().getCoinFlipMap().remove(player.getUniqueId());
            MultiCoinFlip.getInstance().getRedisManager().publish("coinflip_inventory_delete", player.getUniqueId().toString());
            MultiCoinFlip.getEcon().depositPlayer(player, amount);
            player.sendMessage(Color.translate(MultiCoinFlip.getInstance().getLanguage()
                    .getConfiguration().getString("coinflip.cancelled")));

        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(List.of("create", "cancel"));
        }
        if (args.length == 2 && args[1].equalsIgnoreCase("create")) {
            return new ArrayList<>(List.of("amount"));
        }

        return new ArrayList<>();
    }

    private void usage(Player player) {
        for (final String lines : MultiCoinFlip.getInstance().getLanguage().getConfiguration().getStringList("coinflip.usage")) {
            player.sendMessage(Color.translate(lines));
        }
    }
}
