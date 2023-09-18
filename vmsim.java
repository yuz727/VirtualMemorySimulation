
import java.io.*;
import java.util.*;
public class vmsim
{
    // Data Structures Used
    private String Algorithm;
    private int TotalNumFrames, P0Frames, P1Frames, PageSize, PageFaults, NumMemoryAccess, NumWrite, PageBits;
    private int[] MemSplit;
    private Scanner fileScan, OptTrace;

	public static void main( String[] args ) throws FileNotFoundException{
        new vmsim(args);
    }
    
    public vmsim(String[] a) throws FileNotFoundException{  
        // Get Allocation Algorithm, Total Number of Frames, and Page Size 
        Algorithm = a[1];   // Get the type of algorithm used
        TotalNumFrames = Integer.parseInt(a[3]);    // Get the total number of frames that can be used
        PageSize = Integer.parseInt(a[5]);          // Get the page size
        PageBits = 10 + (Integer.toBinaryString(Integer.parseInt(a[5])).length()-1); // Get the number of bits that is the offset
        // Initialise Counters for page fault, disk write, and memory access
        PageFaults = 0;
        NumWrite = 0;
        NumMemoryAccess = 0;
        
        // Calculating Frames Allocated for Each Process
        String[] Temp = a[7].split(":");
        MemSplit = new int[2];
        MemSplit[0] = Integer.parseInt(Temp[0]);
        MemSplit[1] = Integer.parseInt(Temp[1]);
        int multiplier = TotalNumFrames / (MemSplit[0] + MemSplit[1]);
        P0Frames = MemSplit[0] * multiplier;
        P1Frames = MemSplit[1] * multiplier;

        // Parsing Memory Trace
        fileScan = new Scanner(new FileInputStream(a[8]));
        OptTrace = new Scanner(new FileInputStream(a[8]));  // This is used for OPT's second scan to simulate the algorithm in action
        if(Algorithm.equals("lru"))
            LRU();
        else if(Algorithm.equals("opt")){
            OPT();
        }

        // Output Final Result
        System.out.println("Algorithm: " + Algorithm.toUpperCase());
        System.out.println("Number of frames: " + TotalNumFrames);
        System.out.println("Page size: " + PageSize + " KB");
        System.out.println("Total memory accesses: " + NumMemoryAccess);
        System.out.println("Total page faults: " + PageFaults);
        System.out.println("Total writes to disk: " + NumWrite);
    }

    // Least Recently Used Algorithm Simulation
    private void LRU(){
        // Create Data Structure Needed for the Simulation
        String[] MemoryInfo;    // Information on each line of memory trace
        Node curr;
        boolean exists;         // Flag for checking whether the page is in memory
        // Linkedlist PriorityQueues used as memory
        PriorityQueue P0Table = new PriorityQueue(P0Frames);
        PriorityQueue P1Table = new PriorityQueue(P1Frames);
        while(fileScan.hasNextLine()){  // Read in Next Line in the Memory Trace
            exists = false;
            NumMemoryAccess++;
            // Scan in memory information
            MemoryInfo = fileScan.nextLine().split(" ");
            curr = new Node();
            // Get the address
            curr.Address = Long.parseLong(MemoryInfo[1].substring(2), 16) >>> PageBits;
            if(MemoryInfo[2].equals("0")){ // Memory Trace Belong to Process 0
                // Find whether the page is inside the memory
                for(Node Temp = P0Table.Head; Temp != null; Temp = Temp.Next){
                    if(Temp.Address == curr.Address){
                        curr = Temp;
                        exists = true;
                        break;
                    }
                }
                if(MemoryInfo[0].equals("s"))   // Mark the dirty bit flag
                    curr.Accessed = true;
                if(!exists){   // If Page is not inside memory, add it to memory
                    PageFaults++;
                    if(P0Table.Length == P0Table.MaxLength){  // Evict if memory is full using LRU algorithm
                        LeastRecentlyUsed(P0Table);
                    }
                    P0Table.AppendToHead(curr);  // Add page to the memory
                } else {    // If page is in memory
                    // Move page to the front of the linked list to as it is recently used
                    P0Table.MoveToHead(curr);
                }
            }   
            else if(MemoryInfo[2].equals("1")){ // Memory Trace Belong to Process 0
                // Find whether the page is inside the memory
                for(Node Temp = P1Table.Head; Temp != null; Temp = Temp.Next){
                    if(Temp.Address == curr.Address){
                        curr = Temp;
                        exists = true;
                        break;
                    }
                }
                if(MemoryInfo[0].equals("s")) // Mark the dirty bit flag
                    curr.Accessed = true;
                if(!exists){  // If Page is not inside memory, add it to memory
                    PageFaults++;
                    if(P1Table.Length == P1Table.MaxLength){    // Evict if memory is full using LRU algorithm
                        LeastRecentlyUsed(P1Table);
                    }
                    P1Table.AppendToHead(curr); // Add page to the memory
                } else {    // If page is in memory
                    P1Table.MoveToHead(curr); // Move page to the front of the linked list to as it is recently used
                }
            }
        }
    }

