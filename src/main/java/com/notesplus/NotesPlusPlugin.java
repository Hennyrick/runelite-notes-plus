package com.notesplus;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@Slf4j
@PluginDescriptor(
	name = "Notes Plus",
	description = "In-game notes workspace with folders and editor",
	tags = {"notes", "organization", "utility"}
)
public class NotesPlusPlugin extends Plugin
{
	@Inject
	private ClientToolbar clientToolbar;


	private NotesPlusPanel panel;
	private NavigationButton navigationButton;

	@Override
	protected void startUp()
	{
		panel = new NotesPlusPanel();
		BufferedImage icon = createPlaceholderIcon();
		navigationButton = NavigationButton.builder()
			.tooltip("Notes Plus")
			.icon(icon)
			.priority(5)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
		log.debug("Notes Plus started");
	}

	@Override
	protected void shutDown()
	{
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
			navigationButton = null;
		}

		panel = null;
		log.debug("Notes Plus stopped");
	}

	private BufferedImage createPlaceholderIcon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(new Color(46, 125, 50));
		graphics.fillRoundRect(1, 1, 14, 14, 4, 4);
		graphics.setColor(Color.WHITE);
		graphics.fillRect(4, 4, 8, 2);
		graphics.fillRect(4, 8, 8, 2);
		graphics.dispose();
		return image;
	}

	@Provides
	NotesPlusConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(NotesPlusConfig.class);
	}
}
