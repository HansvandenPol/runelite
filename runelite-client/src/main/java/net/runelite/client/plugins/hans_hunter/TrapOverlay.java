/*
 * Copyright (c) 2017, Robin Weymans <Robin.weymans@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.hans_hunter;

import java.awt.*;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ProgressPieComponent;
import net.runelite.client.util.ColorUtil;

/**
 * Represents the overlay that shows timers on traps that are placed by the
 * player.
 */
public class TrapOverlay extends Overlay
{
	/**
	 * The timer is low when only 25% is left.
	 */
	private static final double TIMER_LOW = 0.25; // When the timer is under a quarter left, if turns red.

	private final Client client;
	private final HansHunterPlugin plugin;
	private final HansHunterConfig config;

	private Color colorOpen, colorOpenBorder, colorOpenOldBorder;
	private Color colorEmpty, colorEmptyBorder, colorEmptyOldBorder;
	private Color colorFull, colorFullBorder, colorFullOldBorder;
	private Color colorTrans, colorTransBorder;

	@Inject
	TrapOverlay(Client client, HansHunterPlugin plugin, HansHunterConfig config)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		this.plugin = plugin;
		this.config = config;
		this.client = client;
		colorOpenOldBorder = ColorUtil.fromHex("515904");
		colorEmptyOldBorder = ColorUtil.fromHex("401002");
		colorFullOldBorder = ColorUtil.fromHex("02610b");
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		drawTraps(graphics);
		return null;
	}

	/**
	 * Updates the timer colors.
	 */
	public void updateConfig()
	{
		colorEmptyBorder = config.getEmptyTrapColor();
		colorEmpty = ColorUtil.colorWithAlpha(colorEmptyBorder, (int)(colorEmptyBorder.getAlpha() / 2.5));
		colorFullBorder = config.getFullTrapColor();
		colorFull = ColorUtil.colorWithAlpha(colorFullBorder, (int)(colorFullBorder.getAlpha() / 2.5));
		colorOpenBorder = config.getOpenTrapColor();
		colorOpen = ColorUtil.colorWithAlpha(colorOpenBorder, (int)(colorOpenBorder.getAlpha() / 2.5));
		colorTransBorder = config.getTransTrapColor();
		colorTrans = ColorUtil.colorWithAlpha(colorTransBorder, (int)(colorTransBorder.getAlpha() / 2.5));
	}

	/**
	 * Iterates over all the traps that were placed by the local player, and
	 * draws a circle or a timer on the trap, depending on the trap state.
	 *
	 * @param graphics
	 */
	private void drawTraps(Graphics2D graphics)
	{
		for (Map.Entry<WorldPoint, HansHunterTrap> entry : plugin.getTraps().entrySet())
		{
			HansHunterTrap trap = entry.getValue();

			switch (trap.getState())
			{
				case OPEN:
					drawTimerOnTrap(graphics, trap, colorOpen, colorOpenBorder, colorEmpty, colorOpenBorder, colorOpenOldBorder);
					break;
				case EMPTY:
					drawTimerOnTrap(graphics, trap, colorEmpty, colorEmptyBorder, colorEmpty, colorEmptyBorder, colorEmptyOldBorder);
					break;
				case FULL:
					drawTimerOnTrap(graphics, trap, colorFull, colorFullBorder, colorFull, colorFullBorder, colorFullOldBorder);
					break;
				case TRANSITION:
					drawCircleOnTrap(graphics, trap, colorTrans, colorTransBorder);
					break;
			}
		}
	}

	/**
	 * Draws a timer on a given trap.
	 *
	 * @param graphics
	 * @param trap The trap on which the timer needs to be drawn
	 * @param fill The fill color of the timer
	 * @param border The border color of the timer
	 * @param fillTimeLow The fill color of the timer when it is low
	 * @param borderTimeLow The border color of the timer when it is low
	 */
	private void drawTimerOnTrap(Graphics2D graphics, HansHunterTrap trap, Color fill, Color border, Color fillTimeLow, Color borderTimeLow, Color borderOld)
	{

		if (trap.getWorldLocation().getPlane() != client.getPlane())
		{
			return;
		}
		LocalPoint localLoc = LocalPoint.fromWorld(client, trap.getWorldLocation());
		if (localLoc == null)
		{
			return;
		}
		net.runelite.api.Point loc = Perspective.localToCanvas(client, localLoc, client.getPlane());

		if (loc == null)
		{
			return;
		}

		double timeLeft = 1 - trap.getTrapTimeRelative();

		ProgressPieComponent pie = new ProgressPieComponent();

		if(timeLeft <= TIMER_LOW) {
			pie.setBorderColor(borderTimeLow);
		} else if(timeLeft <= 0.5) {
			pie.setBorderColor(borderOld);
		} else {
			pie.setBorderColor(border);
		}
		pie.setFill(timeLeft > TIMER_LOW ? fill : fillTimeLow);
		pie.setStroke(new BasicStroke(5));
		pie.setPosition(loc);
		pie.setProgress(timeLeft);
		pie.render(graphics);
	}

	/**
	 * Draws a timer on a given trap.
	 *
	 * @param graphics
	 * @param trap The trap on which the timer needs to be drawn
	 * @param fill The fill color of the timer
	 * @param border The border color of the timer
	 */
	private void drawCircleOnTrap(Graphics2D graphics, HansHunterTrap trap, Color fill, Color border)
	{
		if (trap.getWorldLocation().getPlane() != client.getPlane())
		{
			return;
		}
		LocalPoint localLoc = LocalPoint.fromWorld(client, trap.getWorldLocation());
		if (localLoc == null)
		{
			return;
		}
		net.runelite.api.Point loc = Perspective.localToCanvas(client, localLoc, client.getPlane());

		ProgressPieComponent pie = new ProgressPieComponent();
		pie.setFill(fill);
		pie.setBorderColor(border);
		pie.setPosition(loc);
		pie.setProgress(1);
		pie.render(graphics);
	}
}
