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

package software.amazon.awssdk.awscore.retry;

import static software.amazon.awssdk.awscore.retry.AwsDefaultRetrySettings.CLOCK_SKEW_ERROR_CODES;
import static software.amazon.awssdk.awscore.retry.AwsDefaultRetrySettings.THROTTLING_ERROR_CODES;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.conditions.ClockSkewCondition;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.core.retry.conditions.ThrottlingCondition;

/**
 * Retry Policy used by clients when communicating with AWS services.
 */
@Immutable
@SdkPublicApi
public final class AwsRetryPolicy {

    public static final ThrottlingCondition AWS_DEFAULT_THROTTLING_CONDITION =
        ThrottlingCondition.DEFAULT.or(e -> e instanceof SdkServiceException && THROTTLING_ERROR_CODES.contains(
            ((SdkServiceException) e).errorCode()));

    public static final ClockSkewCondition AWS_DEFAULT_CLOCK_SKEW_CONDITION =
        e -> e instanceof SdkServiceException && CLOCK_SKEW_ERROR_CODES.contains(
            ((SdkServiceException) e).errorCode());

    public static final RetryCondition AWS_DEFAULT_RETRY_CONDITION =
        RetryCondition.DEFAULT.or(new RetryOnErrorCodeCondition(AwsDefaultRetrySettings.RETRYABLE_ERROR_CODES))
                              .or(c -> AWS_DEFAULT_THROTTLING_CONDITION.isThrottlingException(c.exception()))
                              .or(c -> AWS_DEFAULT_CLOCK_SKEW_CONDITION.isClockSkewError(c.exception()));

    public static final RetryPolicy DEFAULT =
        RetryPolicy.DEFAULT.toBuilder()
                           .retryCondition(AWS_DEFAULT_RETRY_CONDITION)
                           .throttlingCondition(AWS_DEFAULT_THROTTLING_CONDITION)
                           .clockSkewCondition(AWS_DEFAULT_CLOCK_SKEW_CONDITION)
                           .build();

    private AwsRetryPolicy() {
    }
}
