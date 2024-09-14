# 为什么叫`Ariel`?

`Ariel`是天王星的一颗卫星,也是莎士比亚剧本《暴风雨》中的角色名

# CLI

```bash
# 获取值
java -jar ariel.jar get [env] <key>

# 设置值
java -jar ariel.jar set [env] <key> <value>

```

# 服务器

作为配置中心,提供`restful`接口,供客户端获取配置

```bash
java -jar ariel.jar serve
```
