package src;
import java.util.ArrayList;
import java.util.Date;

import enumeration.PriorityLevel;
import enumeration.TaskCategory;
import enumeration.TaskStatus;

public class Task {
    private int id;
    private String title;
    private String description;
    private PriorityLevel priorityLevel;
    private TaskStatus taskStatus;
    private TaskCategory taskCategory;
    private Date deadline;
    private ArrayList<Task> dependencies = new ArrayList<>();
    private ArrayList<TaskHistoryEntry> history = new ArrayList<>();
    private Engineer assignedEngineer;
    
    public Task(int id, String title, String description, PriorityLevel priorityLevel, TaskStatus taskStatus, TaskCategory taskCategory, Date deadline) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priorityLevel = priorityLevel;
        this.taskStatus = taskStatus;
        this.taskCategory = taskCategory;
        this.deadline = deadline;
    }

    public Task(int id, String title, String description, PriorityLevel priorityLevel, TaskStatus taskStatus, TaskCategory taskCategory, Date deadline, Engineer assignedEngineer) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priorityLevel = priorityLevel;
        this.taskStatus = taskStatus;
        this.taskCategory = taskCategory;
        this.deadline = deadline;
        this.assignedEngineer = assignedEngineer;
    }

    public void updateStatus(TaskStatus newStatus){
        taskStatus = newStatus;
    }

    public void addHistoryEntry(TaskHistoryEntry taskHistoryEntry){
        history.add(taskHistoryEntry);
    }

    public void addDependency(Task dependency){
        dependencies.add(dependency);
    }

    public void displayTask(){
        System.out.println("ID : "+id+
                           "\nTitle : "+title+
                           "\nDescription : "+description+
                           "\nPriority level : "+priorityLevel+
                           "\nTask status : "+taskStatus+
                           "\nTask category : "+taskCategory+
                           "\nDeadline : "+deadline+
                           "\nDepencies : "+dependencies+
                           "\nHistory : "+history+
                           "\nAssigned Engineer : "+assignedEngineer.getName());
    }

    public void markAsDone(){
        taskStatus = TaskStatus.DONE;
    }

    public void changePriority(PriorityLevel newPriorityLevel){
        priorityLevel = newPriorityLevel;
    }

    public void updateDescription(String newDesc){
        description = newDesc;
    }

    public void assignEngineer(Engineer engineer){
        assignedEngineer = engineer;
    }

    public Engineer getAssignedEngineer(){
        return assignedEngineer;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getDeadline() {
        return deadline;
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }
}