package nl.hauntedmc.velocityhotreloader.providers;

import java.io.File;
import java.io.InputStream;
import nl.hauntedmc.velocityhotreloader.config.VHRConfig;

public interface ResourceProvider {

    default InputStream getResource(String resource) {
        return getRawResource(resource + getResourceExtension());
    }

    InputStream getRawResource(String resource);

    VHRConfig load(InputStream is);

    VHRConfig load(File file);

    String getResourceExtension();
}
