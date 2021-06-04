#!/bin/sh
cd $(dirname $0)

cd ../complete

./mvnw clean package test
ret=$?
if [ $ret -ne 0 ]; then
exit $ret
fi
rm -rf target

./gradlew build test
ret=$?
if [ $ret -ne 0 ]; then
exit $ret
fi
rm -rf build

cd ../initial

./mvnw clean compile
ret=$?
if [ $ret -ne 0 ]; then
exit $ret
fi
rm -rf target

./gradlew compileJava
ret=$?
if [ $ret -ne 0 ]; then
exit $ret
fi
rm -rf build

exit
