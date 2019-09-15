# long-long-hash-map
This is a completed entrance test assignment for PARtable project.

The solution is a memory efficient LongLongMap interface implementation 
which is based on an open addressing hash map with linear probing.
Thus, each method which belongs to a public interface of the map has amortized O(1)
complexity. 

Low memory consumption is achieved through serializing key-value pair directly into a
storage byte array in which every other entry is stored.

A second optimization used to achieve this implementation's main goal (namely being memory efficient)
is that this hash map shrinks
its allocated space when number of contained elements reaches minimal load factor
(which is unusual to the most of the java.util hash maps).


### Memory efficiency comparison

Here is a memory efficiency comparison between LongLongHashMap and some of the java.util map implementations.
Axis X represents the number of elements iserted into the map. Axis Y represents the ratio of used space to
data size which is 2\*8 bytes in this case (the lower the better). The code could be found in the test derictory.

![](/memory_consumption/memory_consumption_benchmark.png?raw=true "Memory consumption benchmark")
