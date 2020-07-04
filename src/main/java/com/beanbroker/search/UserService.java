package com.beanbroker.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

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
