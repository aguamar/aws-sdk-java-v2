package software.amazon.awssdk.core.auth.signer_spi;

import software.amazon.awssdk.http.SdkHttpFullRequest;

public interface Signer {

    SdkHttpFullRequest sign(SdkHttpFullRequest request, SignerContext signerContext);
}
