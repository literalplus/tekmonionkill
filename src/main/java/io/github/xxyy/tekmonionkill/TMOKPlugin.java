package io.github.xxyy.tekmonionkill;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.MessageFormat;
import java.util.logging.Level;

/**
 * Main class of the plugin. You know, that one that takes money on death.
 *
 * @author <a href="http://xxyy.github.io/">xxyy</a>
 * @since 28.12.13
 */
public class TMOKPlugin extends JavaPlugin implements Listener {
    private Economy economy;
    private double amountToTake;
    private double amountToGive;

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }

    private String getFormattedMessage(final String messageId) {
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString("msg." + messageId));
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!setupEconomy()) {
            getLogger().log(Level.SEVERE, "Could not find Vault Economy Provider! Install one, for example Essentials.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        amountToTake = getConfig().getDouble("moneytotake", 10.0D);
        amountToGive = getConfig().getDouble("moneytogive", 10.0D);

        getServer().getPluginManager().registerEvents(this, this);
    }

    private boolean tryCreateAccount(final String plrName) {
        return economy.hasAccount(plrName) || economy.createPlayerAccount(plrName);
    }

    private void sendMultilineMessage(final String message, final Player plr) {
        if (message == null || message.equals("none")) {
            return;
        }

        plr.sendMessage(message.split("\n")); //Supports \n in config messages
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDeath(final PlayerDeathEvent evt) {
        final Player plrKiller = evt.getEntity().getKiller();
        final Player plrVictim = evt.getEntity();
        if (plrKiller == null || plrVictim.equals(plrKiller)) {
            return;
        }

        tryCreateAccount(plrVictim.getName());
        tryCreateAccount(plrKiller.getName());

        if (amountToTake != 0) { //Don't take nothing and don't notify players then
            if (!economy.has(plrVictim.getName(), amountToTake)) { //Can't take anything if no money available
                sendMultilineMessage(getFormattedMessage("notenough"), plrVictim);
            } else {
                economy.withdrawPlayer(plrVictim.getName(), amountToTake);
                sendMultilineMessage(
                        MessageFormat.format(getFormattedMessage("tovictim"), amountToTake, plrKiller.getName()), plrVictim);
            }
        }

        if (amountToGive != 0) {
            economy.depositPlayer(plrKiller.getName(), amountToGive);
            sendMultilineMessage(MessageFormat.format(getFormattedMessage("tokiller"), amountToGive, plrVictim.getName()), plrKiller);
        }
    }
}
