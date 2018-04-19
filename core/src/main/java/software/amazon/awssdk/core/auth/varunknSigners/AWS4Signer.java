package software.amazon.awssdk.core.auth.varunknSigners;

import static software.amazon.awssdk.core.util.DateUtils.numberOfDaysSinceEpoch;
import static software.amazon.awssdk.utils.StringUtils.lowerCase;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.auth.AwsCredentials;
import software.amazon.awssdk.core.auth.AwsSessionCredentials;
import software.amazon.awssdk.core.auth.SdkClock;
import software.amazon.awssdk.core.auth.internal.Aws4SignerRequestParamsNew;
import software.amazon.awssdk.core.auth.internal.Aws4SignerUtils;
import software.amazon.awssdk.core.auth.internal.SignerConstants;
import software.amazon.awssdk.core.auth.internal.SignerKey;
import software.amazon.awssdk.core.auth.signers.Presigner;
import software.amazon.awssdk.core.auth.signers.SigningAlgorithm;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.collections.FifoCache;
import software.amazon.awssdk.core.util.CredentialUtils;
import software.amazon.awssdk.core.util.StringUtils;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

public class AWS4Signer extends AbstractAwsSigner {

    private static final int SIGNER_CACHE_MAX_SIZE = 300;
    private static final FifoCache<SignerKey> SIGNER_CACHE = new FifoCache<>(SIGNER_CACHE_MAX_SIZE);
    private static final List<String> LIST_OF_HEADERS_TO_IGNORE_IN_LOWER_CASE = Arrays.asList("connection", "x-amzn-trace-id");

    protected String serviceName;

    protected String regionName;

    /**
     * Date override for testing only.
     */
    private Date overriddenDate;

    /**
     * Whether double url-encode the resource path when constructing the
     * canonical request. By default, we enable double url-encoding.
     *
     * TODO: Different sigv4 services seem to be inconsistent on this. So for
     * services that want to suppress this, they should use new
     * AWS4Signer(false).
     */
    private boolean doubleUrlEncode;

    private SdkClock clock;

    private AwsCredentials awsCredentials;

    @Override
    public SdkHttpFullRequest sign(SdkHttpFullRequest request, SignerContext signerContext){

        // anonymous credentials, don't sign
        if (CredentialUtils.isAnonymous(awsCredentials)) {
            return request;
        }

        return request.copy(b -> doSign(b, signerContext));
    }

    /**
     * This needed SdkHttpFullRequest.Builder, SdkRequest and timeoffset
     */
    private SdkHttpFullRequest.Builder doSign(SdkHttpFullRequest.Builder mutableRequest, SignerContext signerContext) {
        AwsCredentials sanitizedCredentials = sanitizeCredentials(awsCredentials);

        if (sanitizedCredentials instanceof AwsSessionCredentials) {
            addSessionCredentials(mutableRequest, (AwsSessionCredentials) sanitizedCredentials);
        }

        SdkRequest request = null;

        final Aws4SignerRequestParamsNew signerParams = new Aws4SignerRequestParamsNew(mutableRequest, signerContext,
            overriddenDate, regionName, serviceName, SignerConstants.AWS4_SIGNING_ALGORITHM);

        addHostHeader(mutableRequest);
        mutableRequest.header(SignerConstants.X_AMZ_DATE, signerParams.getFormattedSigningDateTime());

        String contentSha256 = calculateContentHash(signerParams, mutableRequest);
        mutableRequest.firstMatchingHeader(SignerConstants.X_AMZ_CONTENT_SHA256)
                      .filter(h -> h.equals("required"))
                      .ifPresent(h -> mutableRequest.header(SignerConstants.X_AMZ_CONTENT_SHA256, contentSha256));

        final String canonicalRequest = createCanonicalRequest(mutableRequest, contentSha256);

        final String stringToSign = createStringToSign(canonicalRequest, signerParams);

        final byte[] signingKey = deriveSigningKey(sanitizedCredentials, signerParams);

        final byte[] signature = computeSignature(stringToSign, signingKey);

        mutableRequest.header(SignerConstants.AUTHORIZATION,
                              buildAuthorizationHeader(signature, sanitizedCredentials, signerParams));

        processRequestPayload(mutableRequest, signature, signingKey, signerParams);
        return mutableRequest;
    }


