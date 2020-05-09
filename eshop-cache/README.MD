#三级缓存架构：EhCache本地堆缓存 + redis分布式缓存 + nginx本地缓存

### spring boot整合 ehcache

因为之前跟大家提过，三级缓存，服务本地堆缓存 + redis分布式缓存 + nginx本地缓存 组成的

每一层缓存在高并发的场景下，都有其特殊的用途，需要综合利用多级的缓存，才能支撑住高并发场景下各种各样的特殊情况

服务本地堆缓存，作用：预防redis层的彻底崩溃，作为缓存的最后一道防线，避免数据库直接裸奔

服务本地堆缓存，我们用什么来做缓存，难道我们自己手写一个类或者程序去管理内存吗？？？java最流行的缓存的框架，ehcache

所以我们也是用ehcache来做本地的堆缓存

spring boot + ehcache整合起来，演示一下是怎么使用的

（1）依赖
```
    <dependencies>
        <dependency>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
          <groupId>org.springframework</groupId>
          <artifactId>spring-context-support</artifactId>
        </dependency>
        <dependency>
          <groupId>net.sf.ehcache</groupId>
          <artifactId>ehcache</artifactId>
          <version>2.8.3</version>
        </dependency>
    </dependencies>
```

（2）缓存配置管理类

```
@Configuration
@EnableCaching
public class CacheConfiguration {

    @Bean
    public EhCacheManagerFactoryBean ehCacheManagerFactoryBean(){
      EhCacheManagerFactoryBean cacheManagerFactoryBean = new EhCacheManagerFactoryBean();
      cacheManagerFactoryBean.setConfigLocation(new ClassPathResource("ehcache.xml"));
      cacheManagerFactoryBean.setShared(true);
      return cacheManagerFactoryBean;
    }
   
    @Bean
    public EhCacheCacheManager ehCacheCacheManager(EhCacheManagerFactoryBean bean){
      return new EhCacheCacheManager(bean.getObject());
    }
     
}
```

（3）ehcache.xml

```
<?xml version="1.0" encoding="UTF-8"?>
<ehcache xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:noNamespaceSchemaLocation="http://ehcache.org/ehcache.xsd"
    updateCheck="false">
  
    <diskStore path="java.io.tmpdir/Tmp_EhCache" />
    
    <defaultCache
        eternal="false"
        maxElementsInMemory="1000"
        overflowToDisk="false"
        diskPersistent="false"
        timeToIdleSeconds="0"
        timeToLiveSeconds="0"
        memoryStoreEvictionPolicy="LRU" />
 
    <cache
        name="local"  
        eternal="false"
        maxElementsInMemory="1000"
        overflowToDisk="false"
        diskPersistent="false"
        timeToIdleSeconds="0"
        timeToLiveSeconds="0"
        memoryStoreEvictionPolicy="LRU" />
      
</ehcache>
```
（4）CacheService

```
@Service("cacheService")  
public class CacheServiceImpl implements CacheService {
   
    public static final String CACHE_NAME = "local";
    
    @Cacheable(value = CACHE_NAME, key = "'key_'+#id")
    public ProductInfo findById(Long id){
       return null;
    }
   
    @CachePut(value = CACHE_NAME, key = "'key_'+#productInfo.getId()")
    public ProductInfo saveProductInfo(ProductInfo productInfo) {
      return productInfo;
    }
     
}
```

（5）写一个Controller测试一下ehcache的整合

```
@Controller
public class CacheTestController {

  @Resource
  private CacheService cacheService;
  
  @RequestMapping("/testPutCache")
  @ResponseBody
  public void testPutCache(ProductInfo productInfo) {
    System.out.println(productInfo.getId() + ":" + productInfo.getName());  
    cacheService.saveProductInfo(productInfo);
  }
  
  @RequestMapping("/testGetCache")
  @ResponseBody
  public ProductInfo testGetCache(Long id) {
    ProductInfo productInfo = cacheService.findById(id);
    System.out.println(productInfo.getId() + ":" + productInfo.getName()); 
    return productInfo;
  }
  
}
```
ehcache已经整合进了我们的系统，spring boot

封装好了对ehcache本地缓存进行添加和获取的方法和service



### spring boot整合 Kafka
1、将kafka整合到spring boot中
```
<!-- Kafka -->
<dependency>
   <groupId>org.springframework.kafka</groupId>
   <artifactId>spring-kafka</artifactId>
</dependency>
```

