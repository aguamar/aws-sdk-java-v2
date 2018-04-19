package software.amazon.awssdk.core.auth.varunknSigners;

import software.amazon.awssdk.http.SdkHttpFullRequest;

public interface Signer {

    SdkHttpFullRequest sign(SdkHttpFullRequest request, SignerContext signerContext);
}
