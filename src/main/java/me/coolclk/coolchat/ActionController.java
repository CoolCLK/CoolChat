package me.coolclk.coolchat;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Controller
@RequestMapping(value = "api")
public class ActionController {
    /**
     * 请求登录结果并储存账号session至浏览器中。
     * @param account 账号
     * @param password 密码
     * @param remember 设定cookie是否为临时（可选）
     * @return { "status": 状态码, "message": 消息 }
     * @author CoolCLK
     */
    @ResponseBody
    @RequestMapping(value = "login")
    public Object loginRequest(HttpServletResponse response, HttpServletRequest request, @RequestParam(value="account") String account, @RequestParam(value="password") String password, @RequestParam(value="remember", defaultValue="false") String remember) {
        Map<String, Object> _request = new HashMap<>();
        if (account != "" && password != "") {
            int status = AccountController.checkAccountAvailable(account, password);
            _request.put("status", status);
            switch (status) {
                case 0:
                case 1: {
                    _request.put("message", "请检查账号或密码是否有误");
                    break;
                }
                case 2: {
                    AccountController.Account loginAccount = AccountController.accountLogin(account, password);
                    assert loginAccount != null;
                    if ((int) ((Map) checkLogin(response, request)).get("status") == 0) {
                        Cookie accountCookie = new Cookie("account_session", loginAccount.createSession());
                        AccountController.saveAccountFile();
                        accountCookie.setMaxAge(-1);
                        if (Objects.equals(remember, "true")) {
                            accountCookie.setMaxAge((int) ConfigController.getValue("account.session.max-life")); // 7 Days
                        }
                        accountCookie.setPath("/");
                        response.addCookie(accountCookie);
                        _request.put("message", "登录成功");
                    } else {
                        _request.put("status", 3);
                        _request.put("message", "您已登录，请先退出登录。");
                    }
                    break;
                }
            }
        }
        else {
            _request.put("status", 0);
            _request.put("message", "空的账号或密码");
        }
        return _request;
    }

    /**
     * 请求注册结果。
     * @param account 账号
     * @param password 密码
     * @param nickname 昵称（可选）
     * @return { "status": 状态码, "message": 消息 }
     * @author CoolCLK
     */
    @ResponseBody
    @RequestMapping(value = "register")
    public Object registerRequest(@RequestParam(value="account") String account, @RequestParam(value="nickname", required = false) String nickname, @RequestParam(value="password") String password) {
        Map<String, Object> request = new HashMap<>();
        if (account != "" && password != "") {
            if (Pattern.compile("\\w+").matcher(account).matches() && Pattern.compile("\\w+").matcher(password).matches()) {
                AccountController.Account newAccount = AccountController.accountRegister(account, password);
                if (newAccount != null) {
                    if (nickname != "") {
                        newAccount.setNickname(nickname);
                    }
                    request.put("status", 1);
                    request.put("message", "注册成功，请重新登录");
                }
                else {
                    request.put("status", 2);
                    request.put("message", "此账号已被注册");
                }
            } else {
                request.put("status", 0);
                request.put("message", "不正确的格式");
            }
        }
        else {
            request.put("status", 0);
            request.put("message", "空的账号或密码");
        }
        return request;
    }

    /**
     * 从*浏览器*上检查是否登录（包括是否为有效登录等）。
     * @return { "status": 状态码, "message": 消息 }
     * @author CoolCLK
     */
    @ResponseBody
    @RequestMapping(value = "checkLogin")
    public Object checkLogin(HttpServletResponse response, HttpServletRequest request) {
        Map<String, Object> _request = new HashMap<>();
        _request.put("status", 0);
        _request.put("message", "未登录");
        if (request.getCookies() != null) {
            Cookie sessionCookie = Arrays.stream(request.getCookies()).filter(c -> Objects.equals(c.getName(), "account_session")).findAny().orElse(null);
            if (sessionCookie != null) {
                if (AccountController.accountLogin(sessionCookie.getValue(), true) != null) {
                    _request.put("status", 1);
                    _request.put("message", "已登录");
                } else {
                    _request.put("status", 0);
                    _request.put("message", "无效的登录");
                }
            }
        }
        return _request;
    }

