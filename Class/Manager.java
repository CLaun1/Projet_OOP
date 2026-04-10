public class Manager extends User {
	private String department;
	
	public Manager(String id, String name, String email, String department) {
		super(id, name, email);
		this.department = department;
	}
	public boolean canCreateTask() {
		return true;
	}
	public boolean canDeleteTask() {
		return false;
	}
	public boolean canAssignTask() {
		return true;
	}
	public String getDepartment() {
		return department;
	}
	public void setDepartment(String department) {
		this.department = department;
	}

}
