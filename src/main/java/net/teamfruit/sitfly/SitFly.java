package net.teamfruit.sitfly;

import com.destroystokyo.paper.profile.ProfileProperty;
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
import org.bukkit.event.entity.HorseJumpEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.spigotmc.event.entity.EntityDismountEvent;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;
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
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equals("sitfly"))
            return false;

        if (!(sender instanceof Player)) {
            sender.sendMessage("プレイヤーからコマンドしてね");
            return true;
        }
        Player player = (Player) sender;

        Optional<ProfileProperty> prop = player.getPlayerProfile().getProperties().stream().filter(e -> "textures".equals(e.getName())).findFirst();
        Optional<byte[]> texture = prop.map(ProfileProperty::getValue).map(Base64.getDecoder()::decode);
        if (!texture.isPresent()) {
            sender.sendMessage("テクスチャを読み取れません");
            return true;
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(texture.get()));
            JFrame jf = new JFrame();
            jf.setContentPane(new JLabel(new ImageIcon(image)));
            jf.setSize(image.getWidth(), image.getHeight());
            jf.setVisible(true);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Skin Texture Load Error", e);
            sender.sendMessage("テキスチャのロードに失敗しました");
            return true;
        }

        Entity vehicle = player.getVehicle();
        if (vehicle != null) {
            PersistentDataContainer persistent = vehicle.getPersistentDataContainer();
            if (persistent.has(horseKey, PersistentDataType.BYTE) && persistent.get(horseKey, PersistentDataType.BYTE) == 1) {
                vehicle.remove();
            }
        }

        player.teleport(player.getLocation().add(0, 2, 0));

        Horse horse = player.getWorld().spawn(player.getLocation(), Horse.class);
        horse.setAI(false);
        horse.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 100, false, false));
        horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
        horse.setGravity(false);
        horse.setJumpStrength(5);
        horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(5);

        horse.getPersistentDataContainer().set(horseKey, PersistentDataType.BYTE, (byte) 1);
        horse.addPassenger(player);

        return true;
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        Entity entity = event.getDismounted();
        PersistentDataContainer persistent = entity.getPersistentDataContainer();
        if (!persistent.has(horseKey, PersistentDataType.BYTE) || persistent.get(horseKey, PersistentDataType.BYTE) != 1)
            return;
        Location loc = entity.getLocation();
        if (loc.getBlock().getRelative(BlockFace.DOWN, 2).isEmpty()) {
            event.setCancelled(true);
            entity.setVelocity(new Vector(0, -.5, 0));
        } else {
            entity.remove();
        }
    }

    @EventHandler
    public void onHorseJump(HorseJumpEvent event) {
        AbstractHorse entity = event.getEntity();
        PersistentDataContainer persistent = entity.getPersistentDataContainer();
        if (!persistent.has(horseKey, PersistentDataType.BYTE) || persistent.get(horseKey, PersistentDataType.BYTE) != 1)
            return;
        Location loc = entity.getLocation();
        if (loc.getBlock().getRelative(BlockFace.UP, 2).isEmpty()) {
            logger.info("" + event.getPower());
            entity.setVelocity(new Vector(0, event.getPower() * event.getPower() * 10, 0));
        }
    }
}
