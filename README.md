# Selenium + TestNG starter framework

A Java 17, Maven, Selenium, and TestNG starter project using the Page Object Model.
The sample test opens `https://example.com` and checks its page heading.

## Prerequisites

- JDK 17 or newer
- Maven 3.8 or newer
- Chrome or Firefox installed for the browser you want to run

Selenium Manager resolves the matching browser driver automatically. The first run
may need network access to download it.

## Run locally

From the project root:

```powershell
mvn clean test
```

The default suite is the smoke suite. To select suites locally:

```powershell
mvn -Dheadless=true -Dtestng.suite=src/test/resources/testng-smoke.xml clean test
mvn -Dheadless=true -Dtestng.suite=src/test/resources/testng-regression.xml clean test
```

The regression suite selects TestNG methods annotated with `@Test(groups =
"regression")` in the automation test package. Add that group to each test you
want included in regression. The sample project currently has a smoke test but
no regression tests yet.

The default browser is Chrome and it runs headed. To run headless or choose
another supported browser:

```powershell
mvn clean test -Dheadless=true
mvn clean test -Dbrowser=firefox -Dheadless=true
```

Configuration precedence is Java system property, environment variable, then
`src/test/resources/config.properties`.

| Setting | Environment variable | Default |
| --- | --- | --- |
| `browser` | `BROWSER` | `chrome` |
| `headless` | `HEADLESS` | `false` |
| `base.url` | `BASE_URL` | `https://example.com` |
| `explicit.wait.seconds` | `EXPLICIT_WAIT_SECONDS` | `10` |

For example, set `BASE_URL` to the application under test and use
`-Dheadless=true` in CI.

## SecuPI Oracle policy tests

The `secupi` Maven profile runs a separate TestNG database suite. Surefire starts
a forked test JVM with the SecuPI `-javaagent` and the requested SecuPI system
properties; the JAR itself is not added to the project or committed. Tests in
the normal suite run in a separate Maven invocation without the agent. In
Jenkins, select `all` to run both suites in separate JVMs.

1. Put the downloaded SecuPI agent JAR somewhere accessible to the test process.
2. Configure a read-only `SELECT` in `src/test/resources/secupi-test.properties`
   (or supply an external properties file with `-Dsecupi.config.file=...`).
   The query should select all columns configured for the role checks.
3. Set `SECUPI_DATABASE_REGULAR_USERNAME`, `SECUPI_DATABASE_REGULAR_PASSWORD`,
   `SECUPI_DATABASE_PRIVILEGED_USERNAME`, and
   `SECUPI_DATABASE_PRIVILEGED_PASSWORD` in the environment. Keep passwords
   out of the properties file and command-line arguments.
4. Run with the agent path and profile:

   ```powershell
   mvn -Psecupi clean test "-Dsecupi.agent.jar=C:\path\to\secupi-agent.jar"
   ```

The sample properties file shows the JDBC URL, query, one mask regular
expression, and column classifications for each role; it contains no expected
cell values. Replace its example table and column names with your test schema.
For each role, put column names in `secupi.expected.<role>.clear.columns` or
`secupi.expected.<role>.masked.columns`. Set
`secupi.masked.value.regex` to the regular expression that matches a fully
masked cell. The test scans every row: clear columns must not match this
expression, and masked columns must match it. Every query column must be
classified exactly once, and the test reports role, row, and column on a
mismatch without logging cell contents. Use `ORDER BY` if you want repeatable
row numbering in test failure messages.

The database suite checks connection/query results using the agent-loaded policy;
it does not automate policy creation or downloading the JAR from the SecuPI UI.
The database URL, policy, agent JAR, test schema, and role credentials must
already be provisioned.

For Jenkins, select `smoke` or `regression` to run that suite without the SecuPI
agent, `secupi` to run only the agent-enabled suite, or `all` to run smoke,
regression, then SecuPI in separate Maven/test JVMs. Add regression-group tests
before selecting the regression suite. Configure these
Jenkins credentials: file credentials `secupi-agent-jar` and
`secupi-test-config`, plus username/password credentials `oracle-regular-user`
and `oracle-privileged-user`. The test config credential should contain the
JDBC URL, query, mask regex, and column classifications, but no passwords.
The Jenkins agent needs JDK 17+, Maven 3.8+, network access for driver
resolution if needed, and Oracle database access.

Add smoke tests to `src/test/resources/testng-smoke.xml` and regression tests
to the `regression` TestNG group discovered by
`src/test/resources/testng-regression.xml`. Add agent-dependent database tests
to `src/test/resources/testng-secupi.xml`; the Java agent is enabled for the
entire forked JVM running that suite, not selectively for individual methods.

## Jenkins

Create a Pipeline job pointing at this repository and configure its agent with
JDK 17+, Maven 3.8+, and the selected browser installed and available on `PATH`.
The included `Jenkinsfile` checks out the repository, runs the selected test
suite headlessly, and publishes Surefire XML reports through Jenkins' JUnit
plugin. Selenium Manager downloads the browser driver when needed, so the agent
needs network access on its first run (or a pre-provisioned driver).

## Project layout

```text
.
|-- Jenkinsfile
|-- pom.xml
|-- src
|   `-- test
|       |-- java
|       |   `-- com/example/automation
|       |       |-- config       # Browser and SecuPI test configuration
|       |       |-- driver       # WebDriver creation and lifecycle access
|       |       |-- pages        # Page objects and shared page actions
|       |       `-- tests        # UI and Oracle policy TestNG tests
|       `-- resources
|           |-- config.properties
|           |-- secupi-test.properties
|           |-- testng-regression.xml
|           |-- testng-secupi.xml
|           |-- testng-smoke.xml
|           `-- testng.xml             # Default suite (smoke)
`-- .gitignore
```

Add new test classes under `src/test/java/com/example/automation/tests` and
corresponding page objects under `src/test/java/com/example/automation/pages`.
