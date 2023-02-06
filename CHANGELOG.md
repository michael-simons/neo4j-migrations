# 2.1.1

## 📝 Documentation
- c9e66f2 docs: add sibethencourt as a contributor for ideas (#840)

## 🧹 Housekeeping
- e4c321e Bump checkstyle from 10.6.0 to 10.7.0 (#853)
- 7048612 Bump asciidoctorj-diagram from 2.2.3 to 2.2.4 (#848)
- 877bf43 Bump byte-buddy.version from 1.12.22 to 1.12.23 (#852)
- 2ce23f6 build(deps-dev): Bump checker-qual from 3.29.0 to 3.30.0 (#851)
- dcffb9a Bump picocli from 4.7.0 to 4.7.1 (#847)
- bc842b6 Bump mockito.version from 5.0.0 to 5.1.0 (#846)
- 01c9ad7 Bump sortpom-maven-plugin from 3.2.0 to 3.2.1 (#843)
- 428a91d Bump quarkus.version from 2.16.0.Final to 2.16.1.Final (#850)
- bb82c8b Bump neo4j-ogm.version from 4.0.1 to 4.0.2 (#849)
- c05ef3f Bump neo4j-cypher-dsl-schema-name-support from 2023.0.1 to 2023.0.2 (#845)
- dbdb81f Bump neo4j-java-driver from 5.4.0 to 5.5.0 (#844)
- a6d72be Bump neo4j-harness from 5.3.0 to 5.4.0 (#842)


# 2.1.0

## 🚀 Features
- 6e9e6f4 feat: Provide a factory method for `MigrationContext`. (#830)

## 🔄️ Refactorings
- 57ad31c refactor: Remove unnessary suppressions.

## 🧹 Housekeeping
- 7d32f07 Bump neo4j-cypher-dsl-schema-name-support from 2023.0.0 to 2023.0.1 (#838)
- aeb8d5d Bump quarkus.version from 2.15.3.Final to 2.16.0.Final (#836)
- c3b9e00 Bump spring-boot.version from 3.0.1 to 3.0.2 (#834)
- 57d13ed Bump maven-surefire-plugin from 3.0.0-M7 to 3.0.0-M8 (#832)
- 7d8ed42 Bump maven-plugin-annotations from 3.7.0 to 3.7.1 (#839)
- be4188e Bump assertj-core from 3.24.1 to 3.24.2 (#837)
- c4ffb0b Bump maven-plugin-plugin from 3.7.0 to 3.7.1 (#835)
- 403f31e Bump graal-sdk from 22.3.0 to 22.3.1 (#833)
- ca31532 Bump maven-failsafe-plugin from 3.0.0-M7 to 3.0.0-M8 (#831)
- 9bdc174 Bump byte-buddy.version from 1.12.21 to 1.12.22 (#826)
- 0fd51e4 Bump maven-project-info-reports-plugin from 3.4.1 to 3.4.2 (#824)
- 62a3347 Bump jna from 5.12.1 to 5.13.0 (#823)
- 9c0d9d4 Bump maven-checkstyle-plugin from 3.2.0 to 3.2.1 (#822)
- ba17dc3 build(deps-dev): Bump error_prone_annotations from 2.17.0 to 2.18.0 (#821)
- da8f60a Bump junit-bom from 5.9.1 to 5.9.2 (#819)
- c8f4849 Bump mockito.version from 4.11.0 to 5.0.0 (#817)
- aa537e2 Bump neo4j-java-driver from 5.3.1 to 5.4.0 (#825)
- 21978a5 Bump quarkus.version from 2.15.2.Final to 2.15.3.Final (#820)
- 6d1cc31 build(deps-dev): Bump spring-data-neo4j from 7.0.0 to 7.0.1 (#818)
- e616458 Bump convict from 6.2.3 to 6.2.4 in /etc/antora (#816)


# 2.0.3

## 🧰 Tasks
- 5fe2942 chore: Extend license header to 2023.

## 🧹 Housekeeping
- ec9292e Bump neo4j-cypher-dsl-schema-name-support from 2022.8.2 to 2023.0.0
- 54f4ac2 Bump quarkus.version from 2.14.2.Final to 2.15.2.Final (#811)
- a457fdb Bump byte-buddy.version from 1.12.20 to 1.12.21 (#814)
- c341f6b Bump classgraph from 4.8.153 to 4.8.154 (#815)
- 0a01df4 build(deps-dev): Bump checker-qual from 3.28.0 to 3.29.0 (#813)
- 030f635 Bump assertj-core from 3.23.1 to 3.24.1 (#812)
- dbab22e Bump json5 from 2.2.1 to 2.2.3 in /etc/antora (#809)
- 83d7d54 Bump neo4j-ogm.version from 4.0.0 to 4.0.1 (#801)
- 5cd8781 Bump checkstyle from 10.5.0 to 10.6.0 (#804)
- 9146a1f Bump maven.version from 3.8.6 to 3.8.7 (#806)
- fb1072f build(deps-dev): Bump error_prone_annotations from 2.16 to 2.17.0 (#808)
- 34cf949 Bump mockito.version from 4.10.0 to 4.11.0 (#807)
- 41b6830 Bump jreleaser-maven-plugin from 1.3.1 to 1.4.0 (#805)
- ca3d5c1 Bump classgraph from 4.8.152 to 4.8.153 (#802)
- 6e804ca Bump neo4j-cypher-dsl-schema-name-support from 2022.8.1 to 2022.8.2 (#800)
- c630222 Bump neo4j-java-driver from 5.3.0 to 5.3.1 (#803)
- 3c01172 Bump spring-boot.version from 3.0.0 to 3.0.1 (#798)

## 🛠 Build
- 6a9b92c build: Remove usage of deprecated JReleaser features.


# 2.0.2

Thanks go out to @Raf23 for using the Maven-Plugin under Windows and creating an excellent bug report.

## 🚀 Features
- 30fe952 Add refactoring `AddSurrogateKey`.

## 🐛 Bug Fixes
- 43eb56d Ensure Maven-Plugin can deal with windows paths. (#784)
- 3ed5f9e `ProductVersion` broke due to removal auf automatic module name in 21be999.

## 🔄️ Refactorings
- 228aa13 Seal concrete refactoring interfaces (#797)
- b816cde Use more JDK17 idioms. (#788)

## 🧹 Housekeeping
- 6915623 Bump neo4j-harness from 5.2.0 to 5.3.0 (#794)
- ff9e878 Bump docker-maven-plugin from 0.40.2 to 0.40.3 (#791)
- 652af2f Bump maven-invoker-plugin from 3.3.0 to 3.4.0 (#793)
- 89bd73a Bump compile-testing from 0.20 to 0.21.0 (#792)
- ef69ba3 Bump mockito.version from 4.9.0 to 4.10.0 (#790)
- ae9eff4 Bump byte-buddy.version from 1.12.19 to 1.12.20 (#789)

## 🛠 Build
- d36f493 Exclude csv from header check, use `project.artifactId`. (#785)


# 2.0.1

## 🚀 Features
- 1910fa4 Add a templated Java based migration that helps loading CSV data. (#777)

## 🐛 Bug Fixes
- 5a7acc8 Process record components proper (thanks to @MaurizioCasciano for the report). (#780)
- 21be999 Remove automatic automatic-module name insertion for core module. (#773)

## 📝 Documentation
- 4fcfbf1 Add demo-in-a-gif.
- f40b777 Update local changelog.

## 🧹 Housekeeping
- c66018f Bump classgraph from 4.8.151 to 4.8.152 (#783)
- ac9251f Bump neo4j-cypher-dsl-schema-name-support from 2022.8.0 to 2022.8.1 (#782)

## 🛠 Build
- a6e932e Upgrade deprecated actions (checkout, cache, setup-java). (#778)


# 2.0.0

After a bit more than year after the [1.0 release](https://github.com/michael-simons/neo4j-migrations/releases/tag/1.0.0), welcome Neo4j-Migrations 2.0. This release includes all the features that have been released in more than 15 releases since 1.0, including the latest changes in 1.15.1, released today as well.

The biggest change is going forward with Java 17, the current LTS release of Java. This enables us to use

* The latest and greatest version of GraalVM
* The current Neo4j-Java-Driver 5.3
* Move to Spring Boot 3.0

All the areas above are intertwined. GraalVM dropped support for Java 8 a while back and will drop support for Java 11 soon. There is no way for us to keep separate versions updated for all of these.
The same applies for being available in Spring Boot 3, which does require Java 17, too.

As we had building Multi-Release-Jars before to be able to provide proper modules on the module path and already seal some interfaces, our codebase actually shrank with the move.

The biggest visible change is that our documentations is now on [Neo4j-Labs: Neo4j-Migrations](https://neo4j.com/labs/neo4j-migrations/2.0/introduction/), beautifully rendered with [Antora](https://antora.org) thanks to the fantastic support by @Mogztter and @adam-cowley.

Moving forward we will mainly bring new features to the main branch, aka into 2.x. If possible without too much hassle, we will backport them into 1.x as well. The 1.x branch will be maintained with dependency upgrades for at least as long as Spring Boot 2.7, Quarkus 2.13 and especially the Neo4j-Java-Driver 4.4 are supported. With regards to the latter, a big thank you goes out to my friend and colleague @injectives who always openly listens to my rambling and makes the Neo4j-Java-Driver the great product which it is today.

To give you an idea about the size and complexity of the project but also the change in it over the course of a year, have a look at the [scc metrics](https://github.com/boyter/scc) from 1.0:

```
───────────────────────────────────────────────────────────────────────────────
                         Files     Lines   Blanks  Comments     Code Complexity
───────────────────────────────────────────────────────────────────────────────
Total                      113     11186     1522      2000     7664        220
───────────────────────────────────────────────────────────────────────────────
Estimated Cost to Develop (organic) $229.231
Estimated Schedule Effort (organic) 7,86 months
Estimated People Required (organic) 2,59
```

and about 13 months later, 2.0:

```
───────────────────────────────────────────────────────────────────────────────
                         Files     Lines   Blanks  Comments     Code Complexity
───────────────────────────────────────────────────────────────────────────────
Total                      536     58795     7882     11628    39285       1616
───────────────────────────────────────────────────────────────────────────────
Estimated Cost to Develop (organic) $1.275.068
Estimated Schedule Effort (organic) 15,08 months
Estimated People Required (organic) 7,51
```

And now, up to the next cool things ahead :)

## 🚀 Features
- b1a4a5f Try to detect `CALL {} IN TRANSACTIONS` and skip managed transactions. (#743)
- d86b14e Upgrade the project to utilize JDK17 full and proper.
- 5f8aa39 Provide a limited set of annotations to define constraints. (#755)
- fca152a Allow programmatic control over repeatable Java based migrations. (#726)

## 🐛 Bug Fixes
- 819e8d8 Fix prepare-release.sh.
- 9edf488 Fix jbang distribution.
- dad1cd4 Manually check for duplicate versions and don’t rely on the constraint. (#727)
- 0e41ee1 Add `migrateBTreeIndexes` cmd to CLI. (#744)

## 🔄️ Refactorings
- bff7e67 Sort duplicate versions by source name.
- dd6d1f0, 10fb84e and 45cf83d More JDK 17 related polishing.
- 67c9f33 Add additional tests
- c1a5538 Include reasons in CLI log when being able to render catalog items. (#723)
- d861a9c Remove `Formattable` trait from catalog items.

## 📝 Documentation
- e3c770a Correct spelling errors.
- 6165e60 add adam-cowley as a contributor for infra (#740)
- 96c9c3c add adriens as a contributor for ideas (#766)
- 34c7c6b add michael-simons as a contributor for talk (#767)
- d053800 add Mogztter as a contributor for code, and doc (#731)
- 2ad8ebd Add links to articles and presentations about the project.
- 1afe134 Update local changelog.
- 49260d2 Clarify Java version requirements.
- f865b5d migrate documentation to Antora (#657)

## 🧹 Housekeeping
- 3a21108 Bump native-maven-plugin from 0.9.18 to 0.9.19 (#771)
- a7869b2 Bump compile-testing from 0.19 to 0.20 (#770)
- 432ac7a Bump checker-qual from 3.27.0 to 3.28.0 (#769)
- 6039680 Bump neo4j-harness from 5.1.0 to 5.2.0 (#772)
- 4ca45d7 Upgrade driver to 5.3, Spring Boot to 3.0 and Quarkus (Extension) to 2.0.
- d20e174 Bump junit-jupiter-causal-cluster-testcontainer-extension from 2022.1.2 to 2022.1.3 and testcontainers from 1.17.5 to 1.17.6
- ccdcdbc Bump classgraph from 4.8.149 to 4.8.151 (#763)
- a71776c Bump archunit from 1.0.0 to 1.0.1 (#764)
- bec24d4 Bump native-maven-plugin from 0.9.17 to 0.9.18 (#761)
- d8e2e6b Bump neo4j-cypher-dsl-schema-name-support from 2022.7.3 to 2022.8.0 (#760)
- 71302a7 Bump quarkus.version from 2.14.0.Final to 2.14.2.Final (#759)
- ddd6ac0 Bump checkstyle from 10.4 to 10.5.0 (#758)
- 9ef9d8f Bump spring-data-neo4j from 6.3.5 to 7.0.0 (#754)
- 6237a64 Bump plexus-classworlds from 2.6.0 to 2.7.0 (#753)
- 270e596 Bump byte-buddy.version from 1.12.18 to 1.12.19 (#751)
- 8e4a4d9 Bump maven-install-plugin from 3.0.1 to 3.1.0 (#750)
- 6c934b5 Bump japicmp-maven-plugin from 0.16.0 to 0.17.1 (#749)
- 14dafc4 Bump commonmark from 0.20.0 to 0.21.0 (#747)
- bd72f7d Bump mockito.version from 4.8.1 to 4.9.0 (#746)
- 332f1a3 Bump neo4j-harness from 4.4.12 to 5.1.0 (#690)
- 1a4f6d4 Bump os-maven-plugin from 1.7.0 to 1.7.1 (#734)
- 0ae21b9 Bump maven-plugin-plugin from 3.6.4 to 3.7.0 (#735)
- 3393102 Bump maven-plugin-annotations from 3.6.4 to 3.7.0 (#733)
- 9f360ad Bump checker-qual from 3.26.0 to 3.27.0 (#736)
- f2e1ca8 Bump quarkus-neo4j.version from 1.6.1 to 1.7.0 (#721)
- 77fc359 Bump quarkus.version from 2.13.3.Final to 2.14.0.Final (#711)
- 5d3814b Bump graal-sdk from 21.3.2 to 22.3.0

## 🛠 Build
- cc349ec Set previous version to latest 1.x version.
- 33af669 Polish native config.
- a741a81 Remove multi-release leftovers.
- 8cb7229 Don’t skip tests based on checking string arguments blindly.
- 9b4368f Improve Antora includes and build
- b6af149 Notify neo4j.com playbooks repository. (#737)
- 13d7d45 Improve Antora version approach and module name. (#738)
- c8e4580 Add full testing support for Neo4j 5. (#741)

## Contributors
We'd like to thank the following people for their contributions:
- @adam-cowley
- @meistermeier
- @Mogztter


# 1.16.1

## 🧹 Housekeeping
- c727dfd Bump checkstyle from 10.6.0 to 10.7.0 (#853)
- 051027e Bump asciidoctorj-diagram from 2.2.3 to 2.2.4 (#848)
- 8cd37a9 Bump byte-buddy.version from 1.12.22 to 1.12.23 (#852)
- 8d7e2db build(deps-dev): Bump checker-qual from 3.29.0 to 3.30.0 (#851)
- 2bdb8f4 Bump picocli from 4.7.0 to 4.7.1 (#847)
- 955e05b Bump mockito.version from 5.0.0 to 5.1.0 (#846)
- fc497dc Bump sortpom-maven-plugin from 3.2.0 to 3.2.1 (#843)


# 1.16.0

Thanks to @sibethencourt for your input on #829.

## 🚀 Features
- e2f9978 feat: Provide a factory method for `MigrationContext`. (#830)

## 🧹 Housekeeping
- 6a82165 Bump quarkus.version from 2.13.5.Final to 2.13.7.Final
- cf1fde8 build(deps-dev): Bump spring-data-neo4j from 6.3.5 to 6.3.7
- 23fcbb9 Bump spring-boot.version from 2.7.5 to 2.7.8
- 59ce4d1 Bump byte-buddy.version from 1.12.21 to 1.12.22 (#826)
- abd5de2 Bump maven-project-info-reports-plugin from 3.4.1 to 3.4.2 (#824)
- 4de87cc Bump jna from 5.12.1 to 5.13.0 (#823)
- db472f8 Bump maven-checkstyle-plugin from 3.2.0 to 3.2.1 (#822)
- fe00ee9 build(deps-dev): Bump error_prone_annotations from 2.17.0 to 2.18.0 (#821)
- c6b2059 Bump mockito.version from 4.11.0 to 5.0.0 (#817)


# 1.15.3

## 🧰 Tasks
- f25102d chore: Extend license header to 2023.

## 🧹 Housekeeping
- fae4037 Bump neo4j-cypher-dsl-schema-name-support from 2022.8.2 to 2022.8.3
- 9c9d277 Bump byte-buddy.version from 1.12.20 to 1.12.21 (#814)
- 9664a76 Bump classgraph from 4.8.153 to 4.8.154 (#815)
- 5982fdb build(deps-dev): Bump checker-qual from 3.28.0 to 3.29.0 (#813)
- 86461be Bump assertj-core from 3.23.1 to 3.24.1 (#812)
- 927bab9 Bump neo4j-ogm.version from 3.2.38 to 3.2.39
- a93e50b Bump neo4j-java-driver from 4.4.9 to 4.4.11
- bb2b8b5 Bump checkstyle from 10.5.0 to 10.6.0 (#804)
- 0e50ee0 Bump maven.version from 3.8.6 to 3.8.7 (#806)
- 3ee9c60 build(deps-dev): Bump error_prone_annotations from 2.16 to 2.17.0 (#808)
- 3bf73da Bump mockito.version from 4.10.0 to 4.11.0 (#807)
- 785559f Bump jreleaser-maven-plugin from 1.3.1 to 1.4.0 (#805)
- 2e498f5 Bump classgraph from 4.8.152 to 4.8.153 (#802)
- 8185369 Bump neo4j-cypher-dsl-schema-name-support from 2022.8.1 to 2022.8.2 (#800)

## 🛠 Build
- fdda6e6 build: Remove usage of deprecated JReleaser features.


# 1.15.2

## 🚀 Features
- 7ddb8d1 Add refactoring `AddSurrogateKey`.

## 🐛 Bug Fixes
- eca5f3d Ensure Maven-Plugin can deal with windows paths. (#784)
- 3259e4c Filter abstract migration classes from scan result.

## 📝 Documentation
- 92aec8b Update local changelog.

## 🧹 Housekeeping
- 7f2ca00 Bump docker-maven-plugin from 0.40.2 to 0.40.3 (#791)
- ccd031a Bump maven-invoker-plugin from 3.3.0 to 3.4.0 (#793)
- dcc7bb9 Bump compile-testing from 0.20 to 0.21.0 (#792)
- 8c4029b Bump mockito.version from 4.9.0 to 4.10.0 (#790)
- 1ac8fed Bump byte-buddy.version from 1.12.19 to 1.12.20 (#789)
- 49171ff Bump classgraph from 4.8.151 to 4.8.152 (#783)
- 5fa19ce Bump neo4j-cypher-dsl-schema-name-support from 2022.8.0 to 2022.8.1 (#782)

## 🛠 Build
- e363de7 Allow native build on GraalVM 22.2.0.
- 4167c81 Upgrade deprecated actions (checkout, cache, setup-java). (#778)


# 1.15.1

## 📝 Documentation
- ad63660 Correct spelling errors.

## 🧹 Housekeeping
- a297bfc Bump native-maven-plugin from 0.9.18 to 0.9.19 (#771)
- 81abd64 Bump compile-testing from 0.19 to 0.20 (#770)
- 23fecb0 Bump checker-qual from 3.27.0 to 3.28.0 (#769)


# 1.15.0

## 🚀 Features
- aca8e6a Try to detect `CALL {} IN TRANSACTIONS` and skip managed transactions. (#743)
- 2efc143 Provide a limited set of annotations to define constraints. (#756)
- 9e4d233 Add `migrateBTreeIndexes` cmd to CLI. (#744) (#745)

## 🐛 Bug Fixes
- 45dab75 Fix typos.

## 🔄️ Refactorings
- 8c11780 Add full testing support for Neo4j 5. (#742)

## 🧹 Housekeeping
- 9a3c02c Bump junit-jupiter-causal-cluster-testcontainer-extension from 2022.1.2 to 2022.1.3 and testcontainers from 1.17.5 to 1.17.6
- 3c44944 Bump classgraph from 4.8.149 to 4.8.151 (#763)
- b749578 Bump archunit from 1.0.0 to 1.0.1 (#764)
- d3305f3 Bump native-maven-plugin from 0.9.17 to 0.9.18 (#761)
- b14861d Bump neo4j-cypher-dsl-schema-name-support from 2022.7.3 to 2022.8.0 (#760)
- 29c622b Bump quarkus.version from 2.13.3.Final to 2.13.5.Final
- cf894af Bump checkstyle from 10.4 to 10.5.0 (#758)
- 7161a4e Bump plexus-classworlds from 2.6.0 to 2.7.0 (#753)
- ba2c7d0 Bump byte-buddy.version from 1.12.18 to 1.12.19 (#751)
- a994396 Bump maven-install-plugin from 3.0.1 to 3.1.0 (#750)
- f7fc11f Bump japicmp-maven-plugin from 0.16.0 to 0.17.1 (#749)
- 263cc3c Bump commonmark from 0.20.0 to 0.21.0 (#747)
- 44e792c Bump mockito.version from 4.8.1 to 4.9.0 (#746)

## 🛠 Build
- 843c19d Don’t skip tests based on checking string arguments blindly.


# 1.14.0

With 1.14.0 we created the 1.x maintenance branch. All new development will happen in the main branch and require JDK 17 from now on. We will backport dependency upgrades as far as possible and if we can bring features over from main without pulling a leg and an arm, we might do so as well.

## 🐛 Bug Fixes
- c61ec51 Manually check for duplicate versions and don’t rely on the constraint. (#728)

## 🔄️ Refactorings
- c9a5be4 Include reasons in CLI log when being able to render catalog items. (#723) (#724)
- 11fc794 Deprecate public `isRepeatable` on `MigrationVersion`. (#722)

## 📝 Documentation
- dea9ae5 Update local changelog.

## 🧹 Housekeeping
- ec816e4 Bump os-maven-plugin from 1.7.0 to 1.7.1 (#734)
- 486f310 Bump maven-plugin-plugin from 3.6.4 to 3.7.0 (#735)
- b597615 Bump maven-plugin-annotations from 3.6.4 to 3.7.0 (#733)
- 92f5b33 Bump checker-qual from 3.26.0 to 3.27.0 (#736)

## 🛠 Build
- d4584f2 Prepare 1.x branch.


# 1.13.3

New feature: *Repeatable migrations*

@MaurizioCasciano brought up the idea of repeatable migrations / refactoring and we delivered: Migrations named `Rxzy__something.cypher` (`xml` catalogs and Java based migrations work too) will now be repeated when their checksum changed. The repetition will be recorded in the subgraph of migrations too so that you can check for it (with a self-referential relation type `REPEATED`, thanks to @meistermeier for the suggestion).

If you need something that always runs, consider a [callback](https://michael-simons.github.io/neo4j-migrations/current/#concepts_callbacks).

⚠️ Heads up ⚠️

This is most likely the last release containing new features. 2.0.0 will drop by the end of this year in which this project will be migrated fully to Java 17 and require Java 17.


## 🚀 Features
- 97819ad Add support for repeatable migrations.

## 📝 Documentation
- c6b0152 Include Neo4j 5 in readme.
- f71145e add MaurizioCasciano as a contributor for ideas (#709)

## 🧹 Housekeeping
- 95afd45 Bump quarkus-neo4j.version from 1.6.0 to 1.6.1 (#710)
- 60526b0 Bump native-maven-plugin from 0.9.16 to 0.9.17 (#712)
- c428cd4 Bump jreleaser-maven-plugin from 1.2.0 to 1.3.1 (#713)
- d9e1073 Bump ivy from 2.5.0 to 2.5.1 (#715)
- 4009303 Bump picocli from 4.6.3 to 4.7.0 (#716)
- 18206a1 Bump checkstyle from 10.3.4 to 10.4 (#714)
- 147f803 Revert "Bump jreleaser-maven-plugin from 1.2.0 to 1.3.0 (#701)"
- bf9a323 Bump maven-shade-plugin from 3.4.0 to 3.4.1 (#698)
- 7e791e4 Bump jreleaser-maven-plugin from 1.2.0 to 1.3.0 (#701)
- 491fbe4 Bump asciidoctorj from 2.5.6 to 2.5.7 (#700)
- b0a0c5f Bump neo4j-ogm.version from 3.2.37 to 3.2.38 (#697)


# 1.13.2

⚠️ This is a breaking change ⚠️

@SaschaPeukert notified us that 7e53e57 will break compatibility with older Spring Boot Versions (older than 2.7.0) as the new dedicated `@AutoConfiguration` was introduced in that release for the first time. It's debatable whether the compatibility of our tool with Spring Boot 2.6 was as promised or coincidentally, but this should not have happened and we are sorry for that.

## 🔄️ Refactorings
- 7e53e57 Migrate to `@AutoConfiguration`. (#696)

## 🧹 Housekeeping
- 4e49df8 Bump mockito.version from 4.8.0 to 4.8.1 (#688)
- 70015ae Bump quarkus.version from 2.13.2.Final to 2.13.3.Final (#687)
- 7758897 Bump commonmark from 0.19.0 to 0.20.0 (#689)
- cc9cb0a Bump native-maven-plugin from 0.9.14 to 0.9.16 (#691)
- 5bb1283 Bump plexus-utils from 3.4.2 to 3.5.0 (#692)
- 3ab929a Bump spring-boot.version from 2.7.4 to 2.7.5 (#693)
- 660a6dc Bump neo4j-cypher-dsl-schema-name-support from 2022.7.2 to 2022.7.3 (#694)
- d024823 Bump spring-data-neo4j from 6.3.3 to 6.3.5 (#685)
- 64bf663 Bump error_prone_annotations from 2.15.0 to 2.16 (#684)
- 69440fd Bump byte-buddy.version from 1.12.17 to 1.12.18 (#683)
- 5f79a41 Bump neo4j-harness from 4.4.11 to 4.4.12 (#682)
- 8a485c1 Bump quarkus.version from 2.13.1.Final to 2.13.2.Final (#680)

## 🛠 Build
- 8ffcea1 Use `inputEncoding` for checkstyle plugin.


# 1.13.1

## 🐛 Bug Fixes
- aaa4c71 Escape String literals for old fulltext indexes. (#670)
- 2a1e724 Sanitize all formattable objects.

## 🔄️ Refactorings
- d65e9f0 Delete unused, internal abstract `AbstractContext` class. (#671)
- 0d81fa9 Log skipped resources if they are not dedicated Cypher callbacks. (#673)

## 📝 Documentation
- ea50f6d Add missing JavaDoc.
- ebf0305 add SergeyPlatonov as a contributor for ideas

## 🧹 Housekeeping
- 1e80ea2 Bump junit-jupiter-causal-cluster-testcontainer-extension from 2022.1.1 to 2022.1.2 and testcontainers from 1.17.4 to 1.17.5
- ce8d2fb Bump archunit from 0.23.1 to 1.0.0 (#675)
- b2ac91c Bump checker-qual from 3.25.0 to 3.26.0 (#679)
- e1b322a Bump quarkus.version from 2.13.0.Final to 2.13.1.Final (#676)
- a3a4035 Bump junit-jupiter-causal-cluster-testcontainer-extension from 2022.1.0 to 2022.1.1 and testcontainers from 1.17.3 to 1.17.4


# 1.13.0

This exiting release - again **without** breaking changes - brings two important features:

1.  Migrating `BTREE` indexes to "future" indexes
    Neo4j 5 will drop the support for `BTREE` indexes. Those indexes must be replaced by `RANGE`, `POINT` or `TEXT` before a store upgrade is attempted. Neo4j 4.4 already "supports" `RANGE` and `POINT` for creating indexes (albeit they are not used in planning right now).
    Neo4j-Migrations offers two new refactorings for that: `migrate.createFutureIndexes` which will create future indexes in parallel to old ones and `migrate.replaceBTreeIndexes` to replace old indexes with new ones prior to upgrade
2. Running refactorings or even whole migrations standalone without storing or requiring metadata

How does that look like? Users have 3 options:
1. Define the new refactoring in a proper catalog based migrations (not going to reiterate that here)
2. Build the refactoring via its api and use the new api on `Migrations` to apply it
```java
Migrations migrations = new Migrations(MigrationsConfig.defaultConfig(), driver);

// Create equivalent future indexes with the suffix `_new` in parallel
Counters counters = migrations.apply(
	MigrateBTreeIndexes.createFutureIndexes("_new")
);

// or drop the old ones and create new ones
counters = migrations.apply(
	MigrateBTreeIndexes.replaceBTreeIndexes()
);
```

or

3. Define a "fake" migration containing only the refactoring and run it via the CLI
   First store the following as `V000__Migrate_indexes.xml` (or any other conform name you prefer):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<migration xmlns="https://michael-simons.github.io/neo4j-migrations">
	<refactor type="migrate.replaceBTreeIndexes">
		<parameters>
			<parameter name="typeMapping">
				<mapping>
					<name>index_on_location</name>
					<type>POINT</type>
				</mapping>
			</parameter>
		</parameters>
	</refactor>
</migration>
```

And run via:

```
./bin/neo4j-migrations -uneo4j -psecret run --migration file:./V000__Migrate_indexes.xml
```

Of course all variants allow to specify the type of replacement index per index / constraint and also excluding or explicitly including indexes and constraints to be migrated.  The [documentation covers all the details](https://michael-simons.github.io/neo4j-migrations/main/#migratingbtreeindexestofutureindexes).

Thanks to @SergeyPlatonov for the ideas to explicitly run single migrations from the CLI.

## 🚀 Features
- de0cc6f Provide refactorings for migrating BTREE indexes to future indexes. (#667)
- 1609fa0 Add the ability to run migrations and refactorings directly without recording them. (#668)

## 📝 Documentation
- 62a6870 Add JavaDocs where missing.

## 🧹 Housekeeping
- eba3cda Bump quarkus-neo4j.version from 1.5.0 to 1.6.0
- 430fbb6 Bump spring-boot.version from 2.7.3 to 2.7.4 (#658)
- 322045b Bump byte-buddy.version from 1.12.16 to 1.12.17 (#659)
- afe7a36 Bump quarkus.version from 2.12.2.Final to 2.13.0.Final (#660)
- 0f2b47e Bump checkstyle from 10.3.3 to 10.3.4 (#661)
- d0385a5 Bump native-maven-plugin from 0.9.13 to 0.9.14 (#662)
- b59ea26 Bump spring-data-neo4j from 6.3.2 to 6.3.3 (#663)
- 2dbe2c5 Bump asciidoctorj from 2.5.5 to 2.5.6 (#664)
- a3f6497 Bump junit-bom from 5.9.0 to 5.9.1 (#665)

## 🛠 Build
- 5bd4483 Ignore files with literal `.extension` during license check.
- a6dde98 Make sure constraints are really gone before running more tests.
- bf56ba8 Improve testing performance.


# 1.12.0

There has been no breaking changes in this release, but we have added some methods and moved things around that haven't been public before to improve your life in case you should use this library on the module path.

## 🚀 Features
- ad76526 Log connection details during Spring and Quarkus startup. (#655)
- f6f6ef9 Try to detect invalid use of enterprise constraints against community edition. (#654)

## 🐛 Bug Fixes
- 08ec086 Add braces to single property node key constraints statements for elder versions. (#653)
- 7a949bd Add several missing JMS requirements and necessities to public API. (#646)

## 📝 Documentation
- 871c36b add SaschaPeukert as a contributor (#649)

## 🧹 Housekeeping
- a8e0586 Bump maven-jar-plugin from 3.2.2 to 3.3.0 (#651)
- 637815a Bump maven-shade-plugin from 3.3.0 to 3.4.0 (#652)
- 8758ae4 Bump quarkus.version from 2.12.1.Final to 2.12.2.Final (#650)


# 1.11.0

This release contains the second Java annotation processor I wrote and I am very excited about it: You can point this tool from within Javac to entities annotated with [Neo4j-OGM](https://github.com/neo4j/neo4j-ogm) OR [Spring Data Neo4j 6](https://github.com/spring-projects/spring-data-neo4j) annotations and it will produce catalog files containing the indexes and constraints that can be derived by the annotated classes.

All indexes and constraints of the Neo4j-OGM auto index manager and all SDN6 annotations are supported. The annotation processor does not ship the dependencies itself, so one possible invocation looks like this:

```
javac -proc:only \
-processorpath neo4j-migrations-1.11.0.jar:neo4j-migrations-annotation-processor-api-1.11.0.jar:neo4j-migrations-annotation-processor-1.11.0.jar \
-Aorg.neo4j.migrations.catalog_generator.output_dir=output \
-Aorg.neo4j.migrations.catalog_generator.default_catalog_name=V01__Create_OGM_schema.xml \
-cp neo4j-ogm-core-3.2.37.jar \
path/to/annotated/entities/*
```

Please have a look at ["Annotation processing"](https://michael-simons.github.io/neo4j-migrations/current/#appendix_annotation) in the manual how to run this for SDN6 and possible ways to integrate it into your build.

One possible output - outlining just some of the constraints - will look like this

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<migration xmlns="https://michael-simons.github.io/neo4j-migrations">
  <!-- This file was generated by Neo4j-Migrations at 2022-09-21T21:21:00+01:00. -->
  <catalog>
    <indexes>
      <index name="my_entities_ogm_singleindexentity_login_property" type="property">
        <label>Entity</label>
        <properties>
          <property>login</property>
        </properties>
      </index>
      <index name="my_entities_ogm_relpropertyindextentity_description_property" type="property">
        <type>RELPROPERTYINDEXTENTITY</type>
        <properties>
          <property>description</property>
        </properties>
      </index>
      <index name="my_entities_ogm_compositeindexentity_name_age_property" type="property">
        <label>EntityWithCompositeIndex</label>
        <properties>
          <property>name</property>
          <property>age</property>
        </properties>
      </index>
    </indexes>
    <constraints>
      <constraint name="my_entities_ogm_entitywithassignedid_id_unique" type="unique">
        <label>EntityWithAssignedId</label>
        <properties>
          <property>id</property>
        </properties>
      </constraint>
      <constraint name="my_entities_ogm_nodepropertyexistenceconstraintentity_login_exists" type="exists">
        <label>Entity</label>
        <properties>
          <property>login</property>
        </properties>
      </constraint>
      <constraint name="my_entities_ogm_relpropertyexistenceconstraintentity_description_exists" type="exists">
        <type>REL</type>
        <properties>
          <property>description</property>
        </properties>
      </constraint>
      <constraint name="my_entities_ogm_nodekeyconstraintentity_name_age_key" type="key">
        <label>Entity</label>
        <properties>
          <property>name</property>
          <property>age</property>
        </properties>
      </constraint>
    </constraints>
  </catalog>
  <apply/>
</migration>
```

This catalog can than be added to your CI/CD or application setup as described in the [manual](https://michael-simons.github.io/neo4j-migrations/current/#concepts_catalog). We do not recommend regenerating it every time in the build as that will break the chain of migrations once you change entities. This might be seen as an inconvenience but we are convinced that a half-automated process here is better than the auto index manager of old that might surprises you with it's upgrade mechanism.

This also the first release that that will ship with Linux ARM64 ootb.

While we did bump the minor version, there are **no** breaking changes.

## 🚀 Features
- 771124c Add annotation processor for Neo4j-OGM and SDN6 entities. (#618)

## 🐛 Bug Fixes
- f8b1da9 Improve quotation papttern. (#616)

## 🔄️ Refactorings
- 9c58ec5 Use Cypher-DSL schema name support for sanitizing names. (#620)

## 📝 Documentation
- fb4ca38 add fbiville as a contributor for ideas (#638)
- e8a71cd add ali-ince as a contributor for userTesting (#637)
- 59fea88 add atomfrede as a contributor for ideas (#636)
- 1b52cf9 add katya-dovgalets as a contributor for code (#635)
- 6791e62 add corneil as a contributor for bug (#634)
- d141781 add marianozunino as a contributor for ideas (#633)
- fd4b41c add aalmiray as a contributor for code, plugin, ideas, mentoring (#632)
- a11f2fc add Dcanzano as a contributor for userTesting, bug (#631)
- 3b02732 add Hosch250 as a contributor for userTesting, bug (#630)
- 832f503 add injectives as a contributor for code, userTesting (#629)
- 1807f29 add SeanKilleen as a contributor for doc (#628)
- 22cf9b7 add ali-ince as a contributor for bug (#627)
- 1fadb5d add michael-simons as a contributor for maintenance (#626)
- 2062be5 add michael-simons as a contributor for code, doc (#625)
- 4aebc75 add meistermeier as a contributor for doc (#624)
- 99b9f60 add bsideup as a contributor for review (#623)
- e5e89ce add meistermeier as a contributor for code (#622)
- 897be21 Update local changelog.

## 🧹 Housekeeping
- 1a7b7b3 Bump neo4j-harness from 4.4.10 to 4.4.11 (#642)
- 91266e4 Bump error_prone_annotations from 2.12.1 to 2.15.0 (#643)
- 2af5eaa Bump byte-buddy.version from 1.12.14 to 1.12.16 (#644)
- 4e1bf2f Bump checker-qual from 3.24.0 to 3.25.0 (#641)
- 8ef853e Bump mockito.version from 4.7.0 to 4.8.0 (#639)
- c2b6c6f Bump quarkus-neo4j.version from 1.4.1 to 1.5.0
- a3ade49 Bump quarkus.version from 2.12.0.Final to 2.12.1.Final
- e783d4d Bump japicmp-maven-plugin from 0.15.7 to 0.16.0 (#617)
- dde32a3 Bump quarkus.version from 2.11.2.Final to 2.12.0.Final (#611)

## 🛠 Build
- 165bcb8 Add linux-aarch_64 to the release pipeline. (#645)
- d8d1dfe Add all-contributors bot.


# 1.10.1

## 📝 Documentation
- b93d9be Use correct heading level.
- 491b793 Improve intro to naming conventions.

## 🧹 Housekeeping
- 95fdff1 Bump quarkus-neo4j.version from 1.4.0 to 1.4.1
- 3a2c08a Bump quarkus.version from 2.11.2.Final to 2.11.3.Final
- 0f8b5d8 Bump checkstyle from 10.3.2 to 10.3.3 (#615)
- 621db9e Bump maven-checkstyle-plugin from 3.1.2 to 3.2.0 (#614)
- 3a31f05 Bump byte-buddy.version from 1.12.13 to 1.12.14 (#613)
- d30cd1a Bump jreleaser-maven-plugin from 1.1.0 to 1.2.0 (#612)


# 1.10.0

There's an exiting new feature in this release: Predefined database refactorings, such as "rename the label `Movie` into `Film`" or "normalise all properties that have the values `ja`, `yes` and `y` and all of `nein`, `no`, `vielleicht` into a boolean `true` respectively `false`". Those refactorings are largely modeled after [apoc.refactor](https://neo4j.com/labs/apoc/4.4/overview/apoc.refactor/) but *do not* require APOC to be installed and run into pure Cypher. They even do support batching in Neo4j 4.4 or higher.

Some inspiration has been taken from @fbiville's post about [Graph Refactoring: The Hard Way](https://medium.com/neo4j/graph-refactoring-the-hard-way-5762067ead46) for the general initiative and the `Merge.nodes` refactoring. Thanks for that, Florent!

The refactorings of this project here are in [`ac.simons.neo4j.migrations.core.refactorings`](https://github.com/michael-simons/neo4j-migrations/tree/main/neo4j-migrations-core/src/main/java/ac/simons/neo4j/migrations/core/refactorings) and can actually be used without the migrations itself, a connected Neo4j driver instance will be enough to use them. We will maybe extract them into a separate module or even separate library in a new org at a later stage. Until than, they will be part of the publicly maintained API here. Have a look at the document how to use them in catalogs or standalone (full example [here](https://michael-simons.github.io/neo4j-migrations/current/#concepts_migrations_catalog-based) and a list of [predefined refactorings](https://michael-simons.github.io/neo4j-migrations/current/#appendix_refactorings)).

This feature does not have *any* breaking changes to the core, so 1.10.0 is a drop-in replacement of 1.9 and depending if you are using it in Spring Boot or Quarkus and your versions there, for 1.8 or 1.7, too.

## 🚀 Features
- cbd1075 Add refactoring `Normalize.asBoolean`. (#606)
- 2ec53be Add support for running predefined refactorings. (#605)

## 🐛 Bug Fixes
- 36459cc Escape escaped Unicode 0060 (backtick) proper. (#607)

## 🔄️ Refactorings
- 794238f Don't double escape already escaped backticks. (#604)

## 📝 Documentation
- 7ea2bd8 Add JavaDoc where required.
- ff0a922 Update local changelog.

## 🧹 Housekeeping
- 2a1ca61 Bump spring-boot.version from 2.7.2 to 2.7.3 (#610)


## Contributors
We'd like to thank the following people for their contributions:
- @meistermeier


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
- @meistermeier


# 1.6.0

*No breaking changes*, the minor version has been bumped to reflect the Quarkus version bump from 2.8.2 to 2.9.1.

## 🧹 Housekeeping
- f0ebfe4 Bump quarkus.version from 2.8.2.Final to 2.9.1.Final (#489)


# 1.5.6

## 🐛 Bug Fixes
- a04da71 Downgrade GraalVM to 21.3.1. (#492)

## 📝 Documentation
- a3752ec Update local changelog.


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
- @meistermeier


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
- @meistermeier


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
- @meistermeier


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
- @aalmiray
- @meistermeier


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
- @aalmiray
- @meistermeier


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
