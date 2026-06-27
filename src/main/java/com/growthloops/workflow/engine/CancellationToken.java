package com.growthloops.workflow.engine;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Per-execution cancellation handle.
 * <p>
 * Cancellation works on two levels:
 * <ul>
 *   <li>a flag the engine polls between steps, and</li>
 *   <li>an interrupt delivered to the worker thread so a step blocked in-flight
 *       (e.g. a {@code delay} sleeping) wakes immediately instead of running to
 *       completion before the flag is next checked.</li>
 * </ul>
 * The engine calls {@link #attachWorker(Thread)} when it begins running so a later
 * {@link #cancel()} from another thread can interrupt the right thread.
 */
public class CancellationToken {

    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private volatile Thread worker;

    /** Records the thread currently executing the workflow, so cancel() can interrupt it. */
    public void attachWorker(Thread worker) {
        this.worker = worker;
    }

    /** Requests cancellation: sets the flag and interrupts the worker thread if attached. */
    public void cancel() {
        cancelled.set(true);
        Thread t = worker;
        if (t != null) {
            t.interrupt();
        }
    }

    public boolean isCancellationRequested() {
        return cancelled.get();
    }
}
