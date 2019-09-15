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
