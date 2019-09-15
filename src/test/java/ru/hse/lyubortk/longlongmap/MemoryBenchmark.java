package ru.hse.lyubortk.longlongmap;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class MemoryBenchmark {
    private static final int MAX_ENTRIES = 5_000_000;
    private static final int ENTRY_BYTES = Long.BYTES * 2;
    private static final int OUTPUT_FREQUENCY = 10_000;
    private static final int MIN_ENTRIES_TO_CHECK_MEMORY_USAGE = 100_000;
    private static final List<Supplier<Map<Long, Long>>> mapsForComparison = Arrays.asList(
            HashMap::new,
            TreeMap::new,
            Hashtable::new
    );

    public static void main(String[] args) throws FileNotFoundException {
        memoryBenchmark();
    }

    private static void memoryBenchmark() throws FileNotFoundException {
        var runtime = Runtime.getRuntime();

        try (var longLongMapWriter = new PrintWriter(createFileName(LongLongHashMap.class))) {
            System.gc();
            final var nonHashMapMemoryUsage = runtime.totalMemory() - runtime.freeMemory();
            var longLongMap = new LongLongHashMap();
            testMemoryConsumption(longLongMap::put, longLongMapWriter, nonHashMapMemoryUsage);
        }

        for (var mapConstructor : mapsForComparison) {
            try(var writer = new PrintWriter(createFileName(mapConstructor.get().getClass()))) {
                System.gc();
                final long nonHashMapMemoryUsage = runtime.totalMemory() - runtime.freeMemory();
                var map = mapConstructor.get();
                testMemoryConsumption(map::put, writer, nonHashMapMemoryUsage);
            }
        }
    }

    private static void testMemoryConsumption(BiConsumer<Long, Long> putMethod,
                                              PrintWriter output,
                                              long nonHashMapMemoryUsage) {
        final var runtime = Runtime.getRuntime();
        for (long i = 0; i < MAX_ENTRIES; i++) {
            putMethod.accept(i + Integer.MAX_VALUE, i + Integer.MAX_VALUE);
            if ((i + 1) % OUTPUT_FREQUENCY == 0 && (i + 1) >= MIN_ENTRIES_TO_CHECK_MEMORY_USAGE) {
                output.flush();
                System.gc();
                final long memoryUsage = runtime.totalMemory()
                        - (runtime.freeMemory() + nonHashMapMemoryUsage);
                final double memoryPerEntry = memoryUsage / (double) (i+1);
                output.println(String.format("%d \t %f", i+1, memoryPerEntry / ENTRY_BYTES));
            }
        }
    }

    private static String createFileName(Class<?> mapClass) {
        return mapClass.getSimpleName() + "Memory";
    }
}
