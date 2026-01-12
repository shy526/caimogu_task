# 踩蘑菇每天7点影响力

## 配置方式
1.  fork仓库

2. 配置四个仓库参数
   - `OWNER_REPO` 用户名/仓库名
   - ~~`CAI_MO_GU_TOKEN`~~ ~~踩蘑菇的Token cmg_token~~
   - `MY_GITHUB_API_TOKEN` githubapiToken 需要读写权限
   - `CMG_NAME` 踩蘑菇帐号名
   - `CMG_PASSWORD` 踩蘑菇密码

~~3. 删除文件~~
   - ~~run.txt~~     ~~(记录上一次运行时间)~~
   - ~~acIds.txt~~   ~~(记录已经评价的游戏)~~
   - ~~postIds.txt~~  ~~(记录已经评论的帖子)~~
   - ~~gameComment.txt~~  ~~(记录评论游戏库评论)~~

## 更新

- 2025.12.10
  - 帖子回复
- 2025.12.12
  - cmgToken问题修复(10-12号之间fork的需要重新修改)
  - 个人主页问题修复
  - 触发器恢复
- 2026-01-11
  - 基于h5接口的自动登录
  - 添加调用h5接口的代码
  - 复刻js签名算法
  - 流程调整
  - 增加对游戏库评论评论
  - 最后一次更新
  - 严格检查可获取影响力频道
  - 重试机制

- 2026-01-12
  - 签名错误问题首字母+自动被转换为空格问题
  - 自动清理非本用户的记录文件

## 踩蘑菇接口(以下是该项目用到的所有接口和html)

- https://www.caimogu.cc/game/find.html?act=fetch&date=2024-07&sort=1&sort_desc=1&page=1
  - 游戏库列表
  
- https://www.caimogu.cc/game/act/score
  - 游戏评分
  
- https://www.caimogu.cc/user/my.html
  - 个人主页
  
- https://www.caimogu.cc/user/act/my_list?act=reply&page=3
  - 个人主页回复列表
  
- https://www.caimogu.cc/circle/act/post_list?id=449&kwType=post&kw=&type=all&topic=&tags=&page=1
  - 莫个圈子中帖子列表
  
- https://www.caimogu.cc/post/comments?id=2264113&pid=0&order=default&page=1
  - 帖子中评论列表
  
- https://www.caimogu.cc/post/act/comment
  - 评论帖子


### h5接口

- https://api.caimogu.cc/v2/game/commentList
  - 游戏评论列表

- https://api.caimogu.cc/v1/game/commentReply
  - 评论游戏库评论
  
- https://api.caimogu.cc/v2/game/score
  - 评论游戏

- https://api.caimogu.cc/v3/post/comment/list
  - 圈子评论列表

- https://api.caimogu.cc/v2/game/list
  - 游戏库列表

- https://api.caimogu.cc/v3/circle/detail/list
  - 圈子中的帖子列表

- https://api.caimogu.cc/v3/post/reply
  - 评价帖子

- https://api.caimogu.cc/v3/my/reply/list
  - 个人主页回复列表
  
-  https://api.caimogu.cc/v3/my/info
   - 用户信息

- https://api.caimogu.cc/v3/login/account
  - 登录