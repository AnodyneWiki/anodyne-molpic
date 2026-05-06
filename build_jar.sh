#!/bin/bash

groovyc -cp "lib/*" -d bin src/*.groovy

mkdir -p tmp_jar
cp -r bin/* tmp_jar/

for f in lib/*.jar; do
	(cd tmp_jar && jar xf ../"$f")
done

echo "Main-Class: Main" > tmp_jar/MANIFEST.MF

jar cfm molpic.jar tmp_jar/MANIFEST.MF -C tmp_jar .
