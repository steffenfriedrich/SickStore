![SickStore](src/main/resources/sickstore.png "SickStore")
=========
<b>S</b>ingle-node <b>i</b>n<b>c</b>onsistent <b>k</b>ey-value <b>store</b> developed to validate measurement methods of NoSQL benchmarking frameworks. Originally developed to validate staleness measurement approaches for our paper titled [Who Watches the Watchmen? On the Lack of Validation in NoSQL Benchmarking](http://subs.emis.de/LNI/Proceedings/Proceedings241/351.pdf), it can also simulate various aspects of system behavior and corresponding anomalies to validate measurement methods.

More information is available at: <http://nosqlmark.informatik.uni-hamburg.de>

## Download

Download the latest Release [sickstore-1.9.2.tar.gz](http://nosqlmark.informatik.uni-hamburg.de/sickstore-1.9.1.tar.gz), [sickstore-1.9.2.zip](http://nosqlmark.informatik.uni-hamburg.de/sickstore-1.9.zip)  or grab dependency via Maven:

```xml
<repository>
    <id>nosqlmark</id>
    <name>NoSQLMark repo</name>
    <url>http://nosqlmark.informatik.uni-hamburg.de/maven2/</url>
</repository>
```

```xml
<dependency>
    <groupId>de.uni-hamburg.informatik.nosqlmark</groupId>
    <artifactId>sickstore</artifactId>
    <version>1.9.1</version>
</dependency>
```

## Building SickStore

SickStore uses [Apache Maven](http://maven.apache.org/) as its build system.
To build, run:

    mvn package

Use `mvn clean package` to first clean the files and directories generated by Maven during its build.
To skip tests run `mvn -DskipTests package`

## Using SickStore
First unpack the archive:

    $ tar -zxvf sickstore-$VERSION.tar.gz
    $ cd sickstore-$VERSION

To start the server you have to provide a configuration YAML:

    $ sh bin/sickstore config/$CONFIG.yml

or under Windows (cmd):

    $ bin\sickstore.bat config/$CONFIG.yml

#### Benchmarking SickStore
If you want to validate your own measurement extension for YCSB (e.g. an approach for measuring time-based staleness) you can use our [YCSB-binding](https://github.com/steffenfriedrich/YCSB) or use the sickstore-client in your own project by adding the maven dependency.
