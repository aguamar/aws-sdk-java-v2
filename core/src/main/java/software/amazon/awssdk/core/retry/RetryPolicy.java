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

package software.amazon.awssdk.core.retry;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.conditions.AndRetryCondition;
import software.amazon.awssdk.core.retry.conditions.ClockSkewCondition;
import software.amazon.awssdk.core.retry.conditions.MaxNumberOfRetriesCondition;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.core.retry.conditions.ThrottlingCondition;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Interface for specifying a retry policy to use when evaluating whether or not a request should be retried. An implementation
 * of this interface can be provided to {@link ClientOverrideConfiguration#retryPolicy} or the {@link #builder()}} can be used
 * to construct a retry policy from SDK provided policies or policies that directly implement {@link BackoffStrategy} and/or
 * {@link RetryCondition}.
 *
 * When using the {@link #builder()} the SDK will use default values for fields that are not provided. The default number of
 * retries that will be used is {@link SdkDefaultRetrySettings#DEFAULT_MAX_RETRIES}. The default retry condition is
 * {@link RetryCondition#DEFAULT} and the default backoff strategy is {@link BackoffStrategy#defaultStrategy()}. The default
 * throttling condition is {@link ThrottlingCondition#DEFAULT} and the default clock skew condition is
 * {@link ClockSkewCondition#DEFAULT}.
 *
 * @see RetryCondition for a list of SDK provided retry condition strategies
 * @see BackoffStrategy for a list of SDK provided backoff strategies
 * @see ThrottlingCondition for a list of SDK provided throttling conditions
 * @see ClockSkewCondition for a list if SDK provided clock skew conditions
 */
@Immutable
@SdkPublicApi
public final class RetryPolicy implements ToCopyableBuilder<RetryPolicy.Builder, RetryPolicy> {

    public static final RetryPolicy DEFAULT = RetryPolicy.builder()
                                                         .backoffStrategy(BackoffStrategy.defaultStrategy())
                                                         .numRetries(SdkDefaultRetrySettings.DEFAULT_MAX_RETRIES)
                                                         .retryCondition(RetryCondition.DEFAULT)
                                                         .throttlingCondition(ThrottlingCondition.DEFAULT)
                                                         .clockSkewCondition(ClockSkewCondition.DEFAULT)
                                                         .build();

    public static final RetryPolicy NONE = RetryPolicy.builder()
                                                      .backoffStrategy(BackoffStrategy.none())
                                                      .retryCondition(RetryCondition.NONE)
                                                      .clockSkewCondition(ClockSkewCondition.NONE)
                                                      .throttlingCondition(ThrottlingCondition.NONE)
                                                      .build();

    private final RetryCondition retryConditionFromBuilder;
    private final RetryCondition retryCondition;
    private final BackoffStrategy backoffStrategy;
    private final Integer numRetries;
    private final ThrottlingCondition throttlingCondition;
    private final ClockSkewCondition clockSkewCondition;

    RetryPolicy(Builder builder) {
        this.backoffStrategy = builder.backoffStrategy;
        this.numRetries = builder.numRetries;
        this.retryConditionFromBuilder = builder.retryCondition;
        this.retryCondition = new AndRetryCondition(new MaxNumberOfRetriesCondition(numRetries), retryConditionFromBuilder);
        this.throttlingCondition = builder.throttlingCondition;
        this.clockSkewCondition = builder.clockSkewCondition;
    }

    public RetryCondition retryCondition() {
        return retryCondition;
    }

    public BackoffStrategy backoffStrategy() {
        return backoffStrategy;
    }

    public Integer numRetries() {
        return numRetries;
    }

    public ThrottlingCondition throttlingCondition() {
        return throttlingCondition;
    }

    public ClockSkewCondition clockSkewCondition() {
        return clockSkewCondition;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return builder().numRetries(numRetries).retryCondition(retryConditionFromBuilder).backoffStrategy(backoffStrategy);
    }

    /**
     * Builder for a {@link RetryPolicy}.
     */
    public static final class Builder implements CopyableBuilder<Builder, RetryPolicy> {

        private ThrottlingCondition throttlingCondition = ThrottlingCondition.DEFAULT;
        private ClockSkewCondition clockSkewCondition = ClockSkewCondition.DEFAULT;
        private Integer numRetries = SdkDefaultRetrySettings.DEFAULT_MAX_RETRIES;
        private BackoffStrategy backoffStrategy = BackoffStrategy.defaultStrategy();
        private RetryCondition retryCondition = RetryCondition.DEFAULT;

        public Builder numRetries(Integer numRetries) {
            this.numRetries = numRetries;
            return this;
        }

        public Integer numRetries() {
            return numRetries;
        }

        public Builder backoffStrategy(BackoffStrategy backoffStrategy) {
            this.backoffStrategy = backoffStrategy;
            return this;
        }

        public BackoffStrategy backoffStrategy() {
            return backoffStrategy;
        }

        public Builder retryCondition(RetryCondition retryCondition) {
            this.retryCondition = retryCondition;
            return this;
        }

        public RetryCondition retryCondition() {
            return retryCondition;
        }

        public Builder throttlingCondition(ThrottlingCondition throttlingCondition) {
            this.throttlingCondition = throttlingCondition;
            return this;
        }

        public ThrottlingCondition throttlingCondition() {
            return throttlingCondition;
        }

        public Builder clockSkewCondition(ClockSkewCondition clockSkewCondition) {
            this.clockSkewCondition = clockSkewCondition;
            return this;
        }

        public ClockSkewCondition clockSkewCondition() {
            return clockSkewCondition;
        }

        @Override
        public RetryPolicy build() {
            return new RetryPolicy(this);
        }
    }
}
