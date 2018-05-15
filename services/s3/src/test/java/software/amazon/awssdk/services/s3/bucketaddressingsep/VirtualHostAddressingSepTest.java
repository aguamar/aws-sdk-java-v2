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

package software.amazon.awssdk.services.s3.bucketaddressingsep;

import static org.assertj.core.api.Assertions.assertThat;
import static utils.S3MockUtils.mockListObjectsResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.builder.ClientHttpConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AdvancedConfiguration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.testutils.service.http.MockHttpClient;

public class VirtualHostAddressingSepTest {

    private static final String TEST_FILE_PATH = "test.json";

    private static  List<HashMap> testCaseList;

    private MockHttpClient mockHttpClient;

    @BeforeClass
    public static void setupClass() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        InputStream jsonData = VirtualHostAddressingSepTest.class.getResourceAsStream(TEST_FILE_PATH);
        testCaseList = objectMapper.readValue(jsonData,
                                              typeFactory.constructCollectionType(List.class, HashMap.class));
    }

    @Before
    public void setup() {
        mockHttpClient = new MockHttpClient();
    }


    @Test
    public void test() throws UnsupportedEncodingException {
        for (Map testCase : testCaseList) {
            assertTestCase(testCase);
        }
    }

    private void assertTestCase(Map params) throws UnsupportedEncodingException {
        final String bucket = (String) params.get("Bucket");
        final String expectedUri = (String) params.get("ExpectedUri");

        S3Client s3Client = constructClient(params);

        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        s3Client.listObjects(ListObjectsRequest.builder().bucket(bucket).build());

        assertThat(mockHttpClient.getLastRequest().getUri())
            .isEqualTo(URI.create(expectedUri));
    }

    private S3Client constructClient(Map params) {
        final String region = (String) params.get("Region");
        final String addressingStyle = (String) params.get("ConfiguredAddressingStyle");
        final boolean useDualstack = (boolean) params.get("UseDualstack");
        final boolean useS3Accelerate = (boolean) params.get("UseS3Accelerate");

        ClientHttpConfiguration httpConfiguration = ClientHttpConfiguration.builder()
                                                                           .httpClient(mockHttpClient)
                                                                           .build();
        return S3Client.builder()
                       .credentialsProvider(StaticCredentialsProvider.create(AwsCredentials.create("akid", "skid")))
                       .httpConfiguration(httpConfiguration)
                       .region(Region.of(region))
                       .advancedConfiguration(S3AdvancedConfiguration.builder()
                                                                     .pathStyleAccessEnabled(isPathStyle(addressingStyle))
                                                                     .accelerateModeEnabled(useS3Accelerate)
                                                                     .dualstackEnabled(useDualstack)
                                                                     .build())
                       .build();
    }

    private Boolean isPathStyle(String addressingStyle) {
        return addressingStyle.equals("path");
    }
}

