/* sbt -- Simple Build Tool
 * Copyright 2009, 2010  Mikko Peltonen, Stuart Roebuck, Mark Harrah
 */
package sbt

	import annotation.tailrec

object SourceModificationWatch
{
	@tailrec def watch(sourcesFinder: PathFinder, pollDelayMillis: Int, state: WatchState)(terminationCondition: => Boolean): (Boolean, WatchState) =
	{
			import state._

		def sourceFiles: Iterable[java.io.File] = sourcesFinder.get
		val (lastModifiedTime, fileCount) =
			( (0L, 0) /: sourceFiles) {(acc, file) => (math.max(acc._1, file.lastModified), acc._2 + 1)}

		val sourcesModified =
			lastModifiedTime > lastCallbackCallTime ||
			previousFileCount != fileCount

		val (triggered, newCallbackCallTime) =
			if (sourcesModified)
				(false, System.currentTimeMillis)
			else
				(awaitingQuietPeriod, lastCallbackCallTime)

		val newState = new WatchState(newCallbackCallTime, fileCount, sourcesModified, if(triggered) count + 1 else count)
		if(triggered)
			(true, newState)
		else
		{
			Thread.sleep(pollDelayMillis)
			if(terminationCondition)
				(false, newState)
			else
				watch(sourcesFinder, pollDelayMillis, newState)(terminationCondition)
		}
	}
}
final class WatchState(val lastCallbackCallTime: Long, val previousFileCount: Int, val awaitingQuietPeriod:Boolean, val count: Int)
object WatchState
{
	def empty = new WatchState(0L, 0, false, 0)
}