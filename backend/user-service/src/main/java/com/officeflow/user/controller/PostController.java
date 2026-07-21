package com.officeflow.user.controller;

import com.officeflow.common.api.ApiResponse;
import com.officeflow.user.dto.PostRequest;
import com.officeflow.user.service.UserService;
import com.officeflow.user.util.RequestUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/posts")
public class PostController {
    private final UserService userService;

    public PostController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<Object> list() {
        return ApiResponse.ok(userService.postList());
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody PostRequest request, HttpServletRequest httpRequest) {
        userService.createPost(request);
        userService.operationLog(RequestUser.userId(httpRequest), RequestUser.username(httpRequest), "岗位管理", "CREATE", httpRequest);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody PostRequest request, HttpServletRequest httpRequest) {
        userService.updatePost(id, request);
        userService.operationLog(RequestUser.userId(httpRequest), RequestUser.username(httpRequest), "岗位管理", "UPDATE", httpRequest);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        userService.deletePost(id);
        userService.operationLog(RequestUser.userId(httpRequest), RequestUser.username(httpRequest), "岗位管理", "DELETE", httpRequest);
        return ApiResponse.ok();
    }
}
