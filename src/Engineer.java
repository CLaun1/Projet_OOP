package src;

public class Engineer extends User {
    private String specialty;

    public Engineer(String id, String name, String email, String specialty) {
        super(id, name, email);
        this.specialty = specialty;
    }

    @Override 
	public boolean canCreateTask() { 
		return false; 
	}
    @Override 
	public boolean canDeleteTask() { 
		return false; 
	}
    @Override 
	public boolean canAssignTask() { 
		return false; 
	}

    /** Updates the status of a task assigned to this engineer. */
    public void updateTaskStatus(String taskId, String newStatus) {
        System.out.println("[Engineer " + name + "] Updating task " + taskId + " → " + newStatus);
    }

    /** Returns an estimated duration in hours for a given task. */
    public int estimateTaskDuration(String taskId) {
        return 8; // Default 8h; override with real logic as needed
    }

    public String getSpecialty() { 
		return specialty; 
	}
    public void setSpecialty(String spec) { 
		this.specialty = spec; 
	}
}
