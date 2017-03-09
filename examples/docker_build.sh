MAVEN_VERSION=3.3-jdk-8
docker run -it --rm -v "$PWD":/usr/src/mymaven -v "$HOME/.m2:/usr/src/mymaven/.m2" -w /usr/src/mymaven maven:${MAVEN_VERSION} mvn clean package