package opensearchissue;

import com.google.common.base.Strings;
import org.apache.hc.core5.http.HttpHost;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.TestClassOrder;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.HealthStatus;
import org.opensearch.client.opensearch.cluster.HealthRequest;
import org.opensearch.client.opensearch.core.CountRequest;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.opensearch.indices.RefreshRequest;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test that starts OpenSearch nodes with little heap to trigger 429 responses when trying to index large batches.
 * The {@link BulkIndex} class splits the bulk requests into smaller chunks for each 429 response until the request
 * goes through.
 */
@Testcontainers
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
class BulkIndexTest {
    public static final String INDEX_NAME = "bulk-test";

    @Nested
    @Order(1)
    @DisplayName("OpenSearch 2.11 with JDK 17")
    class OpenSearch211JDK17 extends OpenSearchBaseTest {
        @Container
        public static OpenSearchContainer container = OpenSearchContainer.create("2.11.1", "17", "256m");

        @Override
        protected int port() {
            return container.getMappedPort(9200);
        }
    }

    @Nested
    @Order(2)
    @DisplayName("OpenSearch 2.11 with JDK 21")
    class OpenSearch211JDK21 extends OpenSearchBaseTest {
        @Container
        public static OpenSearchContainer container = OpenSearchContainer.create("2.11.1", "21", "256m");

        @Override
        protected int port() {
            return container.getMappedPort(9200);
        }
    }

    @Nested
    @Order(3)
    @DisplayName("OpenSearch 2.12 with JDK 17")
    class OpenSearch212JDK17 extends OpenSearchBaseTest {
        @Container
        public static OpenSearchContainer container = OpenSearchContainer.create("2.12.0", "17", "256m");

        @Override
        protected int port() {
            return container.getMappedPort(9200);
        }
    }

    @Nested
    @Order(4)
    @DisplayName("OpenSearch 2.12 with JDK 21")
    class OpenSearch212JDK21 extends OpenSearchBaseTest {
        @Container
        public static OpenSearchContainer container = OpenSearchContainer.create("2.12.0", "21", "256m");

        @Override
        protected int port() {
            return container.getMappedPort(9200);
        }
    }

    static abstract class OpenSearchBaseTest {
        private OpenSearchClient client;
        private BulkIndex indexer;

        protected abstract int port();

        @BeforeEach
        void setUp() throws Exception {
            final var transport = ApacheHttpClient5TransportBuilder
                    .builder(new HttpHost("http", "localhost", port()))
                    .build();

            this.client = new OpenSearchClient(transport);
            this.indexer = new BulkIndex(client);

            deleteIndex();
            createIndex();
        }

        @AfterEach
        void tearDown() throws Exception {
            deleteIndex();
        }

        // The 10 MB batches should not trigger any errors because it will not trigger the memory circuit breaker
        @RepeatedTest(50)
        @DisplayName("10 MB batches")
        void runBulk10MB() throws Exception {
            runBulk(10);
        }

        // The 50 MB batches will trigger the circuit breaker and should trigger errors with JDK 21 but not with JDK 17
        @RepeatedTest(50)
        @DisplayName("50 MB batches")
        void runBulk50MB() throws Exception {
            runBulk(50);
        }

        private void runBulk(int num) throws Exception {
            indexer.index(createBatch(1024 * 1024, num), INDEX_NAME);

            client.indices().refresh(new RefreshRequest.Builder().index(INDEX_NAME).build());

            final var response = client.count(new CountRequest.Builder().index(INDEX_NAME).build());

            assertEquals(num, response.count());
        }

        private void deleteIndex() throws Exception {
            if (client.indices().exists(new ExistsRequest.Builder().index(INDEX_NAME).build()).value()) {
                client.indices().delete(new DeleteIndexRequest.Builder().index(INDEX_NAME).build());
            }
        }

        private void createIndex() throws Exception {
            client.indices().create(new CreateIndexRequest.Builder()
                    .settings(new IndexSettings.Builder()
                            .numberOfShards("1")
                            .numberOfReplicas("0")
                            .build())
                    .index(INDEX_NAME)
                    .build());
            client.cluster().health(new HealthRequest.Builder().waitForStatus(HealthStatus.Green).build());
        }

        private List<IndexData> createBatch(int size, int count) {
            final List<IndexData> list = new ArrayList<>();
            final String message = Strings.repeat("A", size);

            for (int i = 0; i < count; i++) {
                list.add(new IndexData(i + message));
            }

            return list;
        }
    }
}