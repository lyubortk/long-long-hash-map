package ru.hse.lyubortk.longlongmap;

interface LongLongMap {
    /**
     * Returns value that is currently contained in the map.
     */
    long get(long key);
    /**
     * Returns previous value.
     */
    long put(long key, long value);

    /**
     * Returns removed value.
     */
    long remove(long key);
}