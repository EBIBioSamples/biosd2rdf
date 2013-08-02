package uk.ac.ebi.fg.biosd.biosd2rdf.threading;

import java.util.TimerTask;

/**
 * 
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>2 Aug 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public abstract class PoolSizeTuningTimer extends TimerTask
{
	private int minThreads = 5, maxThreads = 200, maxThreadIncr = 50, minThreadIncr = 5;
	private int threadIncr = 25;
		
	private long prevThroughput = 0, prevCompletedTasks = 0;
	
	private double threadDeltaTolerance = 10d/100d;
	
	public PoolSizeTuningTimer ()
	{
	}

	@Override
	public void run ()
	{
		long curThru = getCompletedTasks () - prevCompletedTasks;
		long deltaThru = curThru - prevThroughput;
		
		double relDeltaThru = (double) deltaThru / prevThroughput;
		
		if ( deltaThru > 0 && relDeltaThru > threadDeltaTolerance )
		{
			if ( threadIncr > 0 )
			{
				// Throughput increased after a thread pool enlargement, let's enlarge it again
				setThreadPoolSize ( Math.max ( getThreadPoolSize() + threadIncr, maxThreads ) );
				threadIncr = Math.min ( 2 * threadIncr, maxThreadIncr );
			}
			else
			{
				// Throughput increased after a thread pool shrinking, let's shrink it again
				setThreadPoolSize ( Math.max ( getThreadPoolSize() + threadIncr, minThreads ) );
				threadIncr = Math.max ( 2 * threadIncr, -maxThreadIncr );
			}
		}
		else if ( deltaThru < 0 && -relDeltaThru > threadDeltaTolerance )
		{
			// The optimal is likely in between, do an average using the throughputs as weights
			int curThreads = getThreadPoolSize (), prevThreads = curThreads - threadIncr;
			long newPoolSize = Math.round ( (double) ( prevThreads * prevThroughput + curThreads * curThreads ) / ( prevThroughput + curThru ) );
			setThreadPoolSize ( (int) newPoolSize );

			if ( threadIncr > 0 )
			{
				// Throughput worsened after a thread pool enlargement, let's go back to a smaller pool
				threadIncr = - Math.min ( (int) Math.round ( threadIncr / 2d ), minThreadIncr );
			}
			else
			{
				// Throughput worsened after a thread pool shrinking, let's start enlarging it
				threadIncr = Math.max ( - 2 * threadIncr, maxThreadIncr );
			}
		}
	}

	protected abstract int getThreadPoolSize ();
	protected abstract void setThreadPoolSize ( int size );
	protected abstract long getCompletedTasks ();
}
