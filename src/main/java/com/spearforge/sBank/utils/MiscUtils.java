package com.spearforge.sBank.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.spearforge.sBank.SBank;
import com.spearforge.sBank.model.MoneyPile;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiscUtils {
    
    public static double getInterest(Player player, double _interest) {
        Pattern pattern = Pattern.compile("^sbank\\.interest\\.(\\d+)$"); // sbank.interest.<percent>

        return player.getEffectivePermissions().stream()
            .map(PermissionAttachmentInfo::getPermission)
            .map(pattern::matcher)
            .filter(Matcher::matches) 
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max()
            .orElse((int) _interest);
    }


    public static String formatBalance(double balance) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');

        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);

        return df.format(balance);
    }



    public static boolean isInteger(String s, int radix) {
        if(s.isEmpty()) return false;
        for(int i = 0; i < s.length(); i++) {
            if(i == 0 && s.charAt(i) == '-') {
                if(s.length() == 1) return false;
                else continue;
            }
            if(Character.digit(s.charAt(i),radix) < 0) return false;
        }
        return true;
    }

    public static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            double value = Double.parseDouble(str);
            return value >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static ItemStack getCustomHead(String base64) {

        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        if (base64 == null || base64.isEmpty()) {
            return head; //
        }

        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        profile.getProperties().put("textures", new Property("textures", base64));

        try {
            Field profileField = headMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(headMeta, profile);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        head.setItemMeta(headMeta);
        return head;
    }

    public static ItemStack getMaterialOrHead(String configPath){
        String configuredMaterial = SBank.getGuiConfig().getString(configPath + ".material");
        if (configuredMaterial == null || configuredMaterial.isBlank()) {
            SBank.getPlugin().getLogger().warning("Missing GUI material at " + configPath + ".material; using BARRIER.");
            return new ItemStack(Material.BARRIER);
        }

        try {
            return new ItemStack(Material.valueOf(configuredMaterial.toUpperCase()));
        } catch (IllegalArgumentException e) {
            try {
                return getCustomHead(configuredMaterial);
            } catch (RuntimeException headError) {
                SBank.getPlugin().getLogger().warning("Invalid GUI material at " + configPath + ".material: " + configuredMaterial + "; using BARRIER.");
                return new ItemStack(Material.BARRIER);
            }
        }
    }

    public static ItemStack getPhysicalMoney(Player player, double amount) {
        ItemStack physicalMoney;
        String materialName = SBank.getPlugin().getConfig().getString("physical-money.item.material");

        try {
            Material material = Material.valueOf(materialName);
            physicalMoney = new ItemStack(material);
        } catch (IllegalArgumentException e) {
            physicalMoney = getCustomHead(materialName);
        }

        MoneyPile moneyPile = new MoneyPile(amount, player.getName());

        ItemMeta meta = physicalMoney.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', SBank.getPlugin().getConfig().getString("physical-money.item.name")));
            meta.setLore(TextUtils.replacePlaceholders(SBank.getPlugin().getConfig().getStringList("physical-money.item.lore"), null, null, moneyPile));
            physicalMoney.setItemMeta(meta);
        }

        return physicalMoney;
    }

    public static double extractMoney(List<String> lore) {
        Pattern pattern = Pattern.compile("\\d{1,3}(?:\\.\\d{3})*(?:,\\d+)?\\$");

        for (String line : lore) {

            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                String rawNumber = matcher.group();

                try {
                    String processedNumber = rawNumber.replace(".", "").replace(",", ".").replace("$", "");

                    double amount = Double.parseDouble(processedNumber);
                    return amount;
                } catch (NumberFormatException e) {
                    return -100.0;
                }
            }
        }

        return -100.0;
    }


}
