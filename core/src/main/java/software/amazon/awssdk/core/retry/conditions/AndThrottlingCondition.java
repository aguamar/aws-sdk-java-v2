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

package software.amazon.awssdk.core.retry.conditions;

import static software.amazon.awssdk.core.util.ValidationUtils.assertNotEmpty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Composite retry condition that evaluates to true if all contained retry conditions evaluate to true.
 */
@SdkPublicApi
public class AndThrottlingCondition implements ThrottlingCondition {

    private List<ThrottlingCondition> conditions = new ArrayList<>();

    public AndThrottlingCondition(ThrottlingCondition... conditions) {
        Collections.addAll(this.conditions, assertNotEmpty(conditions, "conditions"));
    }

    /**
     * @return True if any condition returns true. False otherwise.
     */
    @Override
    public boolean isThrottlingException(Exception context) {
        return conditions.stream().allMatch(retryCondition -> retryCondition.isThrottlingException(context));
    }
}
