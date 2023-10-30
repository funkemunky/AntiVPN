package dev.brighten.antivpn.command.impl;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.VPNExecutor;
import dev.brighten.antivpn.command.Command;
import dev.brighten.antivpn.command.CommandExecutor;
import dev.brighten.antivpn.utils.StringUtil;
import dev.brighten.antivpn.utils.json.JSONException;
import dev.brighten.antivpn.web.FunkemunkyAPI;
import dev.brighten.antivpn.web.objects.QueryResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class PlanCommand extends Command {
    @Override
    public String permission() {
        return "antivpn.command.plan";
    }

    @Override
    public String name() {
        return "plan";
    }

    @Override
    public String[] aliases() {
        return new String[] {"queries", "query"};
    }

    @Override
    public String description() {
        return "Info related to KauriVPN Plan";
    }

    @Override
    public String usage() {
        return "";
    }

    @Override
    public String parent() {
        return "antivpn";
    }

    @Override
    public Command[] children() {
        return new Command[0];
    }

    @Override
    public String execute(CommandExecutor executor, String[] args) {
        VPNExecutor.threadExecutor.execute(() -> {
            QueryResponse result;
            try {
                if(AntiVPN.getInstance().getVpnConfig().getLicense().equals("")) {
                    result = FunkemunkyAPI.getQueryResponse();
                } else {
                    result = FunkemunkyAPI.getQueryResponse(AntiVPN.getInstance().getVpnConfig().getLicense());

                    if(!result.isValidPlan()) {
                        executor.sendMessage("&cThe license &f%s &cis not a valid license, " +
                                        "checking your Free plan information...",
                                AntiVPN.getInstance().getVpnConfig().getLicense());

                        result = FunkemunkyAPI.getQueryResponse();
                    }
                }

                String plan = result.getPlanType();
                if(plan.equals("IP")) plan+= " (Free)";

                String queryMax = result.getQueriesMax() == Long.MAX_VALUE
                        ? "Unlimited" : String.valueOf(result.getQueriesMax());

                executor.sendMessage(StringUtil.line("&8"));
                executor.sendMessage("&6&lKauriVPN Plan Information");
                executor.sendMessage("");
                executor.sendMessage("&e%s&8: &f%s", "Plan", plan);
                executor.sendMessage("&e%s&8: &f%s&7/&f%s", "Queries Used",
                        result.getQueries(), queryMax);
                executor.sendMessage(StringUtil.line("&8"));
            } catch(JSONException e) {
                e.printStackTrace();
                executor.sendMessage("&cThere was a JSONException thrown while looking up your query " +
                        "information. Check console for more details.");
            } catch (IOException e) {
                e.printStackTrace();
                executor.sendMessage("&cThere was a IOException thrown while looking up your query " +
                        "information. Check console for more details.");
            }
        });
        return "&7Looking up your query information...";
    }

    @Override
    public List<String> tabComplete(CommandExecutor executor, String alias, String[] args) {
        return null;
    }
}
