package opensearchissue;

import com.google.common.collect.Iterables;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.transport.httpclient5.ResponseException;

import java.io.IOException;
import java.util.List;

/**
 * Bulk indexer that splits the bulk requests if the payload is too large and OpenSearch responds with a 429.
 */
public class BulkIndex {
    private final OpenSearchClient client;

    public BulkIndex(OpenSearchClient client) {
        this.client = client;
    }

    public void index(List<IndexData> documents, String indexName) throws IOException {
        final var bulkOps = documents.stream()
                .map(doc -> new BulkOperation.Builder().index(idx -> idx.document(doc)).build())
                .toList();

        var chunkSize = bulkOps.size();
        var chunks = Iterables.partition(bulkOps, chunkSize);

        while (true) {
            try {
                for (final var chunk : chunks) {
                    client.bulk(new BulkRequest.Builder().index(indexName).operations(chunk).build());
                }
                return;
            } catch (ResponseException e) {
                if (e.status() == 429) {
                    chunkSize /= 2;
                    if (chunkSize <= 1) {
                        throw e;
                    }
                    chunks = Iterables.partition(bulkOps, chunkSize);
                    continue;
                }
                throw e;
            }
        }
    }
}
