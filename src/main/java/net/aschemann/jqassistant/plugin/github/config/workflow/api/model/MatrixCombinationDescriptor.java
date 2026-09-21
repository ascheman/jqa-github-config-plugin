package net.aschemann.jqassistant.plugin.github.config.workflow.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;
import net.aschemann.jqassistant.plugin.github.config.api.model.GitHubDescriptor;

import java.util.List;

@Label("MatrixCombination")
public interface MatrixCombinationDescriptor extends GitHubDescriptor {

    @Relation("HAS_PARAMETER")
    List<WithDescriptor> getParameters();
}
