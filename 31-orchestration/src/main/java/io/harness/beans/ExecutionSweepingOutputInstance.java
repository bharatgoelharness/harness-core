package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Level;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionSweepingOutputInstance.ExecutionSweepingOutputKeys;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.Indexed;
import io.harness.mongo.index.UniqueIndex;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.Wither;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Redesign
@Value
@Builder
@UniqueIndex(name = "levelRuntimeIdUniqueIdx2",
    fields =
    {
      @Field(ExecutionSweepingOutputKeys.planExecutionId)
      , @Field(ExecutionSweepingOutputKeys.levelRuntimeIdIdx), @Field(ExecutionSweepingOutputKeys.name)
    })
@Entity(value = "executionSweepingOutput")
@Document("executionSweepingOutput")
@TypeAlias("executionSweepingOutput")
@FieldNameConstants(innerTypeName = "ExecutionSweepingOutputKeys")
public class ExecutionSweepingOutputInstance implements PersistentEntity, UuidAccess {
  @Wither @Id @org.mongodb.morphia.annotations.Id String uuid;
  @NotNull String planExecutionId;
  @Singular List<Level> levels;
  @NotNull @Trimmed String name;
  String levelRuntimeIdIdx;

  @Getter SweepingOutput value;
  @Wither @CreatedDate Long createdAt;

  @Indexed @Builder.Default Date validUntil = Date.from(OffsetDateTime.now().plusMonths(6).toInstant());
}
