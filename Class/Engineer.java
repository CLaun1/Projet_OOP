public class Engineer extends User {
	private String specialty;
	
	public Engineer(String id, String name, String email, String specialty) {
		super(id, name, email);
		this.specialty = specialty;
	}
	public boolean canCreateTask() {
		return false;
	}
	public boolean canDeleteTask() {
		return false;
	}
	public boolean canAssignTask() {
		return false;
	}
	public String getSpecialty() {
		return specialty;
	}
	public void setSpecialty(String specialty) {
		this.specialty = specialty;
	}
}


