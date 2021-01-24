package kafka.ops.topology;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import kafka.ops.topology.actions.Action;
import kafka.ops.topology.actions.accounts.ClearAccounts;
import kafka.ops.topology.actions.accounts.CreateAccounts;
import kafka.ops.topology.model.Impl.ProjectImpl;
import kafka.ops.topology.model.Impl.TopicImpl;
import kafka.ops.topology.model.Impl.TopologyImpl;
import kafka.ops.topology.model.Project;
import kafka.ops.topology.model.Topic;
import kafka.ops.topology.model.Topology;
import kafka.ops.topology.model.cluster.ServiceAccount;
import kafka.ops.topology.model.users.Consumer;
import kafka.ops.topology.model.users.Producer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class PrincipalManagerTest {

  private Map<String, String> cliOps;
  private Properties props;

  @Mock PrincipalProvider provider;

  ExecutionPlan plan;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  PrincipalManager principalManager;
  TopologyBuilderConfig config;

  BackendController backendController;

  @Mock PrintStream mockPrintStream;

  @Mock ExecutionPlan mockPlan;

  @Before
  public void before() throws IOException {

    Files.deleteIfExists(Paths.get(".cluster-state"));
    backendController = new BackendController();

    cliOps = new HashMap<>();
    cliOps.put(BuilderCli.BROKERS_OPTION, "");
    cliOps.put(BuilderCli.ALLOW_DELETE_OPTION, "true");
    props = new Properties();

    props.put(TopologyBuilderConfig.TOPOLOGY_EXPERIMENTAL_ENABLED_CONFIG, "true");
    props.put(TopologyBuilderConfig.TOPOLOGY_STATE_FROM_CLUSTER, "false");

    plan = ExecutionPlan.init(backendController, mockPrintStream);
    config = new TopologyBuilderConfig(cliOps, props);
    principalManager = new PrincipalManager(provider, config);
  }

  @Test
  public void testFreshGeneration() throws IOException {

    Topology topology = new TopologyImpl();
    topology.setContext("context");
    Project project = new ProjectImpl("foo");
    project.setConsumers(Collections.singletonList(new Consumer("consumer")));
    project.setProducers(Collections.singletonList(new Producer("producer")));
    topology.addProject(project);

    doNothing().when(provider).configure();

    principalManager.applyCreate(topology, plan);
    principalManager.applyDelete(topology, plan);

    Collection<ServiceAccount> accounts =
        Arrays.asList(
            new ServiceAccount(-1, "consumer", "Managed by KTB"),
            new ServiceAccount(-1, "producer", "Managed by KTB"));

    assertThat(plan.getActions()).hasSize(1);
    assertThat(plan.getActions()).containsAnyOf(new CreateAccounts(provider, accounts));
  }

  @Test
  public void testFreshTopicLevelGeneration() throws IOException {

    Topology topology = new TopologyImpl();
    topology.setContext("context");
    Project project = new ProjectImpl("foo");
    Topic topic = new TopicImpl("baa");
    topic.setConsumers(Collections.singletonList(new Consumer("topicConsumer")));
    topic.setProducers(Collections.singletonList(new Producer("topicProducer")));

    project.addTopic(topic);
    topology.addProject(project);

    doNothing().when(provider).configure();

    principalManager.applyCreate(topology, plan);
    principalManager.applyDelete(topology, plan);

    Collection<ServiceAccount> accounts =
        Arrays.asList(
            new ServiceAccount(-1, "topicConsumer", "Managed by KTB"),
            new ServiceAccount(-1, "topicProducer", "Managed by KTB"));

    assertThat(plan.getActions()).hasSize(1);
    assertThat(plan.getActions()).containsAnyOf(new CreateAccounts(provider, accounts));
  }

  @Test
  public void testDeleteAccountsRequired() throws IOException {

    Topology topology = new TopologyImpl();
    topology.setContext("context");
    Project project = new ProjectImpl("foo");
    project.setConsumers(Collections.singletonList(new Consumer("consumer")));
    project.setProducers(Collections.singletonList(new Producer("producer")));
    topology.addProject(project);

    doNothing().when(provider).configure();

    doReturn(new ServiceAccount(123, "consumer", "Managed by KTB"))
        .when(provider)
        .createServiceAccount(eq("consumer"), eq("Managed by KTB"));

    doReturn(new ServiceAccount(124, "producer", "Managed by KTB"))
        .when(provider)
        .createServiceAccount(eq("producer"), eq("Managed by KTB"));

    principalManager.applyCreate(topology, plan);
    principalManager.applyDelete(topology, plan);
    plan.run();
    assertThat(plan.getServiceAccounts()).hasSize(2);

    topology = new TopologyImpl();
    topology.setContext("context");
    project = new ProjectImpl("foo");
    project.setConsumers(Collections.singletonList(new Consumer("consumer")));
    topology.addProject(project);

    backendController = new BackendController();
    plan = ExecutionPlan.init(backendController, mockPrintStream);
    principalManager.applyCreate(topology, plan);
    principalManager.applyDelete(topology, plan);
    ;

    Collection<ServiceAccount> accounts =
        Arrays.asList(new ServiceAccount(124, "producer", "Managed by KTB"));

    assertThat(plan.getActions()).hasSize(1);
    assertThat(plan.getActions()).containsAnyOf(new ClearAccounts(provider, accounts));

    plan.run();

    assertThat(plan.getServiceAccounts()).hasSize(1);
    assertThat(plan.getServiceAccounts())
        .contains(new ServiceAccount(123, "consumer", "Managed by KTB"));
  }

  @Test
  public void testNotRunIfConfigNotExperimental() throws IOException {
    props.put(TopologyBuilderConfig.TOPOLOGY_EXPERIMENTAL_ENABLED_CONFIG, "false");

    config = new TopologyBuilderConfig(cliOps, props);
    principalManager = new PrincipalManager(provider, config);

    Topology topology = new TopologyImpl();

    principalManager.applyCreate(topology, plan);
    principalManager.applyDelete(topology, plan);

    verify(mockPlan, times(0)).add(any(Action.class));
  }

  @Test
  public void testToProcessOnlySelectedPrincipals() throws IOException {

    props.put(
        TopologyBuilderConfig.SERVICE_ACCOUNT_MANAGED_PREFIXES, Collections.singletonList("pro"));

    config = new TopologyBuilderConfig(cliOps, props);
    principalManager = new PrincipalManager(provider, config);

    Topology topology = new TopologyImpl();
    topology.setContext("context");
    Project project = new ProjectImpl("foo");
    project.setConsumers(Collections.singletonList(new Consumer("consumer")));
    project.setProducers(Collections.singletonList(new Producer("producer")));
    topology.addProject(project);

    doNothing().when(provider).configure();

    doReturn(new ServiceAccount(123, "consumer", "Managed by KTB"))
        .when(provider)
        .createServiceAccount(eq("consumer"), eq("Managed by KTB"));

    doReturn(new ServiceAccount(124, "producer", "Managed by KTB"))
        .when(provider)
        .createServiceAccount(eq("producer"), eq("Managed by KTB"));

    principalManager.applyCreate(topology, plan);
    principalManager.applyDelete(topology, plan);
    plan.run();

    assertThat(plan.getServiceAccounts()).hasSize(1);
  }
}
