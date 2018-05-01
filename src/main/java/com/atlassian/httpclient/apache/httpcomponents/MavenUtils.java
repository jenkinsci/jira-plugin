package com.atlassian.httpclient.apache.httpcomponents;

import com.google.common.io.Closeables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import static com.google.common.io.Resources.getResource;
import static java.lang.String.format;

final class MavenUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MavenUtils.class);

    private static final String UNKNOWN_VERSION = "unknown";

    static String getVersion(String groupId, String artifactId)
    {
        final Properties props = new Properties();
        InputStream is = null;
        try
        {
            is = getPomInputStreamUrl(groupId, artifactId).openStream();
            props.load(is);
            return props.getProperty("version", UNKNOWN_VERSION);
        }
        catch (Exception e)
        {
            logger.debug("Could not find version for maven artifact {}:{}", groupId, artifactId);
            logger.debug("Got the following exception:", e);
            return UNKNOWN_VERSION;
        }
        finally
        {
            try
            {
                Closeables.close(is, true);
            }
            catch (IOException e)
            {
                logger.debug("Could not find version for maven artifact {}:{}", groupId, artifactId);
                logger.debug("IOException should not have been thrown.", e);
            }
        }
    }

    private static URL getPomInputStreamUrl(String groupId, String artifactId)
    {
        return getResource(MavenUtils.class, getPomFilePath(groupId, artifactId));
    }

    private static String getPomFilePath(String groupId, String artifactId)
    {
        return format("/META-INF/maven/%s/%s/pom.properties", groupId, artifactId);
    }
}
