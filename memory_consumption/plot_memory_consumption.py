import matplotlib.pyplot as plt

xs = []
ys = []


def read_memory_consuption(file_name):
    f = open(file_name)
    xs.append([])
    ys.append([])
    for line in f.readlines():
        x , y = line.split()
        xs[-1].append(int(x))
        ys[-1].append(float(y))


for name in ["LongLongHashMapMemory", "HashMapMemory", "HashtableMemory", "TreeMapMemory"]:
    read_memory_consuption(name)


plt.plot(xs[0], ys[0], label="LongLongMap")
plt.plot(xs[1], ys[1], label="HashMap")
plt.plot(xs[2], ys[2], label="Hashtable")
plt.plot(xs[3], ys[3], label="TreeMap")
plt.legend()
plt.yticks(range(8))
plt.show()

