package org.prowl.ax25.util;
/*
 * Copyright (C) 2011-2021 Andrew Pavlin, KA2DDO
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

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * This attempts to make a simpler and faster FIFO queue than ArrayBlockingQueue with
 * no guarantees regarding fairness, minimum execution time, and minimum transient
 * memory allocations. Optimized for single producer and single consumer.
 *
 * @param <E> data type of queued records
 * @author Andrew Pavlin, KA2DDO
 */
public class FastBlockingQueue<E> extends AbstractCollection<E> implements BlockingQueue<E>, Serializable {
    private static final long serialVersionUID = -2643327221955157901L;
    private Object[] queue;
    // removal point
    private int head;
    // insertion point
    private int tail;

    /**
     * Create a FastBlockingQueue with the specified maximum queue backlog.
     *
     * @param capacity int maximum capacity of queue
     */
    public FastBlockingQueue(int capacity) {
        queue = new Object[capacity + 1];
        head = 0;
        tail = 0;
    }

    /**
     * Remove everything from the queue.
     */
    @Override
    public synchronized void clear() {
        head = 0;
        tail = 0;
        for (int i = queue.length - 1; i >= 0; i--) {
            queue[i] = null;
        }
    }

    /**
     * Removes all available elements from this queue and adds them
     * to the given collection. A failure
     * encountered while attempting to add elements to
     * collection <code>c</code> may result in elements being in neither,
     * either or both collections when the associated exception is
     * thrown.  Attempts to drain a queue to itself result in
     * <code>IllegalArgumentException</code>. Further, the behavior of
     * this operation is undefined if the specified collection is
     * modified while the operation is in progress.
     *
     * @param c the collection to transfer elements into
     * @return the number of elements transferred
     * @throws UnsupportedOperationException if addition of elements
     *                                       is not supported by the specified collection
     * @throws ClassCastException            if the class of an element of this queue
     *                                       prevents it from being added to the specified collection
     * @throws NullPointerException          if the specified collection is null
     * @throws IllegalArgumentException      if the specified collection is this
     *                                       queue, or some property of an element of this queue prevents
     *                                       it from being added to the specified collection
     */
    public synchronized int drainTo(Collection<? super E> c) {
        if (c == this) {
            throw new IllegalArgumentException("can't drain to self");
        }
        E entry;
        int count = 0;
        Object[] queue = this.queue; // avoid getfield opcode
        int head = this.head;
        while ((entry = (E) queue[head]) != null) {
            queue[head++] = null;
            if (head >= queue.length) {
                head = 0;
            }
            c.add(entry);
            count++;
        }
        if (count > 0) {
            this.head = head;
            notifyAll();
        }
        return count;
    }

    /**
     * Returns an iterator over the elements contained in this collection.
     *
     * @return an iterator over the elements contained in this collection
     */
    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private transient int start = head;

            public boolean hasNext() {
                return start != tail || queue[start] != null;
            }

            public E next() {
                @SuppressWarnings("unchecked")
                E answer = (E) queue[start];
                if (answer == null) {
                    throw new NoSuchElementException("empty queue");
                }
                start++; // don't clear queue, just iterate to end and then stop
                if (start >= queue.length) {
                    start = 0;
                }
                return answer;
            }

