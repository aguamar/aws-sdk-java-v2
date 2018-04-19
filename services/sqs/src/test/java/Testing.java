import org.junit.Test;
import software.amazon.awssdk.services.sqs.SQSClient;

public class Testing {

    @Test
    public void foo() {
        SQSClient client = SQSClient.create();

        client.listQueues();
    }
}
