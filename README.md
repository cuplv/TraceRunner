To import into Intellij
=======================

Download json-simple-1.1.1.jar into libs directory:
https://search.maven.org/#artifactdetails%7Ccom.googlecode.json-simple%7Cjson-simple%7C1.1.1%7Cbundle
(TODO: figure out why gradle is broken again and add this dependency properly)

./gradlew --daemon assemble
./gradlew --daemon idea
DO NOT "IMPORT PROJECT" this will break things
In Intellij import Tracerunner.ipr

To make it build before running it in the ide:
go to gradle tab on left side and expand
Tracerunner > Tasks > build
right click on assemble and click "execute before make"

