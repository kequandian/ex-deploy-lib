# ex-deploy-lib
用于生产环境快速部署 模块lib

#### 用法
- 在生产环境服务器克隆
```shell
$ git clone https://github.com/kequandian/ex-deploy-lib
```
- 配置environment `DL_ROLLBACK`环境变量，以及volumes生产环境`api`目录映射(e.g. /home/xing/cinema/api)
```shell
$ cat docker-compose.yml
    environment:
       DL_ROLLBACK: 'gmic-cad-artifact-1.0.0-standalone.jar'
    volumes:
      - /home/xing/cinema/api:/webapps
```

- 通过scp上传模块至 ex-deploy-lib/api/lib
> 或通过 winscp 上传

- 设置Standalone Docker容器环境变量，同时并执行部署脚本
> 完成后手动执行 `docker-compose restart api`
```shell
$ export DOCKER_CONTAINER='api' && sh ex-deploy-lib.sh
docker-compose restart api
```
