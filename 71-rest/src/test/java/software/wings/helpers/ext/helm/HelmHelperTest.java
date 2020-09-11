package software.wings.helpers.ext.helm;

import static io.harness.expression.ExpressionEvaluator.ARTIFACT_FILE_NAME_VARIABLE;
import static io.harness.expression.ExpressionEvaluator.DEFAULT_ARTIFACT_VARIABLE_NAME;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.IVAN;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static io.harness.rule.OwnerRule.YOGESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.utils.HelmTestConstants.INVALID_VALUES_YAML;
import static software.wings.utils.HelmTestConstants.VALID_VALUES_YAML;

import io.harness.category.element.UnitTests;
import io.harness.delegate.service.ExecutionConfigOverrideFromFileOnDelegate;
import io.harness.exception.WingsException;
import io.harness.helm.HelmConstants;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.GitConfig;
import software.wings.beans.HelmExecutionSummary;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.settings.helm.AmazonS3HelmRepoConfig;
import software.wings.beans.settings.helm.GCSHelmRepoConfig;
import software.wings.beans.settings.helm.HelmRepoConfig;
import software.wings.beans.settings.helm.HttpHelmRepoConfig;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;

import java.util.HashSet;
import java.util.Set;

public class HelmHelperTest extends WingsBaseTest {
  @Mock private ExecutionConfigOverrideFromFileOnDelegate delegateLocalConfigService;

