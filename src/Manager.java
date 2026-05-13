package src;

public class Manager extends User {
    private String department;

    public Manager(String id, String name, String email, String department) {
        super(id, name, email);
        this.department = department;
    }

    @Override 
	public boolean canCreateTask() { 
		return true;  
	}
    @Override 
	public boolean canDeleteTask() { 
		return false; 
	}
    @Override 
	public boolean canAssignTask() { 
		return true;  
	}

    public String getDepartment() { 
		return department; 
	}
    public void setDepartment(String dept) { 
		this.department = dept; 
	}
}