    /**
     * Step 1 of the AWS Signature version 4 calculation. Refer to
     * http://docs.aws
     * .amazon.com/general/latest/gr/sigv4-create-canonical-request.html to
     * generate the canonical request.
     */
    private String createCanonicalRequest(SdkHttpFullRequest.Builder request,
                                          String contentSha256) {
        final String canonicalRequest = request.method().toString() +
                                        SignerConstants.LINE_SEPARATOR +
                                        // This would optionally double url-encode the resource path
                                        getCanonicalizedResourcePath(request.encodedPath(), doubleUrlEncode) +
                                        SignerConstants.LINE_SEPARATOR +
                                        getCanonicalizedQueryString(request.rawQueryParameters()) +
                                        SignerConstants.LINE_SEPARATOR +
                                        getCanonicalizedHeaderString(request.headers()) +
                                        SignerConstants.LINE_SEPARATOR +
                                        getSignedHeadersString(request.headers()) +
                                        SignerConstants.LINE_SEPARATOR +
                                        contentSha256;

        return canonicalRequest;
    }

    /**
     * Step 2 of the AWS Signature version 4 calculation. Refer to
     * http://docs.aws
     * .amazon.com/general/latest/gr/sigv4-create-string-to-sign.html.
     */
    private String createStringToSign(String canonicalRequest,
                                      Aws4SignerRequestParamsNew signerParams) {

        final String stringToSign = signerParams.getSigningAlgorithm() +
                                    SignerConstants.LINE_SEPARATOR +
                                    signerParams.getFormattedSigningDateTime() +
                                    SignerConstants.LINE_SEPARATOR +
                                    signerParams.getScope() +
                                    SignerConstants.LINE_SEPARATOR +
                                    BinaryUtils.toHex(hash(canonicalRequest));

        return stringToSign;
    }

    /**
     * Step 3 of the AWS Signature version 4 calculation. It involves deriving
     * the signing key and computing the signature. Refer to
     * http://docs.aws.amazon
     * .com/general/latest/gr/sigv4-calculate-signature.html
     */
    private byte[] deriveSigningKey(AwsCredentials credentials,
                                    Aws4SignerRequestParamsNew signerRequestParams) {

        final String cacheKey = computeSigningCacheKeyName(credentials,
                                                           signerRequestParams);
        final long daysSinceEpochSigningDate = numberOfDaysSinceEpoch(signerRequestParams.getSigningDateTimeMilli());

        SignerKey signerKey = SIGNER_CACHE.get(cacheKey);

        if (signerKey != null) {
            if (daysSinceEpochSigningDate == signerKey
                .getNumberOfDaysSinceEpoch()) {
                return signerKey.getSigningKey();
            }
        }

        byte[] signingKey = newSigningKey(credentials,
                                          signerRequestParams.getFormattedSigningDate(),
                                          signerRequestParams.getRegionName(),
                                          signerRequestParams.getServiceName());
        SIGNER_CACHE.add(cacheKey, new SignerKey(
            daysSinceEpochSigningDate, signingKey));
        return signingKey;
    }

    /**
     * Computes the name to be used to reference the signing key in the cache.
     */
    private String computeSigningCacheKeyName(AwsCredentials credentials,
                                              Aws4SignerRequestParamsNew signerRequestParams) {
        return credentials.secretAccessKey() + "-" + signerRequestParams.getRegionName() + "-" +
               signerRequestParams.getServiceName();
    }

    /**
     * Step 3 of the AWS Signature version 4 calculation. It involves deriving
     * the signing key and computing the signature. Refer to
     * http://docs.aws.amazon
     * .com/general/latest/gr/sigv4-calculate-signature.html
     */
    private byte[] computeSignature(String stringToSign, byte[] signingKey) {
        return sign(stringToSign.getBytes(Charset.forName("UTF-8")), signingKey,
                    SigningAlgorithm.HmacSHA256);
    }

