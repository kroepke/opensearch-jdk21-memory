Opensearch 2.12.0 has shown higher memory usage then 2.11.0 in our integration tests, leading to circuit breakers tripping where they've previously been fine.

Testing leads us to believe that the JDK 21 upgrade has something to do with it, because running 2.12.0 on JDK 17 does not show the same behavior.

This repository contains a smaller test case, mimicking what our integration test does: bulk indexing of large messages. The nature of the messages aren't important, but trigger the problem more easily.
