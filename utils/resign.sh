#!/usr/bin/env bash
zip -d $1 'META-INF/*.SF' 'META-INF/*.RSA' 'META-INF/*.MF'
echo "password" |jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore ~/.keystore/recompiled.keystore $1 recompiled