    /**
     * Creates the authorization header to be included in the request.
     */
    private String buildAuthorizationHeader(byte[] signature, AwsCredentials credentials,
                                            Aws4SignerRequestParamsNew signerParams) {

        String signingCredentials = credentials.accessKeyId() + "/" + signerParams.getScope();
        String credential = "Credential=" + signingCredentials;
        String signerHeaders = "SignedHeaders=" +
                               getSignedHeadersString(signerParams.httpRequest().headers());
        String signatureHeader = "Signature=" + BinaryUtils.toHex(signature);

        return SignerConstants.AWS4_SIGNING_ALGORITHM + " " + credential + ", " + signerHeaders + ", " + signatureHeader;
    }

    /**
     * Includes all the signing headers as request parameters for pre-signing.
     */
    private void addPreSignInformationToRequest(SdkHttpFullRequest.Builder mutableRequest, AwsCredentials sanitizedCredentials,
                                                Aws4SignerRequestParamsNew signerParams, String timeStamp,
                                                long expirationInSeconds) {

        String signingCredentials = sanitizedCredentials.accessKeyId() + "/" + signerParams.getScope();

        mutableRequest.rawQueryParameter(SignerConstants.X_AMZ_ALGORITHM, SignerConstants.AWS4_SIGNING_ALGORITHM);
        mutableRequest.rawQueryParameter(SignerConstants.X_AMZ_DATE, timeStamp);
        mutableRequest.rawQueryParameter(SignerConstants.X_AMZ_SIGNED_HEADER,
                                         getSignedHeadersString(signerParams.httpRequest().headers()));
        mutableRequest.rawQueryParameter(SignerConstants.X_AMZ_EXPIRES,
                                         Long.toString(expirationInSeconds));
        mutableRequest.rawQueryParameter(SignerConstants.X_AMZ_CREDENTIAL, signingCredentials);
    }

    @Override
    protected void addSessionCredentials(SdkHttpFullRequest.Builder mutableRequest,
                                         AwsSessionCredentials credentials) {
        mutableRequest.header(SignerConstants.X_AMZ_SECURITY_TOKEN, credentials.sessionToken());
    }

    private String getCanonicalizedHeaderString(Map<String, List<String>> headers) {
        final List<String> sortedHeaders = new ArrayList<>(headers.keySet());
        sortedHeaders.sort(String.CASE_INSENSITIVE_ORDER);

        final Map<String, List<String>> requestHeaders = headers;
        StringBuilder buffer = new StringBuilder();
        for (String header : sortedHeaders) {
            if (shouldExcludeHeaderFromSigning(header)) {
                continue;
            }
            String key = lowerCase(header);

            for (String headerValue : requestHeaders.get(header)) {
                StringUtils.appendCompactedString(buffer, key);
                buffer.append(":");
                if (headerValue != null) {
                    StringUtils.appendCompactedString(buffer, headerValue);
                }
                buffer.append("\n");
            }
        }

        return buffer.toString();
    }

    private String getSignedHeadersString(Map<String, List<String>> headers) {
        final List<String> sortedHeaders = new ArrayList<>(headers.keySet());
        sortedHeaders.sort(String.CASE_INSENSITIVE_ORDER);

        StringBuilder buffer = new StringBuilder();
        for (String header : sortedHeaders) {
            if (shouldExcludeHeaderFromSigning(header)) {
                continue;
            }
            if (buffer.length() > 0) {
                buffer.append(";");
            }
            buffer.append(lowerCase(header));
        }

        return buffer.toString();
    }

    private boolean shouldExcludeHeaderFromSigning(String header) {
        return LIST_OF_HEADERS_TO_IGNORE_IN_LOWER_CASE.contains(lowerCase(header));
    }

