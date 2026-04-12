package com.nfu.jasmine.iam.application;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nfu.jasmine.iam.web.dto.LoginDTO;
import com.nfu.jasmine.iam.web.dto.RefreshTokenDTO;
import com.nfu.jasmine.iam.model.entity.User;
import com.nfu.jasmine.iam.web.vo.LoginVO;
import com.nfu.jasmine.iam.web.vo.UserInfoVO;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author jipzeongit
 * @since 2023-05-29
 */
public interface IUserService extends IService<User> {

    LoginVO login(LoginDTO loginDTO);

    LoginVO refreshToken(RefreshTokenDTO refreshTokenDTO);

    UserInfoVO getUserInfo(User loginUser);

    void logout(String token);

    void addUser(User user);

    User getUserById(Integer id);

    void updateUser(User user);

    void deleteUserById(Integer id);

    boolean changePassword(String username, String oldPassword, String newPassword);
}