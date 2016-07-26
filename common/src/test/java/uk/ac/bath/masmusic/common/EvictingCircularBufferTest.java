package uk.ac.bath.masmusic.common;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EvictingCircularBufferTest {

    /** Capacity of the test buffer. */
    private static final int CAPACITY = 5;

    /** Test buffer. */
    private EvictingCircularBuffer<Integer> buffer;

    @Before
    public void setUp() {
        buffer = new EvictingCircularBuffer<>(CAPACITY);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSize() {
        assertThat(buffer.size(), is(0));
        for (int i = 0; i < CAPACITY; i++) {
            buffer.add(0);
            assertThat(buffer.size(), is(i + 1));
        }
        buffer.add(0);
        assertThat(buffer.size(), is(CAPACITY));
    }

    @Test
    public void testIsEmpty() {
        assertThat(buffer.isEmpty(), is(true));
        for (int i = 0; i < CAPACITY + 1; i++) {
            buffer.add(0);
            assertThat(buffer.isEmpty(), is(false));
        }
    }

    @Test
    public void testClear() {
        for (int i = 0; i < CAPACITY + 1; i++) {
            for (int j = 0; j < i; j++) {
                buffer.add(0);
            }
            buffer.clear();
            assertThat(buffer.isEmpty(), is(true));
        }
    }

    @Test
    public void testAddAll() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < CAPACITY; i++) {
            list.add(i);
        }
        buffer.addAll(list);
        assertThat(buffer.size(), is(list.size()));
        for (int i = 0; i < buffer.size(); i++) {
            assertThat(buffer.get(i), is(list.get(i)));
        }

        buffer.clear();
        list.add(CAPACITY);
        buffer.addAll(list);
        assertThat(buffer.size(), is(CAPACITY));
        for (int i = 0; i < buffer.size(); i++) {
            assertThat(buffer.get(i), is(list.get(i + list.size() - CAPACITY)));
        }
    }

    @Test
    public void testContainsObject() {
        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        assertThat(buffer.contains(1), is(true));
        assertThat(buffer.contains(2), is(true));
        assertThat(buffer.contains(3), is(true));
        assertThat(buffer.contains(4), is(false));
    }

    @Test
    public void testContainsAll() {
        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        assertThat(buffer.containsAll(Arrays.asList(1)), is(true));
        assertThat(buffer.containsAll(Arrays.asList(2)), is(true));
        assertThat(buffer.containsAll(Arrays.asList(3)), is(true));
        assertThat(buffer.containsAll(Arrays.asList(1, 2)), is(true));
        assertThat(buffer.containsAll(Arrays.asList(1, 3)), is(true));
        assertThat(buffer.containsAll(Arrays.asList(2, 3)), is(true));
        assertThat(buffer.containsAll(Arrays.asList(1, 2, 3)), is(true));
        assertThat(buffer.containsAll(Arrays.asList(1, 3, 1)), is(true));
        assertThat(buffer.containsAll(Arrays.asList(1, 4)), is(false));
        assertThat(buffer.containsAll(Arrays.asList(4, 1)), is(false));
    }

    @Test
    public void testIsFull() {
        for (int i = 0; i < CAPACITY; i++) {
            assertThat(buffer.isFull(), is(false));
            buffer.add(0);
        }
        assertThat(buffer.isFull(), is(true));
        buffer.add(0);
        assertThat(buffer.isFull(), is(true));
        buffer.remove();
        assertThat(buffer.isFull(), is(false));
    }

    @Test
    public void testRemoveObject() {
        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        assertThat(buffer.remove(new Integer(2)), is(true));
        assertThat(buffer.size(), is(2));
        assertThat(buffer.get(0), is(1));
        assertThat(buffer.get(1), is(3));
    }

    @Test
    public void testRemoveAll() {
        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        assertThat(buffer.removeAll(Arrays.asList(1, 3)), is(true));
        assertThat(buffer.size(), is(1));
        assertThat(buffer.get(0), is(2));
    }

    @Test
    public void testRetainAll() {
        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        assertThat(buffer.retainAll(Arrays.asList(1, 3)), is(true));
        assertThat(buffer.size(), is(2));
        assertThat(buffer.get(0), is(1));
        assertThat(buffer.get(1), is(3));
    }

    @Test
    public void testCapacity() {
        assertThat(buffer.capacity(), is(CAPACITY));
        for (int i = 0; i < CAPACITY + 1; i++) {
            buffer.add(0);
            assertThat(buffer.capacity(), is(CAPACITY));
        }
        buffer.clear();
        assertThat(buffer.capacity(), is(CAPACITY));
    }

    @Test
    public void testToArray() {
        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        Object[] arr = buffer.toArray();
        assertThat(arr.length, is(buffer.size()));
        for (int i = 0; i < arr.length; i++) {
            assertThat((Integer) arr[i], is(buffer.get(i)));
        }
    }

    @Test
    public void testToArrayTyped() {
        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        Integer[] arr = buffer.toArray(new Integer[0]);
        assertThat(arr.length, is(buffer.size()));
        for (int i = 0; i < arr.length; i++) {
            assertThat(arr[i], is(buffer.get(i)));
        }

        for (int i = 0; i < arr.length; i++) {
            arr[i] = -1;
        }
        buffer.toArray(arr);
        for (int i = 0; i < arr.length; i++) {
            assertThat(arr[i], is(buffer.get(i)));
        }
    }

    @Test
    public void testAdd() {
        for (int i = 0; i < CAPACITY; i++) {
            buffer.add(i);
            assertThat(buffer.get(i), is(i));
        }
        buffer.add(CAPACITY);
        assertThat(buffer.get(CAPACITY - 1), is(CAPACITY));
    }

    @Test
    public void testElement() {
        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        assertThat(buffer.element(), is(1));
        assertThat(buffer.size(), is(3));
    }

    @Test(expected = NoSuchElementException.class)
    public void testElementFail() {
        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        buffer.clear();
        buffer.element();
    }

    @Test
    public void testLastElement() {
        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        assertThat(buffer.lastElement(), is(3));
        assertThat(buffer.size(), is(3));
    }

    @Test(expected = NoSuchElementException.class)
    public void testLastElementFail() {
        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        buffer.clear();
        buffer.lastElement();
    }

    @Test
    public void testOffer() {
        for (int i = 0; i < CAPACITY; i++) {
            buffer.offer(i);
            assertThat(buffer.get(i), is(i));
        }
        buffer.offer(CAPACITY);
        assertThat(buffer.get(CAPACITY - 1), is(CAPACITY));
    }

    @Test
    public void testPeek() {
        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        assertThat(buffer.peek(), is(1));
        assertThat(buffer.size(), is(3));
        buffer.clear();
        assertThat(buffer.peek(), is(nullValue()));
    }

    @Test
    public void testPoll() {
        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        assertThat(buffer.poll(), is(1));
        assertThat(buffer.size(), is(2));
        buffer.clear();
        assertThat(buffer.poll(), is(nullValue()));
    }

    @Test
    public void testRemove() {
        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        assertThat(buffer.remove(), is(1));
        assertThat(buffer.size(), is(2));
    }

    @Test(expected = NoSuchElementException.class)
    public void testRemoveFail() {
        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        buffer.clear();
        buffer.remove();
    }

    @Test
    public void testGet() {
        for (int i = 0; i < CAPACITY; i++) {
            buffer.add(i);
            assertThat(buffer.get(i), is(i));
        }
        buffer.add(CAPACITY);
        assertThat(buffer.get(CAPACITY - 1), is(CAPACITY));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetFail() {
        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        buffer.get(3);
    }

}
