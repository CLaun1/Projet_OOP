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
	public void updateTaskStatus(TaskManager manager, int taskId, TaskStatus newStatus)
			throws TaskNotFoundException, DependencyNotCompletedException {
		manager.updateTaskStatus(taskId, newStatus);
	}
	public int estimateTaskDuration(int taskId) {
		return 5;
	}
}


