package net.pulsir.multicoinflip.listener;

import net.pulsir.multicoinflip.MultiCoinFlip;
import net.pulsir.multicoinflip.coinflip.CoinFlip;
import net.pulsir.multicoinflip.utils.color.Color;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class CoinFlipListener implements Listener {

    private final Random random = new Random();

    @EventHandler
    public void onInventory(InventoryClickEvent event) {
        if (event.getView().getTitle().equalsIgnoreCase(Color.translate(MultiCoinFlip.getInstance().getConfiguration()
                .getConfiguration().getString("inventory.title")))
        || event.getView().getTitle().equalsIgnoreCase(Color.translate(MultiCoinFlip.getInstance().getConfiguration()
                .getConfiguration().getString("confirm-inventory.title")))
        || event.getView().getTitle().equalsIgnoreCase(Color.translate(MultiCoinFlip.getInstance().getConfiguration()
                .getConfiguration().getString("coinflip-inventory.title")))) {
            event.setCancelled(true);
        }

        Player player = (Player) event.getWhoClicked();

        if (event.getCurrentItem() == null) return;
        if (event.getCurrentItem().getItemMeta() == null) return;

        if (event.getCurrentItem().getItemMeta().getPersistentDataContainer().has(MultiCoinFlip.getInstance().getItemKey())) {
            UUID coinFlipUUID = UUID.fromString(Objects.requireNonNull(event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(MultiCoinFlip.getInstance().getItemKey(), PersistentDataType.STRING)));

            if (player.getUniqueId().equals(coinFlipUUID)) {
                player.sendMessage(Color.translate(MultiCoinFlip.getInstance().getLanguage()
                        .getConfiguration().getString("coinflip.self")));
                return;
            }

            Inventory inventory = Bukkit.createInventory(player, MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getInt("confirm-inventory.size"),
                    Color.translate(MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getString("confirm-inventory.title")));

            if (MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getConfigurationSection("confirm-inventory.items") == null) return;
            for (String items : Objects.requireNonNull(MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getConfigurationSection("confirm-inventory.items")).getKeys(false)) {
                ItemStack itemStack = new ItemStack(Material.valueOf(MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getString("confirm-inventory.items." + items + ".item")));
                ItemMeta meta = itemStack.getItemMeta();
                meta.setDisplayName(Color.translate(MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getString("confirm-inventory.items." + items + ".name")));
                if (MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getString("confirm-inventory.items." + items + ".action") != null) {
                    meta.getPersistentDataContainer().set(MultiCoinFlip.getInstance().getActionKey(), PersistentDataType.STRING,
                            Objects.requireNonNull(MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getString("confirm-inventory.items." + items + ".action")));
                }
                List<String> lore = new ArrayList<>();
                for (final String lines : MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getStringList("confirm-inventory.items." + items + ".lore")) {
                    lore.add(Color.translate(lines));
                }

                meta.setLore(lore);
                itemStack.setItemMeta(meta);
                inventory.setItem(MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getInt("confirm-inventory.items." + items + ".slot"), itemStack);
            }

            if (MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getBoolean("confirm-inventory.overlay")) {
                for (int i = 0; i < inventory.getSize(); i++) {
                    if (inventory.getItem(i) == null) {
                        inventory.setItem(i, new ItemStack(Material.valueOf(MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getString("confirm-inventory.overlay-item"))));
                    }
                }
            }

            MultiCoinFlip.getInstance().getCoinFlipManager().getPendingCoinFlips().put(player.getUniqueId(), coinFlipUUID);

            player.openInventory(inventory);
        } else if (event.getCurrentItem().getItemMeta().getPersistentDataContainer().has(MultiCoinFlip.getInstance().getActionKey())) {
            String action = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(MultiCoinFlip.getInstance().getActionKey(), PersistentDataType.STRING);
            if (action == null) return;

            if (action.equalsIgnoreCase("cancel")) {
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

                MultiCoinFlip.getInstance().getCoinFlipManager().getPendingCoinFlips().remove(player.getUniqueId());

                player.openInventory(inventory);
            } else if (action.equalsIgnoreCase("confirm")) {
                if (MultiCoinFlip.getInstance().getCoinFlipManager().getPendingCoinFlips().get(player.getUniqueId()) == null) return;
                CoinFlip coinFlip = MultiCoinFlip.getInstance().getCoinFlipManager().getCoinFlipMap().get(MultiCoinFlip
                        .getInstance().getCoinFlipManager().getPendingCoinFlips().get(player.getUniqueId()));

                if (!MultiCoinFlip.getEcon().has(player, coinFlip.getAmount())) {
                    player.closeInventory();

                    Inventory inventory = Bukkit.createInventory(player, MultiCoinFlip.getInstance().getConfiguration()
                            .getConfiguration().getInt("inventory.size"), Color.translate(MultiCoinFlip.getInstance().getConfiguration()
                            .getConfiguration().getString("inventory.title")));

                    for (CoinFlip coin : MultiCoinFlip.getInstance().getCoinFlipManager().getCoinFlipMap().values()) {
                        ItemStack itemStack = new ItemStack(Material.valueOf(MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getString("inventory.item.item")));
                        ItemMeta meta = itemStack.getItemMeta();
                        meta.setDisplayName(Color.translate(Objects.requireNonNull(MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getString("inventory.item.name"))
                                .replace("{creator}", coin.getAuthorName())));
                        meta.getPersistentDataContainer().set(MultiCoinFlip.getInstance().getItemKey(), PersistentDataType.STRING, coin.getAuthor().toString());
                        List<String> lore = new ArrayList<>();
                        for (final String lines : MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getStringList("inventory.item.lore")) {
                            lore.add(Color.translate(lines).replace("{creator}", coin.getAuthorName()).replace("{amount}", String.valueOf(coin.getAmount())));
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

                    player.sendMessage(Color.translate(Objects.requireNonNull(MultiCoinFlip.getInstance().getLanguage().getConfiguration()
                                    .getString("coinflip.insufficient-balance-join")).replace("{balance}", String.valueOf(MultiCoinFlip.getEcon().getBalance(player)))
                            .replace("{needed}", String.valueOf(coinFlip.getAmount()))));

                    player.openInventory(inventory);
                    return;
                }

                MultiCoinFlip.getEcon().withdrawPlayer(player, coinFlip.getAmount());
                coinFlip.setPlayer(player.getUniqueId());
                coinFlip.setPlayerName(player.getName());

                Inventory inventory = Bukkit.createInventory(player, MultiCoinFlip.getInstance().getConfiguration().getConfiguration()
                        .getInt("coinflip-inventory.size"), Objects.requireNonNull(Color.translate(
                        MultiCoinFlip.getInstance().getConfiguration().getConfiguration()
                                .getString("coinflip-inventory.title"))
                ));

                int randomNumber = random.nextInt(100);

                String winnerName = randomNumber < 50 ? coinFlip.getAuthorName() : coinFlip.getPlayerName();
                double amount = coinFlip.getAmount() * 2;

                if (winnerName.equalsIgnoreCase(coinFlip.getAuthorName())) {
                    MultiCoinFlip.getEcon().depositPlayer(Bukkit.getOfflinePlayer(coinFlip.getAuthor()), amount);
                } else if (winnerName.equalsIgnoreCase(coinFlip.getPlayerName())) {
                    MultiCoinFlip.getEcon().depositPlayer(Bukkit.getOfflinePlayer(coinFlip.getPlayer()), amount);
                }

                MultiCoinFlip.getInstance().getRedisManager().publish("coinflip_inventory_delete", coinFlip.getAuthor().toString());
                MultiCoinFlip.getInstance().getCoinFlipManager().getPendingCoinFlips().remove(player.getUniqueId());

                ItemStack winner = new ItemStack(Material.valueOf(MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getString("coinflip-inventory.item.item")));
                ItemMeta meta = winner.getItemMeta();
                meta.setDisplayName(Color.translate(Objects.requireNonNull(MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getString("coinflip-inventory.item.name"))
                        .replace("{player}", winnerName).replace("{amount}", String.valueOf(amount))));
                List<String> lore = new ArrayList<>();
                for (final String lines : MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getStringList("coinflip-inventory.item.lore")) {
                    lore.add(Color.translate(lines).replace("{player}", winnerName).replace("{amount}", String.valueOf(amount)));
                }

                meta.setLore(lore);
                winner.setItemMeta(meta);

                inventory.setItem(MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getInt("coinflip-inventory.item.slot"), winner);

                if (MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getBoolean("coinflip-inventory.overlay")) {
                    for (int i = 0; i < inventory.getSize(); i++) {
                        if (inventory.getItem(i) == null) {
                            inventory.setItem(i, new ItemStack(Material.valueOf(MultiCoinFlip.getInstance().getConfiguration().getConfiguration().getString("coinflip-inventory.overlay-item"))));
                        }
                    }
                }

                player.openInventory(inventory);
            }
        }
    }
}
