package com.wavemotorcycle.commands;

import com.wavemotorcycle.WaveMotorcyclePlugin;
import com.wavemotorcycle.motorcycle.MotorcycleController;
import com.wavemotorcycle.motorcycle.pack.ResourcePackManager;
import com.wavemotorcycle.util.MathUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * /wave command with subcommands and tab completion.
 */
public final class WaveCommand implements TabExecutor {

    private final WaveMotorcyclePlugin plugin;

    public WaveCommand(WaveMotorcyclePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        WaveMotorcyclePlugin plugin = this.plugin;
        boolean isPlayer = sender instanceof Player;
        Player player = isPlayer ? (Player) sender : null;

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help":
                sendHelp(sender);
                break;
            case "give": {
                if (!sender.hasPermission("wavemotorcycle.give")) {
                    sender.sendMessage(plugin.cfg().msgC("msg.no_permission"));
                    return true;
                }
                Player target = args.length > 1 ? Bukkit.getPlayerExact(args[1]) : player;
                if (target == null) {
                    sender.sendMessage(plugin.cfg().msgC("msg.player_not_found"));
                    return true;
                }
                target.getInventory().addItem(plugin.manager().keyItem().create(null));
                target.sendMessage(plugin.cfg().msgC("msg.given"));
                if (target != sender) {
                    sender.sendMessage(plugin.cfg().msg("msg.gave").replace("<player>", target.getName()));
                }
                break;
            }
            case "spawn": {
                if (!sender.hasPermission("wavemotorcycle.spawn")) {
                    sender.sendMessage(plugin.cfg().msgC("msg.no_permission"));
                    return true;
                }
                Player target = args.length > 1 ? Bukkit.getPlayerExact(args[1]) : player;
                if (target == null) {
                    sender.sendMessage(plugin.cfg().msgC("msg.player_not_found"));
                    return true;
                }
                if (plugin.manager().ofPlayer(target) != null) {
                    target.sendMessage(plugin.cfg().msgC("msg.already_riding"));
                    return true;
                }
                Location here = target.getLocation().clone();
                // Spawn two blocks ahead on the horizontal plane (ignore look pitch).
                here.add(new Vector(MathUtil.dirX(here.getYaw()) * 2.0, 0, MathUtil.dirZ(here.getYaw()) * 2.0));
                here.setX(here.getX() + 0.5);
                here.setZ(here.getZ() + 0.5);
                here.setPitch(0f);
                plugin.manager().spawn(here, target, null);
                target.sendMessage(plugin.cfg().msgC("msg.spawned"));
                break;
            }
            case "remove": {
                if (!sender.hasPermission("wavemotorcycle.admin")) {
                    sender.sendMessage(plugin.cfg().msgC("msg.no_permission"));
                    return true;
                }
                if (!isPlayer) {
                    sender.sendMessage(plugin.cfg().msgC("msg.players_only"));
                    return true;
                }
                MotorcycleController bike = plugin.manager().nearest(player.getLocation(), 4.0);
                if (bike == null) {
                    player.sendMessage(plugin.cfg().msgC("msg.no_bike_near"));
                    return true;
                }
                bike.dismount(MotorcycleController.DismountReason.VANILLA);
                plugin.manager().remove(bike, false);
                plugin.manager().keyItem().dropAt(bike.location(), bike.state().uuid());
                player.sendMessage(plugin.cfg().msgC("msg.removed"));
                break;
            }
            case "reload": {
                if (!sender.hasPermission("wavemotorcycle.reload")) {
                    sender.sendMessage(plugin.cfg().msgC("msg.no_permission"));
                    return true;
                }
                plugin.reloadAll();
                sender.sendMessage(plugin.cfg().msgC("msg.reloaded"));
                break;
            }
            case "fuel": {
                if (player == null) {
                    sender.sendMessage(plugin.cfg().msgC("msg.players_only"));
                    return true;
                }
                Player target = args.length > 1 ? Bukkit.getPlayerExact(args[1]) : player;
                if (target == null) {
                    sender.sendMessage(plugin.cfg().msgC("msg.player_not_found"));
                    return true;
                }
                if (!target.equals(player) && !sender.hasPermission("wavemotorcycle.admin")) {
                    sender.sendMessage(plugin.cfg().msgC("msg.no_permission"));
                    return true;
                }
                MotorcycleController bike = plugin.manager().ofPlayer(target);
                if (bike == null) {
                    bike = plugin.manager().nearest(target.getLocation(), 4.0);
                }
                if (bike == null) {
                    sender.sendMessage(plugin.cfg().msgC("msg.no_bike_near"));
                    return true;
                }
                if (!plugin.cfg().fuelEnabled) {
                    sender.sendMessage(plugin.cfg().msgC("msg.fuel_disabled"));
                    return true;
                }
                sender.sendMessage(plugin.cfg().msg("msg.fuel")
                        .replace("<fuel>", String.valueOf((int) Math.ceil(bike.fuel())))
                        .replace("<capacity>", String.valueOf((int) plugin.cfg().fuelCapacity)));
                break;
            }
            case "refuel": {
                if (!sender.hasPermission("wavemotorcycle.admin")) {
                    sender.sendMessage(plugin.cfg().msgC("msg.no_permission"));
                    return true;
                }
                Player target = args.length > 1 ? Bukkit.getPlayerExact(args[1]) : player;
                if (target == null) {
                    sender.sendMessage(plugin.cfg().msgC("msg.player_not_found"));
                    return true;
                }
                MotorcycleController bike = plugin.manager().ofPlayer(target);
                if (bike == null) {
                    bike = plugin.manager().nearest(target.getLocation(), 4.0);
                }
                if (bike == null) {
                    sender.sendMessage(plugin.cfg().msgC("msg.no_bike_near"));
                    return true;
                }
                refuel(bike);
                sender.sendMessage(plugin.cfg().msgC("msg.refueled"));
                break;
            }
            case "pack": {
                if (!sender.hasPermission("wavemotorcycle.admin")) {
                    sender.sendMessage(plugin.cfg().msgC("msg.no_permission"));
                    return true;
                }
                ResourcePackManager pack = plugin.packManager();
                if (!pack.canSend()) {
                    sender.sendMessage(plugin.cfg().msgC("msg.pack_not_configured"));
                    break;
                }
                if (args.length > 1) {
                    Player target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) {
                        sender.sendMessage(plugin.cfg().msgC("msg.player_not_found"));
                        break;
                    }
                    pack.sendPack(target);
                    sender.sendMessage(plugin.cfg().msg("msg.pack_sent").replace("<player>", target.getName()));
                } else if (isPlayer) {
                    pack.sendPack(player);
                    sender.sendMessage(plugin.cfg().msgC("msg.pack_sent_self"));
                } else {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        pack.sendPack(p);
                    }
                    sender.sendMessage(plugin.cfg().msgC("msg.pack_sent_all"));
                }
                break;
            }
            case "light": {
                if (player == null) {
                    sender.sendMessage(plugin.cfg().msgC("msg.players_only"));
                    return true;
                }
                MotorcycleController bike = plugin.manager().ofPlayer(player);
                if (bike == null) {
                    player.sendMessage(plugin.cfg().msgC("msg.not_riding"));
                    return true;
                }
                boolean on = bike.toggleHeadlight();
                player.sendMessage(plugin.cfg().msgC(on ? "msg.light_on" : "msg.light_off"));
                break;
            }
            case "debug": {
                if (!sender.hasPermission("wavemotorcycle.admin")) {
                    sender.sendMessage(plugin.cfg().msgC("msg.no_permission"));
                    return true;
                }
                if (!isPlayer) {
                    sender.sendMessage(plugin.cfg().msgC("msg.players_only"));
                    return true;
                }
                MotorcycleController bike = plugin.manager().ofPlayer(player);
                if (bike == null) {
                    bike = plugin.manager().nearest(player.getLocation(), 4.0);
                }
                if (bike == null) {
                    player.sendMessage(plugin.cfg().msgC("msg.no_bike_near"));
                    return true;
                }
                Location l = bike.location();
                player.sendMessage("§6=== Wave debug ===");
                player.sendMessage("§7ID: §f" + bike.state().uuid());
                player.sendMessage("§7Speed: §f" + String.format("%.3f", bike.speed()) + " blocks/tick");
                player.sendMessage("§7Fuel: §f" + (int) Math.ceil(bike.fuel()) + "/" + (int) plugin.cfg().fuelCapacity);
                player.sendMessage("§7Health: §f" + (int) Math.ceil(bike.health()));
                player.sendMessage("§7Wheelie: §f" + String.format("%.1f", bike.wheelieAngle()) + "° (" + bike.wheelieState() + ")");
                player.sendMessage("§7Engine: §f" + bike.engineState());
                player.sendMessage("§7Headlight: §f" + (bike.headlightOn() ? "ON" : "OFF"));
                player.sendMessage("§7Airborne: §f" + bike.airborne());
                player.sendMessage("§7Anim: §f" + bike.animation().primary());
                player.sendMessage(String.format("§7Position: §f%.1f, %.1f, %.1f (yaw %.1f) %s",
                        l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getWorld().getName()));
                break;
            }
            case "removeall": {
                if (!sender.hasPermission("wavemotorcycle.admin")) {
                    sender.sendMessage(plugin.cfg().msgC("msg.no_permission"));
                    return true;
                }
                int count = plugin.manager().size();
                plugin.manager().removeAll();
                sender.sendMessage(plugin.cfg().msg("msg.removed_all").replace("<count>", String.valueOf(count)));
                break;
            }
            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void refuel(MotorcycleController bike) {
        bike.refuel();
        if (bike.rider() != null) {
            bike.rider().sendMessage(plugin.cfg().msgC("msg.refueled"));
        }
    }

    private void sendHelp(CommandSender sender) {
        WaveMotorcyclePlugin plugin = this.plugin;
        sender.sendMessage("§6=== §eWave Motorcycle §6=== ");
        sender.sendMessage("§e/wave give [player] §7- give a motorcycle key");
        sender.sendMessage("§e/wave spawn §7- spawn a motorcycle in front of you");
        sender.sendMessage("§e/wave remove §7- remove the nearest motorcycle");
        sender.sendMessage("§e/wave fuel [player] §7- show fuel");
        sender.sendMessage("§e/wave refuel [player] §7- refuel (admin)");
        sender.sendMessage("§e/wave pack [player] §7- send the resource pack (admin)");
        sender.sendMessage("§e/wave light §7- toggle your headlight");
        sender.sendMessage("§e/wave debug §7- bike diagnostics (admin)");
        sender.sendMessage("§e/wave removeall §7- remove every bike (admin)");
        sender.sendMessage("§e/wave reload §7- reload configuration");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            String[] subs = {"give", "spawn", "remove", "reload", "fuel", "refuel", "pack", "light", "debug", "removeall", "help"};
            for (String s : subs) {
                if (s.startsWith(partial)) {
                    out.add(s);
                }
            }
            return out;
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("give") || sub.equals("spawn") || sub.equals("fuel") || sub.equals("refuel") || sub.equals("pack")) {
                String partial = args[1].toLowerCase(Locale.ROOT);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase(Locale.ROOT).startsWith(partial)) {
                        out.add(p.getName());
                    }
                }
            }
        }
        return out;
    }
}
