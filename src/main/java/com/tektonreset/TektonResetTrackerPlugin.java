package com.tektonreset;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Tekton Reset Tracker",
	description = "Counts how many Chambers of Xeric Challenge Mode raids you reset at Tekton without killing it, and the time wasted.",
	tags = {"tekton", "cox", "cm", "challenge", "chambers", "xeric", "raid", "reset", "tracker", "counter", "skip"}
)
public class TektonResetTrackerPlugin extends Plugin
{
	// Every NPC state Tekton can be in. Seeing any of these means we reached the Tekton room.
	// The same NPC ids are used in normal CoX and Challenge Mode.
	static final Set<Integer> TEKTON_NPC_IDS = ImmutableSet.of(
		NpcID.RAIDS_TEKTON_WAITING,            // 7540 (sitting at the anvil)
		NpcID.RAIDS_TEKTON_WALKING_STANDARD,   // 7541
		NpcID.RAIDS_TEKTON_FIGHTING_STANDARD,  // 7542
		NpcID.RAIDS_TEKTON_WALKING_ENRAGED,    // 7543
		NpcID.RAIDS_TEKTON_FIGHTING_ENRAGED,   // 7544
		NpcID.RAIDS_TEKTON_HAMMERING           // 7545 (repairing his armour at the anvil)
	);

	@Inject
	private Client client;

	@Inject
	private TektonResetTrackerConfig config;

	@Inject
	private ClientToolbar clientToolbar;

	private TektonResetTrackerPanel panel;
	private NavigationButton navButton;

	// Persisted lifetime totals.
	private int lifetimeResets;
	private long lifetimeWastedMillis;

	// Session totals (reset each client launch).
	private int sessionResets;
	private long sessionWastedMillis;

	// Per-raid state.
	private boolean inRaid;
	private boolean challengeMode; // whether the current raid is Challenge Mode (only CM raids are counted)
	private boolean sawTekton;
	private boolean tektonDefeated;
	private long raidStartMillis;

	@Provides
	TektonResetTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TektonResetTrackerConfig.class);
	}

	@Override
	protected void startUp()
	{
		lifetimeResets = config.lifetimeResets();
		lifetimeWastedMillis = config.lifetimeWastedMillis();
		sessionResets = 0;
		sessionWastedMillis = 0L;

		inRaid = false;
		challengeMode = false;
		sawTekton = false;
		tektonDefeated = false;
		raidStartMillis = 0L;

		panel = new TektonResetTrackerPanel(this);

		navButton = NavigationButton.builder()
			.tooltip("Tekton Reset Tracker")
			.icon(ImageUtil.loadImageResource(getClass(), "/com/tektonreset/panel_icon.png"))
			.priority(7)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);
		refreshPanel();
	}

	@Override
	protected void shutDown()
	{
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
		}
		navButton = null;
		panel = null;
	}

	/**
	 * Drive the raid lifecycle from the IN_RAID varbit. We only poll while logged in so a
	 * hop or logout mid-raid (which can briefly read the varbit as 0) doesn't look like the
	 * player leaving the raid.
	 */
	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		final boolean nowInRaid = client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) == 1;

		if (nowInRaid && !inRaid)
		{
			startRaid();
		}
		else if (!nowInRaid && inRaid)
		{
			endRaid();
		}

		// Latch Challenge Mode for the current raid. Read it while we're still in the raid,
		// because the varbit clears once IN_RAID drops (by which point endRaid() already ran).
		if (inRaid && client.getVarbitValue(VarbitID.RAIDS_CHALLENGE_MODE) == 1)
		{
			challengeMode = true;
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (inRaid && TEKTON_NPC_IDS.contains(event.getNpc().getId()))
		{
			sawTekton = true;
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		final NPC npc = event.getNpc();
		// A Tekton that despawns while dead was actually killed — that's progress, not a reset.
		// Walking out of the room (or his standard -> enraged swap) despawns him without isDead().
		if (inRaid && npc.isDead() && TEKTON_NPC_IDS.contains(npc.getId()))
		{
			tektonDefeated = true;
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		// On logout / world hop / lost connection we stop tracking the current raid WITHOUT
		// recording a reset: the raid persists server-side and the player hasn't left Tekton.
		// If they were still in the raid, the next logged-in game tick re-detects IN_RAID.
		final GameState state = event.getGameState();
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING || state == GameState.CONNECTION_LOST)
		{
			inRaid = false;
			challengeMode = false;
			sawTekton = false;
			tektonDefeated = false;
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (TektonResetTrackerConfig.GROUP.equals(event.getGroup()))
		{
			refreshPanel();
		}
	}

	/**
	 * Refresh the panel periodically so the live "time in raid" stays current while a raid is
	 * in progress (we don't add it to totals until a reset is actually recorded).
	 */
	@Schedule(period = 1, unit = ChronoUnit.SECONDS)
	public void scheduledRefresh()
	{
		if (inRaid)
		{
			refreshPanel();
		}
	}

	// ----------------------------------- Raid lifecycle -----------------------------------

	private void startRaid()
	{
		inRaid = true;
		challengeMode = false;
		sawTekton = false;
		tektonDefeated = false;
		raidStartMillis = System.currentTimeMillis();
	}

	private void endRaid()
	{
		// Only Challenge Mode raids are counted; Normal raids are ignored entirely.
		final boolean reset = challengeMode && sawTekton && !tektonDefeated;
		final long elapsed = Math.max(0L, System.currentTimeMillis() - raidStartMillis);

		inRaid = false;
		challengeMode = false;
		sawTekton = false;
		tektonDefeated = false;

		if (!reset)
		{
			return;
		}

		lifetimeResets++;
		lifetimeWastedMillis += elapsed;
		sessionResets++;
		sessionWastedMillis += elapsed;

		config.lifetimeResets(lifetimeResets);
		config.lifetimeWastedMillis(lifetimeWastedMillis);

		if (config.chatMessage())
		{
			final String message = "Tekton CM reset #" + lifetimeResets + " — "
				+ DurationFormat.format(elapsed) + " wasted (lifetime: "
				+ DurationFormat.format(lifetimeWastedMillis) + ").";
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
		}

		refreshPanel();
	}

	// ----------------------------------- Stats getters -----------------------------------

	int getLifetimeResets()
	{
		return lifetimeResets;
	}

	long getLifetimeWastedMillis()
	{
		return lifetimeWastedMillis;
	}

	int getSessionResets()
	{
		return sessionResets;
	}

	long getSessionWastedMillis()
	{
		return sessionWastedMillis;
	}

	/** Whether a raid is currently being tracked (used to show the live timer in the panel). */
	boolean isInRaid()
	{
		return inRaid;
	}

	/** Milliseconds elapsed in the current raid, or 0 when not in a raid. */
	long getCurrentRaidMillis()
	{
		return inRaid ? Math.max(0L, System.currentTimeMillis() - raidStartMillis) : 0L;
	}

	/** Whether the raid currently being tracked is Challenge Mode (only CM raids are counted). */
	boolean isCurrentRaidChallengeMode()
	{
		return challengeMode;
	}

	/**
	 * Reset all counters (lifetime and session). Invoked from the side panel's Reset button.
	 */
	void reset()
	{
		lifetimeResets = 0;
		lifetimeWastedMillis = 0L;
		sessionResets = 0;
		sessionWastedMillis = 0L;
		config.lifetimeResets(0);
		config.lifetimeWastedMillis(0L);
		refreshPanel();
	}

	private void refreshPanel()
	{
		final TektonResetTrackerPanel p = panel;
		if (p != null)
		{
			SwingUtilities.invokeLater(p::update);
		}
	}
}
