package net.pulsir.multicoinflip;

import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import net.pulsir.multicoinflip.coinflip.manager.CoinFlipManager;
import net.pulsir.multicoinflip.command.CoinFlipCommand;
import net.pulsir.multicoinflip.listener.CoinFlipListener;
import net.pulsir.multicoinflip.redis.RedisManager;
import net.pulsir.multicoinflip.utils.config.Config;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

@Getter
public final class MultiCoinFlip extends JavaPlugin {

    @Getter
    private static MultiCoinFlip instance;

    @Getter private static Economy econ = null;

    private Config configuration;
    private Config language;

    private CoinFlipManager coinFlipManager;
    private RedisManager redisManager;

    private final NamespacedKey itemKey = new NamespacedKey(this, "key");
    private final NamespacedKey actionKey = new NamespacedKey(this, "action");

    @Override
    public void onEnable() {
        instance = this;

        if (!setupEconomy()) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.loadConfiguration();
        this.loadListener(Bukkit.getPluginManager());
        this.loadCommands();

        this.coinFlipManager = new CoinFlipManager();

        this.redisManager = new RedisManager(getConfiguration().getConfiguration().getBoolean("redis.auth"));
        this.redisManager.subscribe();
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    private void loadConfiguration(){
        this.configuration = new Config(this, new File(getDataFolder(), "configuration.yml"),
                new YamlConfiguration(), "configuration.yml");
        this.language = new Config(this, new File(getDataFolder(), "language.yml"),
                new YamlConfiguration(), "language.yml");

        this.configuration.create();
        this.language.create();
    }

    private void loadListener(PluginManager pluginManager) {
        pluginManager.registerEvents(new CoinFlipListener(), this);
    }

    private void loadCommands(){
        Objects.requireNonNull(getCommand("coinflip")).setExecutor(new CoinFlipCommand());
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
}
