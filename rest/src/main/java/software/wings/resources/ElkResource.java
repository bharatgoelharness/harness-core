package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.ExternalServiceAuth;
import software.wings.security.annotations.PublicApi;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisRequest;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.analysis.LogRequest;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rsingh on 08/04/17.
 */
@Api(LogAnalysisResource.ELK_RESOURCE_BASE_URL)
@Path("/" + LogAnalysisResource.ELK_RESOURCE_BASE_URL)
@Produces("application/json")
@AuthRule(ResourceType.SETTING)
public class ElkResource implements LogAnalysisResource {
  @Inject private AnalysisService analysisService;

  @POST
  @Path(LogAnalysisResource.ANALYSIS_STATE_SAVE_LOG_URL)
  @Timed
  @DelegateAuth
  @ExceptionMetered
  public RestResponse<Boolean> saveRawLogData(@QueryParam("accountId") String accountId,
      @QueryParam("stateExecutionId") String stateExecutionId, @QueryParam("workflowId") String workflowId,
      @QueryParam("appId") final String appId, List<LogElement> logData) throws IOException {
    return new RestResponse<>(analysisService.saveLogData(StateType.ELK, appId, stateExecutionId, workflowId, logData));
  }

  @POST
  @Path(LogAnalysisResource.ANALYSIS_STATE_GET_LOG_URL)
  @Timed
  @ExceptionMetered
  @PublicApi
  public RestResponse<List<LogDataRecord>> getRawLogData(@QueryParam("accountId") String accountId,
      @QueryParam("compareCurrent") boolean compareCurrent, LogRequest logRequest) throws IOException {
    return new RestResponse<>(analysisService.getLogData(logRequest, compareCurrent, StateType.ELK));
  }

  @POST
  @Path(LogAnalysisResource.ANALYSIS_STATE_SAVE_ANALYSIS_RECORDS_URL)
  @Timed
  @ExceptionMetered
  @ExternalServiceAuth
  public RestResponse<Boolean> saveLogAnalysisMLRecords(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId,
      LogMLAnalysisRecord mlAnalysisResponse) throws IOException {
    mlAnalysisResponse.setApplicationId(applicationId);
    mlAnalysisResponse.setStateExecutionId(stateExecutionId);
    return new RestResponse<>(analysisService.saveLogAnalysisRecords(mlAnalysisResponse, StateType.ELK));
  }

  @POST
  @Path(LogAnalysisResource.ANALYSIS_STATE_GET_ANALYSIS_RECORDS_URL)
  @Timed
  @ExceptionMetered
  @ExternalServiceAuth
  public RestResponse<LogMLAnalysisRecord> getLogMLAnalysisRecords(
      @QueryParam("accountId") String accountId, LogMLAnalysisRequest mlAnalysisRequest) throws IOException {
    return new RestResponse<>(analysisService.getLogAnalysisRecords(mlAnalysisRequest.getApplicationId(),
        mlAnalysisRequest.getStateExecutionId(), mlAnalysisRequest.getQuery(), StateType.ELK));
  }

  @GET
  @Path(LogAnalysisResource.ANALYSIS_STATE_GET_ANALYSIS_SUMMARY_URL)
  @Timed
  @ExceptionMetered
  public RestResponse<LogMLAnalysisSummary> getLogAnalysisSummary(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId)
      throws IOException {
    return new RestResponse<>(analysisService.getAnalysisSummary(stateExecutionId, applicationId, StateType.ELK));
  }
}
