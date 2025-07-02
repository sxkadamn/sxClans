package net.sxclans.common.clan.war.arena;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;


public class Arena {
    private static List<Arena> arenaList = new ArrayList<>();
    private String name;
    private World world;
    private Location clan1Spawns;
    private Location clan2Spawns;
    private boolean open;

    public Arena(String name) {
        this.name = name;
        arenaList.add(this);
    }


    public String getName() {
        return name;
    }

    public World getWorld() {
        return world;
    }


    public boolean launch() {
        return (this.clan1Spawns
                != null
                && this.clan2Spawns
                != null
                && this.clan1Spawns.distance(this.clan2Spawns) >= 100.0D
                && (this.open = true));
    }

    public static List<Arena> getArenas() {
        return arenaList;
    }

    public static Arena add(String name) {
        return new Arena(name);
    }


    public static Arena get(String name) {
        return arenaList.stream().filter(arena -> arena.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public Location getClan1Spawns() {
        return clan1Spawns;
    }

    public Location getClan2Spawns() {
        return clan2Spawns;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public void setClan1Spawns(Location clan1Spawns) {
        this.clan1Spawns = clan1Spawns;
    }

    public void setClan2Spawns(Location clan2Spawns) {
        this.clan2Spawns = clan2Spawns;
    }
}