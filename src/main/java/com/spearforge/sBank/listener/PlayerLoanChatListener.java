package com.spearforge.sBank.listener;

import com.spearforge.sBank.SBank;
import com.spearforge.sBank.guis.DebtGui;
import com.spearforge.sBank.model.Debt;
import com.spearforge.sBank.utils.MiscUtils;
import com.spearforge.sBank.utils.TextUtils;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;


public class PlayerLoanChatListener implements Listener {

    @SneakyThrows
    @EventHandler
    public void onGivingLoanAmount(AsyncPlayerChatEvent e){
        Player player = e.getPlayer();

        if (LoanGuiListener.getLoanAmount().containsKey(player.getName())){
            e.setCancelled(true);
            if (SBank.getDebts().containsKey(player.getName())) {
                LoanGuiListener.getLoanAmount().remove(player.getName());
                LoanGuiListener.getLoanAgree().remove(player.getName());
                TextUtils.sendMessageWithPrefix(player, SBank.getPlugin().getConfig().getString("messages.active-debt"));
                return;
            }
            String loanAmount = e.getMessage();
            if (!loanAmount.equalsIgnoreCase("cancel")){
                if (MiscUtils.isInteger(loanAmount, 10)){
                    if (Double.parseDouble(loanAmount) >= SBank.getPlugin().getConfig().getInt("loan.min-loan") && Double.parseDouble(loanAmount) <= SBank.getPlugin().getConfig().getInt("loan.max-loan")){
                        if (!LoanGuiListener.getLoanAgree().containsKey(player.getName())){
                            LoanGuiListener.getLoanAmount().put(player.getName(), Double.parseDouble(loanAmount));
                            Debt debt = new Debt();
                            double totalDebt = Double.parseDouble(loanAmount) + ((Double.parseDouble(loanAmount) * SBank.getPlugin().getConfig().getInt("loan.loan-interest") / 100) * SBank.getPlugin().getConfig().getInt("loan.loan-term"));
                            debt.setTotal(totalDebt);
                            debt.setRemaining(totalDebt);
                            debt.setUuid(player.getUniqueId().toString());
                            debt.setUsername(player.getName());
                            debt.setDaily(totalDebt / SBank.getPlugin().getConfig().getInt("loan.loan-term"));
                            LoanGuiListener.getLoanAgree().put(player.getName(), debt);

                                Bukkit.getScheduler().runTask(SBank.getPlugin(), () -> player.openInventory(DebtGui.openAgreementPage(player, debt)));

                        }
                    } else {
                        TextUtils.sendMessageWithPrefix(player, SBank.getPlugin().getConfig().getString("messages.loan-amount-error").replaceAll("%minloan%", String.valueOf(SBank.getPlugin().getConfig().getInt("loan.min-loan"))).replaceAll("%maxloan%", String.valueOf(SBank.getPlugin().getConfig().getInt("loan.max-loan"))));
                    }
                } else {
                    TextUtils.sendMessageWithPrefix(player, SBank.getPlugin().getConfig().getString("messages.loan-amount-error").replaceAll("%minloan%", String.valueOf(SBank.getPlugin().getConfig().getInt("loan.min-loan"))).replaceAll("%maxloan%", String.valueOf(SBank.getPlugin().getConfig().getInt("loan.max-loan"))));
                }
            } else {
                LoanGuiListener.getLoanAmount().remove(player.getName());
                TextUtils.sendMessageWithPrefix(player, SBank.getPlugin().getConfig().getString("messages.loan-cancelled"));
            }
        }
    }


}
