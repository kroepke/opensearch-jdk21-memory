package opensearchissue;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * OpenSearch container that uses the given OpenSearch and JDK version.
 */
public class OpenSearchContainer extends GenericContainer<OpenSearchContainer> {
    public static OpenSearchContainer create(String openSearchVersion, String jdkVersion, String heapSize) {
        final var container = new OpenSearchContainer(openSearchVersion, jdkVersion);
        container.addExposedPort(9200);
        container.waitingFor(new HttpWaitStrategy().forPath("/").forPort(9200));
        container.addEnv("OPENSEARCH_JAVA_OPTS", "-Xms%s -Xmx%s".formatted(heapSize, heapSize));
        return container;
    }

    private OpenSearchContainer(String openSearchVersion, String jdkVersion) {
        super(new ImageFromDockerfile("opensearch-jdk21-memory:%s-jdk%s".formatted(openSearchVersion, jdkVersion), false)
                .withDockerfileFromBuilder(builder -> builder.from("opensearchproject/opensearch:%s".formatted(openSearchVersion))
                        .user("root")
                        .run("dnf install -y java-%s-amazon-corretto".formatted(jdkVersion))
                        .user("opensearch")
                        .expose(9200)
                        .env("network.host", "0.0.0.0")
                        .env("discovery.type", "single-node")
                        .env("plugins.security.ssl.http.enabled", "false")
                        .env("plugins.security.disabled", "true")
                        .env("OPENSEARCH_INITIAL_ADMIN_PASSWORD", "un7chu3iesae4uala6taehieweiwub9zafaizu9laejeshi9Ke")
                        .env("JAVA_HOME", "/usr")));
    }
}
