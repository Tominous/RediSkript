package net.limework.rediskript.skript.elements;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.variables.SerializedVariable;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import net.limework.rediskript.RediSkript;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;

import java.util.Base64;

public class ExprVariableInChannel extends SimpleExpression<Object> {
    private Expression<String> name;
    private Expression<String> channel;
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        name = (Expression<String>) expressions[0];
        channel = (Expression<String>) expressions[1];
        return true;

    }

    @Override
    protected Object[] get(Event event) {
        return null;
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends Object> getReturnType() {
        return Object.class;
    }

    @Override
    public String toString(Event event, boolean b) {
        return null;
    }
    @Override
    public void change(Event e, Object[] changer, Changer.ChangeMode mode) {
        RediSkript plugin = (RediSkript) Bukkit.getPluginManager().getPlugin("RediSkript");
        switch (mode) {
            case SET:
                SerializedVariable.Value serialized = Classes.serialize(changer[0]);
                String encoded = Base64.getEncoder().encodeToString(serialized.data);
                encoded = serialized.type + "^" + encoded;
                plugin.getRm().sendVariables(name.getAll(e), encoded, channel.getSingle(e));
                break;
            case DELETE:
                plugin.getRm().sendVariables(name.getAll(e), null, channel.getSingle(e));
                break;
        }
    }
    @Override
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.DELETE || mode == Changer.ChangeMode.SET)
            return CollectionUtils.array(Object.class);
        return null;
    }
}