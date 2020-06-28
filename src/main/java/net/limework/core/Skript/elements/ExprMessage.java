package net.limework.skLimework.elements;


import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import net.limework.skLimework.Events.onRedisMessage;
import org.bukkit.event.Event;

public class ExprMessage extends SimpleExpression<String> {


    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(Event event, boolean b) {
        return "redis message";
    }

    @Override
    public boolean init(Expression<?>[] expressions, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        return true;
    }


    @Override
    protected String[] get(Event e) {
        if (e instanceof onRedisMessage){
            return new String[]{((onRedisMessage) e).getMessage()};
        }
        return null;
    }
}
