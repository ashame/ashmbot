package net.ashame.irc.bot.messages;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import net.ashame.irc.bot.Bot;
import net.ashame.irc.bot.Main;
import org.jibble.pircbot.User;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Nathan on 1/4/14.
 * http://www.powerbot.org/community/user/523484-nathan-l/
 * http://www.excobot.org/forum/user/906-nathan/
 */
@SuppressWarnings("ALL")
public class CommandProcessor {

    protected final Bot bot;
    protected final Map<String, String> commands = new LinkedHashMap<>();
    protected final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("hh:mm:ss a z");
    protected long lastUse;

    public CommandProcessor(final Bot bot) {
        this.bot = bot;
    }

    public Map<String, String> getCommandMap() {
        return commands;
    }

    public void processCommand(final char heading, final String[] sub, final String channel, final User sender) {
        try {
            if (commands.containsKey(sub[0].toLowerCase())) {
                String[] msg = commands.get(sub[0]).split(":", 2);
                if (msg[0].equalsIgnoreCase("raw")) {
                    bot.sendRawLine(msg[1]);
                    bot.log("Sending RAW line: " + msg[1]);
                    return;
                }
                bot.sendMessage(channel, commands.get(sub[0]));
                return;
            }
            if (sub[0].equalsIgnoreCase("add") && hasPowers(sender)) {
                if (sub.length == 1) {
                    return;
                }
                if (!commands.containsKey(sub[1])) {
                    StringBuilder sb = new StringBuilder("");
                    for (int i = 0; i < (sub.length - 2); i++) {
                        sb.append(sub[i + 2]).append(" ");
                    }
                    bot.sendMessage(channel, "Command added: " + heading + sub[1] + " -> " + sb.toString());
                    commands.put(sub[1].toLowerCase(), sb.toString());
                    saveProperties();
                } else {
                    bot.sendMessage(channel, heading + sub[1] + " already exists!");
                }
            } else if (sub[0].equalsIgnoreCase("remove") && hasPowers(sender)) {
                if (sub.length == 1) {
                    return;
                }
                if (commands.containsKey(sub[1])) {
                    commands.remove(sub[1]);
                    saveProperties();
                    bot.sendMessage(channel, "Removed command: " + heading + sub[1]);
                } else {
                    bot.sendMessage(channel, "The command " + heading + sub[1] + " does not exist.");
                }
            } else if (sub[0].equalsIgnoreCase("ping")) {
                bot.sendMessage(channel, "pong");
            } else if (sub[0].equalsIgnoreCase("time")) {
                bot.sendMessage(channel, "Current Time: " + TIME_FORMAT.format(Calendar.getInstance().getTime()));
            } else if (sub[0].equalsIgnoreCase("userinfo") && hasPowers(sender)) {
                if (sub.length == 1) {
                    bot.sendMessage(channel, "Usage: " + heading + "userinfo <name>");
                    return;
                }
                User user = getUser(sub[1], channel);
                if (user == null) {
                    bot.sendMessage(channel, sub[1] + " was not found in this channel.");
                    return;
                }
                bot.sendMessage(channel, user.getNick() + " - op: " + user.isOp() + ", voice: " + user.hasVoice());
            } else if (sub[0].equalsIgnoreCase("op") && hasPowers(sender)) {
                if (sub.length == 1) {
                    bot.sendMessage(channel, "Usage: " + heading + "op <name>");
                    return;
                }
                bot.op(channel, sub[1]);
            } else if (sub[0].equalsIgnoreCase("ignore") && hasPowers(sender)) {
                if (sub.length == 1) {
                    return;
                }
                if (isSysop(sub[1])) {
                    bot.sendMessage(sender.getNick(), sender.getNick() + " YOU CAN'T TELL ME WHAT TO DO.");
                    return;
                }
                if (bot.ignoreList.containsKey(sub[1])) {
                    bot.sendMessage(channel, sub[1] + " is already ignored by ashmbot.");
                } else {
                    bot.ignoreList.put(sub[1], "banned");
                    saveIgnore();
                    bot.sendMessage(channel, "ashmbot will now ignore all commands from " + sub[1]);
                }
            } else if (sub[0].equalsIgnoreCase("unignore") && hasPowers(sender)) {
                if (sub.length == 1) {
                    return;
                }
                if (!bot.ignoreList.containsKey(sub[1])) {
                    bot.sendMessage(channel, sub[1] + " isn't ignored by ashmbot!");
                } else {
                    bot.ignoreList.remove(sub[1]);
                    saveIgnore();
                    bot.sendMessage(channel, "ashmbot will listen to " + sub[1] + "\'s commands again.");
                }
            } else if (sub[0].equalsIgnoreCase("commands")) {
                final String[] cmds = commands.keySet().toArray(new String[commands.size()]);
                StringBuilder sb = new StringBuilder("");
                sb.append("Commands: ");
                for (String s : cmds) {
                    if (!s.equalsIgnoreCase(cmds[cmds.length - 1])) {
                        sb.append(s).append(", ");
                    } else {
                        sb.append(s);
                    }
                }
                if (cmds.length == 0) {
                    sb.append("None.");
                }
                bot.sendMessage(channel, sb.toString());
            } else if (sub[0].equalsIgnoreCase("join") && isSysop(sender.getNick())) {
                if (sub.length == 1) {
                    return;
                }
                bot.joinChannel(sub[1]);
            } else if (sub[0].equalsIgnoreCase("leave") && isSysop(sender.getNick())) {
                bot.sendRawLine("PART " + channel);
            } else if (sub[0].equalsIgnoreCase("profile") || sub[0].equalsIgnoreCase("rank") || sub[0].equalsIgnoreCase("lookup") || sub[0].equalsIgnoreCase("stats")) {
                if (System.currentTimeMillis() - lastUse < 7000) {
                    bot.sendMessage(channel, "This command can only be used once every 7 seconds.");
                    return;
                }
                String region = "";
                String name = "";
                int summonerId = 0;
                int level = 0;
                switch (sub.length) {
                    case 1:
                        summonerId = getSummonerId("NA", "Ashame");
                        region = "NA";
                        name = "Ashame";
                        break;
                    default:
                        if (sub[sub.length-1].equalsIgnoreCase("na") || sub[sub.length-1].equalsIgnoreCase("euw") || sub[sub.length-1].equalsIgnoreCase("eune")) {
                            region = sub[sub.length-1].toLowerCase();
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < sub.length - 2; i++) {
                                sb.append(sub[i+1]);
                            }
                            name = sb.toString().toLowerCase().replaceAll("\\s", "");
                        } else {
                            StringBuilder sb = new StringBuilder();
                            region = "na";
                            for (int i = 0; i < sub.length - 1; i++) {
                                sb.append(sub[i+1]);
                            }
                            name = sb.toString().toLowerCase().replaceAll("\\s", "");
                        }
                        summonerId = getSummonerId(region, name);
                        break;
                }

                JsonObject profile = null;

                if (Main.summonerCache.get(URLEncoder.encode(name.toLowerCase(), "UTF-8")) != null) {
                    JsonObject summonerCache = JsonObject.readFrom(Main.summonerCache.get(URLEncoder.encode(name.toLowerCase(), "UTF-8")));
                    if (summonerCache.get(region.toLowerCase()) != null) {
                        JsonObject nestedCache = JsonObject.readFrom(summonerCache.get(region.toLowerCase()).asString());
                        bot.log("Reading nested cache: " + nestedCache);
                        level = nestedCache.get("level").asInt();
                        name = URLDecoder.decode(nestedCache.get("name").asString(), "UTF-8");
                    }
                }

                if (level < 30 || summonerId == 0) {
                    JsonObject basicProfile = getBasicProfile(region, URLEncoder.encode(name, "UTF-8"));
                    level = basicProfile.get("summonerLevel").asInt();
                    name = basicProfile.get("name").asString();
                    summonerId = basicProfile.get("id").asInt();
                    saveToCache(name.replaceAll("\\s",""), region.toLowerCase(), summonerId, name, level);
                }

                bot.sendMessage(channel, "Statistics for " + name + " (" + region.toUpperCase() + "), Level " + level + " (http://lolking.net/summoner/"+region.toLowerCase()+"/"+summonerId+")");
                if (level == 30)
                    profile = getLeagueProfile(region, summonerId);
                if (profile != null) {
                    String leagueName = profile.get("name").asString();
                    JsonArray entries = profile.get("entries").asArray();
                    String tier = "";
                    String rank = "";
                    int leaguePoints = 0;
                    int rankedWins = 0;
                    for (int i = 0; i < entries.size(); i++) {
                        if (Integer.parseInt(entries.get(i).asObject().get("playerOrTeamId").asString()) == summonerId) {
                            tier = entries.get(i).asObject().get("tier").asString();
                            rank = entries.get(i).asObject().get("rank").asString();
                            leaguePoints = entries.get(i).asObject().get("leaguePoints").asInt();
                            rankedWins = entries.get(i).asObject().get("wins").asInt();
                        }
                    }
                    bot.sendMessage(channel, tier + " " + rank + ", " + leagueName + ", " + leaguePoints + " LP, " + rankedWins + " Ranked wins");
                }

                int wins = 0;
                int neutralMinionsKilled = 0;
                int totalMinionKills = 0;
                int championKills = 0;
                int assists = 0;
                int turretsKilled = 0;

                JsonArray entries = getStats(region, summonerId).get("playerStatSummaries").asArray();
                if (entries != null) {
                    JsonObject aggregatedStats;
                    for (int i = 0; i < entries.size(); i++) {
                        if (entries.get(i).asObject().get("playerStatSummaryType").asString().equalsIgnoreCase("Unranked")) {
                            JsonObject unrankedStats = entries.get(i).asObject();
                            aggregatedStats = unrankedStats.get("aggregatedStats").asObject();
                            neutralMinionsKilled = aggregatedStats.get("totalNeutralMinionsKilled").asInt();
                            totalMinionKills = aggregatedStats.get("totalMinionKills").asInt();
                            championKills = aggregatedStats.get("totalChampionKills").asInt();
                            assists = aggregatedStats.get("totalAssists").asInt();
                            turretsKilled = aggregatedStats.get("totalTurretsKilled").asInt();
                            wins = unrankedStats.get("wins").asInt();
                        }
                    }
                }
                bot.sendMessage(channel, "Normal wins: " + wins + ", Turrets destroyed: " + turretsKilled + ", Champions killed: " + championKills + ", Total assists: " + assists + ", Total minions killed: " + totalMinionKills + ", Neutral monsters killed: " + neutralMinionsKilled);
                lastUse = System.currentTimeMillis();
            } else if (sub[0].equalsIgnoreCase("greet") && hasPowers(sender)) {
                bot.setGreet(!bot.getGreet());
                bot.sendMessage(channel, "I will " + (bot.getGreet() ? "now greet people on joining" : "no longer message people on joining"));
            } else if (sub[0].equalsIgnoreCase("roulette")) {
                int i = (int) Math.ceil(Math.random() * 100);
                if (i > 30)
                    bot.sendMessage(channel, sender.getNick() + " has been shot and died.");
                else
                    bot.sendMessage(channel, sender.getNick() + " has survived the roulette.");
            } else if (sub[0].equalsIgnoreCase("queries")) {
                bot.sendMessage(channel, "Number of queries to Riot's API this session: " + Main.apiQueries);
            } else if (sub[0].equalsIgnoreCase("printcache") && hasPowers(sender)) {
                String cached = Main.summonerCache.toString().replaceAll("\\\\", "");
                bot.sendMessage(channel, "Cache: " + uploadPastebin(cached));
            } else {
                bot.log(sender.getNick() + " tried to issue command " + heading + sub[0]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveProperties() {
        try {
            final File f = new File(bot.getServer() + "_commands.properties");
            OutputStream out = new FileOutputStream(f);
            Properties prop = new Properties();
            for (Object o : prop.keySet()) {
                if (!commands.keySet().contains(o)) {
                    prop.remove(o);
                }
            }
            for (Object o : commands.keySet()) {
                prop.putAll(commands);
            }
            prop.store(out, "Commands to load for ashmbot on ");
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveIgnore() {
        try {
            final File f = new File(bot.getServer() + "_ignore.txt");
            OutputStream out = new FileOutputStream(f);
            Properties props = new Properties();
            for (Object o : props.keySet()) {
                if (!bot.ignoreList.keySet().contains(o)) {
                    props.remove(o);
                }
            }
            for (Object o : bot.ignoreList.keySet()) {
                props.putAll(bot.ignoreList);
            }
            props.store(out, "List of users that ashmbot will ignore");
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public User getUser(final String nick, final String channel) {
        User[] users = bot.getUsers(channel);
        User u = null;
        for (User user : users) {
            if (user.getNick().equalsIgnoreCase(nick)) {
                u = user;
            }
        }
        return u;
    }

    public boolean hasPowers(final User u) {
        return u.isOp() || u.hasVoice() || isSysop(u.getNick());
    }

    public boolean isSysop(final String nick) {
        return (nick.equalsIgnoreCase("ashame__") && bot.getServer().equalsIgnoreCase("irc.twitch.tv")) || (nick.equalsIgnoreCase("_nathan") && bot.getServer().equalsIgnoreCase("irc.rizon.net")) || (nick.equalsIgnoreCase("ashame") && bot.getServer().equalsIgnoreCase("irc.animebytes.tv"));
    }

    public int getSummonerId(final String region, final String summoner) throws Exception {
        int summonerId = 0;
        JsonObject jsonObject;
        if (Main.summonerCache.get(URLEncoder.encode(summoner.toLowerCase(), "UTF-8")) != null) {
            jsonObject = JsonObject.readFrom(Main.summonerCache.get(URLEncoder.encode(summoner.toLowerCase(), "UTF-8")));
            if (jsonObject.get(region.toLowerCase()) != null) {
                JsonObject nested = JsonObject.readFrom(jsonObject.get(region.toLowerCase()).asString());
                bot.log("Reading summoner ID from cache: " + nested.get("id").asInt());
                summonerId = nested.get("id").asInt();
            }
        }
        return summonerId;
    }

    public JsonObject getBasicProfile(final String region, final String summonerName) {
        JsonObject basicProfile = null;
        try {
            URL url = new URL("https://prod.api.pvp.net/api/lol/" + region.toLowerCase() + "/v1.2/summoner/by-name/" + summonerName + "?api_key=" + Main.riot_api_key);
            URLConnection urlConnection = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            basicProfile = JsonObject.readFrom(reader.readLine());
            reader.close();
            Main.apiQueries++;
        } catch (Exception e) {
            bot.log("Fetching from API failed. Trying decoded url:");
            try {
                URL url = new URL("https://prod.api.pvp.net/api/lol/" + region.toLowerCase() + "/v1.2/summoner/by-name/" + URLDecoder.decode(summonerName, "UTF-8") + "?api_key=" + Main.riot_api_key);
                URLConnection urlConnection = url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                basicProfile = JsonObject.readFrom(reader.readLine());
                reader.close();
                Main.apiQueries++;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return basicProfile;
    }

    public JsonObject getLeagueProfile(final String region, final int summonerId) {
        JsonObject profile = null;
        try {
            URL u = new URL("https://prod.api.pvp.net/api/lol/" + region.toLowerCase() + "/v2.2/league/by-summoner/" + summonerId + "?api_key=" + Main.riot_api_key);
            URLConnection urlConnection = u.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String json = "";
            while ((json = reader.readLine()) != null) {
                sb.append(json);
            }
            json = reader.readLine();
            profile = JsonObject.readFrom(sb.toString()).get(Integer.toString(summonerId)).asObject();
            reader.close();
            Main.apiQueries++;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return profile;
    }

    public JsonObject getStats(final String region, final int summonerId) {
        JsonObject stats = null;
        try {
            URL url = new URL("https://prod.api.pvp.net/api/lol/" + region.toLowerCase() + "/v1.2/stats/by-summoner/" + summonerId + "/summary" + "?api_key=" + Main.riot_api_key);
            URLConnection urlConnection = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String json = "";
            while ((json = reader.readLine()) != null) {
                sb.append(json);
            }
            json = reader.readLine();
            stats = JsonObject.readFrom(sb.toString());
            reader.close();
            Main.apiQueries++;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stats;
    }

    public void saveToCache(final String summoner, final String region, final int id, final String name, final int level) throws Exception {
        JsonObject data;
        JsonObject nested = new JsonObject();

        if (Main.summonerCache.get(URLEncoder.encode(summoner.toLowerCase(), "UTF-8")) != null) {
            data = JsonObject.readFrom(Main.summonerCache.get(URLEncoder.encode(summoner.toLowerCase(), "UTF-8")));
            bot.log("Modifying existing json object: " + data);
        } else {
            data = new JsonObject();
        }

        nested.set("id", id);
        nested.set("level", level);
        nested.set("name", URLEncoder.encode(name, "UTF-8"));
        data.set("summoner", URLEncoder.encode(name.toLowerCase(), "UTF-8"));
        data.set(region.toLowerCase(), nested.toString());

        Main.summonerCache.put(URLEncoder.encode(summoner.toLowerCase(), "UTF-8"), data.toString());
        bot.log("Writing json object as string: " + Main.summonerCache.get(URLEncoder.encode(summoner.toLowerCase(), "UTF-8")));
        try {
            final File f = new File("summoner_cache.txt");
            OutputStream out = new FileOutputStream(f);
            Properties props = new Properties();
            for (Object o : Main.summonerCache.keySet()) {
                props.putAll(Main.summonerCache);
            }
            props.store(out, "Cache of summoner IDs by region.");
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String uploadPastebin(final String text) {
        String newUrl = "";
        try {
            String host = "http://pastebin.com/api/api_post.php";
            URL url = new URL(host);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

            String parameters = "api_option=paste&api_dev_key=" + Main.pastebin_api_key + "&api_paste_code=" + URLEncoder.encode(text, "UTF-8") + "&api_paste_name=api+cache&api_paste_format=java";

            con.setDoOutput(true);
            DataOutputStream out = new DataOutputStream(con.getOutputStream());
            out.writeBytes(parameters);
            out.flush();
            out.close();

            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            newUrl = reader.readLine();
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newUrl;
    }
}