    //Optimal Algorithm Simulation
    private void OPT(){
        // Create Data Structure Needed for the Simulation
        String[] MemoryInfo;    // Information on each line of memory trace
        Node curr;
        // Hash Tables for storing memory traces, Each Process' memory usage is seperated
        Hashtable<Long, PriorityQueue> P0MemoryStorage = new Hashtable<Long, PriorityQueue>(); 
        Hashtable<Long, PriorityQueue> P1MemoryStorage = new Hashtable<Long, PriorityQueue>(); 
        boolean exists; // Flag for checking whether the page is in memory
        // Linkedlist PriorityQueues used as memory
        PriorityQueue P0Table = new PriorityQueue(P0Frames);
        PriorityQueue P1Table = new PriorityQueue(P1Frames);
        PriorityQueue HashValueQueue;

        // First Scan through the Memory Trace File: Scan all memory trace into hashtable
        while(fileScan.hasNextLine()){
            NumMemoryAccess++;
            curr = new Node();
            curr.LineNum = NumMemoryAccess;
            // Scan in memory information
            MemoryInfo = fileScan.nextLine().split(" ");
            // Get the address
            curr.Address = Long.parseLong(MemoryInfo[1].substring(2), 16) >>> PageBits;
            if(MemoryInfo[2].equals("0")){ // Memory Trace Belong to Process 0
                if(MemoryInfo[0].equals("s"))   // Mark the dirty bit flag
                    curr.Accessed = true;
                if(P0MemoryStorage.containsKey(curr.Address)){  // If Address has been stored, append to the linked list in hashtable
                    P0MemoryStorage.get(curr.Address).AppendToTail(curr);;
                }
                else{   // If Address has not been stored, create linked list and add it to the hash table
                    HashValueQueue = new PriorityQueue();
                    P0MemoryStorage.put(curr.Address, HashValueQueue);
                    HashValueQueue.AppendToTail(curr);
                }
            } else if(MemoryInfo[2].equals("1")){   // Memory Trace Belong to Process 1
                if(MemoryInfo[0].equals("s"))   // Mark the dirty bit flag
                    curr.Accessed = true;
                if(P1MemoryStorage.containsKey(curr.Address)){  // If Address has been stored, append to the linked list in hashtable
                    P1MemoryStorage.get(curr.Address).AppendToTail(curr);;
                }
                else{   // If Address has not been stored, create linked list and add it to the hash table
                    HashValueQueue = new PriorityQueue();
                    P1MemoryStorage.put(curr.Address, HashValueQueue);
                    HashValueQueue.AppendToTail(curr);
                }
            }
        }
        
        //  Second Scan through Memory Trace: Optimal Algorithm Simulation
        //  Least Recently Used is used for breaking a tie, so the memory has been be maintained like in LRU simulation
        while(OptTrace.hasNextLine()){
            exists = false;
            // Scan in memory information
            MemoryInfo = OptTrace.nextLine().split(" ");
            curr = new Node();
             // Get the address
            curr.Address = Long.parseLong(MemoryInfo[1].substring(2), 16) >>> PageBits;
            if(MemoryInfo[2].equals("0")){  // Memory Trace Belong to Process 0
                // Find whether the page is inside the memory
                for(Node Temp = P0Table.Head; Temp != null; Temp = Temp.Next){
                    if(Temp.Address == curr.Address){
                        curr = Temp;
                        exists = true;
                        break;
                    }
                }
                if(MemoryInfo[0].equals("s"))   // Mark the dirty bit flag
                    curr.Accessed = true;
                if(!exists){    // If Page is not inside memory, add it to memory
                    PageFaults++;
                    // Remove current line from memory trace storage
                    P0MemoryStorage.get(curr.Address).RemoveHead();
                    if(P0Table.Length == P0Table.MaxLength){    // Evict if memory is full using OPT algorithm
                        Optimal(P0Table, P0MemoryStorage);
                    }
                    // Add Page to Memory
                    P0Table.AppendToHead(curr);
                } else {    
                    // Remove current line from memory trace storage
                    P0MemoryStorage.get(curr.Address).RemoveHead();
                    P0Table.MoveToHead(curr);   // Move page to the front of the linked list to as it is recently used
                }
            }   
            else if(MemoryInfo[2].equals("1")){ // Memory Trace Belong to Process 0
                // Find whether the page is inside the memory
                for(Node Temp = P1Table.Head; Temp != null; Temp = Temp.Next){
                    if(Temp.Address == curr.Address){
                        curr = Temp;
                        exists = true;
                        break;
                    }
                }
                if(MemoryInfo[0].equals("s"))   // Mark the dirty bit flag
                    curr.Accessed = true;
                if(!exists){    // If Page is not inside memory, add it to memory
                    PageFaults++;
                    // Remove current line from memory trace storage
                    P1MemoryStorage.get(curr.Address).RemoveHead();
                    if(P1Table.Length == P1Table.MaxLength){    // Evict if memory is full using OPT algorithm
                        Optimal(P1Table, P1MemoryStorage);
                    }
                    // Add Page to Memory
                    P1Table.AppendToHead(curr);
                } else {
                    // Remove current line from memory trace storage
                    P1MemoryStorage.get(curr.Address).RemoveHead();
                    P1Table.MoveToHead(curr);   // Move page to the front of the linked list to as it is recently used
                }     
            }
        }
    }

