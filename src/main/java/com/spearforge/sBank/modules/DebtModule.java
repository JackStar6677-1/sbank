package com.spearforge.sBank.modules;

import com.spearforge.sBank.SBank;
import com.spearforge.sBank.listener.DebtGuiListener;
import com.spearforge.sBank.utils.MiscUtils;
import com.spearforge.sBank.utils.TextUtils;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DebtModule {

    public static boolean hasDefinedHoursPassed(String username) {
        try {
            String lastDebtPayment = SBank.getDebts().get(username).getLastPaymentDate();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime lastDebtDate = LocalDateTime.parse(lastDebtPayment, formatter);

            LocalDateTime currentTime = LocalDateTime.now();

            Duration duration = Duration.between(lastDebtDate, currentTime);

            long hoursPassed = duration.toHours();
            return hoursPassed >= Long.parseLong(SBank.getPlugin().getConfig().getString("loan.debt-time"));
        } catch (Exception e) {
            SBank.getPlugin().getLogger().warning("An error occurred while checking if the debt time has passed");
            return false;
        }
    }

    public static void updateLastPaymentDate(String username) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.now();
        String dateFormatted = dateTime.format(formatter);

        SBank.getDebts().get(username).setLastPaymentDate(dateFormatted);
    }

    public static String getTimeUntilNextPayment(String username) {
        try {
            String lastDebtPayment = SBank.getDebts().get(username).getLastPaymentDate();
            long debtIntervalHours = Long.parseLong(SBank.getPlugin().getConfig().getString("loan.debt-time"));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime lastDebtDate = LocalDateTime.parse(lastDebtPayment, formatter);

            LocalDateTime nextDebtDate = lastDebtDate.plusHours(debtIntervalHours);
            LocalDateTime currentTime = LocalDateTime.now();

            Duration duration = Duration.between(currentTime, nextDebtDate);

            long hours = duration.toHours();
            long minutes = duration.toMinutes() % 60;
            long seconds = duration.getSeconds() % 60;

            return String.format("%02d hours, %02d minutes, %02d seconds", hours, minutes, seconds);
        } catch (Exception e) {
            return "Error calculating time until next debt";
        }
    }



    public static void payDebt(Player player, Double amount) {
        if (!isValidPayment(player, amount)) return;
        amount = Math.min(amount, SBank.getDebts().get(player.getName()).getRemaining());
        SBank.getDebts().get(player.getName()).setRemaining(SBank.getDebts().get(player.getName()).getRemaining() - amount);
        // handle bank balance
        SBank.getBanks().get(player.getName()).setBalance(SBank.getBanks().get(player.getName()).getBalance() - amount);

        if (SBank.getDebts().get(player.getName()).getRemaining() <= 0){
            TextUtils.sendMessageWithPrefix(player, SBank.getPlugin().getConfig().getString("messages.debt-paid").replaceAll("%money%", MiscUtils.formatBalance(SBank.getDebts().get(player.getName()).getTotal())));
            SBank.getDebts().remove(player.getName());
            try {
                SBank.getDb().removeDebt(player.getName());
            } catch (SQLException ex) {
                SBank.getPlugin().getLogger().warning("An error occurred while removing the debt from the database");
            }
        }else{
            TextUtils.sendMessageWithPrefix(player, SBank.getPlugin().getConfig().getString("messages.debt-payment-success").replaceAll("%money%", MiscUtils.formatBalance(amount)));
            DebtModule.updateLastPaymentDate(player.getName());
            persistDebt(player.getName());
        }
    }

    public static void payDebtFromBalance(Player player, Double amount){
        if (!isValidPayment(player, amount)) return;
        amount = Math.min(amount, SBank.getDebts().get(player.getName()).getRemaining());
        SBank.getDebts().get(player.getName()).setRemaining(SBank.getDebts().get(player.getName()).getRemaining() - amount);
        SBank.getEcon().withdrawPlayer(player, amount);
        // remove from map
        DebtGuiListener.getDebtPayment().remove(player.getName());

        if (SBank.getDebts().get(player.getName()).getRemaining() <= 0){
            TextUtils.sendMessageWithPrefix(player, SBank.getPlugin().getConfig().getString("messages.debt-paid").replaceAll("%money%", MiscUtils.formatBalance(SBank.getDebts().get(player.getName()).getTotal())));
            SBank.getDebts().remove(player.getName());
            try {
                SBank.getDb().removeDebt(player.getName());
            } catch (SQLException ex) {
                SBank.getPlugin().getLogger().warning("An error occurred while removing the debt from the database for " + player.getName());
            }
        }else{
            TextUtils.sendMessageWithPrefix(player, SBank.getPlugin().getConfig().getString("messages.debt-payment-success").replaceAll("%money%", MiscUtils.formatBalance(amount)));
            DebtModule.updateLastPaymentDate(player.getName());
            persistDebt(player.getName());
        }

    }

    private static boolean isValidPayment(Player player, Double amount) {
        return player != null && amount != null && Double.isFinite(amount) && amount > 0
                && SBank.getDebts().containsKey(player.getName());
    }

    private static void persistDebt(String username) {
        try {
            SBank.getDb().updateDebtInDatabase(SBank.getDebts().get(username));
        } catch (SQLException exception) {
            SBank.getPlugin().getLogger().warning("[ERROR] No se pudo persistir el pago de deuda de " + username);
        }
    }

}
