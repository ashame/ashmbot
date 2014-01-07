package net.ashame.irc.bot;

import net.ashame.irc.bot.messages.CommandProcessor;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;

import java.io.*;
import java.util.*;

/**
 * Created by Nathan on 1/4/14.
 * http://www.powerbot.org/community/user/523484-nathan-l/
 * http://www.excobot.org/forum/user/906-nathan/
 */
@SuppressWarnings("ALL")
public class Bot extends PircBot {

    protected final CommandProcessor commandProcessor;
    public Map<String, String> ignoreList = new LinkedHashMap<>();
    public final String API_KEY;
    public boolean greet = false;
    private final int id;

    public Bot() {
        this("ashmbot");
    }

    public Bot(String name) {
        this(name, 0);
    }

    public Bot(int id) {
        this("ashmbot", id);
    }

    public Bot(String name, int id) {
        this.setName(name);
        this.commandProcessor = new CommandProcessor(this);
        this.id = id;
        String key = "";

        Properties props = new Properties();
        try {
            FileInputStream in = new FileInputStream("settings.properties");
            props.load(in);
            key = props.getProperty("API_KEY");
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.API_KEY = key;
    }

    public void onMessage(final String channel, final String sender, final String login, final String hostname, final String message) {
        if (isIgnored(sender)) {
            return;
        }
        char heading = message.charAt(0);
        if (heading == ',') {
            String[] sp = message.split(" ");
            sp[0] = sp[0].toLowerCase().substring(1);
            commandProcessor.processCommand(heading, sp, channel, getUser(channel, sender));
        }
        if (message.contains("The moderators of this room are")) {
            System.out.println("True");
        }
        if (message.contains("hi ashmbot"))
            sendMessage(channel, "hi " + sender);
    }

    public void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {
        if (isConnected() && recipientNick.equalsIgnoreCase(getNick())) {
            sendRawLine("JOIN " + channel);
            sendMessage(channel, "fak u " + kickerNick);
        }
    }

    public User getUser(final String channel, final String nick) {
        User[] users = getUsers(channel);
        for (User user : users) {
            if (user.getNick().equalsIgnoreCase(nick)) {
                return user;
            }
        }
        return null;
    }

    public boolean isIgnored(final String nick) {
        for (String s : ignoreList.keySet()) {
            if (s.equalsIgnoreCase(nick)) {
                return true;
            }
        }
        return false;
    }

    public void onConnect() {
        Properties properties = new Properties();
        try {
            FileInputStream in = new FileInputStream(getServer() + "_commands.properties");
            properties.load(in);
            in.close();
        } catch (Exception e) {
            log("Error loading commands for " + getServer() + " :" + e.getMessage());
        }
        for (final Map.Entry<Object, Object> entry : properties.entrySet()) {
            if (!commandProcessor.getCommandMap().containsKey(entry.getKey())) {
                commandProcessor.getCommandMap().put((String) entry.getKey(), (String) entry.getValue());
            }
        }

        Properties props = new Properties();
        try {
            FileInputStream in = new FileInputStream(getServer() + "_ignore.txt");
            props.load(in);
            in.close();
        } catch (Exception e) {
            log("Error loading ignore file for " + getServer() + ": " + e.getMessage());
        }
        for (Object o : props.keySet()) {
            if (!ignoreList.containsKey(o)) {
                ignoreList.put((String) o, (String) props.get(o));
            }
        }
    }

    public void onJoin(String channel, String sender, String login, String hostname) {
        if (!sender.equalsIgnoreCase(getNick()) && greet) {
            sendMessage(channel, "hi " + sender);
        }
    }

    public void onPart(String channel, String sender, String login, String hostname) {
        if (!sender.equalsIgnoreCase(getNick()) && greet) {
            sendMessage(channel, "RIP " + sender);
        }
    }

    public void log(String line) {
        Main.log("[" + TIME_FORMAT.format(Calendar.getInstance().getTime()) + "] " + line, id);
    }

    public void setGreet(boolean greet) {
        this.greet = greet;
    }

    public boolean getGreet() {
        return this.greet;
    }
}
