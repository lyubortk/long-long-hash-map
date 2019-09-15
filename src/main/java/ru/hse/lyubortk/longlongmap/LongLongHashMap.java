package ru.hse.lyubortk.longlongmap;

import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * This class is a memory efficient LongLongMap interface implementation.
 * The implementation is based on an open addressing hash map with linear probing.
 * Thus, each method which belongs to a public interface of the map has amortized O(1)
 * complexity. Low memory consumption is achieved by serializing key-value pair directly into
 * storage byte array in which every other entry is stored. In order to achieve
 * this implementation's main goal (namely being memory efficient) this hash map will shrink
 * its allocated space when number of contained elements reaches minimal load factor
 * (which is unusual to the most of the java.util hash maps).
 * Note that this implementation is not synchronized.
 */
public class LongLongHashMap implements LongLongMap {
    public static final float MIN_LOAD_FACTOR = 0.25F;
    public static final float MAX_LOAD_FACTOR = 0.75F;
    public static final int MINIMUM_CAPACITY = 32;

    private static final int HEADER_BYTES = Byte.BYTES;
    private static final int KEY_BYTES = Long.BYTES;
    private static final int VALUE_BYTES = Long.BYTES;
    private static final int CELL_BYTES = HEADER_BYTES + KEY_BYTES + VALUE_BYTES;

    private static final int HEADER_OFFSET = 0;
    private static final int KEY_OFFSET = HEADER_OFFSET + HEADER_BYTES;
    private static final int VALUE_OFFSET = KEY_OFFSET + KEY_BYTES;

    private static final byte EMPTY = 0;
    private static final byte OCCUPIED = 1;
    private static final byte DELETED = 2;

    private int effectiveSize = 0;
    private int usedSize = 0;
    private int capacity;
    private byte[] storage;

    // used for testing purposes
    private Function<Long, Integer> hashCodeFunction;

    /**
     * Creates new empty LongLongHashMap with initial capacity set to MINIMUM_CAPACITY.
     * This implementation will rehash and change its capacity
     * if needed in order to keep the number of contained entries
     * in between (MIN_LOAD_FACTOR * capacity) and
     * (MAX_LOAD_FACTOR * capacity).
     * However, this hash map will
     * not rehash and shrink its capacity if it is already equal to MINIMUM_CAPACITY.
     */
    public LongLongHashMap() {
        this(MINIMUM_CAPACITY, (value -> Long.hashCode(value)));
    }

    // there is no need to import Guava for one annotation
    /**
     * Visible for testing.
     */
    LongLongHashMap(int initialCapacity, Function<Long, Integer> hashCodeFunction) {
        capacity = initialCapacity;
        storage = new byte[initialCapacity * CELL_BYTES];
        this.hashCodeFunction = hashCodeFunction;
    }

    /**
     * Returns the value to which the specified key is mapped. If the map does not contain such
     * entry, then NoSuchElementException is thrown (Note that LongLongMap interface obliges
     * this implementation to return primitive types so returning null is not an option).
     * @param key the key whose mapped value will be searched for
     * @return mapped value
     * @throws NoSuchElementException if the map does not contain specified key
     */
    @Override
    public long get(long key) {
        final Long value = getImpl(key);
        if (value == null) {
            throw new NoSuchElementException(createKeyNotFoundMessage(key));
        }
        return value;
    }

    /**
     * Inserts new entry into the map. If the specified key was already mapped to
     * some value then the previous value is returned and size of the map is not increased.
     * If the key was not presented in the map then this method will return Long.MIN_VALUE.
     * (Note that LongLongMap interface obliges this implementation to return primitive types
     * so returning null is not an option).
     * @param key the key which will be mapped to the specified value
     * @param value the value to which the specified key will be mapped
     * @return previously mapped value or Long.MIN_VALUE
     */
    @Override
    public long put(long key, long value) {
        final Long previousValue = putImpl(key, value, true);
        return previousValue != null ? previousValue : Long.MIN_VALUE;
    }

    /**
     * Removes entry from the map and returns its mapped value.
     * If the map does not contain such entry then NoSuchElementException is thrown.
     * (Note that LongLongMap interface obliges
     * this implementation to return primitive types so returning null is not an option).
     * @param key the key which entry will be removed
     * @return mapped value
     * @throws NoSuchElementException if the map does not contain specified key
     */
    @Override
    public long remove(long key) {
        final Long previousValue = removeImpl(key);
        if (previousValue == null) {
            throw new NoSuchElementException(createKeyNotFoundMessage(key));
        }
        return previousValue;
    }

    /**
     * Returns the number of entries contained in the map (i.e. number of key-value pairs)
     * @return the number of key-value pairs in the map.
     */
    public int size() {
        return effectiveSize;
    }

