package com.example.vanillavehicles.command;

import com.example.vanillavehicles.VanillaVehicles;
import com.example.vanillavehicles.util.MessageUtil;
import com.example.vanillavehicles.vehicle.TaxiMeter;
import com.example.vanillavehicles.vehicle.Vehicle;
import com.example.vanillavehicles.vehicle.VehicleManager;
import com.example.vanillavehicles.vehicle.VehicleState;
import com.example.vanillavehicles.vehicle.VehicleType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** /vehicle command with subcommands and tab completion. */
public class VehicleCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = Arrays.asList(
            "spawn", "remove", "removeall", "list", "info", "give", "reload",
            "debug", "help", "garage", "siren", "boost", "lights", "horn",
            "exit", "cargo", "fare", "tool");
    private static final List<String> TOOLS = Arrays.asList(
            "ladder", "fork", "dump", "arm", "bucket", "turret");

    private final VanillaVehicles plugin;

    public VehicleCommand(VanillaVehicles plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "list":
                return list(sender, args);
            case "info":
                return info(sender);
            case "spawn":
                return spawn(sender, args);
            case "remove":
                return remove(sender);
            case "removeall":
                return removeAll(sender);
            case "give":
                return give(sender, args);
            case "reload":
                return reload(sender);
            case "debug":
                return debug(sender);
            case "garage":
                return garage(sender);
            case "siren":
                return driverAction(sender, "siren");
            case "boost":
                return driverAction(sender, "boost");
            case "lights":
                return driverAction(sender, "lights");
            case "horn":
                return driverAction(sender, "horn");
            case "exit":
            case "leave":
                return exit(sender);
            case "cargo":
                return cargo(sender);
            case "fare":
                return fare(sender, args);
            case "tool":
                return tool(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        MessageUtil.sendRaw(sender, "&6==== &e&lVehicles &6====");
        MessageUtil.sendRaw(sender, "&e/vehicle spawn <type> &7- spawn a vehicle");
        MessageUtil.sendRaw(sender, "&e/vehicle garage &7- browse and spawn vehicles");
        MessageUtil.sendRaw(sender, "&e/vehicle remove &7- remove your vehicle");
        MessageUtil.sendRaw(sender, "&e/vehicle removeall &7- remove every vehicle (admin)");
        MessageUtil.sendRaw(sender, "&e/vehicle list [mine] &7- list active vehicles");
        MessageUtil.sendRaw(sender, "&e/vehicle info &7- inspect your vehicle");
        MessageUtil.sendRaw(sender, "&e/vehicle give <type> [player] &7- spawner item");
        MessageUtil.sendRaw(sender, "&e/vehicle siren|boost|lights|horn &7- driver actions");
        MessageUtil.sendRaw(sender, "&e/vehicle tool <action> &7- ladder/fork/dump/arm/bucket/turret");
        MessageUtil.sendRaw(sender, "&e/vehicle cargo &7- open vehicle storage");
        MessageUtil.sendRaw(sender, "&e/vehicle fare [reset] &7- taxi meter");
        MessageUtil.sendRaw(sender, "&e/vehicle exit &7- get out");
        MessageUtil.sendRaw(sender, "&e/vehicle debug &7- toggle debug readout");
        MessageUtil.sendRaw(sender, "&e/vehicle reload &7- reload config");
        MessageUtil.sendRaw(sender, "&7Types: &f" + String.join(", ", VehicleType.ids()));
    }

    private boolean check(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        MessageUtil.send(sender, "&cYou lack permission &f" + permission + "&c.");
        return false;
    }

    private Player needPlayer(CommandSender sender) {
        if (sender instanceof Player) {
            return (Player) sender;
        }
        MessageUtil.send(sender, "&cPlayers only.");
        return null;
    }

    private boolean list(CommandSender sender, String[] args) {
        boolean mineOnly = args.length > 1 && args[1].equalsIgnoreCase("mine");
        VehicleManager manager = plugin.getVehicleManager();
        List<Vehicle> vehicles = new ArrayList<>(manager.getAll());
        if (mineOnly && sender instanceof Player) {
            vehicles = manager.vehiclesOf((Player) sender);
        }
        if (vehicles.isEmpty()) {
            MessageUtil.send(sender, "&7No active vehicles.");
            return true;
        }
        MessageUtil.sendRaw(sender, "&6Active vehicles (" + vehicles.size() + "):");
        for (Vehicle vehicle : vehicles) {
            Player driver = vehicle.getDriver();
            MessageUtil.sendRaw(sender, String.format("&e%s &7[%s] &fHP %.0f &7riders %d &7driver %s &7owner %s",
                    vehicle.getType().getDisplayName(),
                    vehicle.getId().toString().substring(0, 8),
                    vehicle.getHealth(), vehicle.riderCount(),
                    driver == null ? "-" : driver.getName(), vehicle.getOwnerName()));
        }
        return true;
    }

    private boolean info(CommandSender sender) {
        Player player = needPlayer(sender);
        if (player == null) {
            return true;
        }
        Vehicle vehicle = plugin.getVehicleManager().getVehicleOf(player);
        if (vehicle == null) {
            vehicle = plugin.getVehicleManager().nearestVehicle(player.getLocation(), 8, null);
        }
        if (vehicle == null) {
            MessageUtil.send(sender, "&cNo vehicle nearby. Ride one or stand close to it.");
            return true;
        }
        MessageUtil.sendRaw(sender, "&6==== &e&l" + vehicle.getType().getDisplayName() + " &6====");
        MessageUtil.sendRaw(sender, "&7ID: &f" + vehicle.getId());
        MessageUtil.sendRaw(sender, "&7Class: &f" + vehicle.getType().getTier().getDisplayName()
                + " &7(" + vehicle.getType().getCategory().getDisplayName() + ")");
        MessageUtil.sendRaw(sender, String.format("&7Speed: &f%.1f m/s &7(top %.0f)",
                vehicle.getSpeed(), vehicle.getStats().maxSpeed));
        MessageUtil.sendRaw(sender, String.format("&7Health: &f%.0f/%.0f",
                vehicle.getHealth(), vehicle.getStats().maxHealth));
        MessageUtil.sendRaw(sender, "&7State: &f" + vehicle.getState()
                + (vehicle.isGrounded() ? " &7(ground)" : " &7(airborne)")
                + (vehicle.isInWater() ? " &b(water)" : ""));
        Player driver = vehicle.getDriver();
        MessageUtil.sendRaw(sender, "&7Driver: &f" + (driver == null ? "-" : driver.getName()));
        MessageUtil.sendRaw(sender, "&7Riders: &f" + vehicle.riderCount()
                + "/" + (vehicle.getDefinition().getModel().seatCount()
                + vehicle.getCarriages().size() * 6));
        MessageUtil.sendRaw(sender, "&7Owner: &f" + vehicle.getOwnerName());
        MessageUtil.sendRaw(sender, "&7" + vehicle.getType().getDescription());
        return true;
    }

    private boolean spawn(CommandSender sender, String[] args) {
        if (!check(sender, "vehicle.spawn")) {
            return true;
        }
        Player player = needPlayer(sender);
        if (player == null) {
            return true;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, "&cUsage: /vehicle spawn <type>");
            return true;
        }
        VehicleType type = VehicleType.byId(args[1]);
        if (type == null) {
            MessageUtil.send(sender, "&cUnknown type. See /vehicle help.");
            return true;
        }
        VehicleManager manager = plugin.getVehicleManager();
        if (!manager.canSpawn(player, type)) {
            return true;
        }
        Location base = player.getLocation();
        Vector direction = base.getDirection();
        Location spawn = new Location(base.getWorld(),
                base.getX() + direction.getX() * 3.5, base.getY(), base.getZ() + direction.getZ() * 3.5,
                base.getYaw(), 0f);
        Vehicle vehicle = manager.spawn(type, spawn, player);
        if (vehicle != null) {
            MessageUtil.send(sender, "&aSpawned &f" + type.getDisplayName() + "&a. Right-click it to board.");
            plugin.getSoundManager().chime(player);
        }
        return true;
    }

    private boolean remove(CommandSender sender) {
        if (!check(sender, "vehicle.remove")) {
            return true;
        }
        Player player = needPlayer(sender);
        if (player == null) {
            return true;
        }
        VehicleManager manager = plugin.getVehicleManager();
        Vehicle vehicle = manager.getVehicleOf(player);
        if (vehicle == null) {
            vehicle = manager.nearestVehicle(player.getLocation(), 8, player);
        }
        if (vehicle == null) {
            MessageUtil.send(sender, "&cNo vehicle to remove.");
            return true;
        }
        if (!player.getUniqueId().equals(vehicle.getOwner()) && !player.hasPermission("vehicle.admin")) {
            MessageUtil.send(sender, "&cThat is not your vehicle.");
            return true;
        }
        vehicle.removeQuiet();
        MessageUtil.send(sender, "&aVehicle removed.");
        plugin.getSoundManager().click(player);
        return true;
    }

    private boolean removeAll(CommandSender sender) {
        if (!check(sender, "vehicle.admin")) {
            return true;
        }
        int count = plugin.getVehicleManager().getAll().size();
        plugin.getVehicleManager().removeAll();
        MessageUtil.send(sender, "&aRemoved " + count + " vehicles.");
        return true;
    }

    private boolean give(CommandSender sender, String[] args) {
        if (!check(sender, "vehicle.give")) {
            return true;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, "&cUsage: /vehicle give <type> [player]");
            return true;
        }
        VehicleType type = VehicleType.byId(args[1]);
        if (type == null) {
            MessageUtil.send(sender, "&cUnknown type. See /vehicle help.");
            return true;
        }
        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                MessageUtil.send(sender, "&cPlayer not found.");
                return true;
            }
        } else {
            target = needPlayer(sender);
            if (target == null) {
                return true;
            }
        }
        ItemStack item = plugin.getVehicleManager().createSpawnerItem(type);
        target.getInventory().addItem(item).values().forEach(left ->
                target.getWorld().dropItemNaturally(target.getLocation(), left));
        MessageUtil.send(sender, "&aGave &f" + type.getDisplayName() + " spawner &ato &f" + target.getName() + "&a.");
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!check(sender, "vehicle.reload")) {
            return true;
        }
        plugin.getPluginConfig().reload();
        MessageUtil.send(sender, "&aConfiguration reloaded. (Already spawned vehicles keep their stats.)");
        return true;
    }

    private boolean debug(CommandSender sender) {
        if (!check(sender, "vehicle.admin")) {
            return true;
        }
        Player player = needPlayer(sender);
        if (player == null) {
            return true;
        }
        boolean enabled = plugin.toggleDebug(player);
        MessageUtil.send(sender, enabled ? "&aDebug on." : "&7Debug off.");
        return true;
    }

    private boolean garage(CommandSender sender) {
        if (!check(sender, "vehicle.garage")) {
            return true;
        }
        Player player = needPlayer(sender);
        if (player == null) {
            return true;
        }
        plugin.getGarageManager().open(player);
        return true;
    }

    private boolean driverAction(CommandSender sender, String action) {
        Player player = needPlayer(sender);
        if (player == null) {
            return true;
        }
        Vehicle vehicle = plugin.getVehicleManager().getVehicleOf(player);
        if (vehicle == null || vehicle.getState() != VehicleState.ACTIVE) {
            MessageUtil.send(sender, "&cYou are not in a vehicle.");
            return true;
        }
        if (!vehicle.isDriver(player)) {
            MessageUtil.send(sender, "&cOnly the driver can do that.");
            return true;
        }
        switch (action) {
            case "siren":
                vehicle.toggleSiren();
                break;
            case "boost":
                vehicle.boost();
                break;
            case "lights":
                vehicle.toggleLights();
                break;
            case "horn":
            default:
                vehicle.horn();
                break;
        }
        return true;
    }

    private boolean exit(CommandSender sender) {
        Player player = needPlayer(sender);
        if (player == null) {
            return true;
        }
        Vehicle vehicle = plugin.getVehicleManager().getVehicleOf(player);
        if (vehicle == null) {
            MessageUtil.send(sender, "&cYou are not in a vehicle.");
            return true;
        }
        vehicle.exit(player);
        return true;
    }

    private boolean cargo(CommandSender sender) {
        Player player = needPlayer(sender);
        if (player == null) {
            return true;
        }
        Vehicle vehicle = plugin.getVehicleManager().getVehicleOf(player);
        if (vehicle == null) {
            vehicle = plugin.getVehicleManager().nearestVehicle(player.getLocation(), 6, player);
        }
        if (vehicle == null) {
            MessageUtil.send(sender, "&cNo vehicle nearby.");
            return true;
        }
        vehicle.openCargo(player);
        return true;
    }

    private boolean fare(CommandSender sender, String[] args) {
        Player player = needPlayer(sender);
        if (player == null) {
            return true;
        }
        Vehicle vehicle = plugin.getVehicleManager().getVehicleOf(player);
        if (vehicle == null || vehicle.getType() != VehicleType.TAXI) {
            MessageUtil.send(sender, "&cRide a taxi to use the fare meter.");
            return true;
        }
        TaxiMeter meter = vehicle.getTaxiMeter();
        if (meter == null) {
            return true;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("reset")) {
            if (!vehicle.isDriver(player)) {
                MessageUtil.send(sender, "&cOnly the driver can reset the meter.");
                return true;
            }
            meter.reset();
            MessageUtil.send(sender, "&aFare meter reset.");
            return true;
        }
        MessageUtil.send(sender, String.format("&eFare: &f%.2f &7(%d blocks)",
                meter.fare(), (int) meter.getDistance()));
        return true;
    }

    private boolean tool(CommandSender sender, String[] args) {
        Player player = needPlayer(sender);
        if (player == null) {
            return true;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, "&cUsage: /vehicle tool <ladder|fork|dump|arm|bucket|turret>");
            return true;
        }
        Vehicle vehicle = plugin.getVehicleManager().getVehicleOf(player);
        if (vehicle == null) {
            MessageUtil.send(sender, "&cYou are not in a vehicle.");
            return true;
        }
        if (!vehicle.isDriver(player)) {
            MessageUtil.send(sender, "&cOnly the driver can do that.");
            return true;
        }
        if (!vehicle.tool(args[1])) {
            MessageUtil.send(sender, "&cThat tool does not apply to this vehicle.");
            plugin.getSoundManager().deny(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], SUBS, completions);
        } else if (args.length == 2
                && (args[0].equalsIgnoreCase("spawn") || args[0].equalsIgnoreCase("give"))) {
            StringUtil.copyPartialMatches(args[1], VehicleType.ids(), completions);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("tool")) {
            StringUtil.copyPartialMatches(args[1], TOOLS, completions);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            StringUtil.copyPartialMatches(args[2], names, completions);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
            StringUtil.copyPartialMatches(args[1], Arrays.asList("mine", "all"), completions);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("fare")) {
            StringUtil.copyPartialMatches(args[1], Arrays.asList("reset"), completions);
        }
        return completions;
    }
}
