package software.amazon.awssdk.core.auth.varunknSigners;

import software.amazon.awssdk.http.SdkHttpFullRequest;

public interface Presigner {

    SdkHttpFullRequest sign(SdkHttpFullRequest request, PresignerContext presignerContext);
}
