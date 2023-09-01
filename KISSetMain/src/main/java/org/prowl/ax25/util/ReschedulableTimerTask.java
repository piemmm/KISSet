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

import java.util.Date;

/**
 * This class defines a timer task whose delivery time can be changed while the timer is pending delivery,
 * and can be rescheduled any number of times.
 *
 * @author Andrew Pavlin, KA2DDO
 */
abstract public class ReschedulableTimerTask implements Runnable, Comparable<ReschedulableTimerTask> {
    long wakeupTime = Long.MAX_VALUE;
    long repeatInterval = -1;
    private transient ReschedulableTimer queue = null;

    /**
     * Constructor for a ReschedulableTimerTask.
     */
    protected ReschedulableTimerTask() {
    }

    /**
     * Queue this task to the specified timer to be executed after the specified period of time.
     *
     * @param queue ReschedulableTimer to manage this task
     * @param delay interval in milliseconds before this task should be executed
     */
    public void resched(final ReschedulableTimer queue, long delay) {
        if (this.queue != null) {
            // remove from other timer queue
            this.queue.cancel(this);
            this.queue = null;
        }
        wakeupTime = System.currentTimeMillis() + delay;
        repeatInterval = -1;
        this.queue = queue;
        queue.sched(this);
    }

    /**
     * Queue this task to the specified timer to be executed after the specified period of time.
     *
     * @param queue          ReschedulableTimer to manage this task
     * @param delay          interval in milliseconds before this task should be executed
     * @param repeatInterval time in milliseconds after last scheduled run time that this task should run again
     */
    public void resched(final ReschedulableTimer queue, long delay, long repeatInterval) {
        if (this.queue != null && this.queue != queue) {
            // remove from other timer queue
            this.queue.cancel(this);
            this.queue = null;
        }
        wakeupTime = System.currentTimeMillis() + delay;
        this.repeatInterval = repeatInterval;
        this.queue = queue;
        queue.sched(this);
    }

    /**
     * Queue this task to the specified timer to be executed at the specified time.
     *
     * @param queue      ReschedulableTimer to manage this task
     * @param wakeupTime Date object defining the time at which this task should be executed (or as soon after as possible)
     */
    public void resched(final ReschedulableTimer queue, Date wakeupTime) {
        if (this.queue != null && this.queue != queue) {
            // remove from other timer queue
            this.queue.cancel(this);
            this.queue = null;
        }
        this.wakeupTime = wakeupTime.getTime();
        repeatInterval = -1;
        this.queue = queue;
        queue.sched(this);
    }

    /**
     * Queue this task to the specified timer to be executed at the specified time.
     *
     * @param queue          ReschedulableTimer to manage this task
     * @param wakeupTime     Date object defining the time at which this task should be executed (or as soon after as possible)
     * @param repeatInterval time in milliseconds after last scheduled run time that this task should run again
     */
    public void resched(final ReschedulableTimer queue, Date wakeupTime, long repeatInterval) {
        if (this.queue != null && this.queue != queue) {
            // remove from other timer queue
            this.queue.cancel(this);
            this.queue = null;
        }
        if (repeatInterval <= 1) {
            throw new IllegalArgumentException("repeat interval must be positive and > 1 millisecond");
        }
        this.wakeupTime = wakeupTime.getTime();
        this.repeatInterval = repeatInterval;
        this.queue = queue;
        queue.sched(this);
    }

    /**
     * Stop running this task, taking it off the pending queue.
     */
    public void cancel() {
        repeatInterval = -1; // so we don't start up again
        if (queue != null) {
            queue.cancel(this);
        }
    }

    /**
     * Test the order of this task relative to other tasks.
     *
     * @param o another ReschedulableTimerTask
     * @return -1 if this task is due earlier than the other task, 0 if due at the same time,
     * or +1 if this task is due after the other task
     */
    final public int compareTo(ReschedulableTimerTask o) {
        return Long.signum(wakeupTime - o.wakeupTime);
    }

    /**
     * Test if this is the same task instance.
     *
     * @param obj some object
     * @return boolean true if it is this object
     */
    @Override
    final public boolean equals(Object obj) {
        return this == obj;
    }

    /**
     * Get the hashcode for this object.
     *
     * @return hashcode
     */
    @Override
    final public int hashCode() {
        return System.identityHashCode(this);
    }

    /**
     * Test if this task is actively queued to a timer.
     *
     * @return if this task is scheduled to be executed
     */
    public boolean isActive() {
        return queue != null;
    }
}
