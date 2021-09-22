# 服务单元依赖装配

>**主要为实现为Standalone JAR包依赖情况的维护，便于新增服务单元注入依赖，确保Standalone依赖的清晰化和可见化。**

## 1. 流程

![ackTPJ.png](https://s1.ax1x.com/2020/08/06/ackTPJ.png)

- 本地用户根据DependencyUtil.jar包工具生成目标Jar包的**Dependency JSON**；

- 用户根据**Dependency JSON**调用下述`/api/jar/dependencies/match`接口获取依赖匹配情况；

- 用户根据上述接口返回信息处理完成后，调用下述`/api/jar/dependencies/inject`并上传**Dependency JSON**完成依赖装配；

    >**TODO：`/api/jar/dependencies/inject`接口可能存在数据破坏情况，即当前接口未进行常规依赖安全检查，后续可进行完善。**

- 用户可通过`/api/jar/dependencies/{appId}`接口获取最新**Dependency JSON**内容。

## 2. API接口

|              **请求路径**              | **请求方法** |      **请求参数**      |                    **说明**                     |
| :------------------------------------- | :----------: | :--------------------: | :---------------------------------------------- |
|   /api/jar/dependencies/inject    |   **POST**   | **详见下部分请求JSON** | 上传待注入的依赖(Dependency) JSON，由本接口进行分析、判断及注入 |
|       /api/jar/dependencies/{appId}       |   **GET**    |           应用标识ID           |     根据应用标识ID获取依赖（Dependency）详情（JSON）      |
| /api/jar/dependencies/match |   **POST**   | **详见下部分请求JSON** | 根据参数请求体参数verbose返回上传的JAR包依赖中与源JAR包中匹配或不匹配的依赖 |

- **请求JSON**

```json
{
    "appId":"UjQhM2mOOR7I",
	"dependencies":[
		"spring-boot-starter-amqp-1.5.6.RELEASE.jar",
		"spring-boot-starter-1.5.6.RELEASE.jar",
		"spring-boot-1.5.6.RELEASE.jar",
		".......",
		"activation-1.1.1.jar",
		"aspectjweaver-1.9.4.jar"
	],
    "verbose":"true"
}
```

其中verbose参数用于匹配`/api/jar/dependencies/match`接口根据参数值返回对应数据，`verbose:true`则返回不匹配依赖，`verbose:false`则返回匹配依赖。

## 3. 数据表设计

- **t_jar记录表**

|   **字段**   |   **类型**   |   **说明**    |
| :----------: | :----------: | :-----------: |
|      id      |  bigint(20)  |    记录ID     |
|    app_id    | varchar(128) |  应用标识ID   |
|     name     | varchar(255) | JAR包记录名称 |
|    status    | varchar(26)  | 当前记录状态  |
| dependencies |     json     | JAR包依赖JSON |
| create_time  |   datetime   | 记录创建时间  |
| update_time  |   datetime   | 记录更新时间  |

## 4. 请求示例

### a. 获取依赖信息

根据应用标识ID获取依赖（Dependency）详情（JSON）。


- **请求方式：GET**
- **请求地址：<u>/api/jar/dependencies/{appId}</u>**

> ***注：此处标注的占位符{appId}，为需要替换的 变量，根据实际获取值更新。其它接口也将采用相同的标注，后续将不再声明。***


- **参数说明：**

| **参数** | **必须** |      **说明**      |
| :------: | :------: | :----------------: |
|  appId   |    是    | 待查询的应用标识ID |

- **返回结果：** 

```json
{
    "code": 200,
    "message": "Success",
    "data": {
        "id": 1,
        "appId": "UjQhM2mOOR7I",
        "name": "standalone.jar",
        "status": "stat",
        "listDependencies": [
            "spring-boot-starter-amqp-1.5.6.RELEASE.jar",
		   ".......................",
            "activation-1.1.1.jar",
            "aspectjweaver-1.9.4.jar",
            "test-inject.jar"
        ]
    }
}
```

### b. 匹配依赖

返回上传的JAR包依赖中与源JAR包中不匹配的依赖。


- **请求方式：POST**

- **请求地址：<u>/api/jar/dependencies/match</u>**

- **请求参数：**

```json
{
    "appId":"UjQhM2mOOR7I",
	"dependencies":[
		"spring-boot-starter-amqp-1.5.6.RELEASE.jar",
		"spring-boot-starter-1.5.6.RELEASE.jar",
		"spring-boot-1.5.6.RELEASE.jar",
		"activation-1.1.1.jar",
		"aspectjweaver-11.9.4.jar",
                "1.jar"
	],
    "verbose":"true"
}
```

- **返回结果：** 

```json
{
    "code": 200,
    "message": "Success",
    "data": [
        "aspectjweaver-11.9.4.jar",
        "1.jar"
    ]
}
```

### c. 注入依赖

上传待注入的依赖(Dependency) JSON，由本接口进行分析、判断及注入。


- **请求方式：POST**

- **请求地址：<u>/api/jar/dependencies/inject</u>**

- **请求参数：**

```json
{
    "appId":"UjQhM2mOOR7I",
	"dependencies":[
            "test-inject.jar"
	]
}
```

- **返回结果：** 

```json
{
    "code": 200,
    "message": "Success",
    "data": null
}
```


## 5. 疑难点

- 本地生成Dependencies JSON格式？<span style="color:green;font-weight:bold">【DONE】</span>
    - 自定义模块 **（jar-dependency-parse）**

- 云端 **依赖（Dependency）** 如何存储？<span style="color:green;font-weight:bold">【DONE】</span>
    - MySQL -> JSON
- **依赖（Dependency）** 比对的逻辑<span style="color:green;font-weight:bold">【DONE】</span>
    - 如何处理目标Jar包不存在Standalone依赖情况？
        - 返回缺失信息
    - 如何处理Standalone依赖缺失目标Jar包依赖情况？
        - 返回缺失信息                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   
