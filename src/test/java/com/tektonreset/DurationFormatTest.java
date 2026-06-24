package com.tektonreset;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class DurationFormatTest
{
	@Test
	public void formatsUnderAnHourAsMinutesSeconds()
	{
		assertEquals("0:00", DurationFormat.format(0));
		assertEquals("0:05", DurationFormat.format(5_000));
		assertEquals("1:30", DurationFormat.format(90_000));
		assertEquals("59:59", DurationFormat.format(3_599_000));
	}

	@Test
	public void formatsOverAnHourWithHours()
	{
		assertEquals("1:00:00", DurationFormat.format(3_600_000));
		assertEquals("2:03:04", DurationFormat.format((2 * 3600 + 3 * 60 + 4) * 1000L));
	}

	@Test
	public void clampsNegativeToZero()
	{
		assertEquals("0:00", DurationFormat.format(-1234));
	}
}
