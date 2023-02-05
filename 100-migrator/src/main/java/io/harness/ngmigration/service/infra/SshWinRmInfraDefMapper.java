/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service.infra;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.yaml.infra.HostConnectionTypeKind.HOSTNAME;
import static io.harness.yaml.infra.HostConnectionTypeKind.PRIVATE_IP;
import static io.harness.yaml.infra.HostConnectionTypeKind.PUBLIC_IP;

import static software.wings.ngmigration.NGMigrationEntityType.CONNECTOR;

import io.harness.cdng.elastigroup.ElastigroupConfiguration;
import io.harness.cdng.infra.beans.AwsInstanceFilter;
import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.infra.yaml.PdcInfrastructure;
import io.harness.cdng.infra.yaml.PdcInfrastructure.PdcInfrastructureBuilder;
import io.harness.cdng.infra.yaml.SshWinRmAwsInfrastructure;
import io.harness.cdng.infra.yaml.SshWinRmAwsInfrastructure.SshWinRmAwsInfrastructureBuilder;
import io.harness.cdng.infra.yaml.SshWinRmAzureInfrastructure;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.infrastructure.InfrastructureType;
import io.harness.ngmigration.beans.MigrationInputDTO;
import io.harness.ngmigration.beans.NGYamlFile;
import io.harness.ngmigration.beans.NgEntityDetail;
import io.harness.ngmigration.utils.MigratorUtility;
import io.harness.pms.yaml.ParameterField;

