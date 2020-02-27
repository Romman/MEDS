package org.meds.net.handlers;

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
import org.meds.net.*;
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

        // Response packet starts with login_result
        ServerPacket packet = new ServerPacket(ServerCommands.LoginResult);
        String playerLogin = data.getString(0).toLowerCase();
        Character character = daoFactory.getCharacterDAO().findCharacter(playerLogin);

        // Player is not found
        // Sending "Wrong login or password" result
        if (character == null) {
            packet.add(LoginResults.WrongLoginOrPassword);
            session.send(packet);
            return;
        }

        // Check Password
        String receivedPasswordHash = data.getString(1).substring(0, 32);
        String actualPassKeyHash = MD5Hasher.computeHash(character.getPasswordHash() + session.getKey());

        // Hash does not match
        if (!receivedPasswordHash.equalsIgnoreCase(actualPassKeyHash)) {
            packet.add(LoginResults.WrongLoginOrPassword);
            session.send(packet);
            return;
        }

        // Create a Player instance with found id
        // Something happened and a player can not be created


        try {
            session.authenticate(character);
            packet.add(LoginResults.OK);
        } catch (Exception ex) {
            // Something happened and a player can not be created
            // Or setting last login data has failed
            logger.error("Exception while authenticate a player.", ex);
            packet.add(LoginResults.InnerServerError);
            session.send(packet);
            return;
        }

        Player player = sessionContext.getPlayer();

        // Add New Messages
        if (newMessageRepository.size() != 0) {
            packet.add(ServerCommands.MessageList);
            for (NewMessage message : newMessageRepository) {
                packet.add(message.getId()).add(message.getTypeId()).add(message.getMessage());
            }
        }

        // Unknown "cs" values
        packet.addData(ServerCommands._cs, "44", "0").addData(ServerCommands._cs, "45", "0").addData(ServerCommands._cs, "46", "0")
                .addData(ServerCommands._cs, "47", "0").addData(ServerCommands._cs, "48", "0").addData(ServerCommands._cs, "49", "0");

        packet.addData(ServerCommands.ClanInfo, "1", "1", "0", "Clan Name")
                .add(player.getMagicData())
                .add(player.getSkillData())
                .add(player.getGuildData());

        // NOTE: Sometimes the data above is sent as a separate packet

        packet.add(player.getCurrencyData())
                .add(player.getParametersData())
                .addData(ServerCommands.BattleState, BattleStates.NoBattle.toString())
                .add(player.getHealthManaData())
                .add(player.getLevelData());

        session.send(packet);
        packet.clear();

        /*
         * No sharp data since 1.2.7.6
        // sharp "#"
        packet.send(ServerCommands.Sharp, "0", "0");
         * */

        /*
         * Do not remember what is this
        // msg
        String[] msg = new String[] { "msg", "\u00024" };
         * */

        // Empty Item info
        packet.addData(ServerCommands.ItemInfo, "");

        packet.add(player.getInventory().getEquipmentData());

        // Again?? Why???
        packet.add(player.getParametersData());

        // BonusMagicParameters? Why?
        packet.addData(ServerCommands.BonusMagicParameter, "10", "0")
                .addData(ServerCommands.BonusMagicParameter, "15", "0")
                .addData(ServerCommands.BonusMagicParameter, "16", "0")
                .addData(ServerCommands.BonusMagicParameter, "17", "0");

        // TODO: add cm data here (current available magic spells)

        packet.add(player.getInventory().getInventoryData());

        // Unknown Datas
        packet.addData(ServerCommands._wg, "84", "147"); // Weight
        // Possibly extended inventory price data
        packet.addData(ServerCommands._invt, "0", "5 платины");
        packet.addData(ServerCommands.AutoSpell, Integer.toString(player.getAutoSpell())); // Default magic
        packet.addData(ServerCommands._s0, "");

        if (player.isRelax())
            packet.add(ServerCommands.RelaxOn);
        else
            packet.add(ServerCommands.RelaxOff);

        packet.addData(ServerCommands._lh0, "");

        // TODO: add auras
        // TODO: add quest statuses

        // NOTE: Sometimes the data above is sent as a separate packet

        packet.add(player.getAchievementData());

        // prot1 and prot2
        packet.addData(ServerCommands._prot1, "0").addData(ServerCommands._prot2, "0");

        // Last corpse location (Skull icon at the cell with this location)
        packet.addData(ServerCommands._tc, "1997");

        // Unknown
        packet.addData(ServerCommands._hs, "0");

        // TODO: implement Professions
        packet.add(player.getProfessionData());

        // Unknown
        packet.addData(ServerCommands._omg, "7", "0", "0");

        // Notepad notes
        packet.addData(ServerCommands.Notepad, player.getNotepadNotes());

        packet.add(world.getDayTimeData());

        // "omg" again but different numbers
        packet.addData(ServerCommands._omg, "9", "1", "0");

        // Possibly sleep mode
        packet.addData(ServerCommands._zzz, "0");

        // Unknown
        packet.addData(ServerCommands._fpi, "0");
        packet.addData(ServerCommands._swf, "0");
        packet.addData(ServerCommands._fex, "0", "0");

        // "wg" ??? Again???
        packet.addData(ServerCommands._wg, "84", "147");

        packet.add(player.getPosition().getData());
        packet.add(player.getPosition().getCorpseData());
        // TODO: Send the neighbour locations info here

        // Unknown
        // But here is my email and link to the official webshop page
        packet.addData(ServerCommands._hoi, "http://ds-dealer.ru/dsrus/index.php?u=", "email@email.com", "0");

        packet.add(world.getOnlineData());

            /*
            // Mentor
            packet.send(ServerCommands._mmy, "Mentor Name");
             * */

        // Send the custom welcome message
        packet.addData(ServerCommands.ServerMessage, "5001");

        session.send(packet);
    }
}
