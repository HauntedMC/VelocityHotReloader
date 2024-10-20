package nl.hauntedmc.velocityhotreloaded.common.entities;

import java.io.File;
import java.util.Set;

public interface VHRPluginDescription {

    String getId();

    String getName();

    String getVersion();

    String getAuthor();

    File getFile();

    Set<String> getDependencies();
}
