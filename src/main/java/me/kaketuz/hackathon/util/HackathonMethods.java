package me.kaketuz.hackathon.util;

import com.projectkorra.projectkorra.GeneralMethods;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Panda;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public class HackathonMethods {
    public static Color averageColor(Collection<Color> colors) {
        if (colors == null || colors.isEmpty()) {
            return Color.BLACK;
        }

        int totalRed = 0;
        int totalGreen = 0;
        int totalBlue = 0;

        for (Color color : colors) {
            totalRed += color.getRed();
            totalGreen += color.getGreen();
            totalBlue += color.getBlue();
        }
        int avgRed = totalRed / colors.size();
        int avgGreen = totalGreen / colors.size();
        int avgBlue = totalBlue / colors.size();

        return Color.fromRGB(avgRed, avgGreen, avgBlue);
    }

    public static boolean hasSolidBlocksBetween(Location from, Location to, Predicate<Block> ignoringBlocks) {
        if (!Objects.equals(from.getWorld(), to.getWorld())) {
            throw new IllegalArgumentException("All locations should be in same world!");
        }

       Location target = from.clone();
       Vector dir = GeneralMethods.getDirection(from, to);

        for (double i = 0; i < dir.length(); i+= 0.5) {
            target.add(dir.normalize().multiply(0.5));
            if (!ignoringBlocks.test(target.getBlock())) return true;
        }
        return false;
    }
    public static double round(double value, int decimalPlaces) {
        if (decimalPlaces < 0) throw new IllegalArgumentException("decimalPlaces < 0");

        double scale = Math.pow(10, decimalPlaces);
        return Math.round(value * scale) / scale;
    }

    public static Vector getRandom() {
        double pitch = ThreadLocalRandom.current().nextDouble(-90.0, 90.0);
        double yaw = ThreadLocalRandom.current().nextDouble(-180.0, 180.0);
        return new Vector(-Math.cos(pitch) * Math.sin(yaw), -Math.sin(pitch), Math.cos(pitch) * Math.cos(yaw));
    }



}
