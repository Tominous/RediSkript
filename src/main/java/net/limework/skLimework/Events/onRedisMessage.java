package net.limework.skLimework.Events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


public class onRedisMessage extends Event {
    private final static HandlerList HANDLERS = new HandlerList();

    private String channelName;
    private String message;

    onRedisMessage(String channelName , String message) {
        super(true);
        this.channelName = channelName;
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