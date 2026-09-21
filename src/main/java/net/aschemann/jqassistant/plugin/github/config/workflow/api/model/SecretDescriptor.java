package net.aschemann.jqassistant.plugin.github.config.workflow.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import net.aschemann.jqassistant.plugin.github.config.api.model.GitHubDescriptor;

@Label("Secret")
public interface SecretDescriptor extends GitHubDescriptor {

    String getName();
    void setName(String name);

    Boolean getRequired();
    void setRequired(Boolean required);

    String getDescription();
    void setDescription(String description);

    String getValue();
    void setValue(String value);
}
