if [ ! -f ../jar-dependency-api/target/jar-dependency-api-1.0.0-standalone.jar ];then
  cd ../jar-dependency-api
  mvn package
  cd ../jar-dependency
fi

jar1=/Users/vincenthuang/workspace/github.com/zero-io/zero-io-fs/target/zero-io-fs-1.0.0-standalone.jar
jar2=/Users/vincenthuang/workspace/github.com/ex-deploy-lib/jar-dependency/target/jar-dependency.jar

# mvn package && java -jar target/jar-dependency.jar -cmj ../jar-dependency-api/target/jar-dependency-api-1.0.0-standalone.jar  ../jar-dependency-api/target/jar-dependency-api-1.0.0.jar
#mvn package
java -jar target/jar-dependency.jar $jar1 $jar2 $1 $2