  @InjectMocks private HelmHelper helmHelper;

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldReturnTrueWhenStringPresentInValuesYaml() {
    String valuesYaml = "abc\ndef";
    String toFind = "def";

    boolean isPresent = HelmHelper.checkStringPresentInHelmValueYaml(valuesYaml, toFind);

    assertThat(isPresent).isTrue();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldReturnFalseWhenStringNotPresentInValuesYaml() {
    String valuesYaml = "abc\ndef";
    String toFind = "defg";

    boolean isPresent = HelmHelper.checkStringPresentInHelmValueYaml(valuesYaml, toFind);

    assertThat(isPresent).isFalse();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void shouldReturnFalseWhenYamlFileContentIsEmpty() {
    assertThat(HelmHelper.checkStringPresentInHelmValueYaml("", "anything")).isFalse();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldIgnoreCommentsInValuesYaml() {
    String valuesYaml = "abc\n#def";
    String toFind = "def";

    boolean isPresent = HelmHelper.checkStringPresentInHelmValueYaml(valuesYaml, toFind);

    assertThat(isPresent).isFalse();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetRepoUrlForHelmRepoConfigForNullConfig() {
    HelmChartConfigParams chartConfigParams = HelmChartConfigParams.builder().chartUrl("abc").build();
    assertThat(helmHelper.getRepoUrlForHelmRepoConfig(chartConfigParams)).isEqualTo("abc");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetRepoUrlForHelmRepoConfigForNullConfigBlankUrl() {
    HelmChartConfigParams chartConfigParams = HelmChartConfigParams.builder().chartName("stable/etcd").build();
    assertThat(helmHelper.getRepoUrlForHelmRepoConfig(chartConfigParams)).isEqualTo("stable");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetRepoUrlForHelmRepoConfigForHttpConfig() {
    HelmChartConfigParams chartConfigParams =
        HelmChartConfigParams.builder()
            .helmRepoConfig(HttpHelmRepoConfig.builder().chartRepoUrl("a.com").build())
            .build();

    assertThat(helmHelper.getRepoUrlForHelmRepoConfig(chartConfigParams)).isEqualTo("a.com");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetRepoUrlForHelmRepoConfigForS3Config() {
    HelmChartConfigParams chartConfigParams =
        HelmChartConfigParams.builder()
            .helmRepoConfig(AmazonS3HelmRepoConfig.builder().bucketName("foo").build())
            .basePath("bar")
            .build();
    assertThat(helmHelper.getRepoUrlForHelmRepoConfig(chartConfigParams)).isEqualTo("s3://foo/bar");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetRepoUrlForHelmRepoConfigForGcsConfig() {
    HelmChartConfigParams chartConfigParams = HelmChartConfigParams.builder()
                                                  .helmRepoConfig(GCSHelmRepoConfig.builder().bucketName("foo").build())
                                                  .basePath("bar")
                                                  .build();
    assertThat(helmHelper.getRepoUrlForHelmRepoConfig(chartConfigParams)).isEqualTo("gs://foo/bar");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetRepoUrlForHelmRepoConfigForUnknownType() {
    HelmChartConfigParams chartConfigParams =
        HelmChartConfigParams.builder().helmRepoConfig(mock(HelmRepoConfig.class)).basePath("bar").build();

    assertThat(helmHelper.getRepoUrlForHelmRepoConfig(chartConfigParams)).isNullOrEmpty();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testPrepareHelmExecutionSummary() {
    final String releaseName = "release-name";
    HelmChartSpecification chartSpecification =
        HelmChartSpecification.builder().chartUrl("abc.com").chartName("sql").chartVersion("1.1.42").build();
    HelmExecutionSummary helmExecutionSummary =
        helmHelper.prepareHelmExecutionSummary(releaseName, chartSpecification, null);
    assertThat(helmExecutionSummary.getReleaseName()).isEqualTo(releaseName);
    assertThat(helmExecutionSummary.getHelmChartInfo()).isNotNull();
    assertThat(helmExecutionSummary.getHelmChartInfo().getName()).isNotNull();
    assertThat(helmExecutionSummary.getHelmChartInfo().getRepoUrl()).isNotNull();
    assertThat(helmExecutionSummary.getHelmChartInfo().getVersion()).isNotNull();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testPrepareHelmExecutionSummarySourceRepo() {
    final String releaseName = "release-name";
    K8sDelegateManifestConfig delegateManifestConfig = K8sDelegateManifestConfig.builder()
                                                           .manifestStoreTypes(StoreType.HelmSourceRepo)
                                                           .gitConfig(GitConfig.builder().repoUrl("abc.com").build())
                                                           .build();
    HelmExecutionSummary helmExecutionSummary =
        helmHelper.prepareHelmExecutionSummary(releaseName, null, delegateManifestConfig);
    assertThat(helmExecutionSummary.getReleaseName()).isEqualTo(releaseName);
    assertThat(helmExecutionSummary.getHelmChartInfo()).isNotNull();
    assertThat(helmExecutionSummary.getHelmChartInfo().getRepoUrl()).isNotNull();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testPrepareHelmExecutionSummaryChartRepo() {
    final String releaseName = "release-name";
    K8sDelegateManifestConfig delegateManifestConfig =
        K8sDelegateManifestConfig.builder()
            .manifestStoreTypes(StoreType.HelmChartRepo)
            .helmChartConfigParams(
                HelmChartConfigParams.builder().chartName("stable/etcd").chartVersion("1.0.0").build())
            .build();
    HelmExecutionSummary helmExecutionSummary =
        helmHelper.prepareHelmExecutionSummary(releaseName, null, delegateManifestConfig);
    assertThat(helmExecutionSummary.getReleaseName()).isEqualTo(releaseName);
    assertThat(helmExecutionSummary.getHelmChartInfo()).isNotNull();
    assertThat(helmExecutionSummary.getHelmChartInfo().getName()).isNotNull();
    assertThat(helmExecutionSummary.getHelmChartInfo().getRepoUrl()).isNotNull();
    assertThat(helmExecutionSummary.getHelmChartInfo().getVersion()).isNotNull();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testValidateEmptyHelmValueYamlFile() {
    assertThatExceptionOfType(WingsException.class).isThrownBy(() -> helmHelper.validateHelmValueYamlFile(""));
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testValidateHelmValueYamlFile() {
    helmHelper.validateHelmValueYamlFile(VALID_VALUES_YAML);
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> helmHelper.validateHelmValueYamlFile(INVALID_VALUES_YAML));
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testReplaceManifestPlaceholdersWithLocalConfig() {
    String manifestContent = "testKey nonSecretValue";
    String manifestMaskSecret = "testValue nonSecretValue";
    ManifestFile manifestFile = ManifestFile.builder().fileContent(manifestContent).build();
    when(delegateLocalConfigService.replacePlaceholdersWithLocalConfig(manifestContent)).thenReturn(manifestMaskSecret);

    helmHelper.replaceManifestPlaceholdersWithLocalConfig(manifestFile);

    assertThat(manifestFile.getFileContent()).isEqualTo(manifestMaskSecret);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testIsArtifactReferencedInValuesYaml() {
    assertThat(HelmHelper.isArtifactReferencedInValuesYaml("value: Text value\n")).isFalse();

    String dockerImageNameRef = "dockerImageName: " + HelmConstants.HELM_DOCKER_IMAGE_NAME_PLACEHOLDER;
    String dockerImageTagRef = "dockerImageTag: " + HelmConstants.HELM_DOCKER_IMAGE_TAG_PLACEHOLDER;
    assertThat(HelmHelper.isArtifactReferencedInValuesYaml(dockerImageNameRef)).isTrue();
    assertThat(HelmHelper.isArtifactReferencedInValuesYaml(dockerImageTagRef)).isTrue();

    String multipleRefs = "image: ${" + ARTIFACT_FILE_NAME_VARIABLE + "}\n";
    multipleRefs += "buildNo: ${" + DEFAULT_ARTIFACT_VARIABLE_NAME + ".buildNo}\n";
    multipleRefs += "imageTag: " + HelmConstants.HELM_DOCKER_IMAGE_TAG_PLACEHOLDER;
    assertThat(HelmHelper.isArtifactReferencedInValuesYaml(multipleRefs)).isTrue();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testUpdateArtifactVariableNamesReferencedInValuesYaml() {
    Set<String> serviceArtifactVariableNames = new HashSet<>();
    HelmHelper.updateArtifactVariableNamesReferencedInValuesYaml("var: no references", serviceArtifactVariableNames);
    assertThat(serviceArtifactVariableNames).isEmpty();

    HelmHelper.updateArtifactVariableNamesReferencedInValuesYaml(
        "image: ${" + ARTIFACT_FILE_NAME_VARIABLE + "}", serviceArtifactVariableNames);
    assertThat(serviceArtifactVariableNames).hasSize(1);
    assertThat(serviceArtifactVariableNames).contains(DEFAULT_ARTIFACT_VARIABLE_NAME);

    serviceArtifactVariableNames = new HashSet<>();
    HelmHelper.updateArtifactVariableNamesReferencedInValuesYaml(
        "imageTag: " + HelmConstants.HELM_DOCKER_IMAGE_TAG_PLACEHOLDER, serviceArtifactVariableNames);
    assertThat(serviceArtifactVariableNames).contains(DEFAULT_ARTIFACT_VARIABLE_NAME);
  }
}
