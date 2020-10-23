package net.limework.core.skript.elements;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import net.limework.core.events.RedisMessageEvent;
import org.bukkit.event.Event;

public class EvtRedis extends SkriptEvent {

    @Override
    public boolean init(Literal<?>[] literals, int i, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event event) {
        if (!(event instanceof RedisMessageEvent)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "redis message";
    }
}
