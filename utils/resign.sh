#!/usr/bin/env bash
echo "password" |jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore ~/.keystore/recompiled.keystore $1 recompiled
