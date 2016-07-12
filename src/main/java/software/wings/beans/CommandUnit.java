package software.wings.beans;

// TODO: Auto-generated Javadoc

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.github.reinert.jjschema.SchemaIgnore;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Objects;

/**
 * Created by anubhaw on 5/25/16.
 */
@JsonTypeInfo(use = Id.NAME, property = "commandUnitType")
@JsonSubTypes({
  @Type(value = Command.class, name = "COMMAND")
  , @Type(value = ExecCommandUnit.class, name = "EXEC"),
      @Type(value = CopyArtifactCommandUnit.class, name = "COPY_ARTIFACT"),
      @Type(value = CopyAppContainerCommandUnit.class, name = "COPY_CONTAINER")
})
public abstract class CommandUnit {
  @SchemaIgnore private String name;
  private CommandUnitType commandUnitType;
  private ExecutionResult executionResult;
  private boolean artifactNeeded;

  /**
   * Instantiates a new Command unit.
   */
  public CommandUnit(){}

  ;

  /**
   * Instantiates a new command unit.
   *
   * @param commandUnitType the command unit type
   */
  public CommandUnit(CommandUnitType commandUnitType) {
    this.commandUnitType = commandUnitType;
  }

  /**
   * Sets .
   *
   * @param context the context
   */
  public void setup(CommandExecutionContext context) {}

  /**
   * Gets command unit type.
   *
   * @return the command unit type
   */
  @SchemaIgnore
  public CommandUnitType getCommandUnitType() {
    return commandUnitType;
  }

  /**
   * Sets command unit type.
   *
   * @param commandUnitType the command unit type
   */
  public void setCommandUnitType(CommandUnitType commandUnitType) {
    this.commandUnitType = commandUnitType;
  }

  /**
   * Gets execution result.
   *
   * @return the execution result
   */
  @SchemaIgnore
  public ExecutionResult getExecutionResult() {
    return executionResult;
  }

  /**
   * Sets execution result.
   *
   * @param executionResult the execution result
   */
  public void setExecutionResult(ExecutionResult executionResult) {
    this.executionResult = executionResult;
  }

  /**
   * Gets name.
   *
   * @return the name
   */
  @SchemaIgnore
  public String getName() {
    return name;
  }

  /**
   * Sets name.
   *
   * @param name the name
   */
  @SchemaIgnore
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Is artifact needed boolean.
   *
   * @return the boolean
   */
  @SchemaIgnore
  public boolean isArtifactNeeded() {
    return artifactNeeded;
  }

  /**
   * Sets artifact needed.
   *
   * @param artifactNeeded the artifact needed
   */
  @SchemaIgnore
  public void setArtifactNeeded(boolean artifactNeeded) {
    this.artifactNeeded = artifactNeeded;
  }

  /**
   * The Enum ExecutionResult.
   */
  public enum ExecutionResult {
    /**
     * Success execution result.
     */
    SUCCESS, /**
              * Failure execution result.
              */
    FAILURE, /**
              * Running execution result.
              */
    RUNNING;

    /**
     * Created by peeyushaggarwal on 7/8/16.
     */
    public static class ExecutionResultData implements NotifyResponseData {
      private ExecutionResult result;

      private String errorMessage;

      /**
       * Gets result.
       *
       * @return the result
       */
      public ExecutionResult getResult() {
        return result;
      }

      /**
       * Sets result.
       *
       * @param result the result
       */
      public void setResult(ExecutionResult result) {
        this.result = result;
      }

      /**
       * Gets error message.
       *
       * @return the error message
       */
      public String getErrorMessage() {
        return errorMessage;
      }

      /**
       * Sets error message.
       *
       * @param errorMessage the error message
       */
      public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
      }

      @Override
      public String toString() {
        return MoreObjects.toStringHelper(this).add("result", result).add("errorMessage", errorMessage).toString();
      }

      @Override
      public int hashCode() {
        return Objects.hash(result, errorMessage);
      }

      @Override
      public boolean equals(Object obj) {
        if (this == obj) {
          return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
          return false;
        }
        final ExecutionResultData other = (ExecutionResultData) obj;
        return Objects.equals(this.result, other.result) && Objects.equals(this.errorMessage, other.errorMessage);
      }

      /**
       * The type Builder.
       */
      public static final class Builder {
        private ExecutionResult result;
        private String errorMessage;

        private Builder() {}

        /**
         * An execution result data builder.
         *
         * @return the builder
         */
        public static Builder anExecutionResultData() {
          return new Builder();
        }

        /**
         * With result builder.
         *
         * @param result the result
         * @return the builder
         */
        public Builder withResult(ExecutionResult result) {
          this.result = result;
          return this;
        }

        /**
         * With error message builder.
         *
         * @param errorMessage the error message
         * @return the builder
         */
        public Builder withErrorMessage(String errorMessage) {
          this.errorMessage = errorMessage;
          return this;
        }

        /**
         * But builder.
         *
         * @return the builder
         */
        public Builder but() {
          return anExecutionResultData().withResult(result).withErrorMessage(errorMessage);
        }

        /**
         * Build execution result data.
         *
         * @return the execution result data
         */
        public ExecutionResultData build() {
          ExecutionResultData executionResultData = new ExecutionResultData();
          executionResultData.setResult(result);
          executionResultData.setErrorMessage(errorMessage);
          return executionResultData;
        }
      }
    }
  }
}
