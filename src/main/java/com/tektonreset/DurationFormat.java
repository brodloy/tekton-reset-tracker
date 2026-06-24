package com.tektonreset;

/**
 * Formats a millisecond duration as a compact clock string: {@code m:ss} under an hour,
 * {@code h:mm:ss} once it passes an hour. Used for the "time wasted" figures.
 */
final class DurationFormat
{
	private DurationFormat()
	{
	}

	static String format(long millis)
	{
		long totalSeconds = Math.max(0L, millis) / 1000L;
		final long hours = totalSeconds / 3600L;
		totalSeconds %= 3600L;
		final long minutes = totalSeconds / 60L;
		final long seconds = totalSeconds % 60L;

		if (hours > 0)
		{
			return String.format("%d:%02d:%02d", hours, minutes, seconds);
		}
		return String.format("%d:%02d", minutes, seconds);
	}
}
