package glorydark.wardensystem;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.CommandSender;
import cn.nukkit.utils.Config;
import com.sun.tools.javac.Main;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class WardenAPI {

    public static void sendMail(String sender, String receiver, String title, String content, List<String> commands, List<String> messages) {
        if (!Server.getInstance().lookupName(receiver).isPresent()) {
            Player senderPlayer = Server.getInstance().getPlayer(sender);
            if (senderPlayer != null) {
                senderPlayer.sendMessage("§c找不到该玩家！");
            }
            return;
        }
        Config config = new Config(MainClass.path + "/mailbox/" + receiver + ".yml", Config.YAML);
        List<Map<String, Object>> list = config.get("unclaimed", new ArrayList<>());
        Map<String, Object> map = new HashMap<>();
        map.put("sender", sender);
        map.put("title", title);
        map.put("content", content);
        map.put("millis", System.currentTimeMillis());
        map.put("commands", commands);
        map.put("messages", messages);
        list.add(map);
        config.set("unclaimed", list);
        config.save();
    }

    public static void sendMail(String sender, String receiver, String title, String content) {
        sendMail(receiver, sender, title, content, new ArrayList<>(), new ArrayList<>());
    }

    public static void ban(CommandSender operator, String player, String reason, long expire) {
        UUID uuid = getUUID(player);
        if (uuid == null) {
            operator.sendMessage("该玩家不存在！");
            return;
        }
        Config config = new Config(MainClass.path + "/ban/" + uuid + ".yml", Config.YAML);
        config.set("name", player);
        config.set("start", System.currentTimeMillis());
        if (config.exists("end")) {
            long end = config.getLong("end");
            if (System.currentTimeMillis() >= end) {
                config.set("end", expire == -1 ? "permanent" : expire);
            } else {
                config.set("end", expire == -1 ? "permanent" : config.getLong("end") - System.currentTimeMillis() + expire);
            }
        } else {
            config.set("end", expire == -1 ? "permanent" : expire);
        }
        config.set("reason", reason);
        if (operator.isPlayer()){
            config.set("operator", ((Player) operator).getName());
        }else {
            config.set("operator", "控制台");
        }
        config.save();
        Player punished = Server.getInstance().getPlayer(player);
        if (punished != null) {
            if (MainClass.bannedExecuteCommand.equals("")) {
                punished.close("",MainClass.styleData.getBan().replace("@time",WardenAPI.getUnBannedDate(uuid)));
            } else {
                Server.getInstance().dispatchCommand(punished, MainClass.bannedExecuteCommand.replace("{player}", punished.getName()));
            }
        }
        String unBannedDate = WardenAPI.getUnBannedDate(player);
        MainClass.log.log(Level.INFO, operator.getName() + "封禁 [" + player + "]，解封日期：" + unBannedDate + "，原因：" + reason);
        operator.sendMessage("§a成功封禁玩家【" + player + "】！解封日期:" + WardenAPI.getUnBannedDate(player));
        // 向所有在线玩家广播封禁消息
        broadcastMessage("§e[" + player + "] 因游戏作弊被打入小黑屋！");
    }

    public static void unban(CommandSender operator, String player) {
        UUID uuid = getUUID(player);
        if (uuid == null) {
            operator.sendMessage("该玩家不存在！");
            return;
        }
        File banCfg = new File(MainClass.path + "/ban/" + uuid + ".yml");
        if (banCfg.exists()) {
            banCfg.delete();
            operator.sendMessage("§a成功解封玩家【" + player + "】！");
            MainClass.log.log(Level.INFO, operator.getName() + "为玩家 [" + player + "] 解除封禁");
        } else {
            operator.sendMessage("§c该玩家未被封禁！");
        }
    }

    public static void mute(CommandSender operator, String player, String reason, long expire) {
        UUID uuid = getUUID(player);
        if (uuid == null) {
            operator.sendMessage("该玩家不存在！");
            return;
        }
        Config config = new Config(MainClass.path + "/mute/" + uuid + ".yml", Config.YAML);
        config.set("name", player);
        config.set("start", System.currentTimeMillis());
        if (config.exists("end")) {
            long end = config.getLong("end");
            if (System.currentTimeMillis() >= end) {
                config.set("end", expire == -1 ? "permanent" : expire);
            } else {
                config.set("end", expire == -1 ? "permanent" : config.getLong("end") - System.currentTimeMillis() + expire);
            }
        } else {
            config.set("end", expire == -1 ? "permanent" : expire);
        }
        config.set("reason", reason);
        if (operator.isPlayer()){
            config.set("operator", ((Player) operator).getName());
        }else {
            config.set("operator", "控制台");
        }
        config.save();
        MainClass.muted.add(uuid.toString());
        Player punished = Server.getInstance().getPlayer(player);
        if (punished != null) {
            punished.sendMessage("您已被禁言");
        }
        String unMutedDate = WardenAPI.getUnMutedDate(player);
        MainClass.log.log(Level.INFO, operator.getName() + "禁言玩家 [" + player + "]，解封日期：" + unMutedDate + "，原因：" + reason);
        operator.sendMessage("§a成功禁言玩家【" + player + "】！解封时间:" + unMutedDate + "！");
        // 向所有在线玩家广播封禁消息
        broadcastMessage("§e[" + player + "] 因违规发言被禁止发言！");
    }

    public static void unmute(CommandSender operator, String player) {
        UUID uuid = getUUID(player);
        if (uuid == null) {
            operator.sendMessage("该玩家不存在！");
            return;
        }
        File muteCfg = new File(MainClass.path + "/mute/" + uuid + ".yml");
        if (muteCfg.exists()) {
            muteCfg.delete();
            operator.sendMessage("§a成功帮玩家【" + player + "】解除禁言！");
            MainClass.log.log(Level.INFO, operator.getName() + "为玩家 [" + player + "] 解除禁言");
        } else {
            operator.sendMessage("§c该玩家未被禁言！");
        }
    }

    public static void warn(CommandSender operator, String player, String reason) {
        Player to = Server.getInstance().getPlayer(player);
        if (to != null) {
            to.sendMessage(MainClass.styleData.getWarn().replace("@reason",reason));
            operator.sendMessage("§a警告已发送！");
            MainClass.log.log(Level.INFO, operator.getName() + "警告玩家 [" + player + "]");
        } else {
            operator.sendMessage("§c该玩家不存在！");
        }
    }

    public static void kick(CommandSender operator, String player) {
        Player kicked = Server.getInstance().getPlayer(player);
        if (kicked != null) {
            kicked.close("",MainClass.styleData.getKick());
            operator.sendMessage("§a已踢出该玩家！");
            broadcastMessage("§e[" + player + "] 被踢出游戏！");
            MainClass.log.log(Level.INFO, operator.getName() + "踢出玩家 [" + player + "]");
        } else {
            operator.sendMessage("§c该玩家不存在！");
        }
    }

    public static void suspect(CommandSender operator, String player, String reason, long expire) {
        UUID uuid = getUUID(player);
        if (uuid == null) {
            operator.sendMessage("该玩家不存在！");
            return;
        }
        Config config = new Config(MainClass.path + "/suspect/" + uuid + ".yml", Config.YAML);
        config.set("name", player);
        config.set("start", System.currentTimeMillis());
        if (config.exists("end")) {
            long end = config.getLong("end");
            if (System.currentTimeMillis() >= end) {
                config.set("end", expire == -1 ? "permanent" : expire);
            } else {
                config.set("end", expire == -1 ? "permanent" : config.getLong("end") - System.currentTimeMillis() + expire);
            }
        } else {
            config.set("end", expire == -1 ? "permanent" : expire);
        }
        config.set("reason", reason);
        if (operator.isPlayer()){
            config.set("operator", ((Player) operator).getName());
        }else {
            config.set("operator", "控制台");
        }
        config.save();
        Player punished = Server.getInstance().getPlayer(player);
        if (punished != null) {
            punished.sendMessage("§e您已被加入怀疑玩家名单，原因：" + reason);
        }
        MainClass.log.log(Level.INFO, operator.getName() + "将 [" + player + "] 加入嫌疑名单，解除日期：" + getSuspectDate(player) + "，原因：" + reason);
        operator.sendMessage("§a玩家【" + player + "】被加入怀疑玩家名单！原因：" + reason);
        // 向所有在线玩家广播封禁消息
        broadcastMessage("§e[" + player + "] 被加入嫌疑名单！原因：" + reason);
    }

    public static void removeSuspect(CommandSender operator, String player) {
        UUID uuid = getUUID(player);
        if (uuid == null) {
            operator.sendMessage("该玩家不存在！");
            return;
        }
        File file = new File(MainClass.path + "/suspect/" + uuid + ".yml");
        if (file.exists()) {
            file.delete();
            operator.sendMessage("§a成功将玩家【" + player + "】从嫌疑名单移除！");
            MainClass.log.log(Level.INFO, operator.getName() + "将玩家 [" + player + "] 从嫌疑名单中移除!");
        } else {
            operator.sendMessage("§c该玩家未被加入嫌疑名单！");
        }
    }


    public static void broadcastMessage(String message) {
        for (Player player : Server.getInstance().getOnlinePlayers().values()) {
            player.sendMessage(message);
        }
    }

    public static String getUnBannedDate(String player) {
        UUID uuid = getUUID(player);
        if (uuid == null) {
            return "玩家不存在";
        }
        return getUnBannedDate(uuid);
    }

    public static String getUnBannedDate(UUID uuid) {
        File file = new File(MainClass.path + "/ban/" + uuid + ".yml");
        if (file.exists()) {
            Object object = new Config(file, Config.YAML).get("end", "0");
            if (object != null) {
                if (String.valueOf(object).equals("permanent")) {
                    return "永久封禁";
                } else {
                    long end = Long.parseLong(String.valueOf(object));
                    if (end > System.currentTimeMillis()) {
                        return MainClass.getDate(end);
                    }
                }
            }
        }
        return "Unknown";
    }

    public static String getUnMutedDate(String player) {
        UUID uuid = getUUID(player);
        if (uuid == null) {
            return "玩家不存在";
        }
        return getUnMutedDate(uuid);
    }

    public static String getUnMutedDate(UUID uuid) {
        File file = new File(MainClass.path + "/mute/" + uuid + ".yml");
        if (file.exists()) {
            Object object = new Config(file, Config.YAML).get("end", "0");
            if (object != null) {
                if (String.valueOf(object).equals("permanent")) {
                    return "永久封禁";
                } else {
                    long end = Long.parseLong(String.valueOf(object));
                    if (end > System.currentTimeMillis()) {
                        return MainClass.getDate(end);
                    }
                }
            }
        }
        return "Unknown";
    }

    public static String getSuspectDate(String player) {
        UUID uuid = getUUID(player);
        if (uuid == null) {
            return "玩家不存在";
        }
        return getSuspectDate(uuid);
    }

    public static String getSuspectDate(UUID uuid) {
        File file = new File(MainClass.path + "/suspect/" + uuid + ".yml");
        if (file.exists()) {
            Object object = new Config(file, Config.YAML).get("end", "");
            if (object != null) {
                if (String.valueOf(object).equals("permanent")) {
                    return "永久封禁";
                } else {
                    long end = (long) object;
                    if (end > System.currentTimeMillis()) {
                        return MainClass.getDate(end);
                    }
                }
            }
        }
        return "未被封禁";
    }

    public static long getRemainedBannedTime(String player) {
        UUID uuid = getUUID(player);
        if (uuid == null) {
            return 0;
        }
        return getRemainedBannedTime(uuid);
    }

    public static long getRemainedBannedTime(UUID uuid) {
        File file = new File(MainClass.path + "/ban/" + uuid + ".yml");
        if (file.exists()) {
            Config config = new Config(file, Config.YAML);
            Object object = config.get("end");
            if (object.toString().equals("permanent")) {
                return -1;
            } else {
                if (System.currentTimeMillis() >= (long) object) {
                    file.delete();
                    return 0;
                } else {
                    return (long) object - System.currentTimeMillis();
                }
            }
        }
        return 0;
    }

    public static long getRemainedMutedTime(String player) {
        UUID uuid = getUUID(player);
        if (uuid == null) {
            return 0;
        }
        return getRemainedMutedTime(uuid);
    }

    public static long getRemainedMutedTime(UUID uuid) {
        File file = new File(MainClass.path + "/mute/" + uuid + ".yml");
        if (file.exists()) {
            Config config = new Config(file, Config.YAML);
            Object object = config.get("end");
            if (object.toString().equals("permanent")) {
                return -1;
            } else {
                if (System.currentTimeMillis() >= (long) object) {
                    file.delete();
                    return 0;
                } else {
                    return (long) object - System.currentTimeMillis();
                }
            }
        }
        return 0;
    }

    public static long getRemainedSuspectTime(String player) {
        UUID uuid = getUUID(player);
        if (uuid == null) {
            return 0;
        }
        File file = new File(MainClass.path + "/suspect/" + uuid + ".yml");
        if (file.exists()) {
            Config config = new Config(file, Config.YAML);
            Object object = config.get("end");
            if (object.toString().equals("permanent")) {
                return -1;
            } else {
                if (System.currentTimeMillis() >= (long) object) {
                    config.remove(player);
                    config.save();
                    return 0;
                } else {
                    return (long) object - System.currentTimeMillis();
                }
            }
        }
        return 0;
    }

    public static UUID getUUID(String player) {
        Player p = Server.getInstance().getPlayerExact(player);
        if (p != null) {
            return p.getUniqueId();
        } else {
            Optional<UUID> uuid = Server.getInstance().lookupName(player);
            if (uuid.isPresent()) {
                return uuid.get();
            }
        }
        return null;
    }

}
