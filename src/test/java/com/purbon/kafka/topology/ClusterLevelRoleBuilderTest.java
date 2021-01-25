package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.api.mds.ClusterIds.CONNECT_CLUSTER_ID_LABEL;
import static com.purbon.kafka.topology.api.mds.ClusterIds.KAFKA_CLUSTER_ID_LABEL;
import static com.purbon.kafka.topology.api.mds.ClusterIds.SCHEMA_REGISTRY_CLUSTER_ID_LABEL;
import static com.purbon.kafka.topology.api.mds.RequestScope.RESOURCE_NAME;
import static com.purbon.kafka.topology.roles.rbac.RbacPredefinedRoles.SECURITY_ADMIN;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.purbon.kafka.topology.api.mds.ClusterIds;
import com.purbon.kafka.topology.api.mds.MdsApiClient;
import com.purbon.kafka.topology.model.users.Connector;
import com.purbon.kafka.topology.roles.rbac.ClusterLevelRoleBuilder;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class ClusterLevelRoleBuilderTest {

  @Mock
  MdsApiClient apiClient;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private Connector connector;

  private static ClusterIds allClusterIds;
  private static ClusterIds nonClusterIds;

  @BeforeClass
  public static void beforeClass() {

    allClusterIds = new ClusterIds();
    nonClusterIds = new ClusterIds();

    allClusterIds.setConnectClusterID("1234");
    allClusterIds.setSchemaRegistryClusterID("4321");
    allClusterIds.setKafkaClusterId("abcd");
  }

  @Before
  public void before() {
    connector = new Connector();
  }

  @Test
  public void testWithAllClientIdsForConnect() {
    when(apiClient.withClusterIDs()).thenReturn(allClusterIds);

    ClusterLevelRoleBuilder runner = new ClusterLevelRoleBuilder("foo", SECURITY_ADMIN, apiClient);
    runner.forKafkaConnect(connector);

    assertEquals("kafka-connect", runner.getScope().getResource(0).get(RESOURCE_NAME));

    String connectClusterId = "1234";
    Map<String, String> clusterIDs = runner.getScope().getClusterIDs();
    assertEquals(connectClusterId, clusterIDs.get(CONNECT_CLUSTER_ID_LABEL));
  }

  @Test
  public void testWithAllClientIdsForSchemaRegistry() {
    when(apiClient.withClusterIDs()).thenReturn(allClusterIds);

    ClusterLevelRoleBuilder runner = new ClusterLevelRoleBuilder("foo", SECURITY_ADMIN, apiClient);
    runner.forSchemaRegistry();

    String connectClusterId = "4321";
    Map<String, String> clusterIDs = runner.getScope().getClusterIDs();
    assertEquals(connectClusterId, clusterIDs.get(SCHEMA_REGISTRY_CLUSTER_ID_LABEL));
  }

  @Test
  public void testKafkaRun() {
    when(apiClient.withClusterIDs()).thenReturn(allClusterIds);

    ClusterLevelRoleBuilder runner = new ClusterLevelRoleBuilder("foo", SECURITY_ADMIN, apiClient);
    runner.forKafka();

    String clusterId = "abcd";
    Map<String, String> clusterIDs = runner.getScope().getClusterIDs();
    assertEquals(clusterId, clusterIDs.get(KAFKA_CLUSTER_ID_LABEL));
  }

  @Test
  public void testControlCenterRun() {
    when(apiClient.withClusterIDs()).thenReturn(allClusterIds);

    ClusterLevelRoleBuilder runner = new ClusterLevelRoleBuilder("foo", SECURITY_ADMIN, apiClient);
    runner.forControlCenter();

    String clusterId = "abcd";
    Map<String, String> clusterIDs = runner.getScope().getClusterIDs();
    assertEquals(clusterId, clusterIDs.get(KAFKA_CLUSTER_ID_LABEL));
  }

  @Test
  public void testKafkaConnectRun() {
    when(apiClient.withClusterIDs()).thenReturn(allClusterIds);

    ClusterLevelRoleBuilder runner = new ClusterLevelRoleBuilder("foo", SECURITY_ADMIN, apiClient);
    runner.forKafkaConnect();

    Map<String, String> clusterIDs = runner.getScope().getClusterIDs();
    assertEquals("1234", clusterIDs.get(CONNECT_CLUSTER_ID_LABEL));
    assertEquals("abcd", clusterIDs.get(KAFKA_CLUSTER_ID_LABEL));
  }

  @Test
  public void testSchemaRegistryRun() {
    when(apiClient.withClusterIDs()).thenReturn(allClusterIds);

    ClusterLevelRoleBuilder runner = new ClusterLevelRoleBuilder("foo", SECURITY_ADMIN, apiClient);
    runner.forSchemaSubject("foo");

    Map<String, String> clusterIDs = runner.getScope().getClusterIDs();
    assertEquals("4321", clusterIDs.get(SCHEMA_REGISTRY_CLUSTER_ID_LABEL));
    assertEquals("abcd", clusterIDs.get(KAFKA_CLUSTER_ID_LABEL));
  }
}
