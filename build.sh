#!/bin/zsh
# change the version
mvn release:prepare

BUILD_VERSION_WITH_SUFFIX="$(mvn help:evaluate -Dexpression=project.version | grep -e '^[^\[]')"
BUILD_VERSION="$(echo $BUILD_VERSION_WITH_SUFFIX | grep -o '[0-9]\+\.[0-9]\+\.[0-9]\+')"
echo "Will build $BUILD_VERSION ..."

# build jar package
mvn clean package assembly:single

docker build --build-arg VERSION="$BUILD_VERSION" -t router:$BUILD_VERSION .

# restore local artifacts
mvn clean install

rm ./pom.xml.releaseBackup