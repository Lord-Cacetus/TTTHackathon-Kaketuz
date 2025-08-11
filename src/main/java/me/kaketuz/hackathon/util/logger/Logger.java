package me.kaketuz.hackathon.util.logger;

import org.bukkit.Bukkit;




//so many russian words :O
//P.S its just from my lib, and im too lazy to delete info xd



public final class Logger {

    public static void sendToConsole(String message,LoggerValues val) {
        switch (val) {
            case ERROR -> Bukkit.getLogger().severe(ANSIValues.RED.toANSI() + message + ANSIValues.RESET.toANSI());
            case WARNING -> Bukkit.getLogger().warning(ANSIValues.YELLOW.toANSI() + message + ANSIValues.RESET.toANSI());
            case GENERAL, CUSTOM -> Bukkit.getLogger().info(message);
            case SUCCESSFULLY -> Bukkit.getLogger().info(ANSIValues.GREEN.toANSI() + message + ANSIValues.RESET.toANSI());
            case INFO -> Bukkit.getLogger().info(ANSIValues.BLUE.toANSI() + message + ANSIValues.RESET.toANSI());
        }
    }
    public static void sendToConsole(String message, LoggerValues val, ANSIValues ANSI_VAL) {
        switch (val) {
            case ERROR -> Bukkit.getLogger().severe(ANSIValues.RED.toANSI() + message + ANSIValues.RESET.toANSI());
            case WARNING -> Bukkit.getLogger().warning(ANSIValues.YELLOW.toANSI() + message + ANSIValues.RESET.toANSI());
            case GENERAL -> Bukkit.getLogger().info(message);
            case SUCCESSFULLY -> Bukkit.getLogger().info(ANSIValues.GREEN.toANSI() + message + ANSIValues.RESET.toANSI());
            case INFO -> Bukkit.getLogger().info(ANSIValues.BLUE.toANSI() + message + ANSIValues.RESET.toANSI());
            case CUSTOM -> Bukkit.getLogger().info(ANSI_VAL + message + ANSIValues.RESET.toANSI());
        }
    }


    public static void sendGeneral(String message) {
        Bukkit.getLogger().info(message);
    }

    public static void sendWarning(String message) {
        Bukkit.getLogger().warning(ANSIValues.YELLOW.toANSI() + message + ANSIValues.RESET.toANSI());
    }

    public static void sendError(String message) {
        Bukkit.getLogger().severe(ANSIValues.RED.toANSI() + message + ANSIValues.RESET.toANSI());
    }

    public static void sendSuccessfully(String message) {
        Bukkit.getLogger().info(ANSIValues.GREEN.toANSI() + message + ANSIValues.RESET.toANSI());
    }
    public static void sendInfo(String message) {
        Bukkit.getLogger().info(ANSIValues.GREEN.toANSI() + message + ANSIValues.RESET.toANSI());
    }
    public static void sendCustom(String message, ANSIValues val) {
        Bukkit.getLogger().info(val.toANSI() + message + ANSIValues.RESET.toANSI());
    }



}
