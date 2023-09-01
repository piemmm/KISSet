package org.prowl.ax25.util;
/*
 * Copyright (C) 2011-2020 Andrew Pavlin, KA2DDO
 * This file is part of YAAC (Yet Another APRS Client).
 *
 *  YAAC is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  YAAC is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  and GNU Lesser General Public License along with YAAC.  If not,
 *  see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Collections;

/**
 * This class implements a timer whose tasks' delivery times can be adjusted while they are enqueued, and
 * the task objects can be reused after they have been timed out or cancelled.
 *
 * @author Andrew Pavlin, KA2DDO
 */
public final class ReschedulableTimer extends Thread {
    private final ArrayList<ReschedulableTimerTask> timerQueue = new ArrayList<ReschedulableTimerTask>();
    private transient boolean needsResort = false;
    private boolean shutdown = false;

    /**
     * Create a ReschedulableTimer queue.
     *
     * @param name thread name String to assign to this timer (must not be null or empty)
     */
    public ReschedulableTimer(String name) {
        super(name);
        setDaemon(true);
        start();
    }

    synchronized void sched(ReschedulableTimerTask task) {
        final ArrayList<ReschedulableTimerTask> timerQueue = this.timerQueue;
        if (!timerQueue.contains(task)) {
            int pos = 0;
            if (timerQueue.size() > 0) {
                pos = Collections.binarySearch(timerQueue, task);
                if (pos < 0) {
                    pos = -1 - pos;
                }
            }
            timerQueue.add(pos, task);
            if (pos == 0) {
                notifyAll();
            }
        } else {
            needsResort = true;
            notifyAll();
        }
    }

    synchronized void cancel(ReschedulableTimerTask task) {
        final ArrayList<ReschedulableTimerTask> timerQueue = this.timerQueue;
        for (int i = timerQueue.size() - 1; i >= 0; i--) {
            if (timerQueue.get(i) == task) {
                timerQueue.remove(i);
                break;
            }
        }
    }

    /**
     * Execute the timer tasks queued to this timer.
     */
    @Override
    public void run() {
        while (!shutdown) {
            synchronized (this) {
                if (needsResort) {
                    needsResort = false;
                    if (timerQueue.size() > 1) {
                        Collections.sort(timerQueue);
                    }
                }
            }

            long now = System.currentTimeMillis();
            while (timerQueue.size() > 0) {
                ReschedulableTimerTask nextTask;
                synchronized (this) {
                    if (timerQueue.size() > 0) {
                        nextTask = timerQueue.get(0);
                        if (nextTask.wakeupTime > now) {
                            break;
                        }
                        timerQueue.remove(0);
                    } else {
                        continue;
                    }
                }

                try {
                    nextTask.run();
                    if (nextTask.repeatInterval > 1) {
                        while (nextTask.wakeupTime <= now) {
                            // coalesce if we are really late
                            nextTask.wakeupTime += nextTask.repeatInterval;
                        }
                        sched(nextTask);
                    }
                } catch (Throwable e) {
                    e.printStackTrace(System.out);
                }

                now = System.currentTimeMillis();
            }

            synchronized (this) {
                long howLong = Long.MAX_VALUE;
                if (timerQueue.size() > 0) {
                    ReschedulableTimerTask nextTask = timerQueue.get(0);
                    howLong = nextTask.wakeupTime - now;
                }
                try {
                    wait(howLong);
                } catch (InterruptedException e) {
                    e.printStackTrace(System.out);
                }
            }
        }

        // clean up upon termination
        timerQueue.clear();
    }

    /**
     * Stop this timer queue and release all of its resources.
     */
    public void shutdown() {
        if (!isAlive()) {
            throw new IllegalThreadStateException("timer already stopped");
        }
        synchronized (this) {
            shutdown = true;
            notifyAll();
        }
    }

    /**
     * Called by the garbage collector on an object when garbage collection
     * determines that there are no more references to the object.
     * A subclass overrides the <code>finalize</code> method to dispose of
     * system resources or to perform other cleanup.
     *
     * @throws Throwable the <code>Exception</code> raised by this method
     */
    @Override
    protected void finalize() throws Throwable {
        synchronized (this) {
            shutdown = true;
            notifyAll();
        }
        super.finalize();
    }
}
