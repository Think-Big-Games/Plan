package main.java.com.djrapitops.plan.data.listeners;

import com.djrapitops.plugin.task.AbsRunnable;
import com.djrapitops.plugin.utilities.player.Fetch;
import com.djrapitops.plugin.utilities.player.Gamemode;
import com.djrapitops.plugin.utilities.player.IPlayer;
import main.java.com.djrapitops.plan.Plan;
import main.java.com.djrapitops.plan.data.UserData;
import main.java.com.djrapitops.plan.data.cache.DataCache;
import main.java.com.djrapitops.plan.data.handling.info.KickInfo;
import main.java.com.djrapitops.plan.data.handling.info.LoginInfo;
import main.java.com.djrapitops.plan.data.handling.info.LogoutInfo;
import main.java.com.djrapitops.plan.utilities.MiscUtils;
import main.java.com.djrapitops.plan.utilities.NewPlayerCreator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.net.InetAddress;
import java.util.UUID;

/**
 * Event Listener for PlayerJoin, PlayerQuit and PlayerKickEvents.
 *
 * @author Rsl1122
 * @since 2.0.0
 */
public class PlanPlayerListener implements Listener {

    private final Plan plugin;
    private final DataCache handler;

    /**
     * Class Constructor.
     * <p>
     * Copies the references to multiple handlers from Current instance of handler.
     *
     * @param plugin Current instance of Plan
     */
    public PlanPlayerListener(Plan plugin) {
        this.plugin = plugin;
        handler = plugin.getHandler();
    }

    /**
     * PlayerJoinEvent Listener.
     * <p>
     * If player is a new player, creates new data for the player.
     * <p>
     * Adds a LoginInfo to the processingQueue if the user is not new.
     *
     * @param event The Fired event.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLogin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        IPlayer iPlayer = Fetch.wrapBukkit(player);
        plugin.getNotificationCenter().checkNotifications(iPlayer);

        UUID uuid = player.getUniqueId();
        handler.startSession(uuid);
        plugin.getRunnableFactory().createNew(new AbsRunnable("NewPlayerCheckTask") {
            @Override
            public void run() {
                long time = MiscUtils.getTime();
                InetAddress ip = player.getAddress().getAddress();
                boolean banned = player.isBanned();
                String displayName = player.getDisplayName();
                String gm = player.getGameMode().name();
                String worldName = player.getWorld().getName();

                LoginInfo loginInfo = new LoginInfo(uuid, time, ip, banned, displayName, gm, 1, worldName);
                boolean isNewPlayer = !plugin.getDB().wasSeenBefore(uuid);

                if (isNewPlayer) {
                    UserData newUserData = NewPlayerCreator.createNewPlayer(iPlayer);
                    loginInfo.process(newUserData);
                    // TODO Rewrite Register & Login system handler.newPlayer(newUserData);
                } else {
                    // handler.addToPool(loginInfo);
                }
                this.cancel();
            }
        }).runTaskAsynchronously();
    }

    /**
     * PlayerQuitEvent Listener.
     * <p>
     * Adds a LogoutInfo to the processing Queue.
     *
     * @param event Fired event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // TODO Rewrite Logout system
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        handler.endSession(uuid);

        long time = MiscUtils.getTime();
        boolean banned = player.isBanned();
        Gamemode gm = Gamemode.wrap(player.getGameMode());
        String worldName = player.getWorld().getName();

        plugin.addToProcessQueue(new LogoutInfo(uuid, time, banned, gm.name(), handler.getSession(uuid), worldName));
    }

    /**
     * PlayerKickEvent Listener.
     * <p>
     * Adds a KickInfo & LogoutInfo to the processing Queue.
     *
     * @param event Fired event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        handler.endSession(uuid);

        long time = MiscUtils.getTime();
        boolean banned = player.isBanned();
        Gamemode gm = Gamemode.wrap(player.getGameMode());
        String worldName = player.getWorld().getName();

        plugin.addToProcessQueue(new LogoutInfo(uuid, time, banned, gm.name(), handler.getSession(uuid), worldName));
        plugin.addToProcessQueue(new KickInfo(uuid));
    }
}
