package com.tektonreset;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(TektonResetTrackerConfig.GROUP)
public interface TektonResetTrackerConfig extends Config
{
	String GROUP = "tektonresettracker";

	@ConfigSection(
		name = "Notifications",
		description = "Feedback shown when a Tekton reset is recorded.",
		position = 0
	)
	String notificationSection = "notifications";

	@ConfigItem(
		keyName = "chatMessage",
		name = "Chat message on reset",
		description = "Print a game message with your reset count and time wasted whenever a Tekton reset is recorded.",
		position = 1,
		section = notificationSection
	)
	default boolean chatMessage()
	{
		return true;
	}

	// ---- Persisted lifetime statistics (hidden; shown in the side panel) ----

	@ConfigItem(keyName = "lifetimeResets", name = "", description = "", hidden = true)
	default int lifetimeResets()
	{
		return 0;
	}

	@ConfigItem(keyName = "lifetimeResets", name = "", description = "", hidden = true)
	void lifetimeResets(int lifetimeResets);

	@ConfigItem(keyName = "lifetimeWastedMillis", name = "", description = "", hidden = true)
	default long lifetimeWastedMillis()
	{
		return 0L;
	}

	@ConfigItem(keyName = "lifetimeWastedMillis", name = "", description = "", hidden = true)
	void lifetimeWastedMillis(long lifetimeWastedMillis);
}