    private void addHostHeader(SdkHttpFullRequest.Builder mutableRequest) {
        // AWS4 requires that we sign the Host header so we
        // have to have it in the request by the time we sign.

        final StringBuilder hostHeaderBuilder = new StringBuilder(mutableRequest.host());
        if (!SdkHttpUtils.isUsingStandardPort(mutableRequest.protocol(), mutableRequest.port())) {
            hostHeaderBuilder.append(":").append(mutableRequest.port());
        }

        mutableRequest.header(SignerConstants.HOST, hostHeaderBuilder.toString());
    }

    /**
     * Calculate the hash of the request's payload. Subclass could override this
     * method to provide different values for "x-amz-content-sha256" header or
     * do any other necessary set-ups on the request headers. (e.g. aws-chunked
     * uses a pre-defined header value, and needs to change some headers
     * relating to content-encoding and content-length.)
     */
    protected String calculateContentHash(Aws4SignerRequestParamsNew signerRequestParams,
                                          SdkHttpFullRequest.Builder requestBuilder) {
        SdkHttpFullRequest.Builder requestToSign = signerRequestParams.httpRequest();
        InputStream payloadStream = getBinaryRequestPayloadStream(requestToSign.content());
        payloadStream.mark(getReadLimit(signerRequestParams));
        String contentSha256 = BinaryUtils.toHex(hash(payloadStream));
        try {
            payloadStream.reset();
        } catch (IOException e) {
            throw new SdkClientException("Unable to reset stream after calculating AWS4 signature", e);
        }
        return contentSha256;
    }

    /**
     * Subclass could override this method to perform any additional procedure
     * on the request payload, with access to the result from signing the
     * header. (e.g. Signing the payload by chunk-encoding). The default
     * implementation doesn't need to do anything.
     */
    protected void processRequestPayload(SdkHttpFullRequest.Builder requestBuilder,
                                         byte[] signature, byte[] signingKey,
                                         Aws4SignerRequestParamsNew signerRequestParams) {
    }

    /**
     * Calculate the hash of the request's payload. In case of pre-sign, the
     * existing code would generate the hash of an empty byte array and returns
     * it. This method can be overridden by sub classes to provide different
     * values (e.g) For S3 pre-signing, the content hash calculation is
     * different from the general implementation.
     */
    protected String calculateContentHashPresign(Aws4SignerRequestParamsNew signerRequestParams,
                                                 SdkHttpFullRequest.Builder requestBuilder) {
        return calculateContentHash(signerRequestParams, requestBuilder);
    }

    /**
     * Generates an expiration date for the presigned url. If user has specified
     * an expiration date, check if it is in the given limit.
     */
    private long generateExpirationDate(Date expirationDate) {

        long expirationInSeconds = expirationDate != null ? ((expirationDate
                                                                  .getTime() - clock.currentTimeMillis()) / 1000L)
                                                          : SignerConstants.PRESIGN_URL_MAX_EXPIRATION_SECONDS;

        if (expirationInSeconds > SignerConstants.PRESIGN_URL_MAX_EXPIRATION_SECONDS) {
            throw new SdkClientException(
                "Requests that are pre-signed by SigV4 algorithm are valid for at most 7 days. "
                + "The expiration date set on the current request ["
                + Aws4SignerUtils.formatTimestamp(expirationDate
                                                      .getTime()) + "] has exceeded this limit.");
        }
        return expirationInSeconds;
    }

    /**
     * Generates a new signing key from the given parameters and returns it.
     */
    private byte[] newSigningKey(AwsCredentials credentials,
                                 String dateStamp, String regionName, String serviceName) {
        byte[] kSecret = ("AWS4" + credentials.secretAccessKey())
            .getBytes(Charset.forName("UTF-8"));
        byte[] kDate = sign(dateStamp, kSecret, SigningAlgorithm.HmacSHA256);
        byte[] kRegion = sign(regionName, kDate, SigningAlgorithm.HmacSHA256);
        byte[] kService = sign(serviceName, kRegion,
                               SigningAlgorithm.HmacSHA256);
        return sign(SignerConstants.AWS4_TERMINATOR, kService, SigningAlgorithm.HmacSHA256);
    }
}
