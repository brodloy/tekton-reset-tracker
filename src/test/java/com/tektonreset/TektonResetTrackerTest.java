package com.tektonreset;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class TektonResetTrackerTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(TektonResetTrackerPlugin.class);
		RuneLite.main(args);
	}
}
