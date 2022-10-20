/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.migration;

import io.harness.migration.MigrationDetails;
import io.harness.migration.NGMigration;
import io.harness.migration.beans.MigrationType;
import io.harness.ng.core.migration.PopulateYamlFieldInNGEnvironmentMigration;
import io.harness.ng.core.migration.background.AddDeploymentTypeToInfrastructureEntityMigration;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

public class NGCoreBackgroundMigrationDetails implements MigrationDetails {
  @Override
  public MigrationType getMigrationTypeName() {
    return MigrationType.MongoBGMigration;
  }

  @Override
  public boolean isBackground() {
    return true;
  }

  @Override
  public List<Pair<Integer, Class<? extends NGMigration>>> getMigrations() {
    return new ImmutableList.Builder<Pair<Integer, Class<? extends NGMigration>>>()
        .add(Pair.of(1, AddDeploymentTypeToInfrastructureEntityMigration.class))
        .add(Pair.of(2, PopulateYamlFieldInNGEnvironmentMigration.class))
        .build();
  }
}
