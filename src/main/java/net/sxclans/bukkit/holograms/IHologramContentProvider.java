package net.sxclans.bukkit.holograms;

import java.util.List;

public interface IHologramContentProvider<T> {
    List<String> getHeader();
    List<String> getLines(List<T> data);
    List<String> getFooter();
}