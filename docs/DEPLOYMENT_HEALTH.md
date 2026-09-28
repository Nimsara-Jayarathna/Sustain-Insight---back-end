# Deployment health/startup policy

Spring Boot startup on the staging VPS can take around 90 seconds while repositories, PostgreSQL/Flyway, Hibernate, security and Tomcat initialize.

The staging container therefore uses a 120-second Docker health `start_period`, 10-second checks, and 12 retries. The deployment workflow waits up to five minutes and fails immediately only if the container actually exits. An early Docker `unhealthy` state is not treated as a deployment failure while the process is still running.

The scheduled news job also has a configurable startup delay (`NEWS_FETCHING_INITIAL_DELAY`, default 60000 ms) so heavy TF-IDF/AI work does not start immediately while the application is stabilizing.
