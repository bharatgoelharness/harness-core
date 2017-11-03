package software.wings.sm.states;

import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.amazonaws.regions.Regions;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.ElbStateExecutionData;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.ElasticLoadBalancerConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.KmsService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;

import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 10/3/16.
 */
public class ElasticLoadBalancerState extends State {
  @Transient @Inject private transient InfrastructureMappingService infrastructureMappingService;

  @Transient @Inject private transient SettingsService settingsService;

  @Transient @Inject private transient AwsHelperService awsHelperService;

  @Attributes(title = "Operation") private Operation operation;

  @DefaultValue("Instance") @Attributes(title = "Entity") @SchemaIgnore private Entity entity = Entity.Instance;

  @Attributes(title = "Custom Entity") @SchemaIgnore private String custom;

  @Transient @Inject private transient KmsService kmsService;

  @Transient @Inject private transient EncryptionService encryptionService;

  public ElasticLoadBalancerState(String name) {
    super(name, StateType.ELASTIC_LOAD_BALANCER.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    ExecutionStatus status = ExecutionStatus.SUCCESS;

    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(context.getAppId(), phaseElement.getInfraMappingId());

    String loadBalancerName;
    String region;

    if (infrastructureMapping instanceof AwsInfrastructureMapping) {
      loadBalancerName = ((AwsInfrastructureMapping) infrastructureMapping).getLoadBalancerId();
      region = ((AwsInfrastructureMapping) infrastructureMapping).getRegion();
      SettingAttribute settingAttribute = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
      AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();
      encryptionService.decrypt(
          awsConfig, kmsService.getEncryptionDetails(awsConfig, context.getWorkflowId(), context.getAppId()));
      return execute(
          context, loadBalancerName, Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
    } else if (infrastructureMapping instanceof PhysicalInfrastructureMapping) {
      SettingAttribute elbSetting =
          settingsService.get(((PhysicalInfrastructureMapping) infrastructureMapping).getLoadBalancerId());
      ElasticLoadBalancerConfig loadBalancerConfig = (ElasticLoadBalancerConfig) elbSetting.getValue();
      encryptionService.decrypt(loadBalancerConfig,
          kmsService.getEncryptionDetails(loadBalancerConfig, context.getWorkflowId(), context.getAppId()));
      loadBalancerName = loadBalancerConfig.getLoadBalancerName();
      region = loadBalancerConfig.getRegion().name();
      return execute(context, loadBalancerName, Regions.valueOf(region), loadBalancerConfig.getAccessKey(),
          loadBalancerConfig.getSecretKey());
    } else {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "ELB operations not supported");
    }
  }

  public ExecutionResponse execute(
      ExecutionContext context, String loadBalancerName, Regions region, String accessKey, char[] secretKey) {
    ExecutionStatus status;

    InstanceElement instance = context.getContextElement(ContextElementType.INSTANCE);
    final String instanceId = instance.getHost().getInstanceId() != null
        ? instance.getHost().getInstanceId()
        : awsHelperService.getInstanceId(region, accessKey, secretKey, instance.getHost().getHostName());

    String errorMessage = "";

    try {
      boolean result = operation == Operation.Enable ? awsHelperService.registerInstancesWithLoadBalancer(
                                                           region, accessKey, secretKey, loadBalancerName, instanceId)
                                                     : awsHelperService.deregisterInstancesFromLoadBalancer(
                                                           region, accessKey, secretKey, loadBalancerName, instanceId);
      status = result ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED;
    } catch (Exception e) {
      status = ExecutionStatus.ERROR;
      errorMessage = e.getMessage();
    }

    return anExecutionResponse()
        .withStateExecutionData(ElbStateExecutionData.builder().hostName(instance.getHost().getHostName()).build())
        .withExecutionStatus(status)
        .withErrorMessage(errorMessage)
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  /**
   * Getter for property 'operation'.
   *
   * @return Value for property 'operation'.
   */
  public Operation getOperation() {
    return operation;
  }

  /**
   * Setter for property 'operation'.
   *
   * @param operation Value to set for property 'operation'.
   */
  public void setOperation(Operation operation) {
    this.operation = operation;
  }

  public String getCustom() {
    return custom;
  }

  public void setCustom(String custom) {
    this.custom = custom;
  }

  /**
   * Getter for property 'entity'.
   *
   * @return Value for property 'entity'.
   */
  public Entity getEntity() {
    return entity;
  }

  /**
   * Setter for property 'entity'.
   *
   * @param entity Value to set for property 'entity'.
   */
  public void setEntity(Entity entity) {
    this.entity = entity;
  }

  public enum Operation { Enable, Disable }

  @Override
  @SchemaIgnore
  public ContextElementType getRequiredContextElementType() {
    return ContextElementType.INSTANCE;
  }

  private enum Entity {
    Instance("${instance}"),
    Custom("");

    private String expression;

    Entity(String expression) {
      this.expression = expression;
    }

    public String getExpression() {
      return expression;
    }
  }
}
