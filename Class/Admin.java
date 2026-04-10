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
	public void createUser(UserManager manager, User newUser) {
		manager.addUser(this, newUser);
	}
	public void deleteUser(UserManager manager, String userId) {
		manager.removeUser(this, userId);
	}
	public void changeUserRole(UserManager manager, String userId, User newUserObject) {
		User oldUser = manager.getUserById(userId);
		
		if (oldUser == null) {
			System.out.println("Utilisateur introuvable. ");
			return;
		}
		manager.removeUser(this, userId);
		
		manager.addUser(this, newUserObject);
	}
}