```
@Bean
public ServletListenerRegistrationBean servletListenerRegistrationBean() {
	ServletListenerRegistrationBean servletListenerRegistrationBean = 
			new ServletListenerRegistrationBean();
	servletListenerRegistrationBean.setListener(new InitListener());  
	return servletListenerRegistrationBean;
}

public class KafkaConcusmer implements Runnable {

    private final ConsumerConnector consumer;
    private final String topic;
 	
    public ConsumerGroupExample(String topic) {
        consumer = Consumer.createJavaConsumerConnector(createConsumerConfig());
        this.topic = topic;
    }

    private static ConsumerConfig createConsumerConfig(String a_zookeeper, String a_groupId) {
        Properties props = new Properties();
        props.put("zookeeper.connect", "192.168.31.187:2181,192.168.31.19:2181,192.168.31.227:2181");
        props.put("group.id", "eshop-cache-group");
        props.put("zookeeper.session.timeout.ms", "400");
        props.put("zookeeper.sync.time.ms", "200");
        props.put("auto.commit.interval.ms", "1000");
        return new ConsumerConfig(props);
    }
 	
    public void run() {
        Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
        topicCountMap.put(topic, 1);
        Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumer.createMessageStreams(topicCountMap);
        List<KafkaStream<byte[], byte[]>> streams = consumerMap.get(topic);

        for (final KafkaStream stream : streams) {
            new Thread(new KafkaMessageProcessor(stream)).start();
        }
    }

}

public class KafkaMessageProcessor implements Runnable {

    private KafkaStream kafkaStream;
 
    public ConsumerTest(KafkaStream kafkaStream) {
    	this.kafkaStream = kafkaStream;
    }
 
    public void run() {
        ConsumerIterator<byte[], byte[]> it = kafkaStream.iterator();
        while (it.hasNext()) {
        	String message = new String(it.next().message());
        }
    }

}

ServletContext sc = sce.getServletContext();
ApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(sc);
```

2、编写业务逻辑

（1）两种服务会发送来数据变更消息：商品信息服务，商品店铺信息服务，每个消息都包含服务名以及商品id

（2）接收到消息之后，根据商品id到对应的服务拉取数据，这一步，我们采取简化的模拟方式，就是在代码里面写死，会获取到什么数据，不去实际再写其他的服务去调用了

（3）商品信息：id，名称，价格，图片列表，商品规格，售后信息，颜色，尺寸

（4）商品店铺信息：其他维度，用这个维度模拟出来缓存数据维度化拆分，id，店铺名称，店铺等级，店铺好评率

（5）分别拉取到了数据之后，将数据组织成json串，然后分别存储到ehcache中，和redis缓存中

3、测试业务逻辑

（1）创建一个kafka topic

（2）在命令行启动一个kafka producer

（3）启动系统，消费者开始监听kafka topic

C:\Windows\System32\drivers\etc\hosts

（4）在producer中，分别发送两条消息，一个是商品信息服务的消息，一个是商品店铺信息服务的消息

（5）能否接收到两条消息，并模拟拉取到两条数据，同时将数据写入ehcache中，并写入redis缓存中

（6）ehcache通过打印日志方式来观察，redis通过手工连接上去来查询


### 基于 “分发层+应用层” 双层 nginx 架构提升缓存命中率方案分析

1、缓存命中率低

一般会部署多个nginx，但是在负载均衡的情况下，各个nginx缓存命中率是比较低的

2、如何提升缓存命中率

分发层+应用层，双层nginx

分发层nginx，负责流量分发的逻辑和策略，这个里面它可以根据你自己定义的一些规则，比如根据productId去进行hash，然后对后端的nginx数量取模

将某一个商品的访问的请求，就固定路由到一个nginx后端服务器上去，保证说只会从redis中获取一次缓存数据，后面全都是走nginx本地缓存了

后端的nginx服务器，就称之为应用服务器; 最前端的nginx服务器，被称之为分发服务器

看似很简单，其实很有用，在实际的生产环境中，可以大幅度提升你的nginx本地缓存这一层的命中率，大幅度减少redis后端的压力，提升性能


3、使用lua实现请求定向(根据商品id进行hash)分发【因为使用lua所以选用Openresty】

（1）Openresty+lua开发的hello world

    vi /usr/servers/nginx/conf/nginx.conf
    
    在http部分添加：
    
    lua_package_path "/usr/servers/lualib/?.lua;;";  
    lua_package_cpath "/usr/servers/lualib/?.so;;";  
    
    /usr/servers/nginx/conf下，创建一个lua.conf
    server {  
        listen       80;  
        server_name  _;  
    }
    
    在nginx.conf的http部分添加：
    include lua.conf;
    
    验证配置是否正确：/usr/servers/nginx/sbin/nginx -t
    
    在lua.conf的server部分添加：
    
    location /lua {  
        default_type 'text/html';  
        content_by_lua 'ngx.say("hello world,this is eshop-cache01")';  
    } 
    
    验证配置是否正确：/usr/servers/nginx/sbin/nginx -t
    
    重新nginx加载配置
    /usr/servers/nginx/sbin/nginx -s reload  
    
    访问http: http://192.168.31.187/lua
    
    vi /usr/servers/nginx/conf/lua/test.lua
    ngx.say("hello world"); 
    
    修改lua.conf
    location /lua {  
        default_type 'text/html';  
        content_by_lua_file conf/lua/test.lua; 
    }
    
    查看异常日志
    
    tail -f /usr/servers/nginx/logs/error.log

   如法炮制，在另外2个机器上，分别用OpenResty部署eshop-cache02、eshop-cache03


大家可以自己按照上一讲讲解的内容，基于OpenResty在另外两台机器上都部署一下nginx+lua的开发环境

我已经在eshop-cache02和eshop-cache03上都部署好了

我这边的话呢，是打算用eshop-cache01和eshop-cache02作为应用层nginx服务器，用eshop-cache03作为分发层nginx

在eshop-cache03，也就是分发层nginx中，编写lua脚本，完成基于商品id的流量分发策略

当然了，我们这里主要会简化策略，简化业务逻辑，实际上在你的公司中，你可以随意根据自己的业务逻辑和场景，去制定自己的流量分发策略

1、获取请求参数，比如productId
2、对productId进行hash
3、hash值对应用服务器数量取模，获取到一个应用服务器
4、利用http发送请求到应用层nginx
5、获取响应后返回

这个就是基于商品id的定向流量分发的策略，lua脚本来编写和实现

我们作为一个流量分发的nginx，会发送http请求到后端的应用nginx上面去，所以要先引入lua http lib包

cd /usr/hello/lualib/resty/  
wget https://raw.githubusercontent.com/pintsized/lua-resty-http/master/lib/resty/http_headers.lua  
wget https://raw.githubusercontent.com/pintsized/lua-resty-http/master/lib/resty/http.lua 

代码：
```
    -- ngx.say("hello world,this is eshop-cache01")
    
    -- 从请求查询串中获取productId
    local uri_args = ngx.req.get_uri_args()
    local productId = uri_args["productId"]
    
    -- 请求分发
    local host = {"192.168.11.103", "192.168.11.104"}   -- 两台应用层nginx
    local hash = ngx.crc32_long(productId)			    -- 根据productId进行hash路由
    local index = (hash % 2) + 1   						-- lua的索引是从1开始的，所以取模后要+1
    backend = "http://"..host[index]    				-- 目标服务器
    
    
    -- 向目标主机发请求
    local http = require("resty.http")    -- 加载所需模块，需要先在配置文件导入lua http lib包
    local httpc = http.new()			  -- 创建http客户端
    
    local requestPath = uri_args["requestPath"]					-- 构造请求路径
    requestPath = "/"..requestPath.."?productId="..productId
    
    local resp, err = httpc:request_uri(backend, {  			-- 发请求
        method = "GET",  
        path = requestPath
    })
    if not resp then    				-- 若无响应处理，则返回错误
        ngx.say("request error :", err)  
        return
    end
    
    ngx.say(resp.body)                  -- 否则返回请求结果
    
    httpc:close()                       -- 关闭请求客户端
```

nginx -s reload

基于商品id的定向流量分发策略的lua脚本就开发完了，而且也测试过了

我们就可以看到，如果你请求的是固定的某一个商品，那么就一定会将流量打到固定的一个应用nginx上面去


4、三层缓存架构实现

分发层nginx，lua应用，会将商品id，商品店铺id，都转发到应用层nginx
  
  1、应用层nginx的lua脚本接收到请求
  
  2、获取请求参数中的商品id，以及商品店铺id
  
  3、根据商品id和商品店铺id，在nginx本地缓存中尝试获取数据
  
  4、如果在nginx本地缓存中没有获取到数据，那么就到redis分布式缓存中获取数据，如果获取到了数据，还要设置到nginx本地缓存中
  
  但是这里有个问题，建议不要用nginx+lua直接去获取redis数据
  
  因为openresty没有太好的redis cluster的支持包，所以建议是发送http请求到缓存数据生产服务，由该服务提供一个http接口
  
  缓存数生产服务可以基于redis cluster api从redis中直接获取数据，并返回给nginx

cd /usr/hello/lualib/resty/  
wget https://raw.githubusercontent.com/pintsized/lua-resty-http/master/lib/resty/http_headers.lua  
wget https://raw.githubusercontent.com/pintsized/lua-resty-http/master/lib/resty/http.lua 

5、如果缓存数据生产服务没有在redis分布式缓存中没有获取到数据，那么就在自己本地ehcache中获取数据，返回数据给nginx，也要设置到nginx本地缓存中

6、如果ehcache本地缓存都没有数据，那么就需要去原始的服务中拉去数据，该服务会从mysql中查询，拉去到数据之后，返回给nginx，并重新设置到ehcache和redis中

这里先不考虑，后面要专门讲解一套分布式缓存重建并发冲突的问题和解决方案

7、nginx最终利用获取到的数据，动态渲染网页模板

cd /usr/hello/lualib/resty/
wget https://raw.githubusercontent.com/bungle/lua-resty-template/master/lib/resty/template.lua
mkdir /usr/hello/lualib/resty/html
cd /usr/hello/lualib/resty/html
wget https://raw.githubusercontent.com/bungle/lua-resty-template/master/lib/resty/template/html.lua

在hello.conf的server中配置模板位置

set $template_location "/templates";  
set $template_root "/usr/hello/templates";

mkdir /usr/hello/templates

vi product.html

```
<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<title>商品详情页</title>
	</head>
<body>
商品id: {* productId *}<br/>
商品名称: {* productName *}<br/>
商品图片列表: {* productPictureList *}<br/>
商品规格: {* productSpecification *}<br/>
商品售后服务: {* productService *}<br/>
商品颜色: {* productColor *}<br/>
商品大小: {* productSize *}<br/>
店铺id: {* shopId *}<br/>
店铺名称: {* shopName *}<br/>
店铺评级: {* shopLevel *}<br/>
店铺好评率: {* shopGoodCommentRate *}<br/>
</body>
</html>
```

8、将渲染后的网页模板作为http响应，返回给分发层nginx

hello.conf中：
```
lua_shared_dict my_cache 128m;

lua脚本中：

local uri_args = ngx.req.get_uri_args()
local productId = uri_args["productId"]
local shopId = uri_args["shopId"]

local cache_ngx = ngx.shared.my_cache

local productCacheKey = "product_info_"..productId
local shopCacheKey = "shop_info_"..shopId

local productCache = cache_ngx:get(productCacheKey)
local shopCache = cache_ngx:get(shopCacheKey)

if productCache == "" or productCache == nil then
	local http = require("resty.http")
	local httpc = http.new()

	local resp, err = httpc:request_uri("http://192.168.31.179:8080",{
  		method = "GET",
  		path = "/getProductInfo?productId="..productId
	})

	productCache = resp.body
	cache_ngx:set(productCacheKey, productCache, 10 * 60)
end

if shopCache == "" or shopCache == nil then
	local http = require("resty.http")
	local httpc = http.new()

	local resp, err = httpc:request_uri("http://192.168.31.179:8080",{
  		method = "GET",
  		path = "/getShopInfo?shopId="..shopId
	})

	shopCache = resp.body
	cache_ngx:set(shopCacheKey, shopCache, 10 * 60)
end

local cjson = require("cjson")
local productCacheJSON = cjson.decode(productCache)
local shopCacheJSON = cjson.decode(shopCache)

local context = {
	productId = productCacheJSON.id,
	productName = productCacheJSON.name,
	productPrice = productCacheJSON.price,
	productPictureList = productCacheJSON.pictureList,
	productSpecification = productCacheJSON.specification,
	productService = productCacheJSON.service,
	productColor = productCacheJSON.color,
	productSize = productCacheJSON.size,
	shopId = shopCacheJSON.id,
	shopName = shopCacheJSON.name,
	shopLevel = shopCacheJSON.level,
	shopGoodCommentRate = shopCacheJSON.goodCommentRate
}

local template = require("resty.template")
template.render("product.html", context)
```
