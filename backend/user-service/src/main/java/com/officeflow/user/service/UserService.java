package com.officeflow.user.service;

import com.officeflow.common.api.PageResult;
import com.officeflow.common.exception.BusinessException;
import com.officeflow.common.security.JwtUtil;
import com.officeflow.common.security.SecurityConstants;
import com.officeflow.user.dto.ApiPermissionRequest;
import com.officeflow.user.dto.DeptRequest;
import com.officeflow.user.dto.PostRequest;
import com.officeflow.user.dto.RoleRequest;
import com.officeflow.user.dto.UserRequest;
import com.officeflow.user.mapper.ApiPermissionMapper;
import com.officeflow.user.mapper.DeptMapper;
import com.officeflow.user.mapper.LogMapper;
import com.officeflow.user.mapper.MenuMapper;
import com.officeflow.user.mapper.PostMapper;
import com.officeflow.user.mapper.RoleMapper;
import com.officeflow.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final DeptMapper deptMapper;
    private final PostMapper postMapper;
    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;
    private final ApiPermissionMapper apiPermissionMapper;
    private final LogMapper logMapper;

    @Value("${officeflow.jwt.secret:officeflow-secret-key-must-be-at-least-32-bytes}")
    private String jwtSecret;

    @Value("${officeflow.jwt.expire-seconds:86400}")
    private long jwtExpireSeconds;

    public UserService(UserMapper userMapper, DeptMapper deptMapper, PostMapper postMapper, RoleMapper roleMapper,
                       MenuMapper menuMapper, ApiPermissionMapper apiPermissionMapper, LogMapper logMapper) {
        this.userMapper = userMapper;
        this.deptMapper = deptMapper;
        this.postMapper = postMapper;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.apiPermissionMapper = apiPermissionMapper;
        this.logMapper = logMapper;
    }

    @Transactional
    public Map<String, Object> login(String username, String password, HttpServletRequest request) {
        Map<String, Object> user = userMapper.findByUsername(username);
        if (user == null) {
            logMapper.insertLoginLog(null, username, clientIp(request), request.getHeader("User-Agent"), "FAIL", "账号不存在");
            throw new BusinessException("账号或密码错误");
        }
        Long userId = toLong(user.get("id"));
        if (!Objects.equals(String.valueOf(user.get("password")), password)) {
            logMapper.insertLoginLog(userId, username, clientIp(request), request.getHeader("User-Agent"), "FAIL", "密码错误");
            throw new BusinessException("账号或密码错误");
        }
        if (!Objects.equals(toInteger(user.get("status")), 1)) {
            logMapper.insertLoginLog(userId, username, clientIp(request), request.getHeader("User-Agent"), "FAIL", "账号已停用");
            throw new BusinessException("账号已停用");
        }

        List<Map<String, Object>> roles = roleMapper.listByUserId(userId);
        List<String> roleCodes = roles.stream().map(role -> String.valueOf(role.get("roleCode"))).toList();
        List<String> permissions = menuMapper.listPermissionsByUserId(userId);

        Map<String, Object> claims = new HashMap<>();
        claims.put(SecurityConstants.CLAIM_USER_ID, userId);
        claims.put(SecurityConstants.CLAIM_USERNAME, user.get("username"));
        claims.put(SecurityConstants.CLAIM_REAL_NAME, user.get("realName"));
        claims.put(SecurityConstants.CLAIM_DEPT_ID, user.get("deptId"));
        claims.put(SecurityConstants.CLAIM_ROLES, roleCodes);
        claims.put(SecurityConstants.CLAIM_PERMISSIONS, permissions);
        String token = JwtUtil.generateToken(String.valueOf(userId), claims, jwtSecret, jwtExpireSeconds);

        userMapper.updateLastLoginAt(userId);
        logMapper.insertLoginLog(userId, username, clientIp(request), request.getHeader("User-Agent"), "SUCCESS", "登录成功");

        Map<String, Object> profile = profile(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("profile", profile);
        result.put("roles", roles);
        result.put("permissions", permissions);
        result.put("menus", currentMenus(userId));
        result.put("expireSeconds", jwtExpireSeconds);
        return result;
    }

    public Map<String, Object> profile(Long userId) {
        requireLogin(userId);
        Map<String, Object> profile = userMapper.findProfileById(userId);
        if (profile == null) {
            throw new BusinessException("当前用户不存在");
        }
        profile.remove("password");
        profile.put("roles", roleMapper.listByUserId(userId));
        profile.put("roleIds", roleMapper.listRoleIdsByUserId(userId));
        profile.put("permissions", menuMapper.listPermissionsByUserId(userId));
        return profile;
    }

    public List<Map<String, Object>> currentMenus(Long userId) {
        requireLogin(userId);
        return buildTree(menuMapper.listByUserId(userId));
    }

    public List<Map<String, Object>> allMenus() {
        return buildTree(menuMapper.listAll());
    }

    public List<Map<String, Object>> deptTree() {
        return buildTree(deptMapper.listAll());
    }

    public List<Map<String, Object>> postList() {
        return postMapper.listAll();
    }

    public List<Map<String, Object>> enabledUsers() {
        return userMapper.listEnabledUsers();
    }

    public PageResult<Map<String, Object>> userPage(String keyword, Long deptId, Integer status, long pageNum, long pageSize) {
        long safePageNum = Math.max(pageNum, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        long total = userMapper.countUsers(keyword, deptId, status);
        List<Map<String, Object>> records = total == 0 ? List.of() :
                userMapper.listUsers(keyword, deptId, status, (safePageNum - 1) * safePageSize, safePageSize);
        for (Map<String, Object> user : records) {
            Long userId = toLong(user.get("id"));
            user.put("roleIds", roleMapper.listRoleIdsByUserId(userId));
        }
        return PageResult.of(total, safePageNum, safePageSize, records);
    }

    @Transactional
    public void createUser(UserRequest request) {
        String password = hasText(request.password()) ? request.password() : "123456";
        userMapper.insertUser(request, password, defaultText(request.userType(), "EMPLOYEE"), defaultInt(request.status(), 1));
    }

    @Transactional
    public void updateUser(Long id, UserRequest request) {
        if (userMapper.updateUser(id, request, defaultText(request.userType(), "EMPLOYEE"), defaultInt(request.status(), 1)) == 0) {
            throw new BusinessException("员工不存在");
        }
    }

    @Transactional
    public void deleteUser(Long id) {
        if (userMapper.softDelete(id) == 0) {
            throw new BusinessException("员工不存在或管理员账号不能删除");
        }
    }

    @Transactional
    public void updateUserStatus(Long id, Integer status) {
        if (userMapper.updateStatus(id, status) == 0) {
            throw new BusinessException("员工不存在");
        }
    }

    @Transactional
    public void resetPassword(Long id, String password) {
        if (userMapper.resetPassword(id, password) == 0) {
            throw new BusinessException("员工不存在");
        }
    }

    @Transactional
    public void assignUserRoles(Long userId, List<Long> roleIds) {
        userMapper.deleteUserRoles(userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            userMapper.insertUserRoles(userId, roleIds);
        }
    }

    @Transactional
    public void createDept(DeptRequest request) {
        deptMapper.insert(request, defaultLong(request.parentId(), 0L), defaultInt(request.sortOrder(), 0), defaultInt(request.status(), 1));
    }

    @Transactional
    public void updateDept(Long id, DeptRequest request) {
        if (deptMapper.update(id, request, defaultLong(request.parentId(), 0L), defaultInt(request.sortOrder(), 0), defaultInt(request.status(), 1)) == 0) {
            throw new BusinessException("部门不存在");
        }
    }

    @Transactional
    public void deleteDept(Long id) {
        if (deptMapper.softDelete(id) == 0) {
            throw new BusinessException("部门不存在");
        }
    }

    @Transactional
    public void createPost(PostRequest request) {
        postMapper.insert(request, defaultInt(request.sortOrder(), 0), defaultInt(request.status(), 1));
    }

    @Transactional
    public void updatePost(Long id, PostRequest request) {
        if (postMapper.update(id, request, defaultInt(request.sortOrder(), 0), defaultInt(request.status(), 1)) == 0) {
            throw new BusinessException("岗位不存在");
        }
    }

    @Transactional
    public void deletePost(Long id) {
        if (postMapper.softDelete(id) == 0) {
            throw new BusinessException("岗位不存在");
        }
    }

    public List<Map<String, Object>> roleList() {
        List<Map<String, Object>> roles = roleMapper.listAll();
        for (Map<String, Object> role : roles) {
            Long roleId = toLong(role.get("id"));
            role.put("menuIds", roleMapper.listMenuIdsByRoleId(roleId));
            role.put("apiPermissionIds", roleMapper.listApiPermissionIdsByRoleId(roleId));
        }
        return roles;
    }

    @Transactional
    public void createRole(RoleRequest request) {
        roleMapper.insert(request, defaultText(request.dataScope(), "SELF"), defaultInt(request.sortOrder(), 0), defaultInt(request.status(), 1));
    }

    @Transactional
    public void updateRole(Long id, RoleRequest request) {
        if (roleMapper.update(id, request, defaultText(request.dataScope(), "SELF"), defaultInt(request.sortOrder(), 0), defaultInt(request.status(), 1)) == 0) {
            throw new BusinessException("角色不存在");
        }
    }

    @Transactional
    public void deleteRole(Long id) {
        if (roleMapper.softDelete(id) == 0) {
            throw new BusinessException("角色不存在");
        }
    }

    @Transactional
    public void assignRoleMenus(Long roleId, List<Long> menuIds) {
        roleMapper.deleteRoleMenus(roleId);
        if (menuIds != null && !menuIds.isEmpty()) {
            roleMapper.insertRoleMenus(roleId, menuIds);
        }
    }

    @Transactional
    public void assignRoleApiPermissions(Long roleId, List<Long> permissionIds) {
        roleMapper.deleteRoleApiPermissions(roleId);
        if (permissionIds != null && !permissionIds.isEmpty()) {
            roleMapper.insertRoleApiPermissions(roleId, permissionIds);
        }
    }

    public List<Map<String, Object>> apiPermissionList() {
        return apiPermissionMapper.listAll();
    }

    @Transactional
    public void createApiPermission(ApiPermissionRequest request) {
        apiPermissionMapper.insert(request, defaultInt(request.status(), 1));
    }

    @Transactional
    public void updateApiPermission(Long id, ApiPermissionRequest request) {
        if (apiPermissionMapper.update(id, request, defaultInt(request.status(), 1)) == 0) {
            throw new BusinessException("接口权限不存在");
        }
    }

    @Transactional
    public void disableApiPermission(Long id) {
        if (apiPermissionMapper.disable(id) == 0) {
            throw new BusinessException("接口权限不存在");
        }
    }

    public List<Map<String, Object>> loginLogs(int limit) {
        return logMapper.listLoginLogs(Math.min(Math.max(limit, 1), 200));
    }

    public List<Map<String, Object>> operationLogs(int limit) {
        return logMapper.listOperationLogs(Math.min(Math.max(limit, 1), 200));
    }

    public void operationLog(Long userId, String username, String module, String operation, HttpServletRequest request) {
        logMapper.insertOperationLog(userId, username, module, operation, request.getMethod(), request.getRequestURI(), clientIp(request));
    }

    private List<Map<String, Object>> buildTree(List<Map<String, Object>> rows) {
        Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>(row);
            item.put("children", new ArrayList<Map<String, Object>>());
            byId.put(toLong(item.get("id")), item);
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> item : byId.values()) {
            Long parentId = defaultLong(toLong(item.get("parentId")), 0L);
            if (parentId == 0 || !byId.containsKey(parentId)) {
                roots.add(item);
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) byId.get(parentId).get("children");
                children.add(item);
            }
        }
        sortTree(roots);
        return roots;
    }

    private void sortTree(List<Map<String, Object>> nodes) {
        nodes.sort(Comparator.comparingInt(node -> defaultInt(toInteger(node.get("sortOrder")), 0)));
        for (Map<String, Object> node : nodes) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
            sortTree(children);
        }
    }

    private void requireLogin(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String defaultText(String value, String defaultValue) {
        return hasText(value) ? value : defaultValue;
    }

    private Integer defaultInt(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    private Long defaultLong(Long value, Long defaultValue) {
        return value == null ? defaultValue : value;
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }
}
