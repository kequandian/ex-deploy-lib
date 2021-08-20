#!/bin/sh

## export 
# export DOCKER_CONTAINER='api'
####


## main
container=${DOCKER_CONTAINER}

if [ ! $container ];then
   echo 'Usage: export DOCKER_CONTAINER='api' && sh ex-deploy-lib.sh'
   exit
fi

docker-compose run --rm deploy  

## restart
echo docker-compose restart $container
