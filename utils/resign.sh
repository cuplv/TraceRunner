#!/usr/bin/env bash
zip -d $1 'META-INF/*.SF' 'META-INF/*.RSA' 'META-INF/*.MF'
# note password is aaaaaa since this is not a production key and it doesn't matter if anyone steals it
echo "aaaaaa" |jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore ~/.keystore/recompiled.keystore $1 recompiled
