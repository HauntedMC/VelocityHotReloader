package nl.hauntedmc.velocityhotreloader.entities;

import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.meta.PluginDependency;
import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class VelocityPluginDescription {

    private final PluginDescription description;
    private final File file;
    private final String author;
    private final Set<String> dependencies;

    /**
     * Constructs a new VelocityPluginDescription.
     */
    public VelocityPluginDescription(PluginDescription description) {
        this.description = description;

        Optional<Path> sourceOptional = description.getSource();
        if (!sourceOptional.isPresent()) {
            this.file = null;
        } else {
            this.file = sourceOptional.get().toFile();
        }
        this.author = String.join(", ", description.getAuthors());
        this.dependencies = description.getDependencies().stream()
                .map(PluginDependency::getId)
                .collect(Collectors.toSet());
    }

    public String getId() {
        return this.description.getId();
    }

    public String getName() {
        return this.description.getName().orElse("<UNKNOWN>");
    }

    public String getVersion() {
        return this.description.getVersion().orElse("<UNKNOWN>");
    }

    public String getAuthor() {
        return this.author;
    }

    public File getFile() {
        return this.file;
    }

    public Set<String> getDependencies() {
        return this.dependencies;
    }

    public PluginDescription getDescription() {
        return description;
    }
}
