package net.limework.core.Skript.elements;
;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import org.bukkit.event.Event;

public class EvtRedis extends SkriptEvent {



    @Override
    public boolean init(Literal<?>[] literals, int i, SkriptParser.ParseResult parseResult) {

        return true;
    }

    @Override
    public boolean check(Event event) {
        return true;
    }

    @Override
    public String toString(Event event, boolean b) {
        return "redis message";
    }
}
