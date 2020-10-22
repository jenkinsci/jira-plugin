VER=${1:-lts}
IMAGE=jenkins/jenkins:$VER
CONTAINER_NAME="jenkins_$VER"

echo "Starting container $CONTAINER_NAME from $IMAGE"
echo

set -x
docker run -d --name $CONTAINER_NAME -p 8080:8080 $IMAGE
set +x

echo

docker start $CONTAINER_NAME
docker logs -f $CONTAINER_NAME

echo
echo "Daemonized container $CONTAINER_NAME"
echo "Access logs with:"
echo
echo "docker logs -f $CONTAINER_NAME"

