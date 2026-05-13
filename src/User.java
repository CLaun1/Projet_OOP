package src;

public abstract class User {
    protected String id;
    protected String name;
    protected String email;

    public User(String id, String name, String email) {
        this.id    = id;
        this.name  = name;
        this.email = email;
    }

    /** Returns true if this user role is allowed to create tasks. */
    public abstract boolean canCreateTask();

    /** Returns true if this user role is allowed to delete tasks. */
    public abstract boolean canDeleteTask();

    /** Returns true if this user role is allowed to assign tasks to engineers. */
    public abstract boolean canAssignTask();

    // ── Getters & Setters ─────────────────────────────────────────────────────

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
		this.name  = name;  
	}
    public void setEmail(String email) { 
		this.email = email; 
	}

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + id + "] " + name;
    }
}
