
[작성된 블로그 가기](https://beanbroker.github.io/2020/07/04/Spring/spring_es_search1)


# Spring Elastic Search 연동(docker) 1편

# 1. Docker Elastic Search 설치

docker 설치 방법은! 다른 블로그에서 참고하자

> docker 이미지 받아오기
```
docker pull docker.elastic.co/elasticsearch/elasticsearch:7.7.1
```

> 7.x 버전에서는 싱글노드 실행시 아래와 같이 실행

```
docker run -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch:7.7.1
```
 
 위와 같이 실행시 아래와 같이 실행이 되어진다.
 ```
 {"type": "server", "timestamp": "2020-06-28T10:31:35,754Z", "level": "INFO", "component": "o.e.e.NodeEnvironment", "cluster.name": "docker-cluster", "node.name": "dbd93832ee71", "message": "using [1] data paths, mounts [[/ (overlay)]], net usable_space [52.7gb], net total_space [58.4gb], types [overlay]" }
{"type": "server", "timestamp": "2020-06-28T10:31:35,758Z", "level": "INFO", "component": "o.e.e.NodeEnvironment", "cluster.name": "docker-cluster", "node.name": "dbd93832ee71", "message": "heap size [1gb], compressed ordinary object pointers [true]" }
{"type": "server", "timestamp": "2020-06-28T10:31:35,842Z", "level": "INFO", "component": "o.e.n.Node", "cluster.name": "docker-cluster", "node.name": "dbd93832ee71", "message": "node name [dbd93832ee71], node ID [sxcEM2FoQKO5w4Coh2uK6A], cluster name [docker-cluster]" }
{"type": "server", "timestamp": "2020-06-28T10:31:35,847Z", "level": "INFO", "component": "o.e.n.Node", "cluster.name": "docker-cluster", "node.name": "dbd93832ee71", "message": "version[7.7.1], pid[6], build[default/docker/ad56dce891c901a492bb1ee393f12dfff473a423/2020-05-28T16:30:01.040088Z], OS[Linux/4.9.125-linuxkit/amd64], JVM[AdoptOpenJDK/OpenJDK 64-Bit Server VM/14.0.1/14.0.1+7]" }
```

> 정상 실행 확인 방법

postman 또는 그냥 웹브라우저에서 http://localhost:9200/ 접근시 아래와 같은 응답을 받은 것을 확인.

```json
{
  "name" : "dbd93832ee71",
  "cluster_name" : "docker-cluster",
  "cluster_uuid" : "AxvSUvrpRk6lrJSEG5fe5w",
  "version" : {
    "number" : "7.7.1",
    "build_flavor" : "default",
    "build_type" : "docker",
    "build_hash" : "ad56dce891c901a492bb1ee393f12dfff473a423",
    "build_date" : "2020-05-28T16:30:01.040088Z",
    "build_snapshot" : false,
    "lucene_version" : "8.5.1",
    "minimum_wire_compatibility_version" : "6.8.0",
    "minimum_index_compatibility_version" : "6.0.0-beta1"
  },
  "tagline" : "You Know, for Search"
}
```

# 2. spirng과 연동 해보자

## spring boot version : 2.3.1

연동시에 여러가집 방법이 있지만 해당 블로그에선 HighLevelRestClient를 쓸 예정

비교 정리내용 : https://kok202.tistory.com/102 블로그


rest high level client 부터는 tcp connection 이 아닌 http connection 을 맺는다.


> github sample code

클릭 -> [Spring ES sample code](https://github.com/beanbroker/sample_elastic_search)


> 아래처럼 high-level-client 디펜던시 추가

```gradle

compile 'org.elasticsearch:elasticsearch:7.7.0'
compile 'org.elasticsearch.client:elasticsearch-rest-high-level-client:7.7.0'
```


> application.yml

```
server:
  port: 9000
spring:
  elasticsearch:
    rest:
      uris: http://localhost:9200
```

기존에는 아래의 코드 처럼 esconfig를 직접 설정해주어야 했는데 spring boot 2.2 인지 2.3에서 편하게 yml로 config를 설정할수 있게 도와주는 것 같다. 정확하게 어느 버전인지는 찾아보지 않았음.

```java

@Configuration
public class ElasticsearchConfig {

  @Bean(destroyMethod = "close")
  public RestHighLevelClient elasticsearchClient(ElasticsearchProperties properties) {

    return new RestHighLevelClient(RestClient.builder(properties.hosts()));
  }
}

```



> UserController.java 


인덱스 생성과 생성된 index를 가져오는 테스트만을 진행할 것이다.

물론 query build를 통한 search도 가능하나 간단하게 연동을 위한 블로그임을!!! 미리 말씀드린다.

```java
@RestController
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping("/users")
  @ResponseStatus(HttpStatus.CREATED)
  public String createUserProfile(@RequestBody UserProfileDto profile) throws IOException {

    return userService.createUserIndex(profile.toBeanbrokerUser());
  }

  @GetMapping("/users/{userKey}")
  @ResponseStatus(HttpStatus.CREATED)
  public GetResponse getUserProfile(@PathVariable("userKey") String userKey) throws IOException {

    return userService.getUserIndex(userKey);
  }
}

```


> UserService.java

테스트를 위해 사용하려는 IndexRequest, GetRequest가 Deprecated되었다.. 일단 잘 작동하나 왜 Deprecated되었는지는 알수 없다.

```java
@Service
@Slf4j
public class UserService {

  @Autowired private RestHighLevelClient restHighLevelClient;

  @Autowired private ObjectMapper objectMapper;

  private static final String USER_DOC_TYPE = "_doc";
  private static final String USER_INDEX = "user";

  public String createUserIndex(BeanbrokerUser user) throws IOException {

    // set unique index on User
    String uniqueId = UUID.randomUUID().toString();
    user.setUniqueId(uniqueId);

    Map<String, Object> userDocument = objectMapper.convertValue(user, Map.class);

    IndexRequest indexRequest = new IndexRequest(USER_INDEX, USER_DOC_TYPE, uniqueId);
    indexRequest.source(userDocument, XContentType.JSON);

    IndexResponse indexResponse = restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);

    if ("CREATED".equals(indexResponse.getResult().name())) {
      return uniqueId;

    } else {
      return indexResponse.getResult().name();
    }
  }

  public GetResponse getUserIndex(String userKey) throws IOException {

    GetRequest getRequest = new GetRequest(USER_INDEX, USER_DOC_TYPE, userKey);

    GetResponse getResponse = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);

    return getResponse;
  }
}
```



# 테스트 해보자

```java 
curl --location --request POST 'http://localhost:9000/users' \
--header 'Content-Type: application/json' \
--header 'Content-Type: text/plain' \
--header 'Cookie: JSESSIONID=CAA019F0C3D0B5129E22A4DC86CDC7B7' \
--data-raw '{
	
	"userId" : "beanbroker",
	"name" : "pkj",
	"email" : "beanbroker@test.com"
}'
```

위처럼 유저 생성 curl 후 성공 일경우 'f860b9c2-3f45-48ae-95bf-bf43d58ec6bb' 와 같은 id를 받게 된다. 잘생성되었는지 확인해봐야하지 않겠는가?


http://localhost:9200/_search 접근 아래처럼 정상적으로 노출

```json
{
   "took":107,
   "timed_out":false,
   "_shards":{
      "total":1,
      "successful":1,
      "skipped":0,
      "failed":0
   },
   "hits":{
      "total":{
         "value":1,
         "relation":"eq"
      },
      "max_score":1.0,
      "hits":[
         {
            "_index":"user",
            "_type":"_doc",
            "_id":"f860b9c2-3f45-48ae-95bf-bf43d58ec6bb",
            "_score":1.0,
            "_source":{
               "uniqueId":"f860b9c2-3f45-48ae-95bf-bf43d58ec6bb",
               "userId":"beanbroker",
               "name":"pkj",
               "email":"beanbroker@test.com",
               "createdAt":"2020-07-04T17:43:17.726"
            }
         }
      ]
   }
}
```

생성에서 받은 유저의 uniqueid를 users/{value} value에 집어 넣어보자


```java
curl --location --request GET 'http://localhost:9000/users/f860b9c2-3f45-48ae-95bf-bf43d58ec6bb' \
--header 'Content-Type: application/json' \
--header 'Content-Type: text/plain' \
--header 'Cookie: JSESSIONID=CAA019F0C3D0B5129E22A4DC86CDC7B7' \
--data-raw '{
	
	"userId" : "beanbroker",
	"name" : "pkj",
	"email" : "beanbroker@test.com"
}'
```

아래 처럼 결과가 나온다.

```json
{
    "fields": {},
    "id": "f860b9c2-3f45-48ae-95bf-bf43d58ec6bb",
    "type": "_doc",
    "version": 1,
    "seqNo": 0,
    "exists": true,
    "primaryTerm": 1,
    "sourceEmpty": false,
    "sourceAsBytes": "eyJ1bmlxdWVJZCI6ImY4NjBiOWMyLTNmNDUtNDhhZS05NWJmLWJmNDNkNThlYzZiYiIsInVzZXJJZCI6ImJlYW5icm9rZXIiLCJuYW1lIjoicGtqIiwiZW1haWwiOiJiZWFuYnJva2VyQHRlc3QuY29tIiwiY3JlYXRlZEF0IjoiMjAyMC0wNy0wNFQxNzo0MzoxNy43MjYifQ==",
    "sourceInternal": {
        "fragment": true
    },
    "sourceAsBytesRef": {
        "fragment": true
    },
    "sourceAsString": "{\"uniqueId\":\"f860b9c2-3f45-48ae-95bf-bf43d58ec6bb\",\"userId\":\"beanbroker\",\"name\":\"pkj\",\"email\":\"beanbroker@test.com\",\"createdAt\":\"2020-07-04T17:43:17.726\"}",
    "sourceAsMap": {
        "createdAt": "2020-07-04T17:43:17.726",
        "name": "pkj",
        "userId": "beanbroker",
        "uniqueId": "f860b9c2-3f45-48ae-95bf-bf43d58ec6bb",
        "email": "beanbroker@test.com"
    },
    "index": "user",
    "source": {
        "createdAt": "2020-07-04T17:43:17.726",
        "name": "pkj",
        "userId": "beanbroker",
        "uniqueId": "f860b9c2-3f45-48ae-95bf-bf43d58ec6bb",
        "email": "beanbroker@test.com"
    },
    "fragment": false
}
```



http://localhost:9200/user/_search 이렇게하면 user index에 대한 search가 된다.


물론 실제로 서비스로 사용하기 위해선 강력하게 kibana가 셋팅되어야 함을 공유드리며 끝!!! 좀더 자세히 적고 설명도 붙이고 싶으나 약속이 있어서 후려쳐서.. 올려본다......