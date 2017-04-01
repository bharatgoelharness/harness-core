package software.wings.sm;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import com.google.common.collect.Maps;

import com.fasterxml.jackson.annotation.JsonProperty;
import software.wings.api.ExecutionDataValue;
import software.wings.beans.CountsByStatuses;

import java.util.Map;

/**
 * Represents state machine execution data.
 *
 * @author Rishi
 */
public class StateExecutionData {
  private String stateName;
  private Long startTs;
  private Long endTs;
  private ExecutionStatus status;
  private String errorMsg;
  private Integer waitInterval;

  /**
   * Gets state name.
   *
   * @return the state name
   */
  public String getStateName() {
    return stateName;
  }

  /**
   * Sets state name.
   *
   * @param stateName the state name
   */
  public void setStateName(String stateName) {
    this.stateName = stateName;
  }

  /**
   * Gets start ts.
   *
   * @return the start ts
   */
  public Long getStartTs() {
    return startTs;
  }

  /**
   * Sets start ts.
   *
   * @param startTs the start ts
   */
  public void setStartTs(Long startTs) {
    this.startTs = startTs;
  }

  /**
   * Gets end ts.
   *
   * @return the end ts
   */
  public Long getEndTs() {
    return endTs;
  }

  /**
   * Sets end ts.
   *
   * @param endTs the end ts
   */
  public void setEndTs(Long endTs) {
    this.endTs = endTs;
  }

  /**
   * Gets status.
   *
   * @return the status
   */
  public ExecutionStatus getStatus() {
    return status;
  }

  /**
   * Sets status.
   *
   * @param status the status
   */
  public void setStatus(ExecutionStatus status) {
    this.status = status;
  }

  /**
   * Gets error msg.
   *
   * @return the error msg
   */
  public String getErrorMsg() {
    return errorMsg;
  }

  /**
   * Sets error msg.
   *
   * @param errorMsg the error msg
   */
  public void setErrorMsg(String errorMsg) {
    this.errorMsg = errorMsg;
  }

  public Integer getWaitInterval() {
    return waitInterval;
  }

  public void setWaitInterval(Integer waitInterval) {
    this.waitInterval = waitInterval;
  }

  /**
   * Gets execution summary.
   *
   * @return the execution summary
   */
  @JsonProperty("executionSummary")
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionData = Maps.newLinkedHashMap();
    executionData.put("total", anExecutionDataValue().withDisplayName("Total").withValue(1).build());
    CountsByStatuses breakDown = new CountsByStatuses();
    switch (status) {
      case FAILED:
      case ERROR:
      case ABORTED:
      case ABORTING:
      case PAUSED_ON_ERROR:
        breakDown.setFailed(1);
        break;
      case NEW:
      case STARTING:
      case RUNNING:
      case PAUSED:
        breakDown.setInprogress(1);
        break;
      case SUCCESS:
        breakDown.setSuccess(1);
        break;
    }
    executionData.put("breakdown", anExecutionDataValue().withDisplayName("breakdown").withValue(breakDown).build());
    putNotNull(
        executionData, "errorMsg", anExecutionDataValue().withValue(errorMsg).withDisplayName("Error Message").build());
    return executionData;
  }

  /**
   * Sets execution summary.
   *
   * @param ignored the ignored
   */
  public void setExecutionSummary(Map<String, ExecutionDataValue> ignored) {}

  /**
   * Gets execution details.
   *
   * @return the execution details
   */
  @JsonProperty("executionDetails")
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = Maps.newLinkedHashMap();

    putNotNull(executionDetails, "errorMsg",
        anExecutionDataValue().withValue(errorMsg).withDisplayName("Error Message").build());
    putNotNull(
        executionDetails, "startTs", anExecutionDataValue().withValue(startTs).withDisplayName("Started At").build());
    putNotNull(executionDetails, "endTs", anExecutionDataValue().withValue(endTs).withDisplayName("Ended At").build());

    return executionDetails;
  }

  /**
   * Sets execution details.
   *
   * @param ignored the ignored
   */
  public void setExecutionDetails(Map<String, ExecutionDataValue> ignored) {}

  /**
   * Put not null.
   *
   * @param orderedMap the ordered map
   * @param name       the name
   * @param value      the value
   */
  protected void putNotNull(Map<String, ExecutionDataValue> orderedMap, String name, ExecutionDataValue value) {
    if (value != null && value.getValue() != null) {
      orderedMap.put(name, value);
    }
  }

  public static final class StateExecutionDataBuilder {
    private String stateName;
    private Long startTs;
    private Long endTs;
    private ExecutionStatus status;
    private String errorMsg;
    private Integer waitInterval;

    private StateExecutionDataBuilder() {}

    public static StateExecutionDataBuilder aStateExecutionData() {
      return new StateExecutionDataBuilder();
    }

    public StateExecutionDataBuilder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    public StateExecutionDataBuilder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    public StateExecutionDataBuilder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    public StateExecutionDataBuilder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public StateExecutionDataBuilder withErrorMsg(String errorMsg) {
      this.errorMsg = errorMsg;
      return this;
    }

    public StateExecutionDataBuilder withWaitInterval(Integer waitInterval) {
      this.waitInterval = waitInterval;
      return this;
    }

    public StateExecutionData build() {
      StateExecutionData stateExecutionData = new StateExecutionData();
      stateExecutionData.setStateName(stateName);
      stateExecutionData.setStartTs(startTs);
      stateExecutionData.setEndTs(endTs);
      stateExecutionData.setStatus(status);
      stateExecutionData.setErrorMsg(errorMsg);
      stateExecutionData.setWaitInterval(waitInterval);
      return stateExecutionData;
    }
  }
}
