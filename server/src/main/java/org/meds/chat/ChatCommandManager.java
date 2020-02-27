package org.meds.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.meds.Player;
import org.meds.chat.commands.ChatCommand;
import org.meds.chat.commands.ChatCommandHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class ChatCommandManager {

    private static Logger logger = LogManager.getLogger();

    @Autowired
    private ApplicationContext applicationContext;

    private Map<String, ChatCommandHandler> chatCommands;

    public ChatCommandManager() {
        this.chatCommands = Collections.emptyMap();
    }

    @PostConstruct
    private void init() {
        this.chatCommands = this.applicationContext.getBeansWithAnnotation(ChatCommand.class)
                .entrySet()
                .stream()
                .filter(entry -> {
                    // Must implement ChatCommandHandler interface
                    return entry.getValue() instanceof ChatCommandHandler;
                })
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> (ChatCommandHandler) entry.getValue()));
    }

    public void handle(Player player, String command, String commandArgs) {
        // Parsing handlers
        // Each word except surrounding with quotes

        List<String> list = new ArrayList<>();
        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(commandArgs);
        while(m.find()) {
            list.add(m.group(1).replace("\"", ""));
        }

        String[] args = list.toArray(new String[list.size()]);
        org.meds.chat.commands.ChatCommandHandler chatCommandHandler = this.chatCommands.get(command);
        if (chatCommandHandler == null) {
            return;
        }
        if (chatCommandHandler.getMinArgsCount() != -1 && chatCommandHandler.getMinArgsCount() > args.length) {
            return;
        }

        logger.info("Executing the chat command \"{}\" for a player {}", command, player);
        chatCommandHandler.handle(player, args);
    }

}
