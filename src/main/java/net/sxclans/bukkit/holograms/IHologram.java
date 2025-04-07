package net.sxclans.bukkit.holograms;

import org.bukkit.Location;

public interface IHologram {
    void create();
    void update();
    void delete();
    void relocate(Location newLocation);
    boolean isEnabled();
    Location getLocation();
    String getId();
}