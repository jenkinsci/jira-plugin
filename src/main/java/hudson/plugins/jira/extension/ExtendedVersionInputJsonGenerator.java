package hudson.plugins.jira.extension;

import com.atlassian.jira.rest.client.internal.json.JsonParseUtil;
import com.atlassian.jira.rest.client.internal.json.gen.JsonGenerator;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class ExtendedVersionInputJsonGenerator implements JsonGenerator<ExtendedVersionInput> {
    @Override
    public JSONObject generate(ExtendedVersionInput version) throws JSONException {
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", version.getName());
        jsonObject.put("project", version.getProjectKey());

        if (version.getDescription() != null) {
            jsonObject.put("description", version.getDescription());
        }

        if (version.getStartDate() != null) {
            jsonObject.put("startDate", JsonParseUtil.formatDate(version.getStartDate()));
        }

        if (version.getReleaseDate() != null) {
            jsonObject.put("releaseDate", JsonParseUtil.formatDate(version.getReleaseDate()));
        }

        jsonObject.put("released", version.isReleased());
        jsonObject.put("archived", version.isArchived());
        return jsonObject;
    }
}