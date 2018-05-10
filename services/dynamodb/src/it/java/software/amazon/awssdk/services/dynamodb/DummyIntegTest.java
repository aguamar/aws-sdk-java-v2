package software.amazon.awssdk.services.dynamodb;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.core.auth.Aws4Signer;
import software.amazon.awssdk.core.auth.DefaultCredentialsProvider;
import software.amazon.awssdk.core.auth.signer_spi.SignerContext;
import software.amazon.awssdk.core.auth.signer_spi.SignerParams;
import software.amazon.awssdk.core.interceptor.AwsExecutionAttributes;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.http.apache.ApacheSdkHttpClientFactory;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.utils.IoUtils;
import sun.misc.IOUtils;

public class DummyIntegTest {

    @Test
    public void test() {
        DynamoDBClient client = DynamoDBClient.builder().region(Region.US_WEST_1).build();

        Map<String, AttributeValue> item =
            client.getItem(GetItemRequest.builder()
                                         .tableName("VoxTests1")
                                         .key(Collections.singletonMap("UID", AttributeValue.builder()
                                                                                            .s("varunkn")
                                                                                            .build()))
                                         .build())
                  .item();

        System.out.println(item);
    }

    @Test
    public void signWithoutUsingSdkClient() throws Exception {
        SdkHttpFullRequest httpFullRequest = SdkHttpFullRequest.builder()
                                                               .host("dynamodb.us-west-1.amazonaws.com")
                                                               .protocol("https")
                                                               .encodedPath("/")
                                                               .method(SdkHttpMethod.GET)
                                                               .header("Accept", "*/*")
                                                               .content(new ByteArrayInputStream(new byte[] {}))
                                                               .build();
        Aws4Signer signer = new Aws4Signer();
        SdkHttpFullRequest signedRequest = signer.sign(httpFullRequest, constructSignerParams());

        ApacheSdkHttpClientFactory httpClientFactory = ApacheSdkHttpClientFactory.builder().build();
        SdkHttpClient httpClient = httpClientFactory.createHttpClient();

        SdkRequestContext context = SdkRequestContext.builder().build();

        SdkHttpFullResponse response = httpClient.prepareRequest(signedRequest, context)
                                                 .call();


        String str = IoUtils.toString(response.content().get());
        System.out.println(str);
    }

    private SignerContext constructSignerParams() {
        SignerParams signerParams = new SignerParams();
        signerParams.setAwsCredentials(DefaultCredentialsProvider.create().getCredentials());
        signerParams.setSigningName("dynamodb");
        signerParams.setRegion(Region.US_WEST_1);

        SignerContext signerContext = new SignerContext();
        signerContext.putAttribute(AwsExecutionAttributes.SIGNER_PARAMS, signerParams);
        return signerContext;
    }
}