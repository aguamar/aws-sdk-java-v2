package software.amazon.awssdk.services.dynamodb;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import software.amazon.awssdk.core.auth.signers.SignerFactory;
import software.amazon.awssdk.core.auth.signers.SignerProvider;
import software.amazon.awssdk.core.auth.varunknSigners.AWS4Signer;
import software.amazon.awssdk.core.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.services.dynamodb.model.TableStatus;

class Parameters {
    public String access;
    public String secret;
    public String tableName = "PutSampleTestTable";
    public String region = "us-east-1";
    public Long writeCapacity = 15_000L;
    public Integer itemsCount = 30000;
    public Integer maxConnections = 200;
    public Long tableCreationPollingIntervalInMillis = 1000L;
    public Long readCapacity = 10L;
    public String attributeName = "id";
    public Duration acquisionTimeout = Duration.ofMinutes(3);

    public Duration socketTimeout = Duration.ofSeconds(50);
    public Duration connectionTimeout = Duration.ofSeconds(10);
    public Boolean useStrictHostNameVerification = true;
}

public class HighLoadSample {

    private static Random random = new Random(42);

    public static void main(String[] args) {
        Parameters parameters = new Parameters();
        //createTable(parameters);

        DynamoDBAsyncClient client = DynamoDBAsyncClient.builder()
                                                        .region(Region.of(parameters.region))
                                                        .build();


        while (true) {
            long start = System.currentTimeMillis();
            try {
                System.out.println("Iteration...");
                populate(client, createFixture(parameters));
//            } catch (Exception e) {
//                System.err.println(e.getCause());
//                System.exit(1);
            } finally {
                System.out.println((System.currentTimeMillis() - start) / 1000);
            }
        }
    }

    private static List<PutItemRequest> createFixture(Parameters parameters) {
        System.out.println("Creating fixture with count " + parameters.itemsCount);

        List<PutItemRequest> items = new ArrayList<>(parameters.itemsCount);
        for (int i = 0; i < parameters.itemsCount; i++) {
            String value = String.valueOf(random.nextLong());
            Map<String, AttributeValue> item = Collections.singletonMap(parameters.attributeName, AttributeValue.builder().s(value).build());
            items.add(PutItemRequest.builder().tableName(parameters.tableName).item(item).build());
        }
        return items;
    }

    private static void populate(DynamoDBAsyncClient client, List<PutItemRequest> fixture) {
        Stream<CompletableFuture<PutItemResponse>> stream = fixture.parallelStream().map(client::putItem);
        CompletableFuture<Void> futures = CompletableFuture.allOf(stream.toArray(CompletableFuture[]::new));
        futures.join();
    }
}