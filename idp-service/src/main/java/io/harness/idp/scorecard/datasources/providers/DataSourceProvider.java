/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.datasources.providers;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.idp.common.Constants.DATA_POINT_VALUE_KEY;
import static io.harness.idp.common.Constants.ERROR_MESSAGE_KEY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.backstagebeans.BackstageCatalogEntity;
import io.harness.idp.common.CommonUtils;
import io.harness.idp.scorecard.datapoints.entity.DataPointEntity;
import io.harness.idp.scorecard.datapoints.parser.DataPointParser;
import io.harness.idp.scorecard.datapoints.parser.DataPointParserFactory;
import io.harness.idp.scorecard.datapoints.service.DataPointService;
import io.harness.idp.scorecard.datasourcelocations.entity.DataSourceLocationEntity;
import io.harness.idp.scorecard.datasourcelocations.locations.DataSourceLocation;
import io.harness.idp.scorecard.datasourcelocations.locations.DataSourceLocationFactory;
import io.harness.idp.scorecard.datasourcelocations.repositories.DataSourceLocationRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@OwnedBy(HarnessTeam.IDP)
@Slf4j
public abstract class DataSourceProvider {
  private String identifier;

  DataPointService dataPointService;
  DataSourceLocationFactory dataSourceLocationFactory;
  DataSourceLocationRepository dataSourceLocationRepository;
  DataPointParserFactory dataPointParserFactory;

  protected DataSourceProvider(String identifier, DataPointService dataPointService,
      DataSourceLocationFactory dataSourceLocationFactory, DataSourceLocationRepository dataSourceLocationRepository,
      DataPointParserFactory dataPointParserFactory) {
    this.identifier = identifier;
    this.dataPointService = dataPointService;
    this.dataSourceLocationFactory = dataSourceLocationFactory;
    this.dataSourceLocationRepository = dataSourceLocationRepository;
    this.dataPointParserFactory = dataPointParserFactory;
  }

  public abstract Map<String, Map<String, Object>> fetchData(String accountIdentifier, BackstageCatalogEntity entity,
      Map<String, Set<String>> dataPointsAndInputValues, String configs);

  protected abstract Map<String, String> getAuthHeaders(String accountIdentifier, String configs);

  protected Map<String, Map<String, Object>> processOut(String accountIdentifier,
      BackstageCatalogEntity backstageCatalogEntity, Map<String, Set<String>> dataPointsAndInputValues,
      Map<String, String> replaceableHeaders, Map<String, String> possibleReplaceableRequestBodyPairs,
      Map<String, String> possibleReplaceableUrlPairs) {
    Set<String> dataPointIdentifiers = dataPointsAndInputValues.keySet();
    Map<String, List<DataPointEntity>> dataToFetch = dataPointService.getDslDataPointsInfo(
        accountIdentifier, new ArrayList<>(dataPointIdentifiers), this.getIdentifier());

    Map<String, Map<String, Object>> aggregatedData = new HashMap<>();

    for (String dslIdentifier : dataToFetch.keySet()) {
      Map<DataPointEntity, Set<String>> dataToFetchWithInputValues =
          prepareDataToFetch(dataToFetch, dslIdentifier, dataPointsAndInputValues);

      DataSourceLocation dataSourceLocation = dataSourceLocationFactory.getDataSourceLocation(dslIdentifier);
      DataSourceLocationEntity dataSourceLocationEntity = dataSourceLocationRepository.findByIdentifier(dslIdentifier);
      Map<String, Object> response = dataSourceLocation.fetchData(accountIdentifier, backstageCatalogEntity,
          dataSourceLocationEntity, dataToFetchWithInputValues, replaceableHeaders, possibleReplaceableRequestBodyPairs,
          possibleReplaceableUrlPairs);
      log.info("Response for DSL in Process out - dsl Identifier - {} dataToFetchWithInputValues - {} Response - {} ",
          dslIdentifier, dataToFetchWithInputValues, response);

      parseResponseAgainstDataPoint(dataToFetchWithInputValues, response, aggregatedData);
    }
    log.info(
        "Aggregated data for data for DataPoints - {}, aggregated data - {}", dataPointsAndInputValues, aggregatedData);

    return aggregatedData;
  }

  private Map<DataPointEntity, Set<String>> prepareDataToFetch(Map<String, List<DataPointEntity>> dataToFetch,
      String dslIdentifier, Map<String, Set<String>> dataPointsAndInputValues) {
    Map<DataPointEntity, Set<String>> dataToFetchWithInputValues = new HashMap<>();
    dataToFetch.get(dslIdentifier)
        .forEach(dataPointEntity
            -> dataToFetchWithInputValues.put(
                dataPointEntity, dataPointsAndInputValues.get(dataPointEntity.getIdentifier())));
    return dataToFetchWithInputValues;
  }

  private void parseResponseAgainstDataPoint(Map<DataPointEntity, Set<String>> dataToFetchWithInputValues,
      Map<String, Object> response, Map<String, Map<String, Object>> aggregatedData) {
    Map<String, Object> dataPointValues = new HashMap<>();
    for (Map.Entry<DataPointEntity, Set<String>> entry : dataToFetchWithInputValues.entrySet()) {
      DataPointEntity dataPointEntity = entry.getKey();

      Object values;
      String errorMessage = (String) CommonUtils.findObjectByName(response, ERROR_MESSAGE_KEY);
      if (!isEmpty(errorMessage)) {
        Map<String, Object> dataPoint = new HashMap<>();
        dataPoint.put(DATA_POINT_VALUE_KEY, null);
        dataPoint.put(ERROR_MESSAGE_KEY, errorMessage);
        values = dataPoint;
      } else {
        Set<String> inputValues = entry.getValue();
        DataPointParser dataPointParser = dataPointParserFactory.getParser(dataPointEntity.getIdentifier());
        values = dataPointParser.parseDataPoint(response, dataPointEntity, inputValues);
      }
      if (values != null) {
        dataPointValues.put(dataPointEntity.getIdentifier(), values);
      }
    }

    Map<String, Object> providerData = aggregatedData.getOrDefault(getIdentifier(), new HashMap<>());
    providerData.putAll(dataPointValues);
    aggregatedData.put(getIdentifier(), providerData);
  }
}
