/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.core.http.pipeline.stages;

import software.amazon.awssdk.core.RequestExecutionContext;
import software.amazon.awssdk.core.auth.AwsCredentials;
import software.amazon.awssdk.core.auth.CanHandleNullCredentials;
import software.amazon.awssdk.core.auth.signer_spi.Signer;
import software.amazon.awssdk.core.auth.signer_spi.SignerContext;
import software.amazon.awssdk.core.auth.signer_spi.SignerParams;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.HttpClientDependencies;
import software.amazon.awssdk.core.http.InterruptMonitor;
import software.amazon.awssdk.core.http.pipeline.RequestToRequestPipeline;
import software.amazon.awssdk.core.interceptor.AwsExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Sign the marshalled request (if applicable).
 */
// TODO how does signing work with a request provider
public class SigningStage implements RequestToRequestPipeline {

    private final HttpClientDependencies dependencies;

    public SigningStage(HttpClientDependencies dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * Returns the response from executing one httpClientSettings request; or null for retry.
     */
    public SdkHttpFullRequest execute(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
        InterruptMonitor.checkInterrupted();
        SdkHttpFullRequest signedReq = signRequest(request, context);
        System.out.println(signedReq);
        return signedReq;
    }

    /**
     * Sign the request if the signer if provided and credentials are present.
     */
    private SdkHttpFullRequest signRequest(SdkHttpFullRequest request, RequestExecutionContext context) {
        final AwsCredentials credentials = context.executionAttributes().getAttribute(AwsExecutionAttributes.AWS_CREDENTIALS);
        updateInterceptorContext(request, context.executionContext());
        Signer signer = context.signer();
        if (shouldSign(signer, credentials)) {
            adjustForClockSkew(context.executionAttributes());

            //TODO how to pass signerContext
            return signer.sign(request, constructSignerParams(context.executionAttributes()));
        }
        return request;
    }

    private SignerContext constructSignerParams(ExecutionAttributes executionAttributes) {
        SignerParams signerParams = new SignerParams();
        signerParams.setAwsCredentials(executionAttributes.getAttribute(AwsExecutionAttributes.AWS_CREDENTIALS));
        signerParams.setSigningName("dynamodb");//executionAttributes.getAttribute(AwsExecutionAttributes.SERVICE_NAME));
        signerParams.setRegion(executionAttributes.getAttribute(AwsExecutionAttributes.AWS_REGION));

        Integer offset = executionAttributes.getAttribute(AwsExecutionAttributes.TIME_OFFSET);
        signerParams.setTimeOffset(offset);

        SignerContext signerContext = new SignerContext();
        signerContext.putAttribute(AwsExecutionAttributes.SIGNER_PARAMS, signerParams);
        return signerContext;
    }

    /**
     * TODO: Remove when we stop having two copies of the request.
     */
    private void updateInterceptorContext(SdkHttpFullRequest request, ExecutionContext executionContext) {
        executionContext.interceptorContext(executionContext.interceptorContext().copy(b -> b.httpRequest(request)));
    }

    /**
     * We sign if a signer is provided and the credentials are non-null (unless the signer implements the marker interface,
     * {@link
     * CanHandleNullCredentials}).
     *
     * @return True if request should be signed, false if not.
     */
    private boolean shouldSign(Signer signer, AwsCredentials credentials) {
        return signer != null && (credentials != null || signer instanceof CanHandleNullCredentials);
    }

    /**
     * Always use the client level timeOffset.
     */
    private void adjustForClockSkew(ExecutionAttributes attributes) {
        attributes.putAttribute(AwsExecutionAttributes.TIME_OFFSET, dependencies.timeOffset());
    }

}
