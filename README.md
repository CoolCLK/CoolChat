# Cool Chat

[[English](README-english.md)] [[简体中文](.)]

---

感谢你使用 **CoolChat** ！欢迎修改成 ***你自己的版本*** ！

### - 如何使用/部署？

在 Realses 那里找到你想要的版本解压后双击 **launch.cmd** 就可以了~

### - 配置文件看不懂？

在 **config** 文件夹里你可以修改***任何配置文件***，不过***一般***只修改 **application.properties** 与 **coolchat.yml** ~

关于 **application.properties** 属于 **SpringBoot** 的配置文件，大家可以去***百度***、***必应***等去搜索怎么配置~

下面是 **coolchat.yml** 的注解~

```yaml
# 账号设置
account:
  # 账号密钥（应该是这么叫吧？
  session:
    # 账号密钥有限期，以秒为单位
    max-life: 604800

# 聊天设置
chat:
  # 分享文件部分
  file:
    # 最大可分享文件，以比特做单位
    max-size: 26214400
```

---

**分享文件**其实不是***上传文件***到服务器中，而是**在线发送文件的数据**！

**限制文件大小*****现在***只是在代码中***判断是否发送***而已
