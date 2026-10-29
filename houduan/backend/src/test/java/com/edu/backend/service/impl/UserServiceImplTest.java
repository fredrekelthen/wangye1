package com.edu.backend.service.impl;

import com.edu.backend.exception.BusinessException;
import com.edu.backend.model.dto.UserLoginRequest;
import com.edu.backend.model.dto.UserRegisterRequest;
import com.edu.backend.model.entity.User;
import com.edu.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.MessageDigest;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * UserServiceImpl 单元测试：注册 BCrypt 存储、登录、存量 MD5 透明升级、自助改密。
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    private UserServiceImpl userService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl();
        ReflectionTestUtils.setField(userService, "userRepository", userRepository);
        ReflectionTestUtils.setField(userService, "mongoTemplate", mongoTemplate);
    }

    @Test
    public void testRegisterStoresBcryptPassword() {
        UserRegisterRequest req = new UserRegisterRequest();
        req.setUserAccount("student1");
        req.setUserPassword("password123");
        req.setCheckPassword("password123");
        req.setPlanetCode("p001");

        when(userRepository.existsByUserAccount("student1")).thenReturn(false);
        when(userRepository.findByPlanetCode("p001")).thenReturn(Optional.empty());

        userService.userRegister(req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertTrue(captor.getValue().getUserPassword().startsWith("$2"), "注册密码应为 BCrypt 哈希");
    }

    @Test
    public void testLoginWithBcryptPasswordNullsPassword() {
        User user = new User();
        user.setId("id1");
        user.setUserAccount("student1");
        user.setUserPassword(encoder.encode("password123"));
        user.setRole("USER");
        user.setUserStatus(0);

        when(userRepository.findByUserAccount("student1")).thenReturn(Optional.of(user));

        UserLoginRequest req = new UserLoginRequest();
        req.setUserAccount("student1");
        req.setUserPassword("password123");

        User result = userService.userLogin(req);
        assertNotNull(result);
        assertNull(result.getUserPassword(), "登录返回的用户不应携带密码");
    }

    @Test
    public void testLoginUpgradesLegacyMd5Password() throws Exception {
        // 旧 MD5 密码 = md5("password123" + "edu_platform_salt")
        String legacyMd5 = md5("password123" + "edu_platform_salt");
        User user = new User();
        user.setId("id1");
        user.setUserAccount("student1");
        user.setUserPassword(legacyMd5);
        user.setRole("USER");
        user.setUserStatus(0);

        when(userRepository.findByUserAccount("student1")).thenReturn(Optional.of(user));

        UserLoginRequest req = new UserLoginRequest();
        req.setUserAccount("student1");
        req.setUserPassword("password123");

        User result = userService.userLogin(req);
        assertNotNull(result);
        // 存量 MD5 密码登录成功，且触发了透明升级（save 被调用；BCrypt 正确性由 register/changeSelfPassword 用例覆盖）
        verify(userRepository).save(any(User.class));
    }

    @Test
    public void testChangeSelfPasswordRejectsWrongOldPassword() {
        User user = new User();
        user.setId("id1");
        user.setUserAccount("student1");
        user.setUserPassword(encoder.encode("password123"));

        when(userRepository.findById("id1")).thenReturn(Optional.of(user));

        assertThrows(BusinessException.class,
                () -> userService.changeSelfPassword("id1", "wrongpass", "newpass123"));
    }

    @Test
    public void testChangeSelfPasswordSuccess() {
        User user = new User();
        user.setId("id1");
        user.setUserAccount("student1");
        user.setUserPassword(encoder.encode("password123"));

        when(userRepository.findById("id1")).thenReturn(Optional.of(user));

        Boolean result = userService.changeSelfPassword("id1", "password123", "newpass123");
        assertTrue(result);
        verify(userRepository).save(argThat(u -> u.getUserPassword().startsWith("$2")));
    }

    private String md5(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] bytes = md.digest(s.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