    @Nullable
    private Long getImpl(long key) {
        final int cellNumber = findEmptyOrWithSpecifiedKey(key);

        if (getHeader(cellNumber) == OCCUPIED) {
            return getValue(cellNumber);
        }
        return null;
    }

    @Nullable
    private Long putImpl(long key, long value, boolean resizeIsPermitted) {
        int cellNumber = findEmptyOrWithSpecifiedKey(key);
        Long previousValue = null;

        switch (getHeader(cellNumber)) {
            case OCCUPIED:
                previousValue = getValue(cellNumber);
                break;
            case EMPTY:
                effectiveSize++;
                usedSize++;
                break;
            case DELETED:
                effectiveSize++;
                break;
        }

        setHeader(cellNumber, OCCUPIED);
        setKey(cellNumber, key);
        setValue(cellNumber, value);

        if (resizeIsPermitted) {
            resizeIfNeeded();
        }
        return previousValue;
    }

    @Nullable
    private Long removeImpl(long key) {
        int cellNumber = findEmptyOrWithSpecifiedKey(key);
        byte header = getHeader(cellNumber);

        if (header == EMPTY || header == DELETED) {
            return null;
        }
        final long value = getValue(cellNumber);
        setHeader(cellNumber, DELETED);
        effectiveSize--;

        resizeIfNeeded();
        return value;
    }

    private int findEmptyOrWithSpecifiedKey(long key) {
        int cellNumber = hashCodeFunction.apply(key) % capacity;
        cellNumber = cellNumber >= 0 ? cellNumber : cellNumber + capacity;

        boolean cellFound = false;

        while (!cellFound) {
            byte header = getHeader(cellNumber);
            if (header == EMPTY || getKey(cellNumber) == key) {
                cellFound = true;
            } else {
                cellNumber = (cellNumber + 1) % capacity;
            }
        }
        return cellNumber;
    }

    private byte getHeader(int cellNumber) {
        return storage[cellNumber * CELL_BYTES + HEADER_OFFSET];
    }

    private long getKey(int cellNumber) {
        return arrayToLong(storage, cellNumber * CELL_BYTES + KEY_OFFSET);
    }

    private long getValue(int cellNumber) {
        return arrayToLong(storage, cellNumber * CELL_BYTES + VALUE_OFFSET);
    }

    private void setHeader(int cellNumber, byte header) {
        storage[cellNumber * CELL_BYTES + HEADER_OFFSET] = header;
    }

    private void setKey(int cellNumber, long key) {
        writeLongToArray(storage, cellNumber * CELL_BYTES + KEY_OFFSET, key);
    }

    private void setValue(int cellNumber, long value) {
        writeLongToArray(storage, cellNumber * CELL_BYTES + VALUE_OFFSET, value);
    }

    private void resizeIfNeeded() {
        LongLongHashMap newMap = null;

        if (usedSize > capacity * MAX_LOAD_FACTOR) {
            newMap = generateNewMap(true);
        } else if(effectiveSize < capacity * MIN_LOAD_FACTOR) {
            newMap = generateNewMap(false);
        }

        if (newMap == null) {
            return;
        }

        for (int i = 0; i < capacity; i++) {
            byte header = getHeader(i);
            if (header == OCCUPIED) {
                newMap.putImpl(getKey(i), getValue(i), false);
            }
        }

        assignContentFromOtherMap(newMap);
    }

    private LongLongHashMap generateNewMap(boolean resizeIsObligatory) {
        final int newCapacity = Math.max(effectiveSize * 2, MINIMUM_CAPACITY);
        if (newCapacity != capacity || resizeIsObligatory) {
            return new LongLongHashMap(newCapacity, hashCodeFunction);
        }
        return null;
    }

    private void assignContentFromOtherMap(LongLongHashMap otherMap) {
        effectiveSize = otherMap.effectiveSize;
        usedSize = otherMap.usedSize;
        capacity = otherMap.capacity;
        storage = otherMap.storage;
        hashCodeFunction = otherMap.hashCodeFunction;
    }

    private static String createKeyNotFoundMessage(long key) {
        return String.format("Key %d was not found in the map", key);
    }

    private static void writeLongToArray(byte[] array, int offset, long value) {
        for (int i = 0; i < Long.BYTES; i++) {
            array[i + offset] = (byte)(value & 0xFF);
            value >>= Byte.SIZE;
        }
    }

    private static long arrayToLong(byte[] array, int offset) {
        long result = 0;
        for (int i = Long.BYTES - 1; i >= 0; i--) {
            result <<= Byte.SIZE;
            result |= (array[i + offset] & 0xFF);
        }
        return result;
    }

    /**
     * Visible for testing.
     */
    long getCapacity() {
        return capacity;
    }

    /**
     * Visible for testing.
     */
    long getStorageRealCapacity() {
        return storage.length / CELL_BYTES;
    }

    /**
     * Visible for testing
     */
    long getMinimumCapacity() {
        return MINIMUM_CAPACITY;
    }
}
