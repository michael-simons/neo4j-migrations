# 1.9.2

## 🐛 Bug Fixes
- a99b33f Add missing type `property` to allowed index types.

## 🧹 Housekeeping
- 127996f Bump maven-javadoc-plugin from 3.4.0 to 3.4.1 (#600)
- 75191a9 Bump neo4j-harness from 4.4.9 to 4.4.10 (#601)
- 538ccd1 Bump maven-project-info-reports-plugin from 3.4.0 to 3.4.1 (#602)
- 8c6ad22 Bump mockito.version from 4.6.1 to 4.7.0 (#603)
- 123acdd Bump maven-site-plugin from 3.12.0 to 3.12.1 (#599)
- 2c91338 Bump byte-buddy.version from 1.12.12 to 1.12.13 (#598)
- 7ec3a03 Bump objenesis from 3.2 to 3.3 (#597)
- 2c5ebeb Bump quarkus.version from 2.11.1.Final to 2.11.2.Final (#596)

## 🛠 Build
- d736df7 Use `quarkus-extension-maven-plugin` instead of `quarkus-bootstrap-maven-plugin`.


## Contributors
We'd like to thank the following people for their contributions:
- @michael-simons


# 1.9.1

## 🐛 Bug Fixes
- 766333a Render indexes while rendering an XML catalog, too. (#595)

## 🧹 Housekeeping
- c49486c Bump docker-maven-plugin from 0.40.1 to 0.40.2 (#594)
- d33e902 Bump junit-jupiter-causal-cluster-testcontainer-extension (#593)
- ea1619d Bump asciidoctorj from 2.5.4 to 2.5.5 (#592)
- 27f4f44 Bump junit-bom from 5.8.2 to 5.9.0 (#591)
- 4141884 Bump maven-resources-plugin from 3.2.0 to 3.3.0 (#590)
- de709fe Bump checkstyle from 10.3.1 to 10.3.2 (#589)


## Contributors
We'd like to thank the following people for their contributions:
- @michael-simons


# 1.9.0

## 🚀 Features
- 3cb4c25 Add experimental Markdown extension. (#585)
- 1ab051c Add `:USE` command. (#583)
- a83b60b Support well-known Neo4j environment variables. (#587)

## 🐛 Bug Fixes
- 7808fa5 Handle additional Neo4j server agent strings. (#588)
- 3564781 Adjust layout to commonly used one. (#582)
- f50b365 Fix encoding issue in dev ui.
- a5d7d92 Fix spelling. (#567)

## 🔄️ Refactorings
- 462cfba Avoid an unlock attempt if the lock isn’t locked. (#581)
- 9c26342 Deprecate `ac.simons.neo4j.migrations.core.Migration#getDescription`. (#584)

## 🧹 Housekeeping
- b68caee Bump quarkus-neo4j.version from 1.3.2 to 1.4.0
- 78a06f4 Bump quarkus.version from 2.11.0.Final to 2.11.1.Final
- d3dba4a Bump maven-project-info-reports-plugin from 3.3.0 to 3.4.0 (#568)
- b2f5b8a Bump maven-install-plugin from 3.0.0-M1 to 3.0.1 (#579)
- 0a7fbfa Bump spring-boot.version from 2.7.1 to 2.7.2 (#578)
- 1d6434f Bump maven-deploy-plugin from 3.0.0-M2 to 3.0.0 (#577)
- 330be58 Bump maven-assembly-plugin from 3.4.1 to 3.4.2 (#575)
- e76fb8c Bump neo4j-harness from 4.4.8 to 4.4.9 (#580)
- f404a07 Bump quarkus.version from 2.10.2.Final to 2.11.0.Final (#574)
- 129cebf Bump exec-maven-plugin from 3.0.0 to 3.1.0 (#570)
- 8c65341 Bump native-maven-plugin from 0.9.12 to 0.9.13 (#571)
- 9995d00 Bump sortpom-maven-plugin from 3.1.3 to 3.2.0 (#569)


## Contributors
We'd like to thank the following people for their contributions:
- @ali-ince
- @meistermeier
- @michael-simons
- @SeanKilleen


# 1.8.3

## 🚀 Features
- d4c1061 Allow adding preconditions to migrations after they have been applied. (#565)

## 🐛 Bug Fixes
- 207b14c Add a check for Neo4j 5 constraint backing indexes. (#564)

## 🔄️ Refactorings
- 01f2d3b Warn only on empty locations. (#555)

## 🧹 Housekeeping
- bc1b61c Bump quarkus-neo4j.version from 1.3.1 to 1.3.2
- 8349c44 Bump neo4j-java-driver from 4.4.6 to 4.4.9

## 🛠 Build
- 6cc739b Don't drop LOOKUP constraints prior to tests. (#562)


## Contributors
We'd like to thank the following people for their contributions:
- @ali-ince
- @injectives
- @meistermeier
- @michael-simons


# 1.8.2

## 🐛 Bug Fixes
- a0bf11c Strip leading product specific information from version string. (#550)

## 🧹 Housekeeping
- 9d768f8 Bump quarkus-neo4j.version from 1.3.0 to 1.3.1
- 3edeca0 Bump quarkus.version from 2.10.1.Final to 2.10.2.Final (#552)
- b698497 Bump classgraph from 4.8.147 to 4.8.149 (#553)
- 9048202 Bump maven-assembly-plugin from 3.4.0 to 3.4.1 (#554)


## Contributors
We'd like to thank the following people for their contributions:
- @michael-simons
- @Hosch250


# 1.8.1

## 🔄️ Refactorings
- c577565 Avoid using `ServerVersion` in favor of a plain string. (#540)

## 🧹 Housekeeping
- d795d6c Bump testcontainers.version from 1.17.2 to 1.17.3 (#547)
- 61811af Bump jreleaser-maven-plugin from 1.0.0 to 1.1.0 (#543)
- 2b10f8e Bump quarkus.version from 2.10.0.Final to 2.10.1.Final (#545)
- 20c41fd Bump jna from 5.12.0 to 5.12.1 (#548)
- 06f4e78 Bump checkstyle from 10.3 to 10.3.1 (#544)
- 4db301f Bump maven-assembly-plugin from 3.3.0 to 3.4.0 (#546)

## 🛠 Build
- 847c26f Add support for integration tests running on Apple silicon. (#538)
- 3da7550 Use a system property to configure a central Neo4j image to be used as default in integration tests. (#542)
- 3e25207 Completely exclude asciidoc extension from site plugin.
- 9d58c90 Install a Ruby version compatible with JRuby and Maven.
- d49d744 Add mavengem-wagon as extension to avoid site generation errors. (#541)


## Contributors
We'd like to thank the following people for their contributions:
- @michael-simons
- @meistermeier
- @bsideup 


# 1.8.0

## 🚀 Features
- b7883da Add asciidoctor to supported migration formats as external extension.

## 🔄️ Refactorings
- b2981bb Refine extension API.

## 📝 Documentation
- e7d551f Add customer feedback.
- 89b4ece Replace  with the correct version (1.7.0).

## 🧹 Housekeeping
- 7b26a3a Bump byte-buddy.version from 1.12.10 to 1.12.12 (#533)
- 49b509d Bump spring-boot.version from 2.7.0 to 2.7.1 (#532)
- d1a077e Bump quarkus-neo4j.version from 1.2.0 to 1.3.0 (#534)
- 2b9cfe8 Bump build-helper-maven-plugin from 1.12 to 3.3.0 (#535)
- 91b5b9c Bump jna from 5.11.0 to 5.12.0 (#536)
- a010e25 Bump native-maven-plugin from 0.9.11 to 0.9.12 (#537)
- 440eb1d Bump quarkus.version from 2.9.2.Final to 2.10.0.Final (#528)
- 2f86e65 Bump neo4j-harness from 4.4.7 to 4.4.8 (#529)
- c7b8338 Bump maven.version from 3.8.5 to 3.8.6 (#525)
- 2150dc1 Bump docker-maven-plugin from 0.40.0 to 0.40.1 (#526)

## 🛠 Build
- 902b782 Add a CODEOWNERS declaration.
- 0d55287 Optionally use a local database for Windows tests. (#523)


## Contributors
We'd like to thank the following people for their contributions:
- @michael-simons


# 1.7.1

## 🐛 Bug Fixes
- aa86a67 Use proper URIs for filesystem based locations. (#522)
- bea46c5 Check if dbms.procedures is available or not. (#520)
- dc06016 Missing export of catalog package.

## 📝 Documentation
- a192ada Update local changelog.

## 🧹 Housekeeping
- 7cff26f Bump assertj-core from 3.22.0 to 3.23.1 (#519)
- a127f05 Bump neo4j-java-driver from 4.4.5 to 4.4.6 (#518)
- a950fe8 Bump mockito.version from 4.6.0 to 4.6.1 (#517)
- ee390ae Bump asciidoctorj from 2.5.3 to 2.5.4 (#515)


## Contributors
We'd like to thank the following people for their contributions:
- @ali-ince
- @Dcanzano
- @michael-simons


# 1.7.0

It's my pleasure to introduce an exciting new feature with this release: The possibility to manage your constraints and indexes for Neo4j in a version agnostic way!

While we took much our inspiration from [Flyway](https://flywaydb.org) in terms of "just use the databases query language", we needed to opt for something different in this case and opted for XML. Much like [Liquibase](https://www.liquibase.org) did in their beginning:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<migration xmlns="https://michael-simons.github.io/neo4j-migrations">
    <catalog>
        <constraints>
            <constraint name="unique_isbn" type="unique">
                <label>Book</label>
                <properties>
                    <property>isbn</property>
                </properties>
            </constraint>
        </constraints>
    </catalog>
    
    <verify />
    <create item="unique_isbn"/>
    <create>
            <index name="node_index_name">
                <label>Person</label>
                <properties>
                    <property>surname</property>
                </properties>
            </index>
        </create>
</migration>
```

The structure allows for creating global catalogs that can be build in an iterative way per each migration but also for just doing local creates and drops. All create and drop operations comes per default in an idempotent fashion ("create if not exists" / "drop if exists") regardless of Neo4j version. We make sure to translate the structure above into the Cypher matching your version of Neo4j.

Read more about this in the manual, especially the [concepts](https://michael-simons.github.io/neo4j-migrations/current/#concepts_catalog) and have look at this [scenario](https://michael-simons.github.io/neo4j-migrations/current/#usage_defining_asserting_applying_catalogs). There's also a short paragraph why XML is still a good idea in 2022.

This work lays the groundwork of an updated auto-index-manager in [Neo4j-OGM](https://github.com/neo4j/neo4j-ogm) and - for the first time - a similar solution in [SDN6](https://github.com/spring-projects/spring-data-neo4j): We will provide annotation processors for both projects later this year creating catalog based migrations.
With that, SDN5+OGM and SDN6 and later users can have both: Constraints and indexes defined by their class model but executed by their process and not by a random start of a random application.

This work would not have been possible without the help of @meistermeier. Thank you.

While we aim for obeying Semver, we just bumped the minor version, even for such a big feature. We want to spare the 2.0 release for the JDK 17 update, also coming later this year. We are able to do so because this change here does not affect you if you don't use it, neither through any of the integrations nor through the API.

Other notable changes in this release are better logging in the CLI (general warnings are now going through stderr for better filtering / pipelining) and also we do now follow symlinks when traversing file based locations.

## 🚀 Features
- 0dd23a4 Allow definition and maintaining of constraints and indexes. (#502)

## 🔄️ Refactorings
- 23f3449 Use https for xml namespace.
- 711af06 Follow symlinks while scanning for migrations. (#513)
- 547cffa Separate migrations and CLI logging into syserr and sysout. (#512)

## 🧹 Housekeeping
- 111cab4 Bump checkstyle from 10.2 to 10.3 (#510)
- 859d415 Bump docker-maven-plugin from 0.39.1 to 0.40.0 (#509)
- 783f35e Bump sortpom-maven-plugin from 3.1.0 to 3.1.3 (#508)
- fbdfd6c Bump asciidoctorj-diagram from 2.2.1 to 2.2.3 (#507)
- 6e3ce70 Bump mockito.version from 4.5.1 to 4.6.0 (#506)
- 5468022 Bump classgraph from 4.8.146 to 4.8.147 (#505)
- 24bd937 Bump maven-invoker-plugin from 3.2.2 to 3.3.0 (#504)
- 04912c9 Bump quarkus.version from 2.9.1.Final to 2.9.2.Final (#503)
- 72dc4f9 Bump testcontainers.version from 1.17.1 to 1.17.2 (#496)
- a764a77 Bump sortpom-maven-plugin from 3.0.1 to 3.1.0 (#498)
- 0f5f4b3 Bump plexus-utils from 3.4.1 to 3.4.2 (#501)
- 24de271 Bump neo4j-harness from 4.4.6 to 4.4.7 (#500)
- 9ee1123 Bump spring-boot.version from 2.6.7 to 2.7.0 (#497)

## 🛠 Build
- 4ee8e03 Use license-maven-plugin to enforce licenses instead of checkstyle. (#514)
- e5360a7 Upgrade to GraalVM 21.3.2. (#511)

## Contributors
We'd like to thank the following people for their contributions:
- @michael-simons
- @meistermeier


# 1.6.0

*No breaking changes*, the minor version has been bumped to reflect the Quarkus version bump from 2.8.2 to 2.9.1.

## 🧹 Housekeeping
- f0ebfe4 Bump quarkus.version from 2.8.2.Final to 2.9.1.Final (#489)

## Contributors
We'd like to thank the following people for their contributions:
- @michael-simons


# 1.5.6

## 🐛 Bug Fixes
- a04da71 Downgrade GraalVM to 21.3.1. (#492)

## 📝 Documentation
- a3752ec Update local changelog.

## Contributors
We'd like to thank the following people for their contributions:
- @michael-simons


# 1.5.5

## 🐛 Bug Fixes
- 49815d1 Add a test for the GraalVM 22.1 fix. (#488)

## 🔄️ Refactorings
- 4b2d882 Remove superflous field.

## 🧹 Housekeeping
- 3bfc860 Bump quarkus-neo4j.version from 1.1.0 to 1.1.1
- 80575cd Bump quarkus.version from 2.8.2.Final to 2.8.3.Final
- 22a9349 Bump maven-project-info-reports-plugin from 3.2.2 to 3.3.0 (#486)
- be35a51 Bump quarkus.version from 2.8.1.Final to 2.8.2.Final (#485)
- 97de770 Bump byte-buddy.version from 1.12.9 to 1.12.10 (#484)
- 10051a9 Bump junit-jupiter-causal-cluster-testcontainer-extension from 2022.0.3 to 2022.0.4 and testcontainers.version from 1.16.3 to 1.17.1 (#472)
- 8fdf843 Bump maven-javadoc-plugin from 3.3.2 to 3.4.0 (#481)
- e1a541b Bump neo4j-harness from 4.4.5 to 4.4.6 (#480)
- 887de5d Bump mockito.version from 4.4.0 to 4.5.1 (#479)
- 918f9e4 Bump quarkus.version from 2.8.0.Final to 2.8.1.Final (#478)
- 7a90b2a Bump checkstyle from 10.1 to 10.2 (#477)
- 6d1c4c6 Bump nexus-staging-maven-plugin from 1.6.12 to 1.6.13 (#476)
- 14a68e7 Bump maven-site-plugin from 3.11.0 to 3.12.0 (#475)
- 0cbe2de Bump spring-boot.version from 2.6.6 to 2.6.7 (#474)
- 5f5df4e Bump classgraph from 4.8.143 to 4.8.146 (#473)

## 🛠 Build
- a135e42 Create windows binaries with prior version of GraalVM.
- b7ffcbb Disable tc cloud.
- 31c4dd2 Update github-push-action to 0.6.0.
- 557699e Upgrade to GraalVM 22.1.0. (#482)


## Contributors
We'd like to thank the following people for their contributions:
- @michael-simons


# 1.5.4

This is a release to acknowledge the work done by @aalmiray with @jreleaser which just released itself over the weekend in version 1.0.0 and to which I just bumped this repository. 

Thank you for making me rethink releasing my stuff a lot. I truly enjoyed our collaboration.

## 🧹 Housekeeping
- 96438d1 Bump jreleaser-maven-plugin from 0.10.0 to 1.0.0 (#469)
- e86469b Bump byte-buddy.version from 1.12.8 to 1.12.9 (#471)
- 338d0a8 Bump jacoco-maven-plugin.version from 0.8.7 to 0.8.8 (#470)
- d2224e9 Bump maven-clean-plugin from 3.1.0 to 3.2.0 (#468)


# 1.5.3

## 🧹 Housekeeping
- 4227864 Bump quarkus.version from 2.7.5.Final to 2.8.0.Final (#465)
- eec9b8f (deps-dev) Bump junit-jupiter-causal-cluster-testcontainer-extension (#467)
- 23fac84 Bump spring-boot.version from 2.6.5 to 2.6.6 (#466)
- f51a072 Bump checkstyle from 10.0 to 10.1 (#461)
- 396b83c Bump spring-boot.version from 2.6.4 to 2.6.5 (#459)
- 2f7ee1e Bump jna from 5.10.0 to 5.11.0 (#460)
- e59a0d6 Bump neo4j-harness from 4.4.4 to 4.4.5 (#462)
- 5f52e20 Bump native-maven-plugin from 0.9.10 to 0.9.11 (#463)
- 46dc74b Bump classgraph from 4.8.141 to 4.8.143 (#464)


## Contributors
We'd like to thank the following people for their contributions:
- @michael-simons


# 1.5.2

## 🐛 Bug Fixes
- 44a52c5 Produce correct String representation for `VersionPrecondition`.
- 2c2db1a Improve parsing of single line comments. (#447)
- 5379068 Make sure verbose option is recognized in CLI.

## 🔄️ Refactorings
- b8a43fb Add unparsable precondition to exception message. (#448)

## 📝 Documentation
- f60247e Add SDKMAN! installation instructions.
- d763be7 Improve order of concepts.

## 🧹 Housekeeping
- 9a52ef8 Bump neo4j-java-driver from 4.4.3 to 4.4.5 (#457)
- 4e0e616 (deps-dev) Bump junit-jupiter-causal-cluster-testcontainer-extension (#458)
- 8b0f8a5 Bump quarkus.version from 2.7.4.Final to 2.7.5.Final (#456)
- bba3d0d Bump maven.version from 3.8.4 to 3.8.5 (#450)
- 03f1564 Bump quarkus.version from 2.7.3.Final to 2.7.4.Final (#451)
- 7d241fd Bump mockito.version from 4.3.1 to 4.4.0 (#452)
- 0fe080d Bump quarkus-neo4j.version from 1.0.4 to 1.0.5 (#453)
- 6c550a3 Bump maven-compiler-plugin from 3.10.0 to 3.10.1 (#455)


## Contributors
We'd like to thank the following people for their contributions:
- @michael-simons
- @meistermeier


# 1.5.1

## 🐛 Bug Fixes
- f6ed145 Don't swallow exceptions when unlocking fails.

## 🔄️ Refactorings
- 50abfd1 Add additional tests.
- a280036 Improve exception handling and CLI messages.

## 📝 Documentation
- c1d04fa Add better explanation how to create a target database via callbacks.

## 🛠 Build
- 3b1098b Add Sdkman distribution.


## Contributors
We'd like to thank the following people for their contributions:
- @michael-simons
- @marc0der for super quick onboarding to SDKMAN!


# 1.5.0

*No breaking changes* but an exciting new feature: have preconditions asserted against your target database before we try to run your refactoring. This way you can make sure you’ll never end up in an invalid state just because Cypher syntax changed in between versions. 

Preconditions we support out of the box:
* Edition check (enterprise or community)
* Version check (enumerated versions or ranges)
* Custom queries returning a boolean value

All preconditions can be asserted (refactoring will stop when unmet) or assumed (single migration will be skipped). 

Have a look at the docs and learn more about [Preconditions](https://michael-simons.github.io/neo4j-migrations/current/#concepts_preconditions).

Thanks to @fbiville for an inspirational presentation!

## 🚀 Features
- c19fafa Add Support for preconditions. (#443)
- b66dee1 Log invocation of callbacks. (#439)

## 🐛 Bug Fixes
- a3c6547 nexus-staging-maven-plugin JDK17 have been fixed in their latest release.
- 653c06c Don’t try to send single line comments as statements. (#440)

## 🔄️ Refactorings
- 569e03c Reduce visibility of internal API. (#432)

## 📝 Documentation
- b6e75f5 Update local changelog.

## 🧹 Housekeeping
- 4810626 Bump checkstyle from 9.3 to 10.0 (#441)
- a503c55 Bump quarkus.version from 2.7.2.Final to 2.7.3.Final (#442)
- 2ea8fbe Bump docker-maven-plugin from 0.39.0 to 0.39.1 (#437)
- 0dcfd07 Bump classgraph from 4.8.139 to 4.8.141 (#436)
- d9ba177 Bump japicmp-maven-plugin from 0.15.6 to 0.15.7 (#435)
- ac9e9b1 Bump spring-boot.version from 2.6.3 to 2.6.4 (#434)
- 9aaefe9 Bump maven-project-info-reports-plugin from 3.2.1 to 3.2.2 (#433)


## Contributors
We'd like to thank the following people for their contributions:
- @michael-simons
- @meistermeier


# 1.4.0

*No breaking changes*.  The version is bumped due to a couple of new features that required additional API, for example using only remote or locally discovered migrations.

A big "Thank you" goes out to @marianozunino, a long time Neo4j (and Spring Data Neo4j) user, who created his own refactor tooling dubbed [Morpheus](https://github.com/marianozunino/morpheus). We worked together so that this and his tooling use the same meta model. And afterwards, I took some inspiration from the features he had in Morpheus.

The feature I like the most in this release is the new and shiny integration with Quarkus dev-services, check it out: [Dev Services integration](https://michael-simons.github.io/neo4j-migrations/1.4.0/#devservicesintegration).

## 🚀 Features
- c8f29d4 Add unique constraint for migration nodes on Neo4j 4.4. (#428)
- ed04748 Write and read optional config file from CLI. (#427)
- ad644a5 Add `mode` option to `InfoCmd`.
- b9afdd8 Introduce a ChainBuilderMode to select local or remote chains only. (#425)
- d63629e Make `file:./neo4j/migrations` the default for `--location` in the CLI. (#424)
- f3ad97c Add Quarkus Dev-UI integration.
- 5814489 Provide `ConnectionDetails` without retrieving the whole chain of applied migrations.

## 🐛 Bug Fixes
- 085cb4f Fix JaCoCo configuration.

## 🔄️ Refactorings
- b476a79 Avoid unnessary accessors for Quarkus processors.
- 970c84c Use default location only if it exists.
- 16eca1e Avoid printing a full stack trace for expected exceptions in the CLI. (#423)
- 81048fa Reduce visibility of build items. (#412)
- 46263f2 Use Quarkus' built-in class indes and classpath resource utils.
- b5383f1 Improve test.

## 📝 Documentation
- 76e23ef Use correct language for Gradle snippet.
- c2bfe46 Add morpheus to README.
- 2e9594a Update local changelog.

## 🧹 Housekeeping
- ba4c60c Bump nexus-staging-maven-plugin from 1.6.11 to 1.6.12
- 1a152eb quarkus-neo4j extension from 1.0.3 to 1.0.4. (#431)
- 6f00824 Bump quarkus.version from 2.7.1.Final to 2.7.2.Final (#430)
- 2cc1184 Bump maven-compiler-plugin from 3.9.0 to 3.10.0 (#418)
- e47b2d3 Bump native-maven-plugin from 0.9.9 to 0.9.10 (#417)
- 1b828bb Bump maven-site-plugin from 3.10.0 to 3.11.0 (#416)
- 73963b4 Bump nexus-staging-maven-plugin from 1.6.8 to 1.6.11 (#415)

## 🛠 Build
- 2716106 Provide parameter names.
- a2ac68f Remove sysouts.


## Contributors
We'd like to thank the following people for their contributions:
- @michael-simons


# 1.3.3

*No breaking changes*.  Biggest change is the upgrade to Quarkus 2.7 inside the Quarkus extension. Thanks to @lukehutch for a new release of [ClassGraph](https://github.com/classgraph/classgraph) that improves compatibility with Quarkus' classloader.

## 📝 Documentation
- 09cabe1 Add a local changelog.

## 🧹 Housekeeping
- 125540c Revert "Bump nexus-staging-maven-plugin from 1.6.8 to 1.6.10 (#402)"
- a5a0c84 quarkus-neo4j extension from 1.0.2 to 1.0.3.
- 13fcae8 Bump nexus-staging-maven-plugin from 1.6.8 to 1.6.10 (#402)
- ca95751 Bump neo4j-harness from 4.4.3 to 4.4.4 (#403)
- 112f38c Bump picocli from 4.6.2 to 4.6.3 (#404)
- a75a0ef Bump slf4j.version from 1.7.35 to 1.7.36 (#406)
- a6b6452 Bump byte-buddy.version from 1.12.7 to 1.12.8 (#407)
- f5d8c32 Bump maven-javadoc-plugin from 3.3.1 to 3.3.2 (#408)
- 2f281a3 Bump quarkus.version from 2.6.3.Final to 2.7.1.Final (#401)
- fcebed4 Bump classgraph from 4.8.138 to 4.8.139 (#405)


## Contributors
We'd like to thank the following people for their contributions:
- @michael-simons


# 1.3.2

*No breaking changes*.  Mostly dependency upgrades.

Most notable is the fact that the native binaries are now build with **GraalVM 22.0** and benefit from the improvements in the SubstrateVM.

## 🐛 Bug Fixes
- d11401b Make version check more lenient during integration tests.

## 🧹 Housekeeping
- e976934 Bump graal-sdk from 21.3.0 to 22.0.0.2 (#391)
- 3613be7 Bump maven-project-info-reports-plugin from 3.1.2 to 3.2.1 (#400)
- 6eb3e3c Bump docker-maven-plugin from 0.38.1 to 0.39.0 (#399)
- aea9784 Bump quarkus.version from 2.6.2.Final to 2.6.3.Final
- 0db3094 Bump junit-jupiter-causal-cluster-testcontainer-extension from 2022.0.0 to 2022.0.1 and testcontainers.version from 1.16.2 to 1.16.3.
- d34fa93 Bump neo4j-java-driver from 4.4.2 to 4.4.3 (#398)
- 8e20a80 Bump checkstyle from 9.2.1 to 9.3 (#396)
- 83142e5 Bump japicmp-maven-plugin from 0.15.4 to 0.15.6 (#395)
- c10afa9 Bump asciidoctor-maven-plugin from 2.2.1 to 2.2.2 (#393)
- fd3f147 Bump mockito.version from 4.2.0 to 4.3.1 (#392)
- 87e8673 Bump slf4j.version from 1.7.33 to 1.7.35 (#390)
- 7425469 Bump spring-boot.version from 2.6.2 to 2.6.3 (#388)
- d653dac Bump maven-plugin-plugin from 3.6.2 to 3.6.4 (#387)
- b58a5e8 Bump maven-plugin-annotations from 3.6.2 to 3.6.4 (#385)
- 5100c54 Bump quarkus.version from 2.6.1.Final to 2.6.2.Final (#377)
- b41bd6d Bump byte-buddy.version from 1.12.6 to 1.12.7 (#376)
- 1b33ca3 Bump neo4j-harness from 4.4.2 to 4.4.3 (#378)
- d07182a Bump asciidoctorj from 2.5.2 to 2.5.3 (#379)
- 5fec62c Bump slf4j.version from 1.7.32 to 1.7.33 (#380)
- cff33f5 Bump maven-jar-plugin from 3.2.1 to 3.2.2 (#381)
- 34a7979 Bump maven-compiler-plugin from 3.8.1 to 3.9.0 (#382)
- f308d5f Revert "Bump maven-enforcer-plugin from 3.0.0-M3 to 3.0.0"
- 7ef3bff Bump maven-enforcer-plugin from 3.0.0-M3 to 3.0.0

## 🛠 Build
- aef5da1 Use GH usernames if available.
- 391d29f Introduce a 'fast' profile and mute javadoc plugin debug out.
- a8baea5 Simplify Testcontainers code. (#383)
- bcb7b14 Optimize release workflow.
- 18b1ab3 Enable debug information and use bytecode verification during tests.


## Contributors
We'd like to thank the following people for their contributions:
- @michael-simons
- @bsideup


# 1.3.1

## 🐛 Bug Fixes
- 81ea8e5 Check for non-empty classpath locations when in native image.

## 🧰 Tasks
- 24973fd Apply package name change in japicmp config.

## 🧹 Housekeeping
- fe713a9 Bump maven-jar-plugin from 3.2.0 to 3.2.1 (#373)

## 🛠 Build
- 5a74da1 Improve caching of Maven dependencies. (#375)
- 1360fd9 Use actions/cache@v2. (#374)
- dc37038 Add dedicated categories for chores and refactorings.


## Contributors
We'd like to thank the following people for their contributions:
- [Michael Simons](https://github.com/michael-simons)


# 1.3.0

Happy 2nd birthday, _neo4j-migrations_! 🥳 The first pre-release, 0.0.1 has been pushed out to central on January 9th, 2020. Since than, stuff has come a long way and I am super happy about it.

1.3.0 is a big release coming only 4 days after the last one [(1.2.3](https://github.com/michael-simons/neo4j-migrations/releases/tag/1.2.3)) and bumping the minor number. Why is that? 
The API hasn't changed in an incompatible way, but there are 2 changes in behavior

1. 2afaaa5 aligns the behavior of the `enabled` flag in the Spring Boot starter with the behavior in the Quarkus extension: A bean of type `Migrations` will be provided in the application context regardless of that setting. You are free to use it any way you want. You might want to make sure your application is on a valid database or not without us applying the migrations. Or you just want to use the `info` api to get the current version
2. c98d757 makes `locations-to-scan` and `packages-to-scan` [Build Time configuration](https://quarkus.io/guides/config-reference#build-time-configuration), meaning they are evaluated at built respectively augmentation time. This allows to finally complete the feature I had on my list since forever [("Add a Quarkus extension")](https://github.com/michael-simons/neo4j-migrations/issues/8) in full: Delivered with c10c4468 in 1.2.2 for the first time, it is now fully supported (and tested) in natively distributed Quarkus applications as well and with that, feature complete. If you want (or need) file based locations outside an augmented application, please use the new `external-locations`. It supports only file-based locations, does not become part of the image and is changeable without re-augmentation.

## 🚀 Features
- 20f27d3 Compress native CLI binaries. (#372)
- b67fcc0 Add `apply` alias for `migrate` to the CLI.
- c98d757 Support classpath scanning in Quarkus native image.
- 2afaaa5 Allow disabling of migrations in Spring Boot without removing the `Migrations` bean. (#361)

## 🔄️ Refactorings
- ccd6461 Fail early with `classpath://` locations passed to the CLI in native image. (#369)
- 62278f2 Make sure that all sessions handed out use the latest bookmarks known. (#370)
- 3923423 Use transaction functions for all interactions with Neo4j inside the core module. (#365)
- a3422a8 Replace `MigrationsInitializer` in Quarkus extension with ServiceStartBuildItem.
- 4ad5dd0 Use List.sort instead of Collections.

## 📝 Documentation
- 6a5ca16 Improve core JavaDocs.

## 🛠 Build
- 356fa46 Add end-to-end test for the native CLI. (#371)
- 5ffda5c Use official GraalVM action for releasing. (#366)
- 2c96895 Define a stable order for categories in the changelog.


## Contributors
We'd like to thank the following people for their contributions:
- [Michael Simons](https://github.com/michael-simons)
- [Gerrit Meier](https://github.com/meistermeier)


# 1.2.3

## 🚀 Features
- 4840904 Add Neo4j JBang catalog distribution.

## 📝 Documentation
- f80e684 Improve CONTRIBUTING document.

## 🧰 Tasks
- 4079810 Extend license header to 2022.

## 🧹 Housekeeping
- ffddee7 Bump jreleaser-maven-plugin from 0.9.1 to 0.10.0 (#360)
- 1d9da39 Bump native-maven-plugin from 0.9.8 to 0.9.9 (#351)
- fd82573 Bump maven-deploy-plugin from 3.0.0-M1 to 3.0.0-M2 (#358)
- 36cf426 Bump sortpom-maven-plugin from 3.0.0 to 3.0.1 (#359)
- bf76713 Bump system-lambda from 1.2.0 to 1.2.1 (#357)
- 4b144f3 Bump assertj-core from 3.21.0 to 3.22.0 (#356)
- 11b57ce Bump byte-buddy.version from 1.12.5 to 1.12.6 (#349)
- 9e06096 Bump spring-boot.version from 2.6.1 to 2.6.2 (#350)
- 0122f66 Bump quarkus.version from 2.6.0.Final to 2.6.1.Final (#352)
- 9f8a369 Bump maven-site-plugin from 3.9.1 to 3.10.0 (#353)
- e03eef0 Bump plexus-component-annotations from 2.1.0 to 2.1.1 (#354)
- cd829fa Bump checkstyle from 9.2 to 9.2.1 (#355)


## Contributors
We'd like to thank the following people for their contributions:
- [Michael Simons](https://github.com/michael-simons)
- [Gerrit Meier](https://github.com/meistermeier)


# 1.2.2

## 🚀 Features
- c10c446 Add Quarkus-Module. (#343)
- 209b1d6 Allow safe passwords in scripts. (#341)
- 06c7344 Add shell autocompletion for the CLI module.
- e5f017f Support lifecycle callbacks. (#336)

## 📝 Documentation
- 7b3decd Add Quarkus extension to README.
- 6776211 Add Quarkus extension to the list of modules.
- 2a1b096 Use the wording Quarkus extension.
- 8100163 Add instructions for running on the module-path.
- 54ba14d Correct spelling errors and unify wording across documentation and code. (#340)
- befea0e Create a dedicated manual instead of one overly long readme. (#339)

## 🧹 Housekeeping
- de51201 Bump docker-maven-plugin from 0.38.0 to 0.38.1 (#348)
- ad65f91 Bump byte-buddy.version from 1.12.3 to 1.12.5 (#347)
- 3df66f4 Bump quarkus-neo4j.version from 1.0.1 to 1.0.2 (#346)
- 812ef78 Bump mockito.version from 4.1.0 to 4.2.0 (#345)
- d76eadc Bump neo4j-harness from 4.4.0 to 4.4.2
- 9fc602c Bump classgraph from 4.8.137 to 4.8.138 (#338)
- 1fa0c55 Bump neo4j-java-driver from 4.4.1 to 4.4.2 (#337)

## 🛠  Build
- e4663c7 Generate zsh completion only for macos.
- 637412e Provide module-info for JDK 11 and higher instead of only for JDK 17+.
- f32e3ec Exlude JReleaser bits from Sortpom so that the emojis don't turn into cold XML-Entities.
- af91f0c Use one central script for creating the site.
- 1a53bc0 Polish build of docs.
- 318a889 Add zip file to distribution.
- 89e1e79 Reorder plugins so that the zip distribution isn't empty.


## Contributors
We'd like to thank the following people for their contributions:
- [Michael Simons](https://github.com/michael-simons)
- [Gerrit Meier](https://github.com/meistermeier)


# 1.2.1

## 🐛 Fixes
- 0378b27 Ignore files without extensions in filesystem resources.
- 118d078 Fix broken checkstyle config.
- 1e7cc51 Add MANIFEST.MF to native bundle and provide a central way to retrieve the product version. (#329)

## ♻️  Changes
- af1a790 Don't declare unchecked exception explicitly.
- ab7cccb Tidy up CLI. (#335)
- 08a4d0e Pre-filter Cypher scripts that cannot be parsed into a version. (#330)

## 🧹 Housekeeping
- a85bc05 Bump neo4j-harness from 4.3.7 to 4.4.0 (#332)
- f15f9a6 Bump byte-buddy.version from 1.12.2 to 1.12.3 (#333)
- ba09f1c Bump native-maven-plugin from 0.9.7.1 to 0.9.8 (#334)
- dcb531e Bump spring-boot.version from 2.6.0 to 2.6.1 (#331)

## 🛠  Build
- c31f9d1 Move all JReleaser templates into `etc/jreleaser`.
- 689dc17 Update JReleaser configuration with 0.9.1 changes. (#327)

## 📝 Documentation
- 73e279c Improve the structure of the readme.
- ab78aad Add information about  keyword.


## Contributors
We'd like to thank the following people for their contributions:
- [Michael Simons](https://github.com/michael-simons)
- [Andres Almiray](https://github.com/aalmiray)
- [Gerrit Meier](https://github.com/meistermeier)


# 1.2.0

## 🚀 Features
- 922b865 Add `validate` command. (#326)
- 4f3fe73 Add a Java Module system descriptor.

## 🐛 Bug Fixes
- cb2d614 Also catch `Neo.ClientError.Schema.ConstraintAlreadyExists` while locking. (#324)

## 📝 Documentation
- 839c1c2 Improve plain Java example in README.

## 🧹 Housekeeping
- e1a321d Bump checkstyle from 9.1 to 9.2 (#321)
- ea930f9 Bump maven-plugin-plugin from 3.6.1 to 3.6.2 (#322)
- 9655e3f Bump classgraph from 4.8.134 to 4.8.137 (#320)
- aad3209 Bump byte-buddy.version from 1.12.1 to 1.12.2 (#319)
- 14ea910 Bump maven-plugin-annotations from 3.6.1 to 3.6.2 (#318)
- 64f9c11 Bump junit-bom from 5.8.1 to 5.8.2 (#317)
- 2b2fd70 Bump version (needed due to added deprecations).

## 🧰 Build
- 2d3a29a Follow the Sonar manual for aggregating multi-module test-data. (#325)
- 1a1be95 Adapt JReleaser to changed dependabot commit messages.
- cb8c3fb Automate update of old (previous) version.

## Changes
- da5722b Separate integration tests from unittests. (#328)


## Contributors
We'd like to thank the following people for their contributions:
- [Michael Simons](https://github.com/michael-simons)


# 1.1.0

## 🚀 Features
- 08950cf Add `clean` command. (#315)
- 1efb2ba Add support for storing schema database independent from target database. (#303)

## 🐛 Bug Fixes
- 963d389 The match for versions in the default database was too fuzzy. (#311)
- a27e5a6 Fix flaky logging test. (#305)

## 🧰 Build
- 8b9c546 Automate update to README.adoc.
- 3a338e4 Enforce semantic versioning. (#304)
- 87ba6f1 Use testcontainers.cloud if available.

## 🧹 Housekeeping
- bc68f4f Bump spring-boot.version from 2.5.6 to 2.6.0 (#306)
- 2619ed9 Bump maven.version from 3.8.3 to 3.8.4 (#312)
- 61f0322 Bump classgraph from 4.8.133 to 4.8.134 (#314)
- 09475e8 Bump mockito.version from 4.0.0 to 4.1.0 (#313)
- 161e233 Bump classgraph from 4.8.132 to 4.8.133 (#307)
- 73d3b39 Bump neo4j-java-driver from 4.4.0 to 4.4.1 (#300)
- c25494c Bump byte-buddy.version from 1.11.19 to 1.12.1 (#299)
- 7bd06e7 Bump jna from 5.9.0 to 5.10.0 (#298)
- c52c6e4 Bump neo4j-harness from 4.3.6 to 4.3.7 (#297)
- 2bab394 Bump sonar-maven-plugin from 3.9.0.2155 to 3.9.1.2184 (#296)
- ab5b3e5 Bump docker-maven-plugin from 0.37.0 to 0.38.0 (#301)
- 0d13959 Bump classgraph from 4.8.129 to 4.8.132 (#302)

## Changes
- 375541d Provide a better example in the Maven integration test with single config. (#308)

## Tasks
- be63550 Make javadoc happy. (#309)
- 57e07be Rename `master` to `main`.
- 4c126ed Add a code of conduct.
- 74784c1 Rename license file in a generic way.


## Contributors
We'd like to thank the following people for their contributions:
- [Michael Simons](https://github.com/michael-simons)


# 1.0.0

## 🚀 Features
- d20c86d Add support for 4.4 user impersonation. (#292)

## 🐛 Bug Fixes
- 3a7bc1b Fix file mode.
- cd20925 Fix mutable defaults. (#285)
- a637c46 Fix name.
- 147e366 Fix typo.
- 4ace342 Fix low hanging smells. (#277)
- 6efe367 Add test for security hotspot fixes.
- 5bd1a9a Review and fix security hotspots. (#276)

## 📝 Documentation
- aa63ab4 Add simple contributing information.
- b65e2d0 Add homebrew instructions.
- 44f3402 Remove `$` to allow copy & paste via GH ui.
- 5f98717 Document how to add classes to the command line. (#26)

## 🧰 Build
- ffcd3de Use correct workflow name.
- b9e9623 Skip integration test aggregator signing on release.
- e1cc5f9 Use JReleaser for GH releases, uploading artifacts and Homebrew integration.

## 🧹 Housekeeping
- 3d7fa30 Bump neo4j-java-driver from 4.2.7 to 4.4.0 (#290)
- 368ff6c Bump picocli from 4.6.1 to 4.6.2 (#291)
- 4c596f3 Bump checkstyle from 9.0.1 to 9.1 (#289)
- 69103d4 Bump native-maven-plugin from 0.9.6 to 0.9.7.1 (#288)
- e97780d Bump classgraph from 4.8.128 to 4.8.129 (#287)
- acb89a9 Bump testcontainers.version from 1.16.0 to 1.16.2 (#278)
- 896e306 Bump spring-boot.version from 2.5.5 to 2.5.6 (#279)

---
- 1f53e7b Use singletonList. (#286)
- 1f6270d Remove unnecessary line break. (#283)
- 26ecff5 Simplify build steps.
- 81a88ee Analyze only on non-forked prs and no dependabot prs.
- 507ffd8 Aggregate coverage after integration test.
- 8e4b92b Remove superflous name from job.
- e6cb08a Improve Migrations api.
- f58c117 Improve DefaultChainElement.
- 69adb3e Remove remaining unmanaged version. (#29)
- b0e0d08 Improve exception handling.
- afff47b Add badges.
- 63b913e Improve AbstractConnectedMojo and tests.
- c466804 Add more tests.
- 51be13c Set expected coverage to a minimum now that there is at least one test
- 93c0233 Add JaCoCo and Sonar analysis to the build. (#275)


## Contributors
We'd like to thank the following people for their contributions:
- [Michael Simons](https://github.com/michael-simons)
- [Gerrit Meier](https://github.com/meistermeier)
- [Andres Almiray](https://github.com/aalmiray)


# 0.3.2

## 🧹 Housekeeping

* Tons of dependency updates thanks to @dependabot.


# 0.3.1

## 🚀 Features

- GH-238 - Add autocrlf option.

## 🐛 Bug Fixes

- Fix logging on native image.

## 🧹 Housekeeping

- GH-239 - Use transaction functions instead of auto commits where applicable.
- Don't print full stacktrace on auth error.
- Set pool size to 1 in CLI. [improvement]
 
Java and native binaries are now published with each GitHub-Release.


# 0.2.1

## 🚀 Features

* GH-237 - Add `validateOnMigrate` to skip validation of migration-checksums.

## 🐛 Bug Fixes

* GH-232 - Treat single `\r` as correct line endings.

## 🧹 Housekeeping

* Tons of dependency updates thanks to @dependabot.

Noteworthy: Upgraded the example using Neo4j Test-Harness to Neo4j *4.3.2*. Neo4j requires JDK 11, so this project requires now JDK 11 to **build** but **still targets JDK 8**, so that it can be used with Java 8. I do think that this change in the build justifies a minor version bump alone but we have also a new configuration option, hence the 0.2.1 version.

Thanks to @AndreasBoehme for his input on this release!


# 0.1.4

## 🧹 Housekeeping

* Tons of dependency updates thanks to @dependabot.

Noteworthy: Upgraded to Spring Boot 2.5.0.


# 0.1.3

## 🧹 Housekeeping

* Tons of dependency updates thanks to @dependabot.

Especially the upgrade to Classgraph 4.8.103 has to be mentioned, as that fixes a resource scanning issue.


# 0.1.2

## 🚀 Features

* GH-159 - Add support for more flexible version schemes in migration names (thanks to @katya-dovgalets)

## 🧹 Housekeeping

* Tons of dependency updates thanks to @dependabot


# 0.1.1

## 🧹 Housekeeping

* Tons of dependency updates thanks to @dependabot
* The Spring Boot Starter has been updated to Spring Boot 2.4 and thus doesn't require the custom Neo4j driver starter anymore!  🎉


# 0.0.13

## 🧹 Housekeeping

* Tons of dependency updates thanks to @dependabot
* In the Spring Boot starter: Configure the migrations after Neo4j Data, so that we indirectly run after the builtin driver starter from 2.4 on upwards.


# 0.0.12

## 🐛 Bug Fixes

* GH-89: Configure class scanner to handle Spring Boot packaged resources.

## 🧹 Housekeeping

* Tons of dependency updates thanks to @dependabot

Thanks to our contributor @corneil


# 0.0.11

## 🚀 Features

* Created a Maven plugin.

## 🐛 Bug Fixes

* Don't close `System.err` in the CLI (That happened accidentally as I inherited from `java.util.logging.ConsoleHandler`

## 🧹 Housekeeping

* Tons of dependency updates thanks to @dependabot


# 0.0.10

## 🐛 Bug Fixes

Neo4j-Migrations didn't work on instances with anonymous access due to the fact the user management procedures will be removed in such an instance.


# 0.0.9

## 🧹 Housekeeping

* Easier use of the starter: Starter for driver is a non-optional dependency for the starter and stays optional for the auto configuration itself, so that people have choice whether to include it or not.


# 0.0.8

## 🚀 Features

* Spring Boot starter added with instructions

## 🚨Api changes

* Renamed prefix `filesystem:` to `file:` to be consistent with most other tools out there. 

*NOTE:* There are 0.0.6 and 0.0.7 on central, but without the starter due to issues with Maven's release plugin, the Nexus plugin and submodules with a different parent. I'm sorry for that.


# 0.0.5

## 🚀 Features

* CLI Module added with instructions


# 0.0.4

## 🚀 Features

* Record execution time
* Full support for multiple databases in Neo4j 4.0
* Convenience methods `MigrationContext#getSession` and `MigrationContext#getSessionConfig` to for session or session config retrieval in Java based migrations
* Add `Migrations#info`, returning a `MigrationChain` containing the state of your database and all discoverable migrations

## 🚨Api changes

* Public Core API lives now under `ac.simons.neo4j.migrations.core`
* Datamodel change: Execution time is now stored as Neo4j `Duration`
* `MigrationType` is an enum now
* `MigrationState` added


# 0.0.3

## 🚀 Features

* Checksums for Cypher script based migrations (stored and validated)

## 🧹 Housekeeping

* Turned into multi module project
* Added Checkstyle to the build for making contributions easier
* Decoupled discovery of migrations from the actual migrations


# 0.0.2

## 🚀 Features

* Cypher based migrations
* More information stored on `__Neo4jMigration` nodes and `MIGRATED_TO` relations (description, type, database and system user, possible checksums)

## 🚨Api changes

* `ac.simons.neo4j.migrations.Migration#apply` takes in a `MigrationContext`, which contains the driver and the migrations configuration and allows for further changes without breaking that API again.


# 0.0.1

Preview release with basic functionality for use in JHipster.
