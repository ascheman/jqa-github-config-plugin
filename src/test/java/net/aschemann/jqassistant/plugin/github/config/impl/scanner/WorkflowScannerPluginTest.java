package net.aschemann.jqassistant.plugin.github.config.impl.scanner;

import com.buschmais.jqassistant.core.scanner.api.DefaultScope;
import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.buschmais.jqassistant.core.test.plugin.AbstractPluginIT;
import net.aschemann.jqassistant.plugin.github.config.workflow.api.model.ActionDescriptor;
import net.aschemann.jqassistant.plugin.github.config.workflow.api.model.JobDescriptor;
import net.aschemann.jqassistant.plugin.github.config.workflow.api.model.MatrixDimensionDescriptor;
import net.aschemann.jqassistant.plugin.github.config.workflow.api.model.StepDescriptor;
import net.aschemann.jqassistant.plugin.github.config.workflow.api.model.StrategyDescriptor;
import net.aschemann.jqassistant.plugin.github.config.workflow.api.model.WorkflowDescriptor;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowScannerPluginTest extends AbstractPluginIT {

    private static final String SAMPLE_PATH = "/.github/workflows/sample.yml";

    /**
     * When ".github" is configured as a scan root of its own -- which is what
     * jqassistant.yml's include.files does -- the scanner reports paths
     * relative to it and the ".github" segment is gone. Requiring it meant the
     * plugin matched nothing at all while the files sat plainly in the graph.
     */
    @Test
    void acceptsWorkflowPathWithoutGithubPrefix() {
        WorkflowScannerPlugin plugin = new WorkflowScannerPlugin();
        assertThat(plugin.accepts(null, "/workflows/maven.yml", DefaultScope.NONE)).isTrue();
        assertThat(plugin.accepts(null, "/workflows/maven.yaml", DefaultScope.NONE)).isTrue();
        assertThat(plugin.accepts(null, SAMPLE_PATH, DefaultScope.NONE)).isTrue();
        assertThat(plugin.accepts(null, "/ISSUE_TEMPLATE/BUG.yml", DefaultScope.NONE)).isFalse();
        assertThat(plugin.accepts(null, "/dependabot.yml", DefaultScope.NONE)).isFalse();
        assertThat(plugin.accepts(null, "/workflows/README.md", DefaultScope.NONE)).isFalse();
    }

    @Test
    void scanSampleWorkflow() {
        File testFile = new File(getClassesDirectory(WorkflowScannerPluginTest.class), SAMPLE_PATH);

        store.beginTransaction();
        try {
            Descriptor descriptor = getScanner().scan(testFile, SAMPLE_PATH, DefaultScope.NONE);
            assertThat(descriptor).isInstanceOf(WorkflowDescriptor.class);
            WorkflowDescriptor workflow = (WorkflowDescriptor) descriptor;

            assertThat(workflow.getName()).isEqualTo("Sample Build");
            assertThat(workflow.getRunName()).contains("Build for");
            assertThat(workflow.getEnv())
                    .extracting("name").containsExactly("GLOBAL_VAR");
            assertThat(workflow.getPermissions())
                    .extracting("scope").contains("contents", "pull-requests");
            assertThat(workflow.getConcurrency().getCancelInProgress()).isTrue();

            Set<String> events = workflow.getTriggers().stream()
                    .map(t -> t.getEvent()).collect(Collectors.toSet());
            assertThat(events).contains("push", "pull_request", "workflow_dispatch", "schedule");
            workflow.getTriggers().stream()
                    .filter(t -> "schedule".equals(t.getEvent()))
                    .findFirst()
                    .ifPresent(t -> assertThat(t.getCron()).isEqualTo("0 4 * * 1"));

            List<JobDescriptor> jobs = workflow.getJobs();
            assertThat(jobs).hasSize(2);

            JobDescriptor build = findJob(jobs, "build");
            assertThat(build.getRunsOn()).isEqualTo("ubuntu-latest");
            assertThat(build.getTimeoutMinutes()).isEqualTo(30);
            assertThat(build.getEnv()).extracting("name").containsExactly("JOB_VAR");
            assertThat(build.getOutputs()).extracting("name").containsExactly("version");
            assertThat(build.getServices()).hasSize(1);
            assertThat(build.getServices().get(0).getImage()).isEqualTo("postgres:15");

            StrategyDescriptor strategy = build.getStrategy();
            assertThat(strategy).isNotNull();
            assertThat(strategy.getFailFast()).isFalse();
            assertThat(strategy.getMaxParallel()).isEqualTo(4);
            assertThat(strategy.getDimensions()).extracting(MatrixDimensionDescriptor::getName)
                    .containsExactlyInAnyOrder("java", "os");
            assertThat(strategy.getIncludes()).hasSize(1);
            assertThat(strategy.getExcludes()).hasSize(1);

            // XO does not preserve relationship order; the `index` property is the canonical step order.
            List<StepDescriptor> steps = build.getSteps().stream()
                    .sorted(Comparator.comparingInt(StepDescriptor::getIndex))
                    .collect(Collectors.toList());
            assertThat(steps).hasSize(5);
            assertThat(steps).extracting(StepDescriptor::getIndex)
                    .containsExactly(0, 1, 2, 3, 4);

            ActionDescriptor checkout = steps.get(0).getAction();
            assertThat(checkout).isNotNull();
            assertThat(checkout.getOwner()).isEqualTo("actions");
            assertThat(checkout.getRepoOrName()).isEqualTo("checkout");
            assertThat(checkout.getRef()).isEqualTo("v4");
            assertThat(checkout.getKind()).isEqualTo("marketplace");

            StepDescriptor setupJava = steps.get(1);
            assertThat(setupJava.getWith()).extracting("name")
                    .containsExactlyInAnyOrder("java-version", "distribution");

            StepDescriptor runStep = steps.get(2);
            assertThat(runStep.getScript()).isNotNull();
            assertThat(runStep.getScript().getContent()).contains("GITHUB_OUTPUT");
            assertThat(runStep.getEnv()).extracting("name").containsExactly("STEP_VAR");
            assertThat(runStep.getWorkingDirectory()).isEqualTo("./build");

            assertThat(steps.get(3).getAction().getKind()).isEqualTo("local");
            assertThat(steps.get(4).getAction().getKind()).isEqualTo("docker");

            JobDescriptor publish = findJob(jobs, "publish");
            assertThat(publish.getNeeds()).extracting("id").containsExactly("build");
            assertThat(publish.getReusableWorkflow()).isNotNull();
            assertThat(publish.getReusableWorkflow().getOwner()).isEqualTo("octo-org");
            assertThat(publish.getReusableWorkflow().getRepo()).isEqualTo("example");
            assertThat(publish.getReusableWorkflow().getPath()).isEqualTo(".github/workflows/release.yml");
            assertThat(publish.getReusableWorkflow().getRef()).isEqualTo("v1");
            assertThat(publish.getWith()).extracting("name").containsExactly("version");
            assertThat(publish.getPassedSecrets()).extracting("name").containsExactly("inherit");
        } finally {
            store.commitTransaction();
        }
    }

    private static JobDescriptor findJob(List<JobDescriptor> jobs, String id) {
        return jobs.stream().filter(j -> id.equals(j.getId())).findFirst()
                .orElseThrow(() -> new AssertionError("missing job: " + id));
    }

}
