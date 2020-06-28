package net.limework.core.events;

import net.limework.skLimework.AddonPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@SuppressWarnings("unused")
public class RedisMessageEvent extends Event {
    private final static HandlerList HANDLERS = new HandlerList();

    private String channelName;
    private String message;

    public RedisMessageEvent(String channelName , String message) {
        super(true);
        this.channelName = channelName;
        AddonPlugin instance = (AddonPlugin) Bukkit.getPluginManager().getPlugin("SKLimework");
        assert instance != null;
        this.message = message;
    }


    @Override
    public String getEventName() {
        return super.getEventName();
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getMessage() {
        return message;
    }
}
