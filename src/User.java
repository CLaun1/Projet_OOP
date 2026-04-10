package src;
public abstract class User {
	protected String id;
	protected String name;
	protected String email;
	
	public User(String id, String name, String email) {
		this.id = id;
		this.name = name;
		this.email = email;
	}
	public abstract boolean canCreateTask();
	public abstract boolean canDeleteTask();
	public abstract boolean canAssignTask();
	
	public String getId() {
		return id;
	}
	public String getName() {
		return name;
	}
	public String getEmail() {
		return email;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setEmail(String email) {
		this.email = email;
	}
}