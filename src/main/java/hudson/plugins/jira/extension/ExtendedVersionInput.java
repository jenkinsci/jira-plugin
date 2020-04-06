package hudson.plugins.jira.extension;

import com.atlassian.jira.rest.client.api.domain.input.VersionInput;
import java.util.Objects;

import com.google.common.base.MoreObjects;
import org.joda.time.DateTime;

import javax.annotation.Nullable;

public class ExtendedVersionInput extends VersionInput {
	private final DateTime startDate;

	public ExtendedVersionInput(String projectKey, String name, @Nullable String description, @Nullable DateTime startDate, @Nullable DateTime releaseDate, boolean isArchived, boolean isReleased) {
		super(projectKey, name, description, releaseDate, isArchived, isReleased);
		this.startDate = startDate;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper( this)
				.add("parent", super.toString())
				.add("startDate", startDate)
				.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ExtendedVersionInput) {
			ExtendedVersionInput that = (ExtendedVersionInput) obj;
			return Objects.equals(this.startDate, that.startDate);
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return Objects.hash(startDate, super.hashCode());
	}

	DateTime getStartDate() {
		return startDate;
	}
}
