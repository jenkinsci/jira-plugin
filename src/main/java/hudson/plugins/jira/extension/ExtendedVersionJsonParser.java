package hudson.plugins.jira.extension;

import com.atlassian.jira.rest.client.internal.json.JsonObjectParser;
import com.atlassian.jira.rest.client.internal.json.JsonParseUtil;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.joda.time.DateTime;

import java.net.URI;

public class ExtendedVersionJsonParser implements JsonObjectParser<ExtendedVersion> {
    @Override
    public ExtendedVersion parse(JSONObject json) throws JSONException {
        final URI self = JsonParseUtil.getSelfUri(json);
        final Long id = JsonParseUtil.getOptionalLong(json, "id");
        final String name = json.getString("name");
        final String description = JsonParseUtil.getOptionalString(json, "description");
        final boolean isArchived = json.getBoolean("archived");
        final boolean isReleased = json.getBoolean("released");
        final String startDateStr = JsonParseUtil.getOptionalString(json, "startDate");
        final String releaseDateStr = JsonParseUtil.getOptionalString(json, "releaseDate");
        final DateTime startDate = parseDate(startDateStr);
        final DateTime releaseDate = parseDate(releaseDateStr);
        return new ExtendedVersion(self, id, name, description, isArchived, isReleased, startDate, releaseDate);
    }

    private DateTime parseDate(String dateStr) {
        if (dateStr != null) {
            return dateStr.length() > "YYYY-MM-RR".length() ? JsonParseUtil.parseDateTime(dateStr) : JsonParseUtil.parseDate(dateStr);
        } else {
            return null;
        }
    }
}