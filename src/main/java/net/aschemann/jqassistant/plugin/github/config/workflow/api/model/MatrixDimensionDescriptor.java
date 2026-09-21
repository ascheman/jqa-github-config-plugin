package net.aschemann.jqassistant.plugin.github.config.workflow.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import net.aschemann.jqassistant.plugin.github.config.api.model.GitHubDescriptor;

@Label("MatrixDimension")
public interface MatrixDimensionDescriptor extends GitHubDescriptor {

    String getName();
    void setName(String name);

    String[] getValues();
    void setValues(String[] values);
}
