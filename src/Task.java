package src;
import java.util.ArrayList;
import java.util.Date;

import enumeration.PriorityLevel;
import enumeration.TaskCategory;

public class Task {
    private int id;
    private String title;
    private String description;
    private PriorityLevel priorityLevel;
    private TaskCategory taskCategory;
    private Date deadline;
    private ArrayList<Task> dependencies = new ArrayList<>();
    


    
}
