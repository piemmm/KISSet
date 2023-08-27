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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;

/**
 * This class provides an alternative to the Java PermGen heap section used for String.intern(),
 * such that applications won't run out of Java PermGen space while still being able to share
 * constant object declarations.
 *
 * @author Andrew Pavlin, KA2DDO
 */
public abstract class ShareableObjectCache<T> extends AbstractSet<T> {
    /**
     * The default initial capacity -- MUST be a power of two.
     */
    private static final int DEFAULT_INITIAL_CAPACITY = 4096;

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The load fast used when none specified in constructor.
     */
    private static final float DEFAULT_LOAD_FACTOR = 2.0f;
    /**
     * The load factor for the hash table.
     */
    private final float loadFactor;
    /**
     * Reference queue for cleared WeakEntries
     */
    private final ReferenceQueue<T> queue = new ReferenceQueue<T>();
    /**
     * The table, resized as necessary. Length MUST Always be a power of two.
     */
    private Entry<T>[] table;
    /**
     * The number of key-value mappings contained in this weak hash set.
     */
    private int size;
    /**
     * The next size value at which to resize (capacity * load factor).
     */
    private int threshold;
    /**
     * The number of times this WeakHashSet has been structurally modified.
     * Structural modifications are those that change the number of
     * mappings in the map or otherwise modify its internal structure
     * (e.g., rehash).  This field is used to make iterators on
     * Collection-views of the map fail-fast.
     *
     * @see ConcurrentModificationException
     */
    private int modCount;

