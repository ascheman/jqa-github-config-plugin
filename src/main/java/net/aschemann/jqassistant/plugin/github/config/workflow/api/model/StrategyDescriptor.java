package net.aschemann.jqassistant.plugin.github.config.workflow.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;
import net.aschemann.jqassistant.plugin.github.config.api.model.GitHubDescriptor;

import java.util.List;

@Label("Strategy")
public interface StrategyDescriptor extends GitHubDescriptor {

    Boolean getFailFast();
    void setFailFast(Boolean failFast);

    Integer getMaxParallel();
    void setMaxParallel(Integer maxParallel);

    @Relation("HAS_MATRIX_DIMENSION")
    List<MatrixDimensionDescriptor> getDimensions();

    @Relation("HAS_MATRIX_INCLUDE")
    List<MatrixCombinationDescriptor> getIncludes();

    @Relation("HAS_MATRIX_EXCLUDE")
    List<MatrixCombinationDescriptor> getExcludes();
}
