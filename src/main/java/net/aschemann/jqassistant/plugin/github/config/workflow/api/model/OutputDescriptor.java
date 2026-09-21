package net.aschemann.jqassistant.plugin.github.config.workflow.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import net.aschemann.jqassistant.plugin.github.config.api.model.GitHubDescriptor;

@Label("Output")
public interface OutputDescriptor extends GitHubDescriptor {

    String getName();
    void setName(String name);

    String getValue();
    void setValue(String value);

    String getDescription();
    void setDescription(String description);
}
