package ru.hse.lyubortk.longlongmap;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class LongLongHashMapTest {
    private static final long KEY_NOT_IN_MAP = (long) Integer.MIN_VALUE * 10000;
    private static final int MULTIPLE_LONGS_NUMBER = 10000;
    private LongLongHashMap map = new LongLongHashMap();

    @Test
    void sizeAfterPut() {
        assertEquals(0, map.size());
        for (int i = 0; i < MULTIPLE_LONGS_NUMBER; i++) {
            map.put(keyFromInt(i), valueFromInt(i));
            assertEquals(i+1, map.size());
        }
    }

    @Test
    void sizeAfterDoublePut() {
        putMultipleLongs();
        putMultipleLongs();
        assertEquals(MULTIPLE_LONGS_NUMBER, map.size());
    }

    @Test
    void sizeAfterRemove() {
        putMultipleLongs();
        for (int i = 0; i < MULTIPLE_LONGS_NUMBER; i++) {
            assertEquals(MULTIPLE_LONGS_NUMBER - i, map.size());
            map.remove(keyFromInt(i));
        }
        assertEquals(0, map.size());
    }

    @Test
    void getInMap() {
        putMultipleLongs();
        for (int i = 0; i < MULTIPLE_LONGS_NUMBER; i++) {
            assertEquals(valueFromInt(i), map.get(keyFromInt(i)));
            // check that hash table does not delete values on get
            assertEquals(valueFromInt(i), map.get(keyFromInt(i)));
        }
    }

    @Test
    void getNotInMap() {
        putMultipleLongs();
        assertThrows(NoSuchElementException.class, () -> map.get(KEY_NOT_IN_MAP));
    }

    @Test
    void putTest() {
        for (int i = 0; i < MULTIPLE_LONGS_NUMBER; i++) {
            map.put(keyFromInt(i), 0);
            assertEquals(0, map.put(keyFromInt(i), valueFromInt(i)));
            assertEquals(valueFromInt(i), map.put(keyFromInt(i), valueFromInt(i)));
        }
        // check that resizing did not change values
        for (int i = 0; i < MULTIPLE_LONGS_NUMBER; i++) {
            assertEquals(valueFromInt(i), map.put(keyFromInt(i), valueFromInt(i)));
        }
    }

    @Test
    void removeInMap() {
        putMultipleLongs();
        for (int i = 0; i < MULTIPLE_LONGS_NUMBER; i++) {
            assertEquals(valueFromInt(i), map.remove(keyFromInt(i)));
            final int index = i;
            assertThrows(NoSuchElementException.class, () -> map.remove(keyFromInt(index)));
            assertThrows(NoSuchElementException.class, () -> map.get(keyFromInt(index)));
        }
    }

    @Test
    void removeNotInMap() {
        putMultipleLongs();
        assertThrows(NoSuchElementException.class, () -> map.remove(KEY_NOT_IN_MAP));
    }

    @Test
    void testMapWithCollisions() {
        map = new LongLongHashMap(32, ignored -> 28);
        for (int i = 0; i < 14; i++) {
            map.put(i, i);
        }
        for (int i = 0; i < 14; i++) {
            assertEquals(i, map.get(i));
        }
        // check that put and get work after deletion
        for (int i = 0; i < 7; i++) {
            assertEquals(i, map.remove(i));
            final int index = i;
            assertThrows(NoSuchElementException.class, () -> map.remove(index));
        }
        for (int i = 7; i < 14; i++) {
            // check that put returns right value
            assertEquals(i, map.put(i, -i));
            assertEquals(-i, map.get(i));
            assertEquals(-i, map.remove(i));
            final int index = i;
            assertThrows(NoSuchElementException.class, () -> map.remove(index));
        }
        assertEquals(0, map.size());
    }

    @Test
    void worksWithNegativeHashes() {
        map = new LongLongHashMap(32, ignored -> -100);
        putMultipleLongs();
        for (int i = 0; i < MULTIPLE_LONGS_NUMBER; i++) {
            assertEquals(valueFromInt(i), map.remove(keyFromInt(i)));
        }
    }

    @Test
    void shrinksOnDeletion() {
        putMultipleLongs();
        for (int i = 0; i < MULTIPLE_LONGS_NUMBER; i++) {
            map.remove(keyFromInt(i));
            assertEquals(map.getStorageRealCapacity(), map.getCapacity());
        }
        assertEquals(map.getMinimumCapacity(), map.getCapacity());
    }

    private void putMultipleLongs() {
        for (int i = 0; i < MULTIPLE_LONGS_NUMBER; ++i) {
            map.put(keyFromInt(i), valueFromInt(i));
        }
    }

    private long keyFromInt(int key) {
        final long newKey = (long) key + Integer.MAX_VALUE;
        if (newKey == KEY_NOT_IN_MAP) {
            throw new RuntimeException("Test broken: generated key is equal to KEY_NOT_IN_MAP");
        }
        return newKey;
    }

    private long valueFromInt(int value) {
        return ((long) value + Integer.MAX_VALUE) * 2;
    }
}