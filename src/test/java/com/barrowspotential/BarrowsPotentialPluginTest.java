package com.barrowspotential;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BarrowsPotentialPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BarrowsPotentialPlugin.class);
		RuneLite.main(args);
	}
}