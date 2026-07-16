package com.spearforge.sBank.listener;

import com.spearforge.sBank.SBank;
import com.spearforge.sBank.model.Bank;
import com.spearforge.sBank.modules.DebtModule;
import com.spearforge.sBank.utils.MiscUtils;
import com.spearforge.sBank.utils.TextUtils;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerBankChatListener implements Listener {

    @EventHandler
    public void onGivingCustomAmount(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!isPlayerInCustomTransaction(player.getName())) {
            return;
        }

        event.setCancelled(true);
        String message = event.getMessage();
        // Vault, inventories and plugin state must only be used on the server thread.
        Bukkit.getScheduler().runTask(SBank.getPlugin(), () -> processTransaction(player, message));
    }

    private void processTransaction(Player player, String message) {
        String playerName = player.getName();
        if (!isPlayerInCustomTransaction(playerName)) {
            return;
        }

        if (message.equalsIgnoreCase("close")) {
            cancelTransaction(playerName);
            TextUtils.sendMessageWithPrefix(player, SBank.getPlugin().getConfig().getString("messages.custom-amount-not-set"));
            return;
        }

        if (BankGuiListener.getSetName().containsKey(playerName)) {
            handleSetBankName(player, message);
        } else if (BankGuiListener.getCustomPhysicalWithAmount().containsKey(playerName)) {
            handlePhysicalWithdraw(player, message);
        } else if (BankGuiListener.getCustomDepAmount().containsKey(playerName)) {
            handleDeposit(player, message);
        } else if (BankGuiListener.getCustomWithAmount().containsKey(playerName)) {
            handleWithdraw(player, message);
        } else if (DebtGuiListener.getDebtPayment().containsKey(playerName)) {
            handleDebtPayment(player, message);
        } else {
            TextUtils.sendMessageWithPrefix(player, SBank.getPlugin().getConfig().getString("messages.invalid-amount"));
        }
    }

    private boolean isPlayerInCustomTransaction(String playerName) {
        return BankGuiListener.getCustomDepAmount().containsKey(playerName)
                || BankGuiListener.getCustomWithAmount().containsKey(playerName)
                || DebtGuiListener.getDebtPayment().containsKey(playerName)
                || BankGuiListener.getCustomPhysicalWithAmount().containsKey(playerName)
                || BankGuiListener.getSetName().containsKey(playerName);
    }

    private void cancelTransaction(String playerName) {
        BankGuiListener.getCustomDepAmount().remove(playerName);
        BankGuiListener.getCustomWithAmount().remove(playerName);
        BankGuiListener.getCustomPhysicalWithAmount().remove(playerName);
        DebtGuiListener.getDebtPayment().remove(playerName);
        BankGuiListener.getSetName().remove(playerName);
    }

    private void handleSetBankName(Player player, String message) {
        String playerName = player.getName();
        StringBuilder bankName = new StringBuilder();
        String[] args = message.split(" ");

        for (String arg : args) {
            bankName.append(arg).append(" ");
        }

        if (!args[0].equalsIgnoreCase("close")) {
            int maxLength = SBank.getPlugin().getConfig().getInt("bank-name-length");
            if (bankName.length() <= maxLength) {
                Pattern pattern = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(bankName.toString());
                if (!matcher.find()) {
                    SBank.getBanks().get(playerName).setBankname(bankName.toString());
                    TextUtils.sendMessageWithPrefix(player, SBank.getPlugin().getConfig().getString("messages.bank-name-set")
                            .replaceAll("%bankname%", bankName.toString()));
                    BankGuiListener.getSetName().remove(playerName);
                } else {
                    TextUtils.sendMessageWithPrefix(player, SBank.getPlugin().getConfig().getString("messages.bank-name-invalid"));
                }
            } else {
                TextUtils.sendMessageWithPrefix(player, "&cBank name can only be %max% characters long. "
                        .replaceAll("%max%", String.valueOf(maxLength)) + "(include spaces)");
            }
        } else {
            TextUtils.sendMessageWithPrefix(player, SBank.getPlugin().getConfig().getString("messages.bank-name-not-set"));
            BankGuiListener.getSetName().remove(playerName);
        }
    }

    private void handleDeposit(Player player, String message) {
        String playerName = player.getName();
        if (!MiscUtils.isNumeric(message)) {
            TextUtils.sendMessageWithPrefix(player, SBank.getPlugin().getConfig().getString("messages.invalid-amount"));
            return;
        }

        double amount = Double.parseDouble(message);
        double walletBefore = SBank.getEcon().getBalance(player);
        if (walletBefore < amount) {
            BankGuiListener.getCustomDepAmount().remove(playerName);
            TextUtils.sendMessageWithPrefix(player, SBank.getPlugin().getConfig().getString("messages.not-enough-money"));
            return;
        }

        Bank bank = SBank.getBanks().get(playerName);
        double bankBefore = bank.getBalance();
        EconomyResponse response = SBank.getEcon().withdrawPlayer(player, amount);
        BankGuiListener.getCustomDepAmount().remove(playerName);
        if (!response.transactionSuccess()) {
            sendTransactionFailed(player);
            return;
        }

        bank.setBalance(bankBefore + amount);
        SBank.getAuditLogger().record("DEPOSIT", playerName, player.getUniqueId().toString(), amount,
                walletBefore, SBank.getEcon().getBalance(player), bankBefore, bank.getBalance(), "gui-chat");
        TextUtils.sendMessageWithPrefix(player, SBank.getPlugin().getConfig().getString("messages.deposit-success")
                .replaceAll("%money%", MiscUtils.formatBalance(amount)));
    }

    private void handleWithdraw(Player player, String message) {
        String playerName = player.getName();
        if (!MiscUtils.isNumeric(message)) {
            TextUtils.sendMessageWithPrefix(player, SBank.getPlugin().getConfig().getString("messages.invalid-amount"));
            return;
        }

        double amount = Double.parseDouble(message);
        Bank bank = SBank.getBanks().get(playerName);
        if (bank.getBalance() < amount) {
            BankGuiListener.getCustomWithAmount().remove(playerName);
            TextUtils.sendMessageWithPrefix(player, SBank.getPlugin().getConfig().getString("messages.not-enough-money"));
            return;
        }

        double bankBefore = bank.getBalance();
        double walletBefore = SBank.getEcon().getBalance(player);
        EconomyResponse response = SBank.getEcon().depositPlayer(player, amount);
        BankGuiListener.getCustomWithAmount().remove(playerName);
        if (!response.transactionSuccess()) {
            sendTransactionFailed(player);
            return;
        }

        bank.setBalance(bankBefore - amount);
        SBank.getAuditLogger().record("WITHDRAW", playerName, player.getUniqueId().toString(), amount,
                walletBefore, SBank.getEcon().getBalance(player), bankBefore, bank.getBalance(), "gui-chat");
        TextUtils.sendMessageWithPrefix(player, SBank.getPlugin().getConfig().getString("messages.withdraw-success")
                .replaceAll("%money%", MiscUtils.formatBalance(amount)));
    }

    private void handlePhysicalWithdraw(Player player, String message) {
        String playerName = player.getName();
        if (!MiscUtils.isNumeric(message)) {
            TextUtils.sendMessageWithPrefix(player, SBank.getPlugin().getConfig().getString("messages.invalid-amount"));
            return;
        }

        double amount = Double.parseDouble(message);
        Bank bank = SBank.getBanks().get(playerName);
        if (bank.getBalance() < amount) {
            BankGuiListener.getCustomPhysicalWithAmount().remove(playerName);
            TextUtils.sendMessageWithPrefix(player, SBank.getPlugin().getConfig().getString("messages.not-enough-money"));
            return;
        }
        if (player.getInventory().firstEmpty() == -1
                || !player.getInventory().addItem(MiscUtils.getPhysicalMoney(player, amount)).isEmpty()) {
            BankGuiListener.getCustomPhysicalWithAmount().remove(playerName);
            TextUtils.sendMessageWithPrefix(player, SBank.getPlugin().getConfig().getString("messages.inventory-full"));
            return;
        }

        double bankBefore = bank.getBalance();
        double wallet = SBank.getEcon().getBalance(player);
        bank.setBalance(bankBefore - amount);
        BankGuiListener.getCustomPhysicalWithAmount().remove(playerName);
        SBank.getAuditLogger().record("PHYSICAL_WITHDRAW", playerName, player.getUniqueId().toString(), amount,
                wallet, wallet, bankBefore, bank.getBalance(), "inventory-item");
        TextUtils.sendMessageWithPrefix(player, SBank.getPlugin().getConfig().getString("messages.physical-withdraw-success")
                .replaceAll("%money%", MiscUtils.formatBalance(amount)));
    }

    private void handleDebtPayment(Player player, String message) {
        String playerName = player.getName();
        if (!MiscUtils.isNumeric(message)) {
            TextUtils.sendMessageWithPrefix(player, SBank.getPlugin().getConfig().getString("messages.invalid-amount"));
            return;
        }

        double amount = Double.parseDouble(message);
        double remaining = SBank.getDebts().get(playerName).getRemaining();
        double minimumPayment = Math.min(SBank.getDebts().get(playerName).getDaily(), remaining);
        if (amount >= minimumPayment && SBank.getEcon().getBalance(player) >= amount) {
            DebtModule.payDebtFromBalance(player, amount);
        } else {
            DebtGuiListener.getDebtPayment().remove(playerName);
            TextUtils.sendMessageWithPrefix(player, SBank.getPlugin().getConfig().getString("messages.not-enough-money"));
        }
    }

    private void sendTransactionFailed(Player player) {
        TextUtils.sendMessageWithPrefix(player, SBank.getPlugin().getConfig().getString(
                "messages.transaction-failed", "&cThe transaction could not be completed."));
    }
}
