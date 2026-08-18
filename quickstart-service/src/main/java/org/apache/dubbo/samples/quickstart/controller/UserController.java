package org.apache.dubbo.samples.quickstart.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.dubbo.samples.quickstart.entity.User;
import org.apache.dubbo.samples.quickstart.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
@Tag(name = "用户管理", description = "用户的查询、新增、修改和删除接口")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "查询用户列表", description = "返回所有未被逻辑删除的用户")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public List<User> list() {
        return userService.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询用户详情", description = "根据用户 ID 查询用户")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "用户不存在", content = @Content)
    })
    public ResponseEntity<User> get(
            @Parameter(description = "用户 ID", example = "1", required = true)
            @PathVariable("id") Long id) {
        User user = userService.getById(id);
        return user == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(user);
    }

    @PostMapping
    @Operation(summary = "创建用户", description = "创建新用户，请求中的 ID 会被忽略")
    @ApiResponse(responseCode = "201", description = "创建成功")
    public ResponseEntity<User> create(@RequestBody User user) {
        user.setId(null);
        userService.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户", description = "根据用户 ID 更新用户信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "用户不存在", content = @Content)
    })
    public ResponseEntity<User> update(
            @Parameter(description = "用户 ID", example = "1", required = true)
            @PathVariable("id") Long id,
            @RequestBody User user) {
        if (userService.getById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        user.setId(id);
        userService.updateById(user);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "根据用户 ID 逻辑删除用户")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "删除成功", content = @Content),
            @ApiResponse(responseCode = "404", description = "用户不存在", content = @Content)
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "用户 ID", example = "1", required = true)
            @PathVariable("id") Long id) {
        return userService.removeById(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