    /**
     * 请求退出登录结果。
     * @return { "status": 状态码, "message": 消息 }
     * @author CoolCLK
     */
    @ResponseBody
    @RequestMapping(value = "logout")
    public Object logoutRequest(HttpServletResponse response, HttpServletRequest request) {
        Map<String, Object> _request = new HashMap<>();
        _request.put("status", 0);
        _request.put("message", "退出登录异常");
        if (request.getCookies() != null) {
            Arrays.stream(request.getCookies()).filter(c -> Objects.equals(c.getName(), "account_session")).findAny().ifPresent(sessionCookie -> {
                if ((int) ((Map) checkLogin(response, request)).get("status") == 1) {
                    AccountController.Account logoutAccount = AccountController.accountLogin(sessionCookie.getValue(), true);
                    if (logoutAccount != null) {
                        logoutAccount.removeSession(sessionCookie.getValue());
                        AccountController.saveAccountFile();
                    }
                }
                logoutOnlyCookie(response);
                _request.put("status", 1);
                _request.put("message", "退出登录成功");
            });
        }
        return _request;
    }

    /**
     * 请求退出登录结果。
     * @return { "status": 状态码, "message": 消息 }
     * @author CoolCLK
     */
    @ResponseBody
    @RequestMapping(value = "deleteAccount")
    public Object deleteAccountRequest(HttpServletResponse response, HttpServletRequest request) {
        Map<String, Object> _request = new HashMap<>();
        _request.put("status", 0);
        _request.put("message", "注销账号失败");
        if (request.getCookies() != null) {
            Arrays.stream(request.getCookies()).filter(c -> Objects.equals(c.getName(), "account_session")).findAny().ifPresent(sessionCookie -> {
                if ((int) ((Map<String, Object>) checkLogin(response, request)).get("status") == 1) {
                    AccountController.deleteAccount(AccountController.accountLogin(sessionCookie.getValue(), true).account);
                }
                _request.put("status", 1);
                _request.put("message", "注销账号成功");
            });
        }
        return _request;
    }

    /**
     * 获取账号信息（若已登录显示更全面的信息）。
     * @return { "status": 状态码... }
     * @author CoolCLK
     */
    @ResponseBody
    @RequestMapping(value = "profile")
    public Map<String, Object> getAccountProfile(HttpServletResponse response, HttpServletRequest request, @RequestParam(value="account", defaultValue = "") String accountArg) {
        Map<String, Object> _request = new HashMap<>();
        _request.put("status", 0);
        _request.put("owner", false);
        boolean full = false;
        AccountController.Account account = null;
        if (request.getCookies() != null) {
            Cookie sessionCookie = Arrays.stream(request.getCookies()).filter(c -> Objects.equals(c.getName(), "account_session")).findAny().orElse(null);
            if (sessionCookie != null) {
                AccountController.Account gettingAccount = AccountController.accountLogin(sessionCookie.getValue(), true);
                if (gettingAccount != null) {
                    if (!Objects.equals(accountArg, "")) {
                        full = (Objects.equals(gettingAccount.account, accountArg));
                    }
                    else {
                        account = gettingAccount;
                        full = true;
                    }
                }
            }
        }
        if (full || AccountController.checkAccountAvailable(accountArg, null) == 1) {
            if (account == null) {
                account = AccountController.getAccount(accountArg);
            }
            _request.put("status", 1);
            _request.put("account", account.account);
            _request.put("nickname", account.nickname);
            _request.put("registerTime", account.registerTime);
            if (full) {
                _request.put("owner", true);
            }
        }
        return _request;
    }

    /**
     * 更新账号信息（需要登录）。
     * @return { "status": 状态码... }
     * @author CoolCLK
     */
    @ResponseBody
    @RequestMapping(value = "updateProfile")
    public Object updateAccountProfile(HttpServletResponse response, HttpServletRequest request, @RequestParam(value="key") String updateKeyname, @RequestParam(value="value") Object updateValue) {
        Map<String, Object> _request = new HashMap<>();
        _request.put("status", 0);
        if ((int) getAccountProfile(response, request, "").get("status") == 1 && (boolean) getAccountProfile(response, request, "").get("owner")) {
            AccountController.Account account = AccountController.getAccount((String) getAccountProfile(response, request, "").get("account"));;
            switch (updateKeyname) {
                case "nickname": {
                    account.setNickname((String) updateValue);
                    break;
                }
                case "password": {
                    account.setPassword((String) updateValue);
                    break;
                }
            }
            AccountController.saveAccountFile();
        }
        return _request;
    }

    /**
     * 仅在*浏览器*上删除cookie。
     * @author CoolCLK
     */
    public void logoutOnlyCookie(HttpServletResponse response) {
        Cookie accountCookie = new Cookie("account_session", null);
        accountCookie.setMaxAge(0);
        accountCookie.setPath("/");
        response.addCookie(accountCookie);
    }
}