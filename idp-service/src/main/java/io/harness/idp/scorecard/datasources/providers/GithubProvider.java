/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.datasources.providers;

import static io.harness.idp.common.Constants.GITHUB_IDENTIFIER;
import static io.harness.idp.common.Constants.GITHUB_TOKEN;
import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.API_BASE_URL;
import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.AUTHORIZATION_HEADER;
import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.REPOSITORY_BRANCH;
import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.REPOSITORY_NAME;
import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.REPOSITORY_OWNER;
import static io.harness.idp.scorecard.datasourcelocations.constants.DataSourceLocations.REPO_SCM;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptedSecretValue;
import io.harness.idp.backstagebeans.BackstageCatalogEntity;
import io.harness.idp.envvariable.beans.entity.BackstageEnvSecretVariableEntity;
import io.harness.idp.envvariable.repositories.BackstageEnvVariableRepository;
import io.harness.idp.scorecard.datapoints.parser.DataPointParserFactory;
import io.harness.idp.scorecard.datapoints.service.DataPointService;
import io.harness.idp.scorecard.datasourcelocations.locations.DataSourceLocationFactory;
import io.harness.idp.scorecard.datasourcelocations.repositories.DataSourceLocationRepository;
import io.harness.idp.scorecard.datasources.utils.ConfigReader;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.IDP)
public class GithubProvider extends DataSourceProvider {
  private static final String SOURCE_LOCATION_ANNOTATION = "backstage.io/source-location";
  protected GithubProvider(DataPointService dataPointService, DataSourceLocationFactory dataSourceLocationFactory,
      DataSourceLocationRepository dataSourceLocationRepository, DataPointParserFactory dataPointParserFactory,
      BackstageEnvVariableRepository backstageEnvVariableRepository, SecretManagerClientService ngSecretService,
      ConfigReader configReader) {
    super(GITHUB_IDENTIFIER, dataPointService, dataSourceLocationFactory, dataSourceLocationRepository,
        dataPointParserFactory);
    this.backstageEnvVariableRepository = backstageEnvVariableRepository;
    this.ngSecretService = ngSecretService;
    this.configReader = configReader;
  }

  final BackstageEnvVariableRepository backstageEnvVariableRepository;
  final SecretManagerClientService ngSecretService;
  final ConfigReader configReader;
  private static final String TARGET_URL_EXPRESSION_KEY = "appConfig.integrations.github.0.apiBaseUrl";

  @Override
  public Map<String, Map<String, Object>> fetchData(String accountIdentifier, BackstageCatalogEntity entity,
      Map<String, Set<String>> dataPointsAndInputValues, String configs) {
    Map<String, String> authHeaders = this.getAuthHeaders(accountIdentifier, null);
    Map<String, String> replaceableHeaders = new HashMap<>(authHeaders);

    String catalogLocation = entity.getMetadata().getAnnotations().get(SOURCE_LOCATION_ANNOTATION);
    Map<String, String> possibleReplaceableRequestBodyPairs = new HashMap<>();
    if (catalogLocation != null) {
      possibleReplaceableRequestBodyPairs = prepareRequestBodyReplaceablePairs(catalogLocation);
    }

    return processOut(accountIdentifier, entity, dataPointsAndInputValues, replaceableHeaders,
        possibleReplaceableRequestBodyPairs, prepareUrlReplaceablePairs(configs, accountIdentifier));
  }

  @Override
  public Map<String, String> getAuthHeaders(String accountIdentifier, String configs) {
    BackstageEnvSecretVariableEntity backstageEnvSecretVariableEntity =
        (BackstageEnvSecretVariableEntity) backstageEnvVariableRepository
            .findByEnvNameAndAccountIdentifier(GITHUB_TOKEN, accountIdentifier)
            .orElse(null);
    assert backstageEnvSecretVariableEntity != null;
    String secretIdentifier = backstageEnvSecretVariableEntity.getHarnessSecretIdentifier();
    DecryptedSecretValue decryptedValue =
        ngSecretService.getDecryptedSecretValue(accountIdentifier, null, null, secretIdentifier);
    return Map.of(AUTHORIZATION_HEADER, "Bearer " + decryptedValue.getDecryptedValue());
  }

  private Map<String, String> prepareRequestBodyReplaceablePairs(String catalogLocation) {
    Map<String, String> possibleReplaceableRequestBodyPairs = new HashMap<>();

    String[] catalogLocationParts = catalogLocation.split("/");

    try {
      possibleReplaceableRequestBodyPairs.put(REPO_SCM, catalogLocationParts[2]);
      possibleReplaceableRequestBodyPairs.put(REPOSITORY_OWNER, catalogLocationParts[3]);
      possibleReplaceableRequestBodyPairs.put(REPOSITORY_NAME, catalogLocationParts[4]);
      possibleReplaceableRequestBodyPairs.put(REPOSITORY_BRANCH, catalogLocationParts[6]);
    } catch (ArrayIndexOutOfBoundsException e) {
      log.error("Error occurred while reading source location annotation ", e);
    }

    return possibleReplaceableRequestBodyPairs;
  }

  private Map<String, String> prepareUrlReplaceablePairs(String configs, String accountIdentifier) {
    String apiBaseUrl = (String) configReader.getConfigValues(accountIdentifier, configs, TARGET_URL_EXPRESSION_KEY);
    return Map.of(API_BASE_URL, apiBaseUrl);
  }
}
