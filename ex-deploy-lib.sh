#!/bin/sh

## export 
export STANDALONE_CONTAINER='cinema-api'
####


## main
container=${STANDALONE_CONTAINER}

if [ ! $container ];then
   echo 'Usage: ex-deploy-lib.sh <container-name>'
   exit
fi

docker-compose run --rm deploy  

## restart
echo docker-compose restart $container
