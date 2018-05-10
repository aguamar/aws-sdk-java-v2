package software.amazon.awssdk.core.auth.signer_spi;

import java.util.Date;

public class PresignerParams extends SignerParams {

    private Date expirationDate;

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }
}
