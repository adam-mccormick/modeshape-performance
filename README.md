# MODESHAPE PERFORMANCE TEST

This is a quick and dirty test to show how the misconfiguration of the Infinispan cache can lead to lock
timeout exceptions under high load.

The fix is seen in `infinispan.xml` which sets the isolation level to `READ_COMMITTED` and adjust the
concurrency level to the expected number of threads (1000).

To see the error change the `cacheConfiguration` in `repo.json` to `infinispan.bad.xml` and the transaction
exceptions should occur. Compare that configuration with `infinispan.xml` to see the differences and run to
see a clean test.
