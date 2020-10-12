package net.teamfruit.sitfly;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.common.base.Charsets;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import org.bukkit.entity.Player;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Base64;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class SkinValidator {
    public static int getMostColor(BufferedImage image) {
        Map<Integer, Long> colorCount = IntStream.range(0, image.getHeight()).flatMap(iy -> IntStream.range(0, image.getWidth()).map(ix -> image.getRGB(ix, iy)))
                .boxed().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return colorCount.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow(RuntimeException::new).getKey();
    }

    public static boolean isSimilarColor(Color c, Color a) {
        double distance = (c.getRed() - a.getRed()) * (c.getRed() - a.getRed())
                + (c.getGreen() - a.getGreen()) * (c.getGreen() - a.getGreen())
                + (c.getBlue() - a.getBlue()) * (c.getBlue() - a.getBlue());
        return distance < 10;
    }

    public static BufferedImage getPlayerSkinImage(Player player) throws IOException {
        ProfileProperty prop = player.getPlayerProfile().getProperties().stream()
                .filter(e -> "textures".equals(e.getName())).findFirst().orElseThrow(IllegalArgumentException::new);
        byte[] propData = Base64.getDecoder().decode(prop.getValue());
        String url = new JsonParser().parse(new JsonReader(new InputStreamReader(new ByteArrayInputStream(propData), Charsets.UTF_8)))
                .getAsJsonObject().getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
        return ImageIO.read(new URL(url).openStream());
    }

    public static boolean validateSkin(BufferedImage image) {
        BufferedImage imageBody = image.getSubimage(16, 20, 24, 12);
        BufferedImage imageJacket = image.getSubimage(16, 36, 24, 12);
        int colorSonsi = 0xffba4ad4;
        int colorBody = SkinValidator.getMostColor(imageBody);
        int colorJacket = SkinValidator.getMostColor(imageJacket);
        return SkinValidator.isSimilarColor(new Color(colorJacket), new Color(colorSonsi))
                || SkinValidator.isSimilarColor(new Color(colorBody), new Color(colorSonsi));
    }
}