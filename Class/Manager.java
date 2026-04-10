import java.util.ArrayList;
import java.util.List;

public class Manager extends User {
	private String teamId;
	private List<User> teamMembers = new ArrayList<>();
	
	public Manager(String id, String name, String email, String teamId) {
		super(id, name, email);
		this.teamId = teamId;
	}
	public boolean canCreateTask() {
		return true;
	}
	public boolean canDeleteTask() {
		return false;
	}
	public boolean canAssignTask() {
		return false;
	}
	public void addTeamMembers(User user) {
		teamMembers.add(user);
	}
	public List<User> getTeamMembers() {
		return teamMembers;	
	}
	public void reviewTeamProgress(TaskManager manager) {
		System.out.println("Progression de l'équipe" + teamId + ":");
		
		for (User member : teamMembers) {
			System.out.println("-" + member.name + ":");
			
			for (Task t : manager.getAllTasks()) {
				if (t.getAssignedEngineer() != null && t.getAssignedEngineer().equals(member.name)) {
					System.out.println("." + t.getTitle() + "[" + t.getStatus() + "]" );
				}
			}
		}
	}
}
