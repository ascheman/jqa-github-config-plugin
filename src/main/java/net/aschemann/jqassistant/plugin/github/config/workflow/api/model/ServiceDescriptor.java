package net.aschemann.jqassistant.plugin.github.config.workflow.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;
import net.aschemann.jqassistant.plugin.github.config.api.model.GitHubDescriptor;

import java.util.List;

@Label("Service")
public interface ServiceDescriptor extends GitHubDescriptor {

    String getId();
    void setId(String id);

    String getImage();
    void setImage(String image);

    @Relation("HAS_ENV")
    List<EnvDescriptor> getEnv();
}