import software.wings.api.DeploymentType;
import software.wings.beans.AwsInstanceFilter.Tag;
import software.wings.beans.AzureTag;
import software.wings.infra.AwsInstanceInfrastructure;
import software.wings.infra.AzureInstanceInfrastructure;
import software.wings.infra.InfraMappingInfrastructureProvider;
import software.wings.infra.InfrastructureDefinition;
import software.wings.infra.PhysicalInfra;
import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.CgEntityNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public class SshWinRmInfraDefMapper implements InfraDefMapper {
  @Override
  public List<String> getConnectorIds(InfrastructureDefinition infrastructureDefinition) {
    List<String> connectorIds = new ArrayList<>();
    switch (infrastructureDefinition.getCloudProviderType()) {
      case AWS:
        AwsInstanceInfrastructure awsInfra = (AwsInstanceInfrastructure) infrastructureDefinition.getInfrastructure();
        connectorIds.add(awsInfra.getCloudProviderId());
        if (StringUtils.isNotBlank(awsInfra.getHostConnectionAttrs())) {
          connectorIds.add(awsInfra.getHostConnectionAttrs());
        }
        break;
      case AZURE:
        AzureInstanceInfrastructure azureInfra =
            (AzureInstanceInfrastructure) infrastructureDefinition.getInfrastructure();
        connectorIds.add(azureInfra.getCloudProviderId());
        if (StringUtils.isNotBlank(azureInfra.getHostConnectionAttrs())) {
          connectorIds.add(azureInfra.getHostConnectionAttrs());
        }
        break;
      case PHYSICAL_DATA_CENTER:
        PhysicalInfra pdcInfra = (PhysicalInfra) infrastructureDefinition.getInfrastructure();
        if (StringUtils.isNotBlank(pdcInfra.getHostConnectionAttrs())) {
          connectorIds.add(pdcInfra.getHostConnectionAttrs());
        }
        break;
      default:
        throw new InvalidRequestException("Unsupported Infra for K8s deployment");
    }
    return connectorIds;
  }

  @Override
  public ServiceDefinitionType getServiceDefinition(InfrastructureDefinition infrastructureDefinition) {
    return infrastructureDefinition.getDeploymentType() == DeploymentType.SSH ? ServiceDefinitionType.SSH
                                                                              : ServiceDefinitionType.WINRM;
  }

  @Override
  public InfrastructureType getInfrastructureType(InfrastructureDefinition infrastructureDefinition) {
    switch (infrastructureDefinition.getCloudProviderType()) {
      case AWS:
        return InfrastructureType.SSH_WINRM_AWS;
      case AZURE:
        return InfrastructureType.SSH_WINRM_AZURE;
      case PHYSICAL_DATA_CENTER:
        return InfrastructureType.PDC;
      default:
        throw new InvalidRequestException("Unsupported Infra for K8s deployment");
    }
  }

  @Override
  public Infrastructure getSpec(MigrationInputDTO inputDTO, InfrastructureDefinition infrastructureDefinition,
      Map<CgEntityId, NGYamlFile> migratedEntities, Map<CgEntityId, CgEntityNode> entities,
      List<ElastigroupConfiguration> elastigroupConfiguration) {
    switch (infrastructureDefinition.getCloudProviderType()) {
      case AWS:
        return getAwsSshInfra(migratedEntities, infrastructureDefinition.getInfrastructure());
      case AZURE:
        return getAzureSshInfra(migratedEntities, infrastructureDefinition.getInfrastructure());
      case PHYSICAL_DATA_CENTER:
        return getPdcSshInfra(migratedEntities, infrastructureDefinition.getInfrastructure());
      default:
        throw new InvalidRequestException("Unsupported Infra for ssh deployment");
    }
  }

  private Infrastructure getAzureSshInfra(
      Map<CgEntityId, NGYamlFile> migratedEntities, InfraMappingInfrastructureProvider infrastructure) {
    AzureInstanceInfrastructure azureInfra = (AzureInstanceInfrastructure) infrastructure;
    NgEntityDetail connectorDetail =
        migratedEntities.get(CgEntityId.builder().type(CONNECTOR).id(azureInfra.getCloudProviderId()).build())
            .getNgEntityDetail();
    return SshWinRmAzureInfrastructure.builder()
        .credentialsRef(ParameterField.createValueField(
            MigratorUtility.getSecretRef(migratedEntities, azureInfra.getHostConnectionAttrs(), CONNECTOR)
                .toSecretRefStringValue()))
        .connectorRef(ParameterField.createValueField(MigratorUtility.getIdentifierWithScope(connectorDetail)))
        .subscriptionId(ParameterField.createValueField(azureInfra.getSubscriptionId()))
        .resourceGroup(ParameterField.createValueField(azureInfra.getResourceGroup()))
        .tags(ParameterField.createValueField(getAzureTagsMap(azureInfra.getTags())))
        .hostConnectionType(ParameterField.createValueField(
            azureInfra.isUsePublicDns() ? PUBLIC_IP : (azureInfra.isUsePrivateIp() ? PRIVATE_IP : HOSTNAME)))
        .build();
  }

  private Infrastructure getAwsSshInfra(
      Map<CgEntityId, NGYamlFile> migratedEntities, InfraMappingInfrastructureProvider infrastructure) {
    AwsInstanceInfrastructure awsInfra = (AwsInstanceInfrastructure) infrastructure;
    NgEntityDetail connectorDetail =
        migratedEntities.get(CgEntityId.builder().type(CONNECTOR).id(awsInfra.getCloudProviderId()).build())
            .getNgEntityDetail();
    SshWinRmAwsInfrastructureBuilder builder = SshWinRmAwsInfrastructure.builder();
    if (awsInfra.getAwsInstanceFilter() != null) {
      AwsInstanceFilter awsInstanceFilter =
          AwsInstanceFilter.builder()
              .vpcs(awsInfra.getAwsInstanceFilter().getVpcIds())
              .tags(ParameterField.createValueField(getAwsTagsMap(awsInfra.getAwsInstanceFilter().getTags())))
              .build();
      builder.awsInstanceFilter(awsInstanceFilter);
    }
    return builder
        .credentialsRef(ParameterField.createValueField(
            MigratorUtility.getSecretRef(migratedEntities, awsInfra.getHostConnectionAttrs(), CONNECTOR)
                .toSecretRefStringValue()))
        .connectorRef(ParameterField.createValueField(MigratorUtility.getIdentifierWithScope(connectorDetail)))
        .region(ParameterField.createValueField(awsInfra.getRegion()))
        .hostConnectionType(ParameterField.createValueField(awsInfra.isUsePublicDns() ? PUBLIC_IP : PRIVATE_IP))
        .build();
  }

  private Infrastructure getPdcSshInfra(
      Map<CgEntityId, NGYamlFile> migratedEntities, InfraMappingInfrastructureProvider infrastructure) {
    PhysicalInfra pdcInfra = (PhysicalInfra) infrastructure;
    PdcInfrastructureBuilder builder = PdcInfrastructure.builder();
    builder.credentialsRef(ParameterField.createValueField(
        MigratorUtility.getSecretRef(migratedEntities, pdcInfra.getHostConnectionAttrs(), CONNECTOR)
            .toSecretRefStringValue()));
    if (isNotEmpty(pdcInfra.getHostNames())) {
      builder.hosts(ParameterField.createValueField(pdcInfra.getHostNames()));
    }
    Map<String, String> expressions = pdcInfra.getExpressions();
    if (isNotEmpty(expressions) && expressions.containsKey(PhysicalInfra.hostname)
        && expressions.containsKey(PhysicalInfra.hostArrayPath)) {
      Map<String, String> hostAttrs = new HashMap<>();
      expressions.forEach((k, v) -> {
        if (!isNotEmpty(v)) {
          if (PhysicalInfra.hostArrayPath.equals(k) && isNotEmpty(v)) {
            builder.hostObjectArray(ParameterField.createValueField(v));
          }
          hostAttrs.put(k, v);
        }
      });
      builder.hostAttributes(ParameterField.createValueField(hostAttrs));
    }

    return builder.build();
  }

  private Map<String, String> getAwsTagsMap(List<Tag> tags) {
    if (tags == null) {
      return Collections.emptyMap();
    }
    return tags.stream().collect(Collectors.toMap(Tag::getKey, Tag::getValue));
  }

  private Map<String, String> getAzureTagsMap(List<AzureTag> tags) {
    if (tags == null) {
      return Collections.emptyMap();
    }
    return tags.stream().collect(Collectors.toMap(AzureTag::getKey, AzureTag::getValue));
  }
}