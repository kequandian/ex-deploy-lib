#!/usr/bin/bash

## customer config for deploy target ###########
#export DEPLOYLESS_TARGET='ems@mall.smallsaas.cn:/home/ems/am'
################################################

#### split from target  below ### 
target=${DEPLOYLESS_TARGET}
if [ ! $target ];then
  echo env DEPLOYLESS_TARGET not exported
  exit
fi

app_path=${target##*:}  ## cur before :
ssh_host=${target%%:*}
## debug
#echo ssh_host= $ssh_host 
#echo app_path= $app_path
#echo ssh $ssh_host \"cd $app_path/api \&\& sh docker-deploy-lib.sh\"
#exit
## end debug ####


## support multi modules
mod_list=$@

if [  $# == 0 ];then
   echo 'Usage: deployless <module> [module2] [...]'
   echo '  e.g. deployless am-fault am-ticket'
   exit
fi


## deploy web
deploy_web() {
    ## means web, check dist
   if [ ! -d dist ];then
      echo you try to deply web, but dist not exists
      exit
   fi

   ## package dist with tar
   echo tar -cvf dist.tar dist 
   tar -cvf dist.tar dist
   echo scp dist.tar $target/web
   scp dist.tar $target/web
   ## clean after scp
   echo rm dist.tar
   rm dist.tar

   echo ssh $ssh_host \"cd $app_path/web \&\& sh deploy.sh\"
   ssh $ssh_host "cd $app_path/web && sh deploy.sh"
}


## deploy lib
deploy_lib() {
  list=()
  for jar in $(ls target/*.jar);do
     if [ $jar == target/*standalone.jar ];then
        echo $jar >/dev/null
     else
       list="$list $jar"
     fi
  done

  if [ ! $list ];then 
     echo no .jar found !
     exit
  fi


  echo scp $list $target/api/lib
  scp $list $target/api/lib

  ### comment for batch deploy
  ##echo ssh $ssh_host \"cd $app_path/api \&\& sh docker-deploy-lib.sh\"
  ##ssh $ssh_host "cd $app_path/api && sh docker-deploy-lib.sh"
}


## main  ##
## ensure go into pack root first
#pack_dir=$(dirname $0)
#cd $pack_dir
#cd ..

## flag deploy lib
deploy_ok=()
for mod in $mod_list;do
   echo $mod

   if [ -d $mod ];then 
      cd $mod 

      if [ -f package.json ];then
         deploy_web
      else
         deploy_ok='ok'
         deploy_lib
      fi

      cd ..
   fi
done


## deploy finally
if [ $deploy_ok ];then
   echo ssh $ssh_host \"cd $app_path/api \&\& sh docker-deploy-lib.sh\"
   ssh $ssh_host "cd $app_path/api && sh docker-deploy-lib.sh"
fi

# done
echo Done


