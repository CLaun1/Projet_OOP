package src;

import java.util.Date;

import enumeration.PriorityLevel;
import enumeration.TaskCategory;
import enumeration.TaskStatus;

public class TaskHistoryEntry extends Task{

    public TaskHistoryEntry(int id, String title, String description, PriorityLevel priorityLevel, TaskStatus taskStatus, TaskCategory taskCategory, Date deadline, Engineer assignedEngineer) {
        super(id, title, description, priorityLevel, taskStatus, taskCategory, deadline, assignedEngineer);
    }


}
