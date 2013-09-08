package uk.ac.ebi.fg.biosd.biosd2rdf.threading;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;

import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * TODO: Comment me!
 *
 * <dl><dt>date</dt><dd>2 Aug 2013</dd></dl>
 * @author Marco Brandizi
 *
 */
public abstract class PoolSizeTuner
{
	private int minThreads = 5, maxThreads = 200, maxThreadIncr = 50, minThreadIncr = 5;
	private double threadDeltaTolerance = 10d/100d;
	private int threadIncr;
	
	private int periodMins = 10;

	private int prevThreads;
	private long prevThroughput, prevCompletedTasks;
	
	private Timer poolSizeTunerTimer = null;
	
	protected Logger log = LoggerFactory.getLogger ( this.getClass () );

	private void run ()
	{
		final long curCompletedTasks = getCompletedTasks ();
		final long curThru = curCompletedTasks - prevCompletedTasks;
		final long deltaThru = curThru - prevThroughput;
		
		final double relDeltaThru = (double) deltaThru / prevThroughput;
		
		int curThreads = getThreadPoolSize ();
		
		if ( deltaThru > 0 && relDeltaThru > threadDeltaTolerance )
		{
			if ( threadIncr > 0 )
			{
				// Throughput increased after a thread pool enlargement, let's enlarge it again
				setThreadPoolSize ( min ( curThreads + threadIncr, maxThreads ) );
				threadIncr = min ( 2 * threadIncr, maxThreadIncr );
			}
			else if ( threadIncr < 0 )
			{
				// Throughput increased after a thread pool shrinking, let's shrink it again
				setThreadPoolSize ( max ( curThreads + threadIncr, minThreads ) );
				threadIncr = - min ( - 2 * threadIncr, maxThreadIncr );
			}
			else // threadIncr == 0 
				// throughput didn't increase in reaction to pool size variation, let's see if an increase amplifies this
				setThreadPoolSize ( min ( curThreads + ( threadIncr = minThreadIncr ), maxThreads ) );
		}
		else if ( deltaThru < 0 && -relDeltaThru > threadDeltaTolerance )
		{
			// The optimal is likely in between, do an average using the throughputs as weights
			long newPoolSize = round ( (double) ( prevThreads * prevThroughput + curThreads * curThru ) / ( prevThroughput + curThru ) );
			setThreadPoolSize ( (int) newPoolSize );

			if ( threadIncr > 0 )
			{
				// Throughput worsened after a thread pool enlargement, let's go back to a smaller pool
				threadIncr = - max ( (int) round ( threadIncr / 2d ), minThreadIncr );
			}
			else if ( threadIncr < 0 )
			{
				// Throughput worsened after a thread pool shrinking, let's start enlarging it
				threadIncr = min ( - 2 * threadIncr, maxThreadIncr );
			}
			else // threadIncr == 0 
				// throughput didn't decrease in reaction to pool size variation, let's see if a decrease can mitigate this
				setThreadPoolSize ( max ( curThreads + ( threadIncr = -minThreadIncr ), minThreads ) );
		}
		else
			// No significant throughput variation observed, let's zero the current thread increment and let's leave the pool
			// size as it is
			threadIncr = 0;

		if ( log.isTraceEnabled () )
			log.trace ( String.format ( 
				"Pool Size Tuner, throughput: %s, new increment: %d, new pool size: %s", curThru, threadIncr, getThreadPoolSize () 
		));

		prevCompletedTasks = curCompletedTasks;
		prevThroughput = curThru;
		
		prevThreads = curThreads;
	}

	protected abstract int getThreadPoolSize ();
	protected abstract void setThreadPoolSize ( int size );
	protected abstract long getCompletedTasks ();
	
	public void start ()
	{
		log.trace ( "Starting the thread pool tuner" );
		this.stop (); // Be sure it's off
		initVariables ();
		poolSizeTunerTimer = new Timer ( "Task Pool Size Optimiser" );
		poolSizeTunerTimer.scheduleAtFixedRate
		( 
			new TimerTask() 
			{
				@Override
				public void run () {
					PoolSizeTuner.this.run ();
				}
			}, 
			1000 * 60 * periodMins, 1000 * 60 * periodMins 
		);
	}
	
	public void stop ()
	{
		if ( poolSizeTunerTimer == null ) return; 
		poolSizeTunerTimer.cancel ();
		poolSizeTunerTimer = null;
		log.trace ( "Thread pool tuner stopped" );
	}
	
	private void initVariables ()
	{
		threadIncr = 20;

		prevThreads = getThreadPoolSize ();
		prevThroughput = 0; prevCompletedTasks = 0;
	}
	
}
