SickStore
=========

## Using Sickstore as M

## Building SickStore

SickStore is built using [Apache Maven](http://maven.apache.org/).
To build SickStore run:

    mvn -DskipTests clean package


Download
--------
Download [the latest JAR](http://nosqlmark.informatik.uni-hamburg.de/nosqlmark/) or grab via Maven:

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
    <version>1.2</version>
</dependency>
```
    
Contribute
----------

To publish a new SickStore version ([Articel on javaworld.com](http://www.javaworld.com/article/2073230/maven-repository-in-three-steps.html).):

    mvn install -DperformRelease=true -DcreateChecksum=true
        
This will create all necessary folder structures in your local Maven2 repository 
(which is located ~/.m2/repository/de/uni-hamburg/informatik/nosqlmark/sickstore, and will install all artifacts there (along with md5 and sha1 checksums). 

Next copy the folder branch (de/.../sickstore/[versionnumber])  with the installed library from your local repository to the maven2 folder of the HTTP server.
And rename the file maven-metadata-local.xml to maven-metadata.xml and create checksums for it: 

    cd maven2/de/uni-hamburg/informatik/nosqlmark/sickstore/
    mv maven-metadata-local.xml maven-metadata.xml
    md5sum maven-metadata.xml > maven-metadata.xml.md5
    sha1sum maven-metadata.xml > maven-metadata.xml.sha1
    
Just make sure that the repository is accessible from the browser. Try to go to this URL go to http://nosqlmark.informatik.uni-hamburg.de/maven2/de/uni-hamburg/informatik/nosqlmark/sickstore/1.1/
    