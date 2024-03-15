# What is this

Opensearch 2.12.0 has shown higher memory usage then 2.11.0 in our integration tests, leading to circuit breakers tripping where they've previously been fine.

Testing leads us to believe that the JDK 21 upgrade has something to do with it, because running 2.12.0 on JDK 17 does not show the same behavior.

This repository contains a smaller test case, mimicking what our integration test does: bulk indexing of large messages. The nature of the messages aren't important, but trigger the problem more easily.

# How to run

Use `docker-compose up` in this directory.
That will create two services, `jdk17` and `jdk21`.
The former is based on the official image and simply adds Corretto 17 to run OpenSearch 2.12.0 on the previous JDK. Other JDK 17 distributions work identically.

Two ports are mapped, `9201` and `9202`.

Then run `send-data.sh`, which produces a bulk index file, and runs the same bulk operation multiple times against each OpenSearch server.
It only shows output when it finds that a circuit breaker has triggered.
