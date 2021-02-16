# lolicon-api-cache
Lolicon Api Cache，带有图片缓存的代理
### 使用的[Api地址](https://api.lolicon.app/#/setu)
## 配置说明
- api-key: '' 你的apikey
- cache: 10 图片缓存数量
- size1200: true 是否压缩图片
- auto-delete-download-image: false 使用过的图片自动删除
- reuse-image: true 当日调用额度不足时是否重用使用过的图片
- confounding: true 图片最后一比特随机

默认8080端口，如上配置可写入yml中，也可使用/config接口配置

## 启动
Java版本为1.8，检查Java版本请在终端中输入```java -version```

在终端中使用 ```java -jar <application-name>.jar```启动应用

若要修改默认配置，可修改同目录的yml文件
