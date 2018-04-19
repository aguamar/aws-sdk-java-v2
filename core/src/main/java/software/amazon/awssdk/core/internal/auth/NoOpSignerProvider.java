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

package software.amazon.awssdk.core.internal.auth;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.auth.signers.NoOpSigner;
import software.amazon.awssdk.core.auth.signers.Signer;
import software.amazon.awssdk.core.auth.signers.SignerProvider;
import software.amazon.awssdk.core.auth.signers.SignerProviderContext;

@SdkInternalApi
public class NoOpSignerProvider extends SignerProvider {

    private Signer signer;

    public NoOpSignerProvider() {
        this.signer = new NoOpSigner();
    }

    @Override
    public Signer getSigner(SignerProviderContext context) {
        return signer;
    }

}
