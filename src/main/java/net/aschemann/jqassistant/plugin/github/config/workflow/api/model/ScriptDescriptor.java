package net.aschemann.jqassistant.plugin.github.config.workflow.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import net.aschemann.jqassistant.plugin.github.config.api.model.GitHubDescriptor;

@Label("Script")
public interface ScriptDescriptor extends GitHubDescriptor {

    String getContent();
    void setContent(String content);

    String getShell();
    void setShell(String shell);
}
