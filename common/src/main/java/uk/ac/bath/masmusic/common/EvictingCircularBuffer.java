package uk.ac.bath.masmusic.common;

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Circular buffer with fixed capacity that evicts older entries.
 *
 * This collection allows for null values. The implementation is not
 * thread-safe.
 *
 * @param <E>
 *            Buffer element type.
 *
 * @author Javier Dehesa
 */
public class EvictingCircularBuffer<E> extends AbstractList<E>
        implements Queue<E> {

    /** Data. */
    private final Object[] data;

    /** Index of the first element. */
    private int first;

    /** Index of the last element. */
    private int last;

    /**
     * Constructor.
     *
     * @param capacity
     *            Capacity of the list.
     * @throws IllegalArgumentException
     *             If the capacity is not positive
     */
    public EvictingCircularBuffer(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("The capacity must be positive");
        }
        this.data = new Object[capacity + 1];
        this.first = 0;
        this.last = 0;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        for (E e : c) {
            add(e);
        }
        return !c.isEmpty();
    }

    @Override
    public void clear() {
        last = first;
    }

    @Override
    public boolean contains(Object o) {
        for (int i = 0; i < size(); i++) {
            E e = get(i);
            if (e == o || (o != null && e != null && e.equals(o))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isEmpty() {
        return first == last;
    }

    /**
     * Returns true if the buffer is full.
     *
     * Note that, due to the evicting behavior of the buffer, being full does
     * not impede to add new elements to it.
     *
     * @return Whether the buffer is full.
     */
    public boolean isFull() {
        return ((last + 1) % data.length) == first;
    }

    @Override
    public Iterator<E> iterator() {
        return listIterator();
    }

    @Override
    public boolean remove(Object o) {
        for (int i = 0; i < size(); i++) {
            E e = get(i);
            if (e == o || (o != null && e != null && e.equals(o))) {
                int idx = (first + i) % data.length;
                last = Math.floorMod(last - 1, data.length);
                while (idx < last) {
                    int nextIdx = (idx + 1) % data.length;
                    data[idx] = data[nextIdx];
                    idx = nextIdx;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean removed = false;
        for (Object o : c) {
            removed = remove(o) || removed;
        }
        return removed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        int i = 0;
        boolean mod = false;
        while (i < size()) {
            if (c.contains(get(i))) {
                i++;
            } else {
                remove(get(i));
                mod = true;
            }
        }
        return mod;
    }

    @Override
    public int size() {
        if (last >= first) {
            return last - first;
        } else {
            return data.length - first + last;
        }
    }

    /**
     * @return The capacity of the buffer
     */
    public int capacity() {
        return data.length - 1;
    }

    @Override
    public Object[] toArray() {
        return toArray(new Object[0]);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        T[] r = a.length >= size()
                ? a
                : (T[]) Array.newInstance(a.getClass().getComponentType(),
                        size());
        for (int i = 0; i < size(); i++) {
            r[i] = (T) get(i);
        }
        if (r.length > size()) {
            r[size()] = null;
        }
        return r;
    }

    @Override
    public boolean add(E e) {
        if (isFull()) {
            first = (first + 1) % data.length;
        }
        data[last] = e;
        last = (last + 1) % data.length;
        return true;
    }

    @Override
    public E element() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return get(0);
    }

    @Override
    public boolean offer(E e) {
        return add(e);
    }

    @Override
    public E peek() {
        if (isEmpty()) {
            return null;
        }
        return get(0);
    }

    @Override
    public E poll() {
        E elem = peek();
        if (!isEmpty()) {
            first = (first + 1) % data.length;
        }
        return elem;
    }

    @Override
    public E remove() {
        E elem = element();
        if (!isEmpty()) {
            first = (first + 1) % data.length;
        }
        return elem;
    }

    @SuppressWarnings("unchecked")
    @Override
    public E get(int index) {
        if (size() <= index) {
            throw new IndexOutOfBoundsException();
        }
        return (E) data[(first + index) % data.length];
    }

    /**
     * Retrieves the last element of the buffer.
     *
     * @throws NoSuchElementException
     *             if this buffer is empty
     * @return the last element of the queue
     */
    public E lastElement() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return get(size() - 1);
    }

}