    // Least Recently Used Eviction Algorithm
    // The less recently used a page is, further to the tail that page would be
    // The tail node in the memory is always the least recently used page
    private void LeastRecentlyUsed(PriorityQueue Table){
        Node curr = Table.Tail;
        // Increment Disk Write Count if the dirty bit flag is marked
        if(curr.Accessed)
            NumWrite++;
        // Remove least recently used page
        Table.RemoveTail();
    }

    // Optimal Evition Algoithm
    // Each address is check to see when it is next used, the one that is going to be used the latest is removed
    // If there is a tie, LRU is used to break the tie
    // The less recently used a page is, further to the tail that page would be
    // The tail node in the memory is always the least recently used page
    private void Optimal(PriorityQueue Table, Hashtable<Long, PriorityQueue> MemoryInfo){
        Node Evicted = new Node(), curr = new Node();
        int line = 0;
        //  Traverse through Memory to find the page to be evicted
        for(Node Temp = Table.Tail; Temp != null; Temp = Temp.Prev){
            curr = MemoryInfo.get(Temp.Address).Head;   // Get the next time where the page is used
            if(curr == null){   // Page is not used in memory again
                Evicted = Temp;
                break;
            } else if (curr != null && curr.LineNum > line){    // Linear Search algorithm for finding the evicted page
                line = curr.LineNum;
                Evicted = Temp;
            } 
        }
        // Increment Disk Write Count if the dirty bit flag is marked
        if(Evicted.Accessed)
            NumWrite++;
        // Evict Page
        Table.Remove(Evicted);
    }
}

// Node Class used in the Linked List Priority Queue, Each represents a line in the memory trace
class Node{
    boolean Accessed;   // Dirty Bit Flag
    long Address;       // Address of the memory
    int LineNum;        // The line number of this line in the memory trace
                        // Not used in LRU

    // Linked List Node pointers
    Node Next;          
    Node Prev;
    
    // Constructor
    Node(){
        Accessed = false;
        Address = 0;
        LineNum = 0;
        Next = Prev = null;
    }
}

// Linked List Priority Queue
// the Head of the list is the most recently used page, and the tail is the least recently used page
class PriorityQueue{
    int Length;     // Current Length of the linked list
    int MaxLength;  // Maximum number of pages that the linked list can hold
    // Linked List Pointers
    Node Head;      
    Node Tail;

    // Constructor Used for Hash Table Value to hold memory traces
    PriorityQueue(){
        Head = Tail = null;
        Length = 0;
        this.MaxLength = Integer.MAX_VALUE;
    }

    // Constructor Used for Simulating Memory
    PriorityQueue(int MaxLength){
        Head = Tail = null;
        Length = 0;
        this.MaxLength = MaxLength;
    }
    
    void AppendToHead(Node n){
        n.Prev = n.Next = null;
        if(Length == 0){  // Queue is Empty
            Length++;
            Head = n;
            Tail = n;
            return;
        }
        n.Next = Head;
        n.Next.Prev = n;
        Head = n;
        Length++;
    }

    void AppendToTail(Node n){
        n.Prev = n.Next = null;
        if(Length == 0){  // Queue is Empty
            Length++;
            Head = n;
            Tail = n;
            return;
        }
        n.Prev = Tail;
        n.Prev.Next = n;
        Tail = n;
        Length++;
    }

    // Move Node n to the head of the Queue
    void MoveToHead(Node n){
        if(n == Head)
            return;
        if(n == Tail){
            n.Prev.Next = null;
            Tail = n.Prev;
            n.Next = Head;
            n.Next.Prev = n;
            n.Prev = null;
            Head = n; 
            return;
        }
        n.Prev.Next = n.Next;
        n.Next.Prev = n.Prev;
        n.Next = Head;
        n.Next.Prev = n;
        n.Prev = null;
        Head = n; 
    }  

    // Remove Node n from the Queue
    void Remove(Node n){
        if(n == Head){
            RemoveHead();
            return;
        }
        if(n == Tail){
            RemoveTail();
            return;
        }
        n.Prev.Next = n.Next;
        n.Next.Prev = n.Prev;
        Length--;
    }

    void RemoveHead(){
        if(Length == 1){
            Head = Tail = null;
            Length--;
            return;
        } 
        Head = Head.Next;
        Head.Prev = null;
        Length--;
    }

    void RemoveTail(){
        if(Length == 1){
            Head = Tail = null;
            Length--;
            return;
        } 
        Tail = Tail.Prev;
        Tail.Next = null;
        Length--;
    }
}           