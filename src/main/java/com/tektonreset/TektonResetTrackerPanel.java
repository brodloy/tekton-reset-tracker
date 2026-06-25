package com.tektonreset;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.QuantityFormatter;

/**
 * Side panel: live statistics only. All configuration lives in the plugin's
 * settings (the gear icon).
 */
class TektonResetTrackerPanel extends PluginPanel
{
	private static final Dimension MIN_SIZE = new Dimension(PluginPanel.PANEL_WIDTH, 240);
	private static final Color TEKTON_ORANGE = new Color(214, 122, 48);
	private static final Color COMPLETE_GREEN = new Color(0, 200, 83);

	private final TektonResetTrackerPlugin plugin;

	private final JLabel lifetimeResets = valueLabel();
	private final JLabel lifetimeWasted = valueLabel();
	private final JLabel sessionResets = valueLabel();
	private final JLabel sessionWasted = valueLabel();
	private final JLabel currentStatus = valueLabel();

	private final JButton copyButton = new JButton("Copy stats");

	TektonResetTrackerPanel(TektonResetTrackerPlugin plugin)
	{
		this.plugin = plugin;

		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setMinimumSize(MIN_SIZE);

		final JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		final JLabel title = new JLabel("Tekton Reset Tracker");
		title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
		title.setForeground(TEKTON_ORANGE);
		title.setAlignmentX(Component.LEFT_ALIGNMENT);
		content.add(title);

		final JLabel subtitle = new JLabel("Challenge Mode only");
		subtitle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 11f));
		subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
		content.add(subtitle);
		content.add(Box.createVerticalStrut(10));

		content.add(buildAllTimeCard());
		content.add(Box.createVerticalStrut(10));
		content.add(buildSessionCard());
		content.add(Box.createVerticalStrut(10));
		content.add(buildCurrentCard());
		content.add(Box.createVerticalStrut(10));
		content.add(fill(buildButtonRow()));

		add(content, BorderLayout.NORTH);

		update();
	}

	private JPanel buildAllTimeCard()
	{
		final JPanel card = card();
		card.add(header("All-time"));
		card.add(statRow("Resets", lifetimeResets));
		card.add(statRow("Time wasted", lifetimeWasted));
		return card;
	}

	private JPanel buildSessionCard()
	{
		final JPanel card = card();
		card.add(header("This session"));
		card.add(statRow("Resets", sessionResets));
		card.add(statRow("Time wasted", sessionWasted));
		return card;
	}

	private JPanel buildCurrentCard()
	{
		final JPanel card = card();
		card.add(header("Current raid"));
		card.add(statRow("Status", currentStatus));
		return card;
	}

	private JPanel buildButtonRow()
	{
		final JPanel row = new JPanel(new GridLayout(1, 2, 8, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);

		final JButton reset = new JButton("Reset");
		reset.setToolTipText("Clear all lifetime and session counts");
		reset.addActionListener(e -> plugin.reset());

		copyButton.setToolTipText("Copy your Tekton reset stats to the clipboard");
		copyButton.addActionListener(e -> copyStats());

		row.add(reset);
		row.add(copyButton);
		return row;
	}

	private void copyStats()
	{
		final String text = "Tekton Reset Tracker\n"
			+ "All-time: " + plugin.getLifetimeResets() + " resets ("
			+ DurationFormat.format(plugin.getLifetimeWastedMillis()) + " wasted)\n"
			+ "Session: " + plugin.getSessionResets() + " resets ("
			+ DurationFormat.format(plugin.getSessionWastedMillis()) + " wasted)";

		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);

		copyButton.setText("Copied!");
		final Timer timer = new Timer(1500, e -> copyButton.setText("Copy stats"));
		timer.setRepeats(false);
		timer.start();
	}

	/**
	 * Refresh the displayed numbers. Must be called on the Swing Event Dispatch Thread.
	 */
	void update()
	{
		lifetimeResets.setText(QuantityFormatter.formatNumber(plugin.getLifetimeResets()));
		lifetimeWasted.setText(DurationFormat.format(plugin.getLifetimeWastedMillis()));
		sessionResets.setText(QuantityFormatter.formatNumber(plugin.getSessionResets()));
		sessionWasted.setText(DurationFormat.format(plugin.getSessionWastedMillis()));

		if (!plugin.isInRaid())
		{
			currentStatus.setText("Not in a raid");
			currentStatus.setForeground(Color.LIGHT_GRAY);
		}
		else if (!plugin.isCurrentRaidChallengeMode())
		{
			currentStatus.setText("Normal raid (not tracked)");
			currentStatus.setForeground(Color.LIGHT_GRAY);
		}
		else if (plugin.hasProgressedPastTekton())
		{
			currentStatus.setText("Moved past Tekton");
			currentStatus.setForeground(COMPLETE_GREEN);
		}
		else if (plugin.isTektonCompleted())
		{
			currentStatus.setText("Completed Tekton - " + DurationFormat.format(plugin.getCurrentRaidMillis()));
			currentStatus.setForeground(TEKTON_ORANGE);
		}
		else
		{
			currentStatus.setText("CM raid - " + DurationFormat.format(plugin.getCurrentRaidMillis()));
			currentStatus.setForeground(TEKTON_ORANGE);
		}
	}

	// --------------------------------- Small helpers ---------------------------------

	private static JPanel card()
	{
		final JPanel card = new JPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setAlignmentX(Component.LEFT_ALIGNMENT);
		return card;
	}

	private static JLabel header(String text)
	{
		final JLabel label = new JLabel(text);
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
		return label;
	}

	private static JLabel valueLabel()
	{
		final JLabel label = new JLabel("0");
		label.setHorizontalAlignment(SwingConstants.RIGHT);
		return label;
	}

	/** A caption on the left and a value on the right, full width. */
	private static JPanel statRow(String caption, JLabel value)
	{
		final JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		final JLabel cap = new JLabel(caption);
		cap.setForeground(Color.LIGHT_GRAY);
		row.add(cap, BorderLayout.WEST);
		row.add(value, BorderLayout.CENTER);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
		return row;
	}

	/** Stretch a control to full width while keeping its natural height. */
	private static <T extends JComponent> T fill(T component)
	{
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
		component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
		return component;
	}
}
