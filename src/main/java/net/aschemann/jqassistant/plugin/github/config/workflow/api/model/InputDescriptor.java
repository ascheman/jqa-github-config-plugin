package net.aschemann.jqassistant.plugin.github.config.workflow.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import net.aschemann.jqassistant.plugin.github.config.api.model.GitHubDescriptor;

@Label("Input")
public interface InputDescriptor extends GitHubDescriptor {

    String getName();
    void setName(String name);

    String getType();
    void setType(String type);

    String getDescription();
    void setDescription(String description);

    Boolean getRequired();
    void setRequired(Boolean required);

    String getDefaultValue();
    void setDefaultValue(String defaultValue);
}
