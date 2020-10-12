package net.teamfruit.sitfly;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.HorseJumpEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.spigotmc.event.entity.EntityDismountEvent;

import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SitFly extends JavaPlugin implements Listener {
    private Logger logger;
    private NamespacedKey horseKey;

    @Override
    public void onEnable() {
        // Plugin startup logic
        logger = getLogger();
        horseKey = new NamespacedKey(this, "fly_horse");
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // コマンドチェック
        if (!command.getName().equals("sitfly"))
            return false;

        // プレイヤーのみ
        if (!(sender instanceof Player)) {
            sender.sendMessage(new ComponentBuilder()
                    .append(new ComponentBuilder("[かめすたプラグイン] ").color(ChatColor.LIGHT_PURPLE).create())
                    .append(new ComponentBuilder("プレイヤーからコマンドしてね").color(ChatColor.RED).create())
                    .create()
            );
            return true;
        }
        Player player = (Player) sender;

        // 既に乗っている場合は解除
        Entity vehicle = player.getVehicle();
        if (vehicle != null) {
            PersistentDataContainer persistent = vehicle.getPersistentDataContainer();
            if (persistent.has(horseKey, PersistentDataType.BYTE) && persistent.get(horseKey, PersistentDataType.BYTE) == 1) {
                vehicle.remove();
                sender.sendMessage(new ComponentBuilder()
                        .append(new ComponentBuilder("[かめすたプラグイン] ").color(ChatColor.LIGHT_PURPLE).create())
                        .append(new ComponentBuilder("解除しました").color(ChatColor.GREEN).create())
                        .create()
                );
                return true;
            }
        }

        // スキンチェック
        boolean isValid;
        try {
            BufferedImage image = SkinValidator.getPlayerSkinImage(player);
            isValid = SkinValidator.validateSkin(image, getConfig().getInt("settings.color"), getConfig().getDouble("settings.limit"));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Skin Texture Load Error", e);
            sender.sendMessage(new ComponentBuilder()
                    .append(new ComponentBuilder("[かめすたプラグイン] ").color(ChatColor.LIGHT_PURPLE).create())
                    .append(new ComponentBuilder("スキンの検証に失敗しました").color(ChatColor.RED).create())
                    .create()
            );
            return true;
        }

        // スキンダメ
        if (!isValid) {
            sender.sendMessage(new ComponentBuilder()
                    .append(new ComponentBuilder("[かめすたプラグイン] ").color(ChatColor.LIGHT_PURPLE).create())
                    .append(new ComponentBuilder()
                            .append("空中浮遊は").color(ChatColor.RED).bold(false)
                            .append("赤紫色").color(ChatColor.LIGHT_PURPLE).bold(true)
                            .append("の修道着を身に着ける必要があります。").color(ChatColor.RED).bold(false)
                            .append("スキンを変更してください。").color(ChatColor.GREEN).bold(false).create())
                    .create()
            );
            return true;
        }

        // 位置調整
        player.teleport(player.getLocation().add(0, 2, 0));

        // 馬生成
        Horse horse = player.getWorld().spawn(player.getLocation(), Horse.class);
        horse.setAI(false);
        horse.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 100, false, false));
        horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
        horse.setGravity(false);
        horse.setJumpStrength(5);
        horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(getConfig().getDouble("settings.speed"));

        // タグ付け
        horse.getPersistentDataContainer().set(horseKey, PersistentDataType.BYTE, (byte) 1);

        // 乗馬
        horse.addPassenger(player);

        return true;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();

        if (!(entity instanceof Horse))
            return;

        // タグ付けチェック
        PersistentDataContainer persistent = entity.getPersistentDataContainer();
        if (!persistent.has(horseKey, PersistentDataType.BYTE) || persistent.get(horseKey, PersistentDataType.BYTE) != 1)
            return;

        // 落下ダメ無効
        event.setCancelled(true);
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        Entity entity = event.getDismounted();

        // タグ付けチェック
        PersistentDataContainer persistent = entity.getPersistentDataContainer();
        if (!persistent.has(horseKey, PersistentDataType.BYTE) || persistent.get(horseKey, PersistentDataType.BYTE) != 1)
            return;

        // Shiftして、地面だったら降りる、それ以外だったら下がる、
        Location loc = entity.getLocation();
        if (loc.getBlock().getRelative(BlockFace.DOWN, 2).isEmpty()) {
            event.setCancelled(true);
            entity.setVelocity(new Vector(0, -getConfig().getDouble("settings.down"), 0));
        } else {
            entity.remove();
        }
    }

    @EventHandler
    public void onHorseJump(HorseJumpEvent event) {
        AbstractHorse entity = event.getEntity();

        // タグ付けチェック
        PersistentDataContainer persistent = entity.getPersistentDataContainer();
        if (!persistent.has(horseKey, PersistentDataType.BYTE) || persistent.get(horseKey, PersistentDataType.BYTE) != 1)
            return;

        // Spaceしたら少し上がる
        Location loc = entity.getLocation();
        if (loc.getBlock().getRelative(BlockFace.UP, 2).isEmpty()) {
            entity.setVelocity(new Vector(0, event.getPower() * event.getPower() * getConfig().getDouble("settings.jump"), 0));
        }
    }
}
