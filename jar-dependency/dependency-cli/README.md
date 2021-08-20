# dependency-cli

## 使用方法

```shell
$ dependency

Usage: dependency Options [Variables...]
e.g. dependency -p ./lib/test.jar
用于Jar包依赖输出 和 Maven module依赖对比

Options:
  -c, --compare </path/to/module1> </path/to/module2> 对比两个Maven module依赖情况
    -> -b --boolean 判断module1 / jar1 是否能够装配入 module2 / jar2中 
  -p, --parse </path/to/the-app.jar> [...]  解析Jar包依赖并输出
  -j, --JSON 输出为JSON格式
  -v, --version  输出当前工具版本信息
```
