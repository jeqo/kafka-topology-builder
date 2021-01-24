package kafka.ops.topology.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import java.util.Map;
import kafka.ops.topology.model.Impl.ProjectImpl;
import kafka.ops.topology.model.users.Connector;
import kafka.ops.topology.model.users.Consumer;
import kafka.ops.topology.model.users.KStream;
import kafka.ops.topology.model.users.Producer;
import kafka.ops.topology.model.users.Schemas;

@JsonDeserialize(as = ProjectImpl.class)
public interface Project {

  String getName();

  List<String> getZookeepers();

  List<Consumer> getConsumers();

  void setConsumers(List<Consumer> consumers);

  List<Producer> getProducers();

  void setProducers(List<Producer> producers);

  List<KStream> getStreams();

  void setStreams(List<KStream> streams);

  List<Connector> getConnectors();

  void setConnectors(List<Connector> connectors);

  List<Schemas> getSchemas();

  void setSchemas(List<Schemas> schemas);

  List<Topic> getTopics();

  void addTopic(Topic topic);

  void setTopics(List<Topic> topics);

  String namePrefix();

  void setRbacRawRoles(Map<String, List<String>> rbacRawRoles);

  Map<String, List<String>> getRbacRawRoles();

  void setPrefixContextAndOrder(Map<String, Object> asFullContext, List<String> order);
}
