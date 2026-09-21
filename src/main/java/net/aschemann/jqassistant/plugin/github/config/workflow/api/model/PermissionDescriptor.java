package net.aschemann.jqassistant.plugin.github.config.workflow.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import net.aschemann.jqassistant.plugin.github.config.api.model.GitHubDescriptor;

@Label("Permission")
public interface PermissionDescriptor extends GitHubDescriptor {

    String getScope();
    void setScope(String scope);

    String getLevel();
    void setLevel(String level);
}
