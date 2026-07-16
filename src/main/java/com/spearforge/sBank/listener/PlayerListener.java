package com.spearforge.sBank.listener;

import com.spearforge.sBank.SBank;
import com.spearforge.sBank.model.Bank;
import com.spearforge.sBank.model.Debt;
import com.spearforge.sBank.utils.MiscUtils;
import com.spearforge.sBank.utils.TextUtils;
import net.milkbowl.vault.economy.EconomyResponse;
import lombok.SneakyThrows;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e){
        int percentage = SBank.getPlugin().getConfig().getInt("after-death-percentage");
        double balance = SBank.getEcon().getBalance(e.getEntity());
        double cut = balance * percentage / 100;
        Player p = e.getEntity();
        if (SBank.getPlugin().getConfig().getBoolean("lose-money-after-death")){
            if (e.getEntity().getKiller() == null){
                if (!p.hasPermission("sbank.dontlosemoney")){
                    if (balance > 0) {
                        EconomyResponse response = SBank.getEcon().withdrawPlayer(e.getEntity(), cut);
                        if (response.transactionSuccess()) {
                            double bank = SBank.getBanks().containsKey(p.getName()) ? SBank.getBanks().get(p.getName()).getBalance() : 0;
                            SBank.getAuditLogger().record("DEATH_PENALTY", p.getName(), p.getUniqueId().toString(), cut,
                                    balance, SBank.getEcon().getBalance(p), bank, bank, "environmental-death");
                            TextUtils.sendMessageWithPrefix(p, SBank.getPlugin().getConfig().getString("after-death-message").replaceAll("%money%", MiscUtils.formatBalance(cut)));
                        }
                    }
                }
            }
        }
    }

    @SneakyThrows
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        String defaultBankName = SBank.getPlugin().getConfig().getString("default-bank-name");
        double startMoney = SBank.getPlugin().getConfig().getDouble("start-money");
        if (!SBank.getDb().hasBank(p.getName())){
            Bank bank = new Bank();
            bank.setUsername(p.getName());
            bank.setUuid(p.getUniqueId().toString());
            bank.setBankname(defaultBankName);
            bank.setBalance(startMoney);
            SBank.getBanks().put(p.getName(), bank);
            SBank.getDb().setBankInDatabase(bank);
        } else {
            Bank bank = SBank.getDb().getBank(p.getName());
            SBank.getBanks().put(p.getName(), bank);
        }
    }

    @SneakyThrows
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e){
        Player p = e.getPlayer();
        Bank bank = SBank.getBanks().get(p.getName());
        Debt debt = SBank.getDebts().get(p.getName());

        SBank.getDb().updateBankInDatabase(bank);
        SBank.getBanks().remove(p.getName());
        if (debt != null) {
            SBank.getDb().updateDebtInDatabase(debt);
        }

        List<Map<String, ?>> maps = Arrays.asList(
                LoanGuiListener.getLoanAmount(),
                LoanGuiListener.getLoanAgree(),
                DebtGuiListener.getDebtPayment(),
                BankGuiListener.getCustomDepAmount(),
                BankGuiListener.getCustomWithAmount(),
                BankGuiListener.getCustomPhysicalWithAmount(),
                BankGuiListener.getSetName()
        );

        for (Map<String, ?> map : maps) {
            map.remove(p.getName());
        }
    }

}
