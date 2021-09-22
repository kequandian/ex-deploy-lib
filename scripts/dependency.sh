#!/bin/bash
option=$1

########dependency-cli.jar path########
jar_path=$(dirname $(readlink -f $0))/lib/dependency-cli.jar
compare='^-c[jb]?$'
parse='^-p[j]?$'
download='^-d$'
version='^-v$'
#############debug############
#echo jar_path=${jar_path}
#echo option=${option}
##############################

if [ ! -f $jar_path ]
then
  echo "dependency: 无法获取jar包: $jar_path"
  exit;
fi

usage(){
	cat <<- EOF
	
	Usage: dependency Options [Variables...]
	e.g. dependency -p ./lib/test.jar
	用于Jar包依赖输出 和 Maven module or Jar依赖对比

	Options:
	  -c, --compare </path/to/module1> </path/to/module2> 对比两个Maven module or Jar依赖情况
        -> -b --boolean 判断module1 / jar1 是否能够装配入 module2 / jar2中
	  -d  --download 根据参数groupId:artifactId:Version下载依赖文件到当前目录
	  -j, --JSON 输出为JSON格式
	  -p, --parse </path/to/the-app.jar> [...]  解析Jar包依赖并输出
	  -v, --version  输出当前工具版本信息
	EOF
	exit
}


if [ ! $option ]
then
	usage
	exit
fi

if [[ "$option" =~ $compare ]]
then
	java -jar $jar_path $option $(readlink -f "$2") $(readlink -f "$3")
elif [[ "$option" =~ $parse ]]
then
	java -jar $jar_path $option $(readlink -f "$2")
elif [[ "$option" =~ $version ]]
then
	java -jar $jar_path $*
elif [[ "$option" =~ $download ]]
then
	artifact=$2
	if [ ! $artifact ]
	then
		usage
		exit
	fi
	mvn dependency:get -Dartifact=$artifact -Ddest=./
fi