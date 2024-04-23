package com.barrowspotential;

import lombok.val;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Wrapper for ClientThread that guards against case where work is invoked after the plugin is shut down.
 */
public final class ClientThreadMarshal
{
	private final AtomicReference<Queue<Runnable>> workQueue = new AtomicReference<>();
	private final ClientThread clientThread;

	@Inject
	public ClientThreadMarshal( ClientThread clientThread )
	{
		this.workQueue.set( new ConcurrentLinkedQueue<>() );
		this.clientThread = clientThread;
	}

	private void execute()
	{
		val work = workQueue.get();

		if ( work != null )
		{
			Runnable r = work.poll();

			while ( r != null )
			{
				r.run();

				r = work.poll();
			}
		}
	}

	/**
	 * Add to the work queue, running it immediately if on the client thread
	 */
	public void invoke( Runnable r )
	{
		val work = workQueue.get();

		if ( work != null )
		{
			work.add( r );

			clientThread.invoke( this::execute );
		}
	}

	/**
	 * Throw out the work queue, preventing current or future work from being invoked
	 */
	public void dispose()
	{
		workQueue.set( null );
	}
}
