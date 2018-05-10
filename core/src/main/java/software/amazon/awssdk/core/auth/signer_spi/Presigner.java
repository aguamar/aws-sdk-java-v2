package software.amazon.awssdk.core.auth.signer_spi;

import software.amazon.awssdk.http.SdkHttpFullRequest;

public interface Presigner {

    SdkHttpFullRequest presign(SdkHttpFullRequest request, SignerContext signerContext);
}