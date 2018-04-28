package software.wings.helpers.ext.helm.request;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.service.impl.ContainerServiceParams;

import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 3/22/18.
 */
@Data
public class HelmInstallCommandRequest extends HelmCommandRequest {
  private HelmChartSpecification chartSpecification;
  private Integer newReleaseVersion;
  private Integer prevReleaseVersion;
  private String namespace;
  private long timeoutInMillis;
  private Map<String, String> valueOverrides;
  private List<String> variableOverridesYamlFiles;

  public HelmInstallCommandRequest() {
    super(HelmCommandType.INSTALL);
  }

  @Builder
  public HelmInstallCommandRequest(String accountId, String appId, String kubeConfigLocation, String commandName,
      String activityId, ContainerServiceParams containerServiceParams, String releaseName,
      HelmChartSpecification chartSpecification, int newReleaseVersion, int prevReleaseVersion, String namespace,
      long timeoutInMillis, Map<String, String> valueOverrides, List<String> variableOverridesYamlFiles) {
    super(HelmCommandType.INSTALL, accountId, appId, kubeConfigLocation, commandName, activityId,
        containerServiceParams, releaseName);
    this.chartSpecification = chartSpecification;
    this.newReleaseVersion = newReleaseVersion;
    this.prevReleaseVersion = prevReleaseVersion;
    this.namespace = namespace;
    this.timeoutInMillis = timeoutInMillis;
    this.valueOverrides = valueOverrides;
    this.variableOverridesYamlFiles = variableOverridesYamlFiles;
  }
}
