dependencies:
  pre: 
   - sudo service docker start; sudo service docker status; sudo docker -v; sudo docker info; 
   
  post:
   - sudo docker build -t=jira_plugin_build .
   - sudo docker run --name jira_plugin_build -v /home/ubuntu:/usr/src/app -d jira_plugin_build
   - sudo docker save -o $CIRCLE_ARTIFACTS/jira_plugin_build.tar jira_plugin_build

deployment:
  dockerhub:
    branch: master
    commands:
      - $DOCKER_HUB_TRIGGER
      
test:
  override:
  - docker images | grep jira_plugin_build 
