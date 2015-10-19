To import into Intellij
=======================
./gradlew --daemon assemble
./gradlew --daemon idea
DO NOT "IMPORT PROJECT" this will break things
In Intellij import Tracerunner.ipr

To make it build before running it in the ide:
go to gradle tab on left side and expand
Tracerunner > Tasks > build
right click on assemble and click "execute before make"

