package nl.hauntedmc.velocityhotreloader.providers;

import java.io.File;
import java.io.InputStream;
import nl.hauntedmc.velocityhotreloader.config.VHRConfig;

public interface ResourceProvider {

    InputStream getRawResource(String resource);

    VHRConfig load(File file);

    String getResourceExtension();
}
