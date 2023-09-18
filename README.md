# VirtualMemorySimulation

A virtual memory simulator that takes traces file with memory accesses, and simulate access and eviction for pages. Using the Least Recently Used and Optimal replacement algorithms.
For each memory trace file, the program will show the algorithm used, number of frames allocated, page size, total number of memory accesses, total number of page faults, and total number of write to disk.

## To Run The Program
After compiling, run the following command through the terminal / commandline:
```
java vmsim -a <opt|lru> –n <numframes> -p <pagesize in KB> -s <memory split> <tracefile>
```
