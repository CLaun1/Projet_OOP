public class Admin extends User{
	private int adminLevel;
	
	public Admin (String id, String name, String email, int adminLevel) {
		super(id, name, email);
		this.adminLevel = adminLevel;
	}
	public boolean canCreateTask() {
		return true;
	}
	public boolean canDeleteTask() {
		return true;
	}
	public boolean canAssignTask() {
		return true;
	}
	public int getAdminLevel() {
		return adminLevel;
	}
	public void setAdminLevel(int adminLevel) {
		this.adminLevel = adminLevel;
	}
}