            public void remove() {
                throw new UnsupportedOperationException("don't allow removes from the middle of the queue");
            }
        };
    }

    /**
     * Returns the number of elements in this queue.
     *
     * @return the number of elements in this collection
     */
    @Override
    public synchronized int size() {
        int delta = tail - head;
        if (delta < 0) {
            // queue has wrapped
            delta += queue.length;
        } else if (delta == 0 && queue[head] != null) {
            delta = queue.length;
        }
        return delta;
    }

    /**
     * Returns the number of elements in this queue, not using synchronization to save time.
     * As such, the answer may be inaccurate, but it will be obtained quicker.
     *
     * @return the number of elements in this collection
     */
    public final int fastSize() {
        int delta;
        if ((delta = tail - head) < 0) {
            // queue has wrapped
            delta += queue.length;
        } else if (delta == 0 && queue[head] != null) {
            delta = queue.length;
        }
        return delta;
    }

    /**
     * Inserts the specified element into this queue, waiting if necessary
     * for space to become available.
     *
     * @param e the element to add
     * @throws InterruptedException     if interrupted while waiting
     * @throws ClassCastException       if the class of the specified element
     *                                  prevents it from being added to this queue
     * @throws NullPointerException     if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *                                  element prevents it from being added to this queue
     */
    public synchronized void put(E e) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException(getClass().getName() + " does not support null queue elements");
        }
        Object[] queue = this.queue;
        while (head == tail && queue[tail] != null) {
            wait();
        }
        int tail = this.tail;
        queue[tail++] = e;
        if (tail >= queue.length) {
            tail = 0;
        }
        this.tail = tail;
        notifyAll();
    }

    /**
     * Inserts all the non-null elements in the specified array into this queue, waiting
     * if necessary for sufficient space to become available.
     *
     * @param e      the array of elements to add
     * @param length the int length of the array to test (not all of array may be in use)
     * @throws InterruptedException     if interrupted while waiting
     * @throws ClassCastException       if the class of the specified element
     *                                  prevents it from being added to this queue
     * @throws NullPointerException     if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *                                  element prevents it from being added to this queue
     */
    public synchronized void putAll(E[] e, int length) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException(getClass().getName() + " does not support null arrays");
        }
        for (int i = 0; i < length; i++) {
            Object elem;
            if ((elem = e[i]) != null) {
                while (head == tail && queue[tail] != null) {
                    wait();
                }
                int tail = this.tail;
                queue[tail++] = elem;
                if (tail >= queue.length) { // have to check queue.length every time, as while blocked in the wait()
                    //   someone else might increase the queue capacity
                    tail = 0;
                }
                this.tail = tail;
                notifyAll();
            }
        }
    }

    /**
     * Inserts the specified element into this queue, waiting up to the
     * specified wait time if necessary for space to become available.
     *
     * @param e       the element to add
     * @param timeout how long to wait before giving up, in units of
     *                <code>unit</code>
     * @param unit    a <code>TimeUnit</code> determining how to interpret the
     *                <code>timeout</code> parameter
     * @return <code>true</code> if successful, or <code>false</code> if
     * the specified waiting time elapses before space is available
     * @throws InterruptedException     if interrupted while waiting
     * @throws ClassCastException       if the class of the specified element
     *                                  prevents it from being added to this queue
     * @throws NullPointerException     if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *                                  element prevents it from being added to this queue
     */
    public synchronized boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException(getClass().getName() + " does not support null queue elements");
        }
        long now = System.currentTimeMillis();
        long endTime = now + unit.toMillis(timeout);
        while (now < endTime && head == tail && queue[tail] != null) {
            wait(endTime - now);
            now = System.currentTimeMillis();
        }
        if (head == tail && queue[tail] != null) {
            return false;
        }
        queue[tail++] = e;
        if (tail >= queue.length) {
            tail = 0;
        }
        notifyAll();
        return true;
    }

    /**
     * Retrieves and removes the head of this queue, waiting if necessary
     * until an element becomes available.
     *
     * @return the head of this queue
     * @throws InterruptedException if interrupted while waiting
     */
    public synchronized E take() throws InterruptedException {
        Object[] queue = this.queue;
        while (queue[head] == null) {
            wait();
        }
        int head = this.head;
        @SuppressWarnings("unchecked")
        E answer = (E) queue[head];
        queue[head++] = null;
        if (head >= queue.length) {
            head = 0;
        }
        this.head = head;
        notifyAll();
        return answer;
    }

    /**
     * Retrieves and removes the head of this queue, waiting up to the
     * specified wait time if necessary for an element to become available.
     *
     * @param timeout how long to wait before giving up, in units of
     *                <code>unit</code>
     * @param unit    a <code>TimeUnit</code> determining how to interpret the
     *                <code>timeout</code> parameter
     * @return the head of this queue, or <code>null</code> if the
     * specified waiting time elapses before an element is available
     * @throws InterruptedException if interrupted while waiting
     */
    public synchronized E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long now = System.currentTimeMillis();
        long endTime = now + unit.toMillis(timeout);
        Object[] queue = this.queue;
        while (now < endTime && queue[head] == null) {
            wait(endTime - now);
            now = System.currentTimeMillis();
        }
        int head = this.head;
        @SuppressWarnings("unchecked")
        E answer = (E) queue[head];
        if (answer != null) {
            queue[head++] = null;
            if (head >= queue.length) {
                head = 0;
            }
            this.head = head;
            notifyAll();
        }
        return answer;
    }

    /**
     * Returns the number of additional elements that this queue can ideally
     * (in the absence of memory or resource constraints) accept without
     * blocking, or <code>Integer.MAX_VALUE</code> if there is no intrinsic
     * limit.
     * <p>Note that you <em>cannot</em> always tell if an attempt to insert
     * an element will succeed by inspecting <code>remainingCapacity</code>
     * because it may be the case that another thread is about to
     * insert or remove an element.
     *
     * @return the remaining capacity
     */
    public int remainingCapacity() {
        return queue.length - size();
    }

    /**
     * Removes at most the given number of available elements from
     * this queue and adds them to the given collection.  A failure
     * encountered while attempting to add elements to
     * collection <code>c</code> may result in elements being in neither,
     * either or both collections when the associated exception is
     * thrown.  Attempts to drain a queue to itself result in
     * <code>IllegalArgumentException</code>. Further, the behavior of
     * this operation is undefined if the specified collection is
     * modified while the operation is in progress.
     *
     * @param c           the collection to transfer elements into
     * @param maxElements the maximum number of elements to transfer
     * @return the number of elements transferred
     * @throws UnsupportedOperationException if addition of elements
     *                                       is not supported by the specified collection
     * @throws ClassCastException            if the class of an element of this queue
     *                                       prevents it from being added to the specified collection
     * @throws NullPointerException          if the specified collection is null
     * @throws IllegalArgumentException      if the specified collection is this
     *                                       queue, or some property of an element of this queue prevents
     *                                       it from being added to the specified collection
     */
    public synchronized int drainTo(Collection<? super E> c, int maxElements) {
        if (c == this) {
            throw new IllegalArgumentException("can't drain to self");
        }
        Object[] queue = this.queue; // avoid getfield opcode
        E entry;
        int count = 0;
        int head = this.head;
        final int qLength = queue.length;
        while ((entry = (E) queue[head]) != null && count < maxElements) {
            queue[head++] = null;
            if (head >= qLength) {
                head = 0;
            }
            c.add(entry);
            count++;
        }
        if (count > 0) {
            this.head = head;
            notifyAll();
        }
        return count;
    }

    /**
     * Removes at most the given number of available elements from
     * this queue and adds them to the given array.  A failure
     * encountered while attempting to add elements to
     * array <code>a</code> may result in elements being in neither,
     * either or both queue and array when the associated exception is
     * thrown. Further, the behavior of
     * this operation is undefined if the specified array is
     * modified while the operation is in progress.
     *
     * @param a the array to transfer elements into
     * @return the number of elements transferred
     * @throws UnsupportedOperationException if addition of elements
     *                                       is not supported by the specified collection
     * @throws ClassCastException            if the class of an element of this queue
     *                                       prevents it from being added to the specified collection
     * @throws NullPointerException          if the specified collection is null
     * @throws IllegalArgumentException      if the specified collection is this
     *                                       queue, or some property of an element of this queue prevents
     *                                       it from being added to the specified collection
     */
    public synchronized int drainTo(E[] a) {
        int maxElements = a.length;
        Object[] queue = this.queue; // avoid getfield opcode
        E entry;
        int count = 0;
        int head = this.head;
        boolean wasFull = head == this.tail;
        final int qLength = queue.length;
        while (count < maxElements && (entry = (E) queue[head]) != null) {
            queue[head++] = null;
            if (head >= qLength) {
                head = 0;
            }
            a[count++] = entry;
        }
        if (count > 0) {
            this.head = head;
            if (wasFull) {
                notifyAll();
            }
        }
        return count;
    }

    /**
     * Inserts the specified element into this queue if it is possible to do
     * so immediately without violating capacity restrictions.
     * When using a capacity-restricted queue, this method is generally
     * preferable to {@link #add}, which can fail to insert an element only
     * by throwing an exception.
     *
     * @param e the element to add
     * @return <code>true</code> if the element was added to this queue, else
     * <code>false</code>
     * @throws ClassCastException       if the class of the specified element
     *                                  prevents it from being added to this queue
     * @throws NullPointerException     if the specified element is null and
     *                                  this queue does not permit null elements
     * @throws IllegalArgumentException if some property of this element
     *                                  prevents it from being added to this queue
     */
    public synchronized boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException(getClass().getName() + " does not support null queue elements");
        }
        int tail; // avoid getfield opcode
        Object[] queue = this.queue;
        if ((tail = this.tail) == head && queue[tail] != null) {
            // queue is full
            return false;
        }
        queue[tail++] = e;
        if (tail >= queue.length) {
            tail = 0;
        }
        this.tail = tail;
        notifyAll();
        return true;
    }

    /**
     * Inserts the specified element into this queue if it is possible to do
     * so immediately without violating capacity restrictions.
     * When using a capacity-restricted queue, this method is generally
     * preferable to {@link #add}, which can fail to insert an element only
     * by throwing an exception.
     *
     * @param e the element to add
     * @return <code>true</code> if the element was added to this queue, else
     * <code>false</code>
     * @throws ClassCastException       if the class of the specified element
     *                                  prevents it from being added to this queue
     * @throws NullPointerException     if the specified element is null and
     *                                  this queue does not permit null elements
     * @throws IllegalArgumentException if some property of this element
     *                                  prevents it from being added to this queue
     */
    public synchronized boolean fastOffer(E e) {
        if (e == null) {
            throw new NullPointerException(getClass().getName() + " does not support null queue elements");
        }
        int tail; // avoid getfield opcode
        Object[] queue = this.queue;
        if ((tail = this.tail) == head && queue[tail] != null) {
            // queue is full
            notifyAll(); // get a consumer to make room
            return false;
        }
        queue[tail++] = e;
        if (tail >= queue.length) {
            tail = 0;
        }
        this.tail = tail;
        return true;
    }

    /**
     * Add an element to the queue, throwing an exception if the queue is full and has no
     * more room.
     *
     * @throws IllegalStateException if queue is already full
     */
    @Override
    public boolean add(E e) {
        if (!offer(e)) {
            throw new IllegalStateException("queue full");
        }
        return true;
    }

    /**
     * Retrieves and removes the head of this queue,
     * or returns <code>null</code> if this queue is empty.
     *
     * @return the head of this queue, or <code>null</code> if this queue is empty
     */
    public synchronized E poll() {
        Object[] queue = this.queue; // avoid getfield opcode
        int head = this.head;
        @SuppressWarnings("unchecked")
        E answer = (E) queue[head];
        if (answer != null) {
            queue[head++] = null;
            if (head >= queue.length) {
                head = 0;
            }
            this.head = head;
            notifyAll();
        }
        return answer;
    }

    /**
     * Retrieves, but does not remove, the head of this queue,
     * or returns <code>null</code> if this queue is empty.
     *
     * @return the head of this queue, or <code>null</code> if this queue is empty
     */
    public synchronized E peek() {
        return (E) queue[head];
    }

    /**
     * Retrieves, but does not remove, the head of this queue.  This method
     * differs from {@link #peek peek} only in that it throws an exception if
     * this queue is empty.
     *
     * <p>This implementation returns the result of <code>peek</code>
     * unless the queue is empty.
     *
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    public E element() {
        E x = peek();
        if (x != null)
            return x;
        else
            throw new NoSuchElementException();
    }

    /**
     * Retrieves and removes the head of this queue.  This method differs
     * from {@link #poll poll} only in that it throws an exception if this
     * queue is empty.
     *
     * <p>This implementation returns the result of <code>poll</code>
     * unless the queue is empty.
     *
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    public E remove() {
        E x = poll();
        if (x != null)
            return x;
        else
            throw new NoSuchElementException();
    }

    /**
     * Check if this queue contains the specified object (or at least an instance that matches by
     * equals()).
     */
    @Override
    public synchronized boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        for (int i = queue.length - 1; i >= 0; i--) {
            if (queue[i] != null && (queue[i] == o || queue[i].equals(o))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Report the total number of queue slots in the queue.
     *
     * @return maximum backlog capacity of the queue
     */
    public int getCapacity() {
        return queue.length;
    }

    /**
     * Enlarge the queue's backlog capacity. Note this is a no-op if the specified
     * parameter is less than or equal to the current capacity of the queue.
     *
     * @param capacity the new backlog capacity for the queue
     */
    public synchronized void expandCapacity(int capacity) {
        if (capacity > queue.length) {
            Object[] tmp = new Object[capacity];
            if (queue[head] != null) {
                if (head >= tail) {
                    System.arraycopy(queue, head, tmp, 0, queue.length - head);
                    System.arraycopy(queue, 0, tmp, queue.length - head, tail);
                    tail += queue.length - head;
                } else {
                    System.arraycopy(queue, head, tmp, 0, tail - head);
                    tail -= head;
                }
            } else {
                tail = 0;
            }
            head = 0;
            queue = tmp;
        }
    }
}
