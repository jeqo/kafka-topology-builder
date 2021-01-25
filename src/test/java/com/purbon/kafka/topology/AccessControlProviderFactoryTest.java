package com.purbon.kafka.topology;

import static com.purbon.kafka.topology.BuilderCli.BROKERS_OPTION;
import static com.purbon.kafka.topology.TopologyBuilderConfig.ACCESS_CONTROL_IMPLEMENTATION_CLASS;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_KAFKA_CLUSTER_ID_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_PASSWORD_CONFIG;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_SERVER;
import static com.purbon.kafka.topology.TopologyBuilderConfig.MDS_USER_CONFIG;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.purbon.kafka.topology.api.adminclient.TopologyBuilderAdminClient;
import com.purbon.kafka.topology.api.mds.MdsApiClient;
import com.purbon.kafka.topology.api.mds.MdsApiClientBuilder;
import com.purbon.kafka.topology.roles.RbacProvider;
import com.purbon.kafka.topology.roles.SimpleAclProvider;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class AccessControlProviderFactoryTest {

  @Mock TopologyBuilderAdminClient adminClient;

  @Mock
  MdsApiClientBuilder mdsApiClientBuilder;

  @Mock
  MdsApiClient mdsApiClient;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  Map<String, String> cliOps;
  Properties props;

  @Before
  public void before() {
    cliOps = new HashMap<>();
    cliOps.put(BROKERS_OPTION, "");
    props = new Properties();
  }

  @Test
  public void testRbacConfig() throws IOException {

    props.put(ACCESS_CONTROL_IMPLEMENTATION_CLASS, "com.purbon.kafka.topology.roles.RBACProvider");
    props.put(MDS_SERVER, "http://localhost:8090");
    props.put(MDS_USER_CONFIG, "alice");
    props.put(MDS_PASSWORD_CONFIG, "alice-secret");
    props.put(MDS_KAFKA_CLUSTER_ID_CONFIG, "UtBZ3rTSRtypmmkAL1HbHw");

    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);

    when(mdsApiClientBuilder.build()).thenReturn(mdsApiClient);

    AccessControlProviderFactory factory =
        new AccessControlProviderFactory(config, adminClient, mdsApiClientBuilder);

    AccessControlProvider provider = factory.get();

    verify(mdsApiClient, times(1)).login("alice", "alice-secret");
    verify(mdsApiClient, times(1)).authenticate();

    assertThat(provider, instanceOf(RbacProvider.class));
  }

  @Test
  public void testAclsConfig() throws IOException {

    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);

    AccessControlProviderFactory factory =
        new AccessControlProviderFactory(config, adminClient, mdsApiClientBuilder);

    assertThat(factory.get(), instanceOf(SimpleAclProvider.class));
  }

  @Test(expected = IOException.class)
  public void testWrongProviderConfig() throws IOException {

    props.put(
        ACCESS_CONTROL_IMPLEMENTATION_CLASS, "com.purbon.kafka.topology.roles.MyCustomProvider");

    TopologyBuilderConfig config = new TopologyBuilderConfig(cliOps, props);

    when(mdsApiClientBuilder.build()).thenReturn(mdsApiClient);

    AccessControlProviderFactory factory =
        new AccessControlProviderFactory(config, adminClient, mdsApiClientBuilder);
    factory.get();
  }
}
