package glorydark.wardensystem;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.scheduler.NukkitRunnable;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import glorydark.wardensystem.data.*;
import glorydark.wardensystem.forms.FormMain;
import glorydark.wardensystem.forms.WardenEventListener;
import glorydark.wardensystem.reports.matters.BugReport;
import glorydark.wardensystem.reports.matters.ByPassReport;
import glorydark.wardensystem.reports.matters.Reward;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MainClass extends PluginBase {

    public static HashMap<String, WardenData> staffData = new HashMap<>();

    public static List<BugReport> bugReports = new ArrayList<>();

    public static List<ByPassReport> byPassReports = new ArrayList<>();

    public static HashMap<Player, Integer> mails = new HashMap<>();

    public static String path;

    public static HashMap<String, Reward> rewards = new HashMap<>();

    public static List<String> muted = new ArrayList<>();

    public static HashMap<String, SuspectData> suspectList = new HashMap<>();

    public static Logger log;

    public static List<OfflineData> offlineData = new ArrayList<>();

    public static HashMap<Player, PlayerData> playerData = new HashMap<>();

    public static List<String> forbid_modify_worlds;

    public static String bannedExecuteCommand;
    public static StyleData styleData;

    public static String getDate(long millis) {
        Date date = new Date(millis);
        SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒");
        return format.format(date);
    }

    public static String getUltraPrecisionDate(long millis) {
        Date date = new Date(millis);
        SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒SSS");
        return format.format(date);
    }

    @Override
    public void onLoad() {
        this.getLogger().info("WardenSystem 正在加载！");
    }

    @Override
    public void onEnable() {
        path = this.getDataFolder().getPath();
        log = Logger.getLogger("WardenSystem_" + UUID.randomUUID());
        new File(path + "/logs/").mkdirs();
        FileHandler fileHandler;
        try {
            fileHandler = new FileHandler(path + "/logs/" + getDate(System.currentTimeMillis()) + ".log");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        fileHandler.setFormatter(new LoggerFormatter());
        log.addHandler(fileHandler);
        new File(path + "/bugreports/").mkdirs();
        new File(path + "/bypassreports/").mkdirs();
        new File(path + "/staffs/").mkdirs();
        new File(path + "/ban/").mkdirs();
        new File(path + "/mute/").mkdirs();
        new File(path + "/suspect/").mkdirs();
        new File(path + "/mailbox/").mkdirs();
        this.saveResource("config.yml", false);
        this.saveResource("rewards.yml", false);
        Config config = new Config(path + "/config.yml", Config.YAML);
        styleData = new StyleData();
        styleData.setBan(config.getString("style.ban"));
        styleData.setKick(config.getString("style.kick"));
        styleData.setMute(config.getString("style.mute"));
        styleData.setWarn(config.getString("style.warn"));
        styleData.setRp(config.getString("style.rp"));
        forbid_modify_worlds = new ArrayList<>(config.getStringList("forbid_modify_worlds"));
        bannedExecuteCommand = config.getString("banned_execute_command", "");
        for (String player : new ArrayList<>(config.getStringList("devs"))) {
            WardenData data = new WardenData(player, null, new Config(path + "/staffs/" + player + ".yml", Config.YAML));
            data.setLevelType(WardenLevelType.Dev);
            staffData.put(player, data);
        }
        for (String player : new ArrayList<>(config.getStringList("admins"))) {
            if (staffData.containsKey(player)) {
                continue;
            }
            WardenData data = new WardenData(player, null, new Config(path + "/staffs/" + player + ".yml", Config.YAML));
            data.setLevelType(WardenLevelType.ADMIN);
            staffData.put(player, data);
        }
        for (String player : new ArrayList<>(config.getStringList("staffs"))) {
            if (staffData.containsKey(player)) {
                continue;
            }
            WardenData data = new WardenData(player, null, new Config(path + "/staffs/" + player + ".yml", Config.YAML));
            data.setLevelType(WardenLevelType.Staff);
            staffData.put(player, data);
        }
        for (File file : Objects.requireNonNull(new File(path + "/bugreports/").listFiles())) {
            if (file.isDirectory()) {
                continue;
            }
            Config brc = new Config(file, Config.YAML);
            String filename = file.getName().split("\\.")[0];
            BugReport report = new BugReport(brc.getString("info"), brc.getString("player"), Long.parseLong(filename), brc.getBoolean("anonymous"));
            bugReports.add(report);
        }
        for (File file : Objects.requireNonNull(new File(path + "/bypassreports/").listFiles())) {
            if (file.isDirectory()) {
                continue;
            }
            Config brc = new Config(file, Config.YAML);
            String filename = file.getName().split("\\.")[0];
            ByPassReport report = new ByPassReport(brc.getString("info"), brc.getString("player"), brc.getString("suspect"), Long.parseLong(filename), brc.getBoolean("anonymous"));
            byPassReports.add(report);
        }
        for (File file : Objects.requireNonNull(new File(path + "/mute/").listFiles())) {
            muted.add(file.getName().replace(".yml", ""));
        }
        File suspects = new File(path + "/suspect/");

        for (File suspectFile : Objects.requireNonNull(suspects.listFiles())) {
            String uuid = suspectFile.getName().replace(".yml", "");
            Config suspectCfg = new Config(suspectFile, Config.YAML);
            long end = suspectCfg.getLong("end");
            if (System.currentTimeMillis() >= end) {
                suspectFile.delete();
                continue;
            }
            suspectList.put(uuid, new SuspectData(uuid, suspectCfg.getLong("start"), end));
        }
        Config rewardCfg = new Config(path + "/rewards.yml", Config.YAML);
        rewards.put("无奖励", new Reward(new ConfigSection()));
        for (String key : rewardCfg.getKeys(false)) {
            rewards.put(key, new Reward(rewardCfg.getSection(key)));
        }
        new NukkitRunnable() {
            @Override
            public void run() {
                offlineData.removeIf(OfflineData::isExpired);
            }
        }.runTaskTimer(this, 0, 20);
        this.getServer().getPluginManager().registerEvents(new WardenEventListener(), this);
        this.getServer().getCommandMap().register("", new WardenCommand(config.getString("command")));
        // this.getServer().getCommandMap().register("", new TestCommand("test"));
        this.getLogger().info("WardenSystem 加载成功！");
    }

    @Override
    public void onDisable() {
        this.getLogger().info("WardenSystem 卸载中...");
    }

    public static class WardenCommand extends Command {

        public WardenCommand(String name) {
            super(name);
        }

        @Override
        public boolean execute(CommandSender commandSender, String s, String[] strings) {
            if (commandSender.isPlayer()) {
                if (staffData.containsKey(commandSender.getName())) {
                    FormMain.showWardenMain((Player) commandSender);
                } else {
                    FormMain.showPlayerMain((Player) commandSender);
                }
            } else {
                switch (strings[0]) {
                    case "add":
                        if (strings.length < 2) {
                            return true;
                        }
                        if (!Server.getInstance().lookupName(strings[2]).isPresent()) {
                            commandSender.sendMessage("§c找不到玩家！");
                            return true;
                        }
                        Config config = new Config(path + "/config.yml", Config.YAML);
                        List<String> admins = new ArrayList<>(config.getStringList("admins"));
                        List<String> staffs = new ArrayList<>(config.getStringList("staffs"));
                        List<String> devs = new ArrayList<>(config.getStringList("devs"));
                        switch (strings[1]) {
                            case "admin":
                                staffs.remove(strings[2]);
                                devs.remove(strings[2]);
                                admins.add(strings[2]);
                                if (staffData.containsKey(strings[2])) {
                                    staffData.get(strings[2]).setLevelType(WardenLevelType.ADMIN);
                                } else {
                                    WardenData data = new WardenData(strings[2], null, new Config(path + "/staffs/" + strings[2] + ".yml", Config.YAML));
                                    data.setLevelType(WardenLevelType.ADMIN);
                                    staffData.put(strings[2], data);
                                }
                                commandSender.sendMessage("§a成功为玩家【" + strings[2] + "】赋予协管主管权限！");
                                log.log(Level.INFO, "CONSOLE成功为玩家【" + strings[2] + "】赋予协管主管权限！");
                                break;
                            case "staff":
                                staffs.add(strings[2]);
                                devs.remove(strings[2]);
                                admins.remove(strings[2]);
                                if (staffData.containsKey(strings[2])) {
                                    staffData.get(strings[2]).setLevelType(WardenLevelType.Staff);
                                } else {
                                    WardenData data = new WardenData(strings[2], null, new Config(path + "/staffs/" + strings[2] + ".yml", Config.YAML));
                                    data.setLevelType(WardenLevelType.Staff);
                                    staffData.put(strings[2], data);
                                }
                                commandSender.sendMessage("§a成功为玩家【" + strings[2] + "】赋予协管权限！");
                                log.log(Level.INFO, "CONSOLE成功为玩家【" + strings[2] + "】赋予协管权限！");
                                break;
                            case "dev":
                                staffs.remove(strings[2]);
                                devs.add(strings[2]);
                                admins.remove(strings[2]);
                                if (staffData.containsKey(strings[2])) {
                                    staffData.get(strings[2]).setLevelType(WardenLevelType.Dev);
                                } else {
                                    WardenData data = new WardenData(strings[2], null, new Config(path + "/staffs/" + strings[2] + ".yml", Config.YAML));
                                    data.setLevelType(WardenLevelType.Dev);
                                    staffData.put(strings[2], data);
                                }
                                commandSender.sendMessage("§a成功为玩家【" + strings[2] + "】赋予测试员权限！");
                                log.log(Level.INFO, "CONSOLE成功为玩家【" + strings[2] + "】赋予测试员权限！");
                                break;
                        }
                        config.set("admins", admins);
                        config.set("devs", devs);
                        config.set("staffs", staffs);
                        config.save();
                        break;
                    case "remove":
                        if (strings.length < 2) {
                            return true;
                        }
                        if (!Server.getInstance().lookupName(strings[2]).isPresent()) {
                            commandSender.sendMessage("§c找不到玩家！");
                            return true;
                        }
                        config = new Config(path + "/config.yml", Config.YAML);
                        admins = new ArrayList<>(config.getStringList("admins"));
                        staffs = new ArrayList<>(config.getStringList("staffs"));
                        devs = new ArrayList<>(config.getStringList("devs"));
                        switch (strings[1]) {
                            case "opstaff":
                                admins.remove(strings[2]);
                                staffData.remove(strings[2]);
                                commandSender.sendMessage("§a成功解除玩家【" + strings[2] + "】协管主管权限！");
                                log.log(Level.INFO, "CONSOLE解除玩家【" + strings[2] + "】协管主管权限！");
                                break;
                            case "staff":
                                staffs.remove(strings[2]);
                                staffData.remove(strings[2]);
                                commandSender.sendMessage("§a成功解除玩家【" + strings[2] + "】协管权限！");
                                log.log(Level.INFO, "CONSOLE解除玩家【" + strings[2] + "】协管权限！");
                                break;
                            case "dev":
                                devs.remove(strings[2]);
                                staffData.remove(strings[2]);
                                commandSender.sendMessage("§a成功解除【" + strings[2] + "】测试员权限！");
                                log.log(Level.INFO, "CONSOLE解除【" + strings[2] + "】测试员权限！");
                                break;
                        }
                        config.set("admins", admins);
                        config.set("devs", devs);
                        config.set("staffs", staffs);
                        config.save();
                        break;
                    case "ban":
                        if (strings.length < 2) {
                            return true;
                        }
                        WardenAPI.ban(commandSender, strings[1], "控制台封禁", -1);
                        break;
                    case "unban":
                        if (strings.length < 2) {
                            return true;
                        }
                        WardenAPI.unban(commandSender, strings[1]);
                        break;
                    case "mute":
                        if (strings.length < 2) {
                            return true;
                        }
                        WardenAPI.mute(commandSender, strings[1], "控制台封禁", -1);
                        break;
                    case "unmute":
                        if (strings.length < 2) {
                            return true;
                        }
                        WardenAPI.unmute(commandSender, strings[1]);
                        break;
                    case "warn":
                        if (strings.length < 3) {
                            return true;
                        }
                        WardenAPI.warn(commandSender, strings[1], strings[2]);
                        break;
                    case "kick":
                        if (strings.length < 2) {
                            return true;
                        }
                        WardenAPI.kick(commandSender, strings[1]);
                        break;
                    case "list":
                        if (MainClass.bugReports.size() > 0) {
                            commandSender.sendMessage("bug反馈：");
                            for (BugReport report : MainClass.bugReports) {
                                commandSender.sendMessage("- 反馈玩家：" + report.getPlayer() + "，反馈内容：" + report.getInfo() + "，日期：" + MainClass.getDate(report.getMillis()) + "，毫秒数：" + report.millis);
                            }
                        } else {
                            commandSender.sendMessage("暂无未处理的bug反馈！");
                        }
                        if (MainClass.byPassReports.size() > 0) {
                            commandSender.sendMessage("bypass反馈：");
                            for (ByPassReport report : MainClass.byPassReports) {
                                commandSender.sendMessage("- 反馈玩家：" + report.getPlayer() + "，被举报玩家，" + report.getSuspect() + "，反馈内容：" + report.getInfo() + "，日期：" + MainClass.getDate(report.getMillis()) + "，毫秒数：" + report.millis);
                            }
                        } else {
                            commandSender.sendMessage("暂无未处理的bug反馈！");
                        }
                        break;
                    case "refreshworkload":
                        if (MainClass.staffData.size() > 0) {
                            for (WardenData value : MainClass.staffData.values()) {
                                value.clearMonthlyWorkload();
                                commandSender.sendMessage("已经成功清空当前绩效，协管绩效报告已发送给各位协管！");
                            }
                        }
                        break;
                    case "workload":
                        if (MainClass.staffData.size() > 0) {
                            log.log(Level.INFO, "CONSOLE执行：/warden workload");
                            Map<String, Integer> cacheMap = new HashMap<>();
                            for (Map.Entry<String, WardenData> entry : MainClass.staffData.entrySet()) {
                                if (entry.getValue().getLevelType() == WardenLevelType.ADMIN) {
                                    continue;
                                }
                                cacheMap.put(entry.getKey(), entry.getValue().getDealBugReportTimes() + entry.getValue().getDealBugReportTimes());
                            }
                            List<Map.Entry<String, Integer>> list = cacheMap.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue)).collect(Collectors.toList());
                            if (list.size() == 0) {
                                log.log(Level.INFO, "CONSOLE执行结果：暂无数据");
                                commandSender.sendMessage("暂无数据");
                            }
                            StringBuilder builder = new StringBuilder("- 最拉协管榜单（Bottom） -");
                            int i = 1;
                            for (Map.Entry<String, Integer> entry : list) {
                                builder.append("\n[").append(i).append("] ").append(entry.getKey()).append(" - ").append(entry.getValue());
                                i++;
                            }

                            Collections.reverse(list);
                            builder.append("\n\n").append("- 优秀协管榜单（Top） -");
                            i = 1;
                            for (Map.Entry<String, Integer> entry : list) {
                                builder.append("\n[").append(i).append("] ").append(entry.getKey()).append(" - ").append(entry.getValue());
                                i++;
                            }
                            commandSender.sendMessage(builder.toString());
                            log.log(Level.INFO, "CONSOLE执行结果：\n" + builder);
                        }
                        break;
                }
            }
            return true;
        }
    }
}
