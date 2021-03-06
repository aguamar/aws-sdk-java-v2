package software.amazon.awssdk.services.query;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import org.w3c.dom.Node;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.client.handler.AwsSyncClientHandler;
import software.amazon.awssdk.awscore.config.AwsSyncClientConfiguration;
import software.amazon.awssdk.awscore.http.DefaultErrorResponseHandler;
import software.amazon.awssdk.core.client.ClientExecutionParams;
import software.amazon.awssdk.core.client.SyncClientHandler;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.StaxResponseHandler;
import software.amazon.awssdk.core.runtime.transform.StandardErrorUnmarshaller;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;
import software.amazon.awssdk.services.query.model.APostOperationRequest;
import software.amazon.awssdk.services.query.model.APostOperationResponse;
import software.amazon.awssdk.services.query.model.APostOperationWithOutputRequest;
import software.amazon.awssdk.services.query.model.APostOperationWithOutputResponse;
import software.amazon.awssdk.services.query.model.InvalidInputException;
import software.amazon.awssdk.services.query.model.QueryException;
import software.amazon.awssdk.services.query.transform.APostOperationRequestMarshaller;
import software.amazon.awssdk.services.query.transform.APostOperationResponseUnmarshaller;
import software.amazon.awssdk.services.query.transform.APostOperationWithOutputRequestMarshaller;
import software.amazon.awssdk.services.query.transform.APostOperationWithOutputResponseUnmarshaller;
import software.amazon.awssdk.services.query.transform.InvalidInputExceptionUnmarshaller;

/**
 * Internal implementation of {@link QueryClient}.
 *
 * @see QueryClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultQueryClient implements QueryClient {
    private final SyncClientHandler clientHandler;

    private final List<Unmarshaller<SdkServiceException, Node>> exceptionUnmarshallers;

    private final AwsSyncClientConfiguration clientConfiguration;

    protected DefaultQueryClient(AwsSyncClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsSyncClientHandler(clientConfiguration, null);
        this.exceptionUnmarshallers = init();
        this.clientConfiguration = clientConfiguration;
    }

    @Override
    public final String serviceName() {
        return SERVICE_NAME;
    }

    /**
     * <p>
     * Performs a post operation to the query service and has no output
     * </p>
     *
     * @param aPostOperationRequest
     * @return Result of the APostOperation operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.APostOperation
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/APostOperation" target="_top">AWS
     *      API Documentation</a>
     */
    @Override
    public APostOperationResponse aPostOperation(APostOperationRequest aPostOperationRequest) throws InvalidInputException,
                                                                                                     SdkServiceException, SdkClientException, QueryException {

        StaxResponseHandler<APostOperationResponse> responseHandler = new StaxResponseHandler<APostOperationResponse>(
            new APostOperationResponseUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(new ClientExecutionParams<APostOperationRequest, APostOperationResponse>()
                                         .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                         .withInput(aPostOperationRequest).withMarshaller(new APostOperationRequestMarshaller()));
    }

    /**
     * <p>
     * Performs a post operation to the query service and has modelled output
     * </p>
     *
     * @param aPostOperationWithOutputRequest
     * @return Result of the APostOperationWithOutput operation returned by the service.
     * @throws InvalidInputException
     *         The request was rejected because an invalid or out-of-range value was supplied for an input parameter.
     * @throws SdkException
     *         Base class for all exceptions that can be thrown by the SDK (both service and client). Can be used for
     *         catch all scenarios.
     * @throws SdkClientException
     *         If any client side error occurs such as an IO related failure, failure to get credentials, etc.
     * @throws QueryException
     *         Base class for all service exceptions. Unknown exceptions will be thrown as an instance of this type.
     * @sample QueryClient.APostOperationWithOutput
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/query-service-2010-05-08/APostOperationWithOutput"
     *      target="_top">AWS API Documentation</a>
     */
    @Override
    public APostOperationWithOutputResponse aPostOperationWithOutput(
        APostOperationWithOutputRequest aPostOperationWithOutputRequest) throws InvalidInputException, SdkServiceException,
                                                                                SdkClientException, QueryException {

        StaxResponseHandler<APostOperationWithOutputResponse> responseHandler = new StaxResponseHandler<APostOperationWithOutputResponse>(
            new APostOperationWithOutputResponseUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
            .execute(new ClientExecutionParams<APostOperationWithOutputRequest, APostOperationWithOutputResponse>()
                         .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                         .withInput(aPostOperationWithOutputRequest)
                         .withMarshaller(new APostOperationWithOutputRequestMarshaller()));
    }

    private List<Unmarshaller<SdkServiceException, Node>> init() {
        List<Unmarshaller<SdkServiceException, Node>> unmarshallers = new ArrayList<>();
        unmarshallers.add(new InvalidInputExceptionUnmarshaller());
        unmarshallers.add(new StandardErrorUnmarshaller(QueryException.class));
        return unmarshallers;
    }

    @Override
    public void close() {
        clientHandler.close();
    }
}
