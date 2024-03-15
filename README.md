# What is this

Opensearch 2.12.0 has shown higher memory usage then 2.11.0 in our integration tests, leading to circuit breakers tripping where they've previously been fine.

Testing leads us to believe that the JDK 21 upgrade has something to do with it, because running 2.12.0 on JDK 17 does not show the same behavior.

This repository contains a smaller test case, mimicking what our integration test does: bulk indexing of large messages. The nature of the messages aren't important, but trigger the problem more easily.

# How to run

Use `docker-compose up` in this directory.
That will create four services, `jdk17` and `jdk21` versions of 2.12.0 and 2.11.1.
For 2.11, we manually installed a Corretto 21, and for 2.12, we did the same for Corretto 17 in the custom Docker images.
Each service has its 9200 port mapped between `9201` and `9204`.

Then run `send-data.sh`, which produces a bulk index file and runs the same bulk operation multiple times against each OpenSearch server.
It only shows output when it finds that a circuit breaker has triggered.
You will see that the JDK 17-based OpenSearch nodes do not run into memory issues, whereas the JDK 21 ones do so regularly.

The test is non-deterministic because it depends on GC activity and timing, but the trend is very clear. 
