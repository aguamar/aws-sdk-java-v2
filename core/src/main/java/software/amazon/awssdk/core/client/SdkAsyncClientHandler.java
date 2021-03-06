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

package software.amazon.awssdk.core.client;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.ServiceAdvancedConfiguration;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.config.SdkAsyncClientConfiguration;

/**
 * Client handler for SDK clients.
 */
@ThreadSafe
@Immutable
public class SdkAsyncClientHandler extends BaseClientHandler implements AsyncClientHandler {

    private final AsyncClientHandler delegateHandler;

    public SdkAsyncClientHandler(SdkAsyncClientConfiguration asyncClientConfiguration,
                                 ServiceAdvancedConfiguration serviceAdvancedConfiguration) {
        super(asyncClientConfiguration, serviceAdvancedConfiguration);
        this.delegateHandler = new SdkAsyncClientHandlerImpl(asyncClientConfiguration, serviceAdvancedConfiguration);
    }

    @Override
    public <InputT extends SdkRequest, OutputT extends SdkResponse> CompletableFuture<OutputT> execute(
            ClientExecutionParams<InputT, OutputT> executionParams) {
        return delegateHandler.execute(addErrorResponseHandler(executionParams));
    }

    @Override
    public <InputT extends SdkRequest, OutputT extends SdkResponse, ReturnT> CompletableFuture<ReturnT> execute(
            ClientExecutionParams<InputT, OutputT> executionParams,
            AsyncResponseTransformer<OutputT, ReturnT> asyncResponseTransformer) {
        return delegateHandler.execute(addErrorResponseHandler(executionParams), asyncResponseTransformer);
    }

    @Override
    public void close() {
        delegateHandler.close();
    }
}
