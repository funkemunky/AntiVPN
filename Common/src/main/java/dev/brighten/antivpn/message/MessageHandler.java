package dev.brighten.antivpn.message;

import dev.brighten.antivpn.AntiVPN;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class MessageHandler {
    private final Map<String, VpnString> messages = new HashMap<>();

    public VpnString getString(String key) {
        if(!messages.containsKey(key)) {
            throw new NullPointerException("There is no VpnString with the key \"" + key + "\"");
        }

        return messages.get(key);
    }

    public void reloadStrings() {
        for (VpnString value : messages.values()) {
            value.updateString();
        }
    }

    public void clearStrings() {
        messages.clear();
    }

    public void addString(VpnString string, Function<VpnString, String> getter) {
        string.setConfigStringGetter(getter);
        getter.apply(string);
        AntiVPN.getInstance().getExecutor().log("Added string " + string.getKey());
        messages.put(string.getKey(), string);
    }

    public void initStrings(Function<VpnString, String> getter) {
        addString(new VpnString("command-misc-playerRequired",
                "&cYou must be a player to execute this command!"), getter);
        addString(new VpnString("command-alerts-toggled",
                "&7Your player proxy notifications have been set to: &e%state%"), getter);
        addString(new VpnString("command-reload-complete",
                        "&aSuccessfully reloaded KauriVPN plugin!"), getter);
        addString(new VpnString("no-permission", "&cNo permission."), getter);
    }
}
