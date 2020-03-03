package org.meds.net.handlers;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.meds.Player;
import org.meds.World;
import org.meds.data.dao.DAOFactory;
import org.meds.data.domain.Character;
import org.meds.data.domain.NewMessage;
import org.meds.database.Repository;
import org.meds.enums.BattleStates;
import org.meds.enums.LoginResults;
import org.meds.enums.Parameters;
import org.meds.net.*;
import org.meds.net.message.MessageIdentity;
import org.meds.net.message.MessageWriteStream;
import org.meds.net.message.ServerMessage;
import org.meds.net.message.ServerMessageIdentity;
import org.meds.net.message.server.AutoSpellMessage;
import org.meds.net.message.server.BattleMessage;
import org.meds.net.message.server.BonusMagicParameterMessage;
import org.meds.net.message.server.ChatMessage;
import org.meds.net.message.server.ClansMessage;
import org.meds.net.message.server.LoginResultMessage;
import org.meds.net.message.server.MessageListMessage;
import org.meds.net.message.server.RelaxOffMessage;
import org.meds.net.message.server.RelaxOnMessage;
import org.meds.net.message.server._csMessage;
import org.meds.net.message.server._lh0Message;
import org.meds.util.MD5Hasher;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Romman.
 */
@ClientCommand(ClientCommandTypes.Login)
public class LoginCommandHandler implements ClientCommandHandler {

    private static Logger logger = LogManager.getLogger();

    @Autowired
    private DAOFactory daoFactory;
    @Autowired
    private SessionContext sessionContext;
    @Autowired
    private World world;
    @Autowired
    private Repository<NewMessage> newMessageRepository;

    @Override
    public int getMinDataLength() {
        return 2;
    }

    @Override
    public boolean isAuthenticatedOnly() {
        return false;
    }

