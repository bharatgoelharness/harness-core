/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.freeze.service;

import io.harness.freeze.beans.FreezeEntityType;
import io.harness.freeze.beans.response.FreezeSummaryResponseDTO;

import java.util.List;
import java.util.Map;

public interface FreezeEvaluateService {
  List<FreezeSummaryResponseDTO> getActiveFreezeEntities(
      String accountId, String orgIdentifier, String projectIdentifier, Map<FreezeEntityType, List<String>> entityMap);

  boolean globalFreezeActive(String accountId, String orgIdentifier, String projectIdentifier);

  boolean shouldDisableDeployment(String accountId, String orgIdentifier, String projectIdentifier);
}
