package hudson.plugins.jira.extension;

import com.atlassian.jira.rest.client.api.domain.Version;
import java.net.URI;
import javax.annotation.Nullable;
import org.joda.time.DateTime;

public class ExtendedVersion extends Version {

  private final DateTime startDate;

  public ExtendedVersion(URI self, @Nullable Long id, String name, String description,
      boolean archived, boolean released, @Nullable DateTime startDate,
      @Nullable DateTime releaseDate) {
    super(self, id, name, description, archived, released, releaseDate);
    this.startDate = startDate;
  }

  @Nullable
  public DateTime getStartDate() {
    return startDate;
  }
}
