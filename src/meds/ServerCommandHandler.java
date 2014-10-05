package meds;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;

import meds.database.Hibernate;
import meds.database.entity.CharacterInfo;
import meds.database.entity.CharacterSpell;
import meds.enums.Races;
import meds.logging.Logging;
import meds.util.MD5Hasher;

import org.hibernate.Session;
import org.hibernate.Transaction;

public class ServerCommandHandler implements Runnable
{
    private HashMap<String, CommandHandler> commands;

    public ServerCommandHandler()
    {
        this.commands = new HashMap<String, ServerCommandHandler.CommandHandler>();
        this.commands.put("character", new CharacterCommandHandler());
        this.commands.put("help", new HelpCommandHandler());
        this.commands.put("shutdown", new ShutdownCommandHandler());
    }

    @Override
    public void run()
    {
        Logging.Debug.log("ServerCommandHandler started");
        Scanner scanner = new Scanner(System.in);
        String line;
        while(scanner.hasNext())
        {
            line = scanner.nextLine();
            if (line.isEmpty())
                continue;
            line = line.toLowerCase();
            Logging.Debug.log("Command entered: " + line);
            String[] commandArgs = line.split(" ");

            CommandHandler handler = this.commands.get(commandArgs[0]);
            if (handler == null)
            {
                System.out.println("Command \"" + commandArgs[0] + "\" not found. To see the list of all available commands type \"help\"");
                continue;
            }

            // Check subcommands
            String[] subCommands = handler.getSubCommands();
            if (subCommands != null)
            {
                if (commandArgs.length == 1)
                {
                    this.notifySubCommands(commandArgs[0], subCommands);
                    continue;
                }
                boolean subCommandFound = false;
                for (int i = 0; i < subCommands.length; ++i)
                {
                    if (subCommands[i].equals(commandArgs[1]))
                    {
                        subCommandFound = true;
                        break;
                    }
                }
                if (!subCommandFound)
                {
                    System.out.println("Unknown subcommand \"" + commandArgs[1] + "\".");
                    this.notifySubCommands(commandArgs[0], subCommands);
                    continue;
                }
            }
            handler.handle(Arrays.copyOfRange(commandArgs, 1, commandArgs.length));
        }
        scanner.close();
        Logging.Debug.log("ServerCommandHandler does not have next");
    }

    private void notifySubCommands(String command, String[] subCommands)
    {
        System.out.println("Use the following subcommands for the command \"" + command + "\":");
        for (int i = 0; i < subCommands.length; ++i)
            System.out.println(" " + subCommands[i]);
    }

    private interface CommandHandler
    {
        public String[] getSubCommands();
        public void handle(String[] args);
    }

    private class ShutdownCommandHandler implements CommandHandler
    {
        @Override
        public String[] getSubCommands()
        {
            return null;
        }
        @Override
        public void handle(String[] args)
        {
            Logging.Info.log("Shutting down...");
            Program.Exit();
        }
    }

    private class CharacterCommandHandler implements CommandHandler
    {
        @Override
        public String[] getSubCommands()
        {
            return new String[] { "ban", "create", "delete" };
        }

        @Override
        public void handle(String[] args)
        {
            if (String.valueOf("create").equals(args[0]))
                this.create(Arrays.copyOfRange(args, 1, args.length));
        }

        private void create(String[] args)
        {
            if (args.length < 1)
            {
                System.out.println("Character create error: Missing character name.");
                return;
            }
            if (args.length < 2)
            {
                System.out.println("Character create error: Missing character password.");
                return;
            }

            String login = args[0].toLowerCase();
            String charName = Character.toUpperCase(login.charAt(0)) + login.substring(1);

            meds.database.entity.Character character = new meds.database.entity.Character();
            character.setLogin(login);
            character.setPasswordHash(MD5Hasher.ComputeHash(MD5Hasher.ComputeHash(args[1]) + "dsdarkswords"));

            Session session = Hibernate.getSessionFactory().openSession();
            Transaction tx = session.beginTransaction();
            session.save(character);
            tx.commit();
            session.close();
            int characterId = character.getId();

            CharacterInfo characterInfo = new CharacterInfo();
            characterInfo.setCharacterId(characterId);
            characterInfo.setName(charName);
            characterInfo.setAvatarId(22); // Elf
            characterInfo.setRace(Races.Elf.getValue());
            // Every base stat is 10
            characterInfo.setBaseCon(10);
            characterInfo.setBaseStr(10);
            characterInfo.setBaseDex(10);
            characterInfo.setBaseInt(10);
            // Location - Seastone Star
            characterInfo.setLocationId(3);
            characterInfo.setHomeId(3);

            characterInfo.getSpells().put(38, new CharacterSpell(characterId, 38, 1)); // Examine
            characterInfo.getSpells().put(54, new CharacterSpell(characterId, 54, 1)); // First Aid
            characterInfo.getSpells().put(60, new CharacterSpell(characterId, 60, 1)); // Relax

            session = Hibernate.getSessionFactory().openSession();
            tx = session.beginTransaction();
            session.save(characterInfo);
            tx.commit();
            session.close();
        }
    }

    private class HelpCommandHandler implements CommandHandler
    {
        @Override
        public String[] getSubCommands()
        {
            return null;
        }

        @Override
        public void handle(String[] args)
        {
            System.out.println("Available commands:");
            for (Entry<String, CommandHandler> entry : ServerCommandHandler.this.commands.entrySet())
            {
                System.out.println(" " + entry.getKey());
            }
        }
    }

}
