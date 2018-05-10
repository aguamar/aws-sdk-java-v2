package software.amazon.awssdk.core.auth.signer_spi;

import software.amazon.awssdk.core.auth.AwsCredentials;
import software.amazon.awssdk.core.regions.Region;

/**
 * Parameters that are used during signing.
 *
 * Required parameters vary based on signer implementations. Signer implementations might only use a
 * subset of params in this class.
 */
public class SignerParams {

    private AwsCredentials awsCredentials;

    private String signingName;

    private Region region;

    private Integer timeOffset;

    public AwsCredentials getAwsCredentials() {
        return awsCredentials;
    }

    public void setAwsCredentials(AwsCredentials awsCredentials) {
        this.awsCredentials = awsCredentials;
    }

    public String getSigningName() {
        return signingName;
    }

    public void setSigningName(String signingName) {
        this.signingName = signingName;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public Integer getTimeOffset() {
        return timeOffset;
    }

    public void setTimeOffset(Integer timeOffset) {
        this.timeOffset = timeOffset;
    }
}
