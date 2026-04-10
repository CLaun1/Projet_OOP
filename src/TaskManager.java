package src;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class TaskManager{


    private Map<String, Task> tasks = new HashMap<>();
    private PriorityQueue<Task> readyQueue = new PriorityQueue<>();
    private Set<Task> inProgress = new HashSet<>();

    

}
