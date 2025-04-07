package net.sxclans.bukkit.holograms;

import java.util.List;

public interface IHologramDataProvider<T> {
    List<T> getData();
    long getUpdateInterval();
}