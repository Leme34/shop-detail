### 基于 nginx+lua 完成商品详情页访问流量实时上报 kafka 的开发

在nginx这一层，接收到访问请求的时候，就把请求的流量上报发送给kafka

这样的话，storm才能去消费kafka中的实时的访问日志，然后去进行缓存热数据的统计

用得技术方案非常简单，从lua脚本直接创建一个kafka producer，发送数据到kafka

wget https://github.com/doujiang24/lua-resty-kafka/archive/master.zip

yum install -y unzip

unzip master.zip

cp -rf /usr/local/lua-resty-kafka-master/lib/resty /usr/hello/lualib

nginx -s reload

local cjson = require("cjson")  
local producer = require("resty.kafka.producer")  

local broker_list = {  
    { host = "192.168.31.187", port = 9092 },  
    { host = "192.168.31.19", port = 9092 },  
    { host = "192.168.31.227", port = 9092 }
}

local log_json = {}  
log_json["headers"] = ngx.req.get_headers()  
log_json["uri_args"] = ngx.req.get_uri_args()  
log_json["body"] = ngx.req.read_body()  
log_json["http_version"] = ngx.req.http_version()  
log_json["method"] =ngx.req.get_method() 
log_json["raw_reader"] = ngx.req.raw_header()  
log_json["body_data"] = ngx.req.get_body_data()  

local message = cjson.encode(log_json);  

local productId = ngx.req.get_uri_args()["productId"]

local async_producer = producer:new(broker_list, { producer_type = "async" })   
local ok, err = async_producer:send("access-log", productId, message)  

if not ok then  
    ngx.log(ngx.ERR, "kafka send err:", err)  
    return  
end

两台机器上都这样做，才能统一上报流量到kafka

bin/kafka-topics.sh --zookeeper 192.168.31.187:2181,192.168.31.19:2181,192.168.31.227:2181 --topic access-log --replication-factor 1 --partitions 1 --create

bin/kafka-console-consumer.sh --zookeeper 192.168.31.187:2181,192.168.31.19:2181,192.168.31.227:2181 --topic access-log --from-beginning

（1）kafka在187上的节点死掉了，可能是虚拟机的问题，杀掉进程，重新启动一下

nohup bin/kafka-server-start.sh config/server.properties &

（2）需要在nginx.conf中，http部分，加入resolver 8.8.8.8;

（3）需要在kafka中加入advertised.host.name = 192.168.31.187，重启三个kafka进程

（4）需要启动eshop-cache缓存服务，因为nginx中的本地缓存可能不在了，而查询服务不可用的话就会访问出错
