package hudson.plugins.jira;

import java.util.Calendar;

import hudson.plugins.jira.soap.RemoteVersion;

public class JiraVersion implements Comparable<JiraVersion> {

	private final String name;
	private final Calendar releaseDate;
	private final boolean released;
	private final boolean archived;
	
	
	public JiraVersion(String name, Calendar releaseDate, boolean released, boolean archived) {
		this.name = name;
		this.releaseDate = releaseDate;
		this.released = released;
		this.archived = archived;
	}
	
    public JiraVersion(RemoteVersion version) {
        this(version.getName(),version.getReleaseDate(),version.isReleased(),version.isArchived());
    }

	public int compareTo(JiraVersion that) {
		int result = this.releaseDate.compareTo(that.releaseDate);
		if(result == 0 ) {
			return this.name.compareTo(that.name);
		}
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (archived ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((releaseDate == null) ? 0 : releaseDate.hashCode());
		result = prime * result + (released ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JiraVersion other = (JiraVersion) obj;
		if (archived != other.archived)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (releaseDate == null) {
			if (other.releaseDate != null)
				return false;
		} else if (!releaseDate.equals(other.releaseDate))
			return false;
		if (released != other.released)
			return false;
		return true;
	}

	public String getName() {
		return name;
	}

	public Calendar getReleaseDate() {
		return releaseDate;
	}

	public boolean isReleased() {
		return released;
	}

	public boolean isArchived() {
		return archived;
	}
	
	
}
