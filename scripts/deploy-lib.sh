#!/usr/bin/env bash
op=$1  #only for help

## app.jar lib.jar
standalone=$1
newlib=$2

usage() {
   echo "Usage: deploy-lib.sh <app.jar> <lib.jar>"
   echo "e.g. deploy-lib.sh <app-1.0.0-standlone.jar> <app-1.0.0.jar>" 
}

if [ "$op"x = "-h"x -o "$op"x = "--help"x ];then 
   usage
   exit
fi 

## find standalone
if [ ! $standalone ];then
   list=$(ls *-standalone.jar)
   ## check result
   i=0
   for it in $list;do
       i=$(($i+1))
   done
   if [ $i -eq 1 ];then
       standalone=$list
   fi

   ## 
   if [ ! $standalone ];then
      echo no standalone jar file exists!
      exit
   fi
fi

## find new lib
if [ ! $newlib ];then
   ## skip standalone and get other
   for lib in $(ls *.jar lib/*.jar);do
      skip=$(echo $lib | grep standalone.jar)
      if [ ! $skip ];then
         $newlib=$lib
      fi
   done

   ##
   if [ ! $newlib ];then
     echo no new lib found !
     exit
   fi
fi

## update lib
JAR_BIN=$(which jar)

localputjar() {
    app=$1
    jar=$2  #means lib

    ## check if standalone contains current jar
    jarlib=$(basename $jar)
    jarok=$($JAR_BIN tf $app | grep $jarlib)
    if [ $jarok ];then
       ## do jar 0uf
       jardir=$(dirname $jarok)
       if [ ! -d $jardir ];then
          mkdir -p $jardir
       fi

       echo mv $jar $jardir
       mv $jar $jardir
       echo $JAR_BIN 0uf $app $jarok
       $JAR_BIN 0uf $app $jarok

       ## clean up
       jarroot=${jardir%%\/*}  ## get ONLY the BOOT-INT
       rm -rf $jarroot
       echo done.
    fi
}

localputjar $standalone $newlib
