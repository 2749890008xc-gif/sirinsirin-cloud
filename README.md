# sirinsirin 视频交流平台 服务端代码
本项目为sirinsirin视频交流平台服务端代码，本项目采用微服务架构，其中：
1. sirinsirin-cloud-admin为管理员模块，用于处理管理员端的各种请求；
2. sirinsirin-cloud-web为web模块，用于处理用户端的各种请求；
3. sirinsirin-cloud-base为主依赖模块，用于规定日志等信息；
4. sirinsirin-cloud-common为公共依赖模块，用于为各模块提供依赖；
5. sirinsirin-cloud-gateway为网关模块，主要负责网关拦截与过滤；
6. sirinsirin-cloud-interact为互动行为模块，主要负责针对用户的评论、点赞、投币、收藏等行为进行操作；
7. sirinsirin-cloud-resource为资源模块，用于负责处理用户上传的图片、视频等文件资源。
