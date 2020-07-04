package com.beanbroker.search;

import org.elasticsearch.action.get.GetResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

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