    @Override
    public void handle(ClientCommandData data) {
        Session session = sessionContext.getSession();
        /*
         * Data Structure
         * 0 - Login name (Username)
         * 1 - 64 bytes hash
         *     first 32 bytes = MD5((MD5(PASSWORD) + SESSION_KEY)
         */

        /*
         * Sometimes the next data is divided into separates packets
         * for ex.:
         * 1) login_result, cs
         * 2) kinf, mi, gi, si
         * 3) parameters data: sti, hp, exp, eq, inv etc.
         * 4) location data: tl, loc, li, pss etc.
         */


        String playerLogin = data.getString(0).toLowerCase();
        Character character = daoFactory.getCharacterDAO().findCharacter(playerLogin);

        // Player is not found
        // Sending "Wrong login or password" result
        if (character == null) {
            session.send(new LoginResultMessage(LoginResults.WrongLoginOrPassword));
            return;
        }

        // Check Password
        String receivedPasswordHash = data.getString(1).substring(0, 32);
        String actualPassKeyHash = MD5Hasher.computeHash(character.getPasswordHash() + session.getKey());

        // Hash does not match
        if (!receivedPasswordHash.equalsIgnoreCase(actualPassKeyHash)) {
            session.send(new LoginResultMessage(LoginResults.WrongLoginOrPassword));
            return;
        }

        // Create a Player instance with found id
        // Something happened and a player can not be created

        List<ServerMessage> messages;
        try {
            session.authenticate(character);
            // Response packet starts with "login_result"
            messages = new ArrayList<>(20);
            messages.add(new LoginResultMessage(LoginResults.OK));
        } catch (Exception ex) {
            // Something happened and a player can not be created
            // Or setting last login data has failed
            logger.error("Exception while authenticate a player.", ex);
            session.send(new LoginResultMessage(LoginResults.InnerServerError));
            return;
        }

        Player player = sessionContext.getPlayer();

        // Add New Messages
        if (newMessageRepository.size() != 0) {
            List<MessageListMessage.MessageInfo> messageInfos = new ArrayList<>(newMessageRepository.size());
            for (NewMessage message : newMessageRepository) {
                messageInfos.add(new MessageListMessage.MessageInfo(
                        message.getId(), message.getTypeId(), message.getMessage())
                );
            }
            messages.add(new MessageListMessage(messageInfos));
        }

        // Send "cs" values
        messages.add(new _csMessage(44, 0));
        messages.add(new _csMessage(45, 0));
        messages.add(new _csMessage(46, 0));
        messages.add(new _csMessage(47, 0));
        messages.add(new _csMessage(48, 0));
        messages.add(new _csMessage(49, 0));

        messages.add(new ClansMessage(1, 0, "Dummy Clan Name"));
        messages.add(player.getMagicData());
        messages.add(player.getSkillData());
        messages.add(player.getGuildData());

        // NOTE: Sometimes the data above is sent as a separate packet

        messages.add(player.getCurrencyData());
        messages.add(player.getCurrencyData());
        messages.add(player.getParametersData());
        messages.add(new BattleMessage(BattleStates.NoBattle));
        messages.add(player.getHealthManaData());
        messages.add(player.getLevelData());

        session.send(messages);
        messages = new ArrayList<>(20);

        /*
         * No sharp data since 1.2.7.6
         * sharp "#"
         *
         * Message:
         * # 0 0
         * */

        // Empty Item info
        // TODO: Do we need it? Trace the absence of this message as how the client works
        // ServerMessageIdentity.ItemInfo and empty string ""

        messages.add(player.getInventory().getEquipmentData());

        // Again?? Why???
        // Trace the absence of this message and client reaction to that
        messages.add(player.getParametersData());

        // BonusMagicParameters? Why?
        messages.add(new BonusMagicParameterMessage(Parameters.parse(10), 0));
        messages.add(new BonusMagicParameterMessage(Parameters.parse(15), 0));
        messages.add(new BonusMagicParameterMessage(Parameters.parse(16), 0));
        messages.add(new BonusMagicParameterMessage(Parameters.parse(17), 0));

        // TODO: add cm data here (current available magic spells)

        messages.add(player.getInventory().getInventoryData());

        // Unknown Datas
        messages.add(new UnclassifiedMessage(ServerMessageIdentity._wg, "84", "147")); // Weight
        // Possibly extended inventory price data
        messages.add(new UnclassifiedMessage(ServerMessageIdentity._invt, "0", "5 platinum"));

        messages.add(new AutoSpellMessage(player.getAutoSpell())); // Default magic
        messages.add(new UnclassifiedMessage(ServerMessageIdentity._s0, ""));

        if (player.isRelax()) {
            messages.add(new RelaxOnMessage());
        } else {
            messages.add(new RelaxOffMessage());
        }

        messages.add(new _lh0Message());

        // TODO: add auras
        // TODO: add quest statuses

        // NOTE: Sometimes the data above is sent as a separate packet

        messages.add(player.getAchievementData());

        // prot1 and prot2
        messages.add(new UnclassifiedMessage(ServerMessageIdentity._prot1, "0"));
        messages.add(new UnclassifiedMessage(ServerMessageIdentity._prot2, "0"));

        // Last corpse location (Skull icon at the cell with this location)
        messages.add(new UnclassifiedMessage(ServerMessageIdentity._tc, "1997"));

        // Unknown
        messages.add(new UnclassifiedMessage(ServerMessageIdentity._hs, "0"));

        // TODO: implement Professions
        messages.add(player.getProfessionData());

        // Unknown
        messages.add(new UnclassifiedMessage(ServerMessageIdentity._hs, "7", "0", "0"));

        // Notepad notes
        messages.add(new UnclassifiedMessage(ServerMessageIdentity.Notepad, player.getNotepadNotes()));

        messages.add(world.getDayTimeData());

        // "omg" again but different numbers
        messages.add(new UnclassifiedMessage(ServerMessageIdentity._hs,  "9", "1", "0"));

        // Possibly sleep mode
        messages.add(new UnclassifiedMessage(ServerMessageIdentity._zzz,  "0"));

        // Unknown
        messages.add(new UnclassifiedMessage(ServerMessageIdentity._fpi,  "0"));
        messages.add(new UnclassifiedMessage(ServerMessageIdentity._swf,  "0"));
        messages.add(new UnclassifiedMessage(ServerMessageIdentity._fex,  "0", "0"));

        // "wg" ??? Again???
        messages.add(new UnclassifiedMessage(ServerMessageIdentity._wg,  "84", "147"));

        messages.add(player.getPosition().getData());
        messages.add(player.getPosition().getCorpseData());
        // TODO: Send the neighbour locations info here

        // Unknown
        // Here are the player's email address and the link to their official webshop page
        messages.add(new UnclassifiedMessage(
                ServerMessageIdentity._hoi,  "http://ds-dealer.ru/dsrus/index.php?u=", "email@email.com", "0")
        );

        messages.add(world.getOnlineData());

            /*
            // Mentor
            packet.send(ServerCommands._mmy, "Mentor Name");
             * */

        // Send the custom welcome message
        messages.add(new ChatMessage(5001));

        session.send(messages);
    }

    /**
     * Temporary implementation.
     * Should be removed when all messages types will be researched and classified.
     */
    private static class UnclassifiedMessage implements ServerMessage {

        final ServerMessageIdentity identity;
        final String[] params;

        public UnclassifiedMessage(ServerMessageIdentity identity, String... params) {
            this.identity = identity;
            this.params = params;
        }

        @Override
        public MessageIdentity getIdentity() {
            return this.identity;
        }

        @Override
        public void serialize(MessageWriteStream stream) {
            for (String param : params) {
                stream.writeString(param);
            }
        }
    }
}
