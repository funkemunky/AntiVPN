package dev.brighten.antivpn.api;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.brighten.antivpn.AntiVPN;
import lombok.Getter;
import lombok.Setter;

import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

@Getter
public abstract class APIPlayer {
    private final UUID uuid;
    private final String name;
    private final InetAddress ip;
    @Setter
    private boolean alertsEnabled;

    private static final Cache<String, CheckResult> checkResultCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(2000)
            .build();

    public APIPlayer(UUID uuid, String name, InetAddress ip) {
        this.uuid = uuid;
        this.name = name;
        this.ip = ip;
    }

    public abstract void sendMessage(String message);

    public abstract void kickPlayer(String reason);

    public abstract boolean hasPermission(String permission);

    public void updateAlertsState() {
        //Updating into database so its synced across servers and saved on logout.
        AntiVPN.getInstance().getDatabase().updateAlertsState(uuid, alertsEnabled);
    }

    public CheckResult checkPlayer(Consumer<CheckResult> onKick) {
        if (hasPermission("antivpn.bypass") //Has bypass permission
                //Is exempt
                || (uuid != null && AntiVPN.getInstance().getExecutor().isWhitelisted(uuid))
                //Or has a name that starts with a certain prefix. This is for Bedrock exempting.
                || AntiVPN.getInstance().getExecutor().isWhitelisted(ip.getHostAddress() + "/32")
                || AntiVPN.getInstance().getVpnConfig().getPrefixWhitelists().stream()
                .anyMatch(name::startsWith)) return new CheckResult(null, ResultType.WHITELISTED);

        CheckResult cachedResult = checkResultCache.getIfPresent(ip.getHostAddress());

        if(cachedResult != null) {
            if(cachedResult.response().getIp().equals(ip.getHostAddress())) {
                AntiVPN.getInstance().getExecutor().log(Level.FINE, "Cached result for " + ip.getHostAddress() + " is " + cachedResult.resultType());
                return cachedResult;
            }
        }

        AntiVPN.getInstance().getExecutor().checkIp(ip.getHostAddress())
                .thenAccept(result -> {
                    if(!result.isSuccess()) {
                        AntiVPN.getInstance().getExecutor().log(Level.WARNING, "The API query was not a success! " +
                                "You may need to upgrade your license on " +
                                "https://funkemunky.cc/shop");
                    }
                    // If the countryList() size is zero, no need to check.
                    // Running country check first
                    CheckResult checkResult;
                    if (!AntiVPN.getInstance().getVpnConfig().countryList().isEmpty()
                            && !((uuid != null && AntiVPN.getInstance().getExecutor()
                            .isWhitelisted(uuid))
                            //Or has a name that starts with a certain prefix. This is for Bedrock exempting.
                            || AntiVPN.getInstance().getExecutor().isWhitelisted(ip.getHostAddress() + "/32"))
                            // This bit of code will decide whether or not to kick the player
                            // If it contains the code and it is set to whitelist, it will not kick
                            // as they are equal and vise versa. However, if the contains does not match
                            // the state, it will kick.
                            && AntiVPN.getInstance().getVpnConfig().countryList()
                            .contains(result.getCountryCode())
                            != AntiVPN.getInstance().getVpnConfig().whitelistCountries()) {
                        //Using our built in kicking system if no commands are configured
                        checkResult = new CheckResult(result, ResultType.DENIED_COUNTRY);
                    } else if (result.isProxy()) {
                        checkResult = new CheckResult(result, ResultType.DENIED_PROXY);
                    } else {
                        checkResult = new CheckResult(result, ResultType.ALLOWED);
                    }

                    AntiVPN.getInstance().getExecutor().log(Level.FINE, "Result for " + ip.getHostAddress() + " is " + checkResult.resultType());

                    checkResultCache.put(ip.getHostAddress(), checkResult);
                    onKick.accept(checkResult);
                    AntiVPN.getInstance().checked++;
                });
        return new CheckResult(null, ResultType.UNKNOWN);
    }
}
