if [ ! -f ../jar-dependency-api/target/jar-dependency-api-1.0.0-standalone.jar ];then
  cd ../jar-dependency-api
  mvn package
  cd ../jar-dependency
fi

mvn package && java -jar target/jar-dependency.jar -cmj ../jar-dependency-api/target/jar-dependency-api-1.0.0-standalone.jar  ../jar-dependency-api/target/jar-dependency-api-1.0.0.jar
