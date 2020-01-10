package dev.brighten.pl.listeners.impl;

import dev.brighten.pl.vpn.VPNResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

@RequiredArgsConstructor
@Getter
public class VPNCheckEvent extends Event {
    private UUID uuid;
    private final VPNResponse response;
    private static HandlerList handlerList = new HandlerList();

    public HandlerList getHandlers() {
        return handlerList;
    }
}