    /**
     * Constructs a new, empty <code>ShareableObjectCache</code> with the given initial
     * capacity and the given load factor.
     *
     * @param initialCapacity The initial capacity of the <code>ShareableObjectCache</code>
     * @param loadFactor      The load factor of the <code>ShareableObjectCache</code>
     * @throws IllegalArgumentException if the initial capacity is negative,
     *                                  or if the load factor is nonpositive.
     */
    protected ShareableObjectCache(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal Initial Capacity: " +
                    initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;

        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal Load factor: " +
                    loadFactor);
        int capacity = 1;
        while (capacity < initialCapacity)
            capacity <<= 1;
        table = new Entry[capacity];
        this.loadFactor = loadFactor;
        threshold = (int) (capacity * loadFactor);
    }

    /**
     * Constructs a new, empty <code>ShareableObjectCache</code> with the given initial
     * capacity and the default load factor (2.0).
     *
     * @param initialCapacity The initial capacity of the <code>ShareableObjectCache</code>
     * @throws IllegalArgumentException if the initial capacity is negative
     */
    protected ShareableObjectCache(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs a new, empty <code>ShareableObjectCache</code> with the default initial
     * capacity and load factor.
     */
    protected ShareableObjectCache() {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        threshold = (int) (DEFAULT_INITIAL_CAPACITY * DEFAULT_LOAD_FACTOR);
        table = new Entry[DEFAULT_INITIAL_CAPACITY];
    }

    // internal utilities

    /**
     * Applies a supplemental hash function to a given hashCode, which
     * defends against poor quality hash functions.  This is critical
     * because HashMap uses power-of-two length hash tables, that
     * otherwise encounter collisions for hashCodes that do not differ
     * in lower bits. Note: Null keys always map to hash 0, thus index 0.
     */
    static int hash(int h) {
        // This function ensures that hashCodes that differ only by
        // constant multiples at each bit position have a bounded
        // number of collisions (approximately 8 at default load factor).
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }

    /**
     * Expunges stale entries from the table.
     */
    private void expungeStaleEntries() {
        Entry<T> e;
        while ((e = (Entry<T>) queue.poll()) != null) {
            int h = e.hash;
            int i = h & (table.length - 1);

            Entry<T> prev = table[i];
            Entry<T> p = prev;
            while (p != null) {
                Entry<T> next = p.next;
                if (p == e) {
                    if (prev == e)
                        table[i] = next;
                    else
                        prev.next = next;
                    e.next = null;  // Help GC
                    size--;
                    break;
                }
                prev = p;
                p = next;
            }
        }
    }

    /**
     * Returns the table after first expunging stale entries.
     */
    private Entry<T>[] getTable() {
        expungeStaleEntries();
        return table;
    }

    /**
     * Returns the number of key-value mappings in this map.
     * This result is a snapshot, and may not reflect unprocessed
     * entries that will be removed before next attempted access
     * because they are no longer referenced.
     */
    public int size() {
        if (size == 0)
            return 0;
        expungeStaleEntries();
        return size;
    }

    /**
     * Returns <code>true</code> if this set contains no entries.
     * This result is a snapshot, and may not reflect unprocessed
     * entries that will be removed before next attempted access
     * because they are no longer referenced.
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns the interned key to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
     * key.equals(k))}, then this method returns {@code v}; otherwise
     * it returns {@code null}.  (There can be at most one such mapping.)
     *
     * @see #add(Object)
     */
    private T get(Object key, int h) {
        Entry[] tab = table; // don't expunge if we're only reading
        int index = h & (tab.length - 1);
        Entry<T> e = tab[index];
        while (e != null) {
            T y;
            if (e.hash == h && ((y = e.get()) == key || key.equals(y)))
                return y;
            e = e.next;
        }
        return null;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for this key, the old
     * value is preserved (since it is a duplicate, by the definition and
     * purpose of this class).
     *
     * @param key key with which the specified value is to be associated.
     * @return boolean true if this was a new entry in the cache
     */
    public synchronized boolean add(T key) {
        int h = hash(key.hashCode());
        Entry<T>[] tab = getTable();
        int i = h & (tab.length - 1);

        for (Entry<T> e = tab[i]; e != null; e = e.next) {
            Object y = e.get();
            if (h == e.hash && (key == y || key.equals(y))) {
                return false;
            }
        }

        modCount++;
        tab[i] = new Entry<T>(key, queue, h, tab[i]);
        if (++size >= threshold)
            resize(tab.length * 2);
        return true;
    }

    /**
     * Look for the specified key in the cache.
     *
     * @param t T hashable object
     * @return cached equal-value of t (or t itself if never listed in cache before)
     */
    public T internKey(T t) {
        if (t == null) {
            return null; // don't put null in our cache
        }
        int h = hash(t.hashCode());
        T tmp;
        if ((tmp = get(t, h)) != null) {
            return tmp;
        }
        synchronized (this) {
            // in case of a collision for the same string value
            if ((tmp = get(t, h)) != null) {
                return tmp;
            }

            // no competition, add the new value
            Entry<T>[] tab = getTable();
            int i = h & (tab.length - 1);

            modCount++;
            tab[i] = new Entry<T>(t, queue, h, tab[i]);
            if (++size >= threshold)
                resize(tab.length * 2);
            return t;
        }
    }

    /**
     * Rehashes the contents of this map into a new array with a
     * larger capacity.  This method is called automatically when the
     * number of keys in this map reaches its threshold.
     * <p>
     * If current capacity is MAXIMUM_CAPACITY, this method does not
     * resize the map, but sets threshold to Integer.MAX_VALUE.
     * This has the effect of preventing future calls.
     *
     * @param newCapacity the new capacity, MUST be a power of two;
     *                    must be greater than current capacity unless current
     *                    capacity is MAXIMUM_CAPACITY (in which case value
     *                    is irrelevant).
     */
    void resize(int newCapacity) {
        Entry[] oldTable = table;
        int oldCapacity = oldTable.length;
        if (oldCapacity == MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }

        @SuppressWarnings("unchecked")
        Entry<T>[] newTable = new Entry[newCapacity];
        transfer(oldTable, newTable);
        table = newTable;

        /*
         * If ignoring null elements and processing ref queue caused massive
         * shrinkage, then restore old table.  This should be rare, but avoids
         * unbounded expansion of garbage-filled tables.
         */
        if (size >= threshold / 2) {
            threshold = (int) (newCapacity * loadFactor);
        } else {
            expungeStaleEntries();
            transfer(newTable, oldTable);
            table = oldTable;
        }
    }

    /**
     * Transfers all entries from src to dest tables
     */
    private void transfer(Entry[] src, Entry[] dest) {
        final int srcLength = src.length;
        final int destMask = dest.length - 1;
        for (int j = 0; j < srcLength; ++j) {
            Entry<T> e = src[j];
            src[j] = null;
            while (e != null) {
                Entry<T> next = e.next;
                Object key = e.get();
                if (key == null) {
                    e.next = null;  // Help GC
                    size--;
                } else {
                    int i = e.hash & destMask;
                    e.next = dest[i];
                    dest[i] = e;
                }
                e = next;
            }
        }
    }

    /**
     * Removes the entry for a key from this cache if it is present.
     * More formally, if this cache contains an entry for key <code>k</code>
     * such that <code>(key==null ?  k==null :
     * key.equals(k))</code>, that mapping is removed.  (The map can contain
     * at most one such mapping.)
     *
     * <p>Returns the value to which this map previously associated the key,
     * or <code>null</code> if the map contained no mapping for the key.  A
     * return value of <code>null</code> does not <i>necessarily</i> indicate
     * that the map contained no mapping for the key; it's also possible
     * that the map explicitly mapped the key to <code>null</code>.
     *
     * <p>The map will not contain a mapping for the specified key once the
     * call returns.
     *
     * @param key key whose mapping is to be removed from the map
     * @return boolean true if an entry formerly existed matching the key
     */
    public boolean remove(Object key) {
        int h = hash(key.hashCode());
        Entry<T>[] tab = getTable();
        int i = h & (tab.length - 1);
        Entry<T> prev = tab[i];
        Entry<T> e = prev;

        while (e != null) {
            Entry<T> next = e.next;
            Object y = e.get();
            if (h == e.hash && (key == y || key.equals(y))) {
                modCount++;
                size--;
                if (prev == e)
                    tab[i] = next;
                else
                    prev.next = next;
                return true;
            }
            prev = e;
            e = next;
        }

        return false;
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
    public void clear() {
        // clear out ref queue. We don't need to expunge entries
        // since table is getting cleared.
        while (queue.poll() != null)
            ;

        modCount++;
        Entry[] tab = table;
        for (int i = 0; i < tab.length; ++i)
            tab[i] = null;
        size = 0;

        // Allocation of array may have caused GC, which may have caused
        // additional entries to go stale.  Removing these entries from the
        // reference queue will make them eligible for reclamation.
        while (queue.poll() != null)
            ;
    }

    abstract protected Class getType();

    /**
     * Returns a string description of this collection.
     *
     * @return a string description of this collection
     */
    @Override
    public String toString() {
        return getType().getSimpleName() + "[#=" + size() + ']';
    }

    /**
     * Returns an iterator over the elements contained in this collection.
     *
     * @return an iterator over the elements contained in this collection
     */
    public Iterator<T> iterator() {
        return new KeyIterator();
    }

    /**
     * The entries in this hash table extend WeakReference, using its main ref
     * field as the key.
     */
    private static class Entry<T> extends WeakReference<T> {
        private final int hash;
        private Entry<T> next;

        /**
         * Creates new entry.
         */
        Entry(T key,
              ReferenceQueue<T> queue,
              int hash, Entry<T> next) {
            super(key, queue);
            this.hash = hash;
            this.next = next;
        }

        public T getKey() {
            return get();
        }

        public boolean equals(Object o) {
            if (!(o instanceof Entry e))
                return false;
            Object k1 = get();
            Object k2 = e.get();
            return Objects.equals(k1, k2);
        }

        public int hashCode() {
            Object k = get();
            return (k == null ? 0 : k.hashCode());
        }

        public String toString() {
            return String.valueOf(getKey());
        }
    }

    private abstract class HashIterator<T> implements Iterator<T> {
        int index;
        Entry<T> entry = null;
        Entry<T> lastReturned = null;
        int expectedModCount = modCount;

        /**
         * Strong reference needed to avoid disappearance of key
         * between hasNext and next
         */
        Object nextKey = null;

        /**
         * Strong reference needed to avoid disappearance of key
         * between nextEntry() and any use of the entry
         */
        Object currentKey = null;

        HashIterator() {
            index = (size() != 0 ? table.length : 0);
        }

        public boolean hasNext() {
            Entry[] t = table;

            while (nextKey == null) {
                Entry<T> e = entry;
                int i = index;
                while (e == null && i > 0)
                    e = t[--i];
                entry = e;
                index = i;
                if (e == null) {
                    currentKey = null;
                    return false;
                }
                nextKey = e.get(); // hold on to key in strong ref
                if (nextKey == null)
                    entry = entry.next;
            }
            return true;
        }

        /**
         * The common parts of next() across different types of iterators
         */
        protected Entry<T> nextEntry() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (nextKey == null && !hasNext())
                throw new NoSuchElementException();

            lastReturned = entry;
            entry = entry.next;
            currentKey = nextKey;
            nextKey = null;
            return lastReturned;
        }

        public void remove() {
            if (lastReturned == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();

            ShareableObjectCache.this.remove(currentKey);
            expectedModCount = modCount;
            lastReturned = null;
            currentKey = null;
        }

    }

    private class KeyIterator extends HashIterator<T> {
        public T next() {
            return nextEntry().getKey();
        }
    }
}
