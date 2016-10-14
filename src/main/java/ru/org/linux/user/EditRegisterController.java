/*
 * Copyright 1998-2016 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.user;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.auth.IPBlockDao;
import ru.org.linux.auth.UserDetailsImpl;
import ru.org.linux.auth.UserDetailsServiceImpl;
import ru.org.linux.email.EmailService;
import ru.org.linux.site.Template;
import ru.org.linux.util.ExceptionBindingErrorProcessor;
import ru.org.linux.util.StringUtil;
import ru.org.linux.util.URLUtil;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@Controller
@RequestMapping("/people/{nick}/edit")
public class EditRegisterController {
  private static final Logger logger = LoggerFactory.getLogger(EditRegisterController.class);

  @Autowired
  RememberMeServices rememberMeServices;

  @Autowired
  @Qualifier("authenticationManager")
  private AuthenticationManager authenticationManager;

  @Autowired
  private UserDetailsServiceImpl userDetailsService;

  @Autowired
  private IPBlockDao ipBlockDao;

  @Autowired
  private UserDao userDao;

  @Autowired
  private UserService userService;

  @Autowired
  private EmailService emailService;

  @RequestMapping(method = RequestMethod.GET)
  public ModelAndView show(
      @ModelAttribute("form") EditRegisterRequest form,
      @PathVariable String nick,
      HttpServletRequest request,
      HttpServletResponse response
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);
    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }
    if(!tmpl.getNick().equals(nick)) {
      throw new AccessViolationException("Not authorized");
    }
    User user = tmpl.getCurrentUser();
    UserInfo userInfo = userDao.getUserInfoClass(user);

    ModelAndView mv = new ModelAndView("edit-reg");

    form.setEmail(user.getEmail());
    form.setUrl(userInfo.getUrl());
    form.setTown(userInfo.getTown());
    form.setName(user.getName());
    form.setInfo(StringEscapeUtils.unescapeHtml4(userDao.getUserInfo(user)));

    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

    return mv;
  }

  @RequestMapping(method = RequestMethod.POST)
  public ModelAndView edit(
      HttpServletRequest request,
      HttpServletResponse response,
      @Valid @ModelAttribute("form") EditRegisterRequest form,
      Errors errors
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    String nick = tmpl.getNick();
    String password = Strings.emptyToNull(form.getPassword());

    if (password!=null && password.equalsIgnoreCase(nick)) {
      errors.reject(null, "пароль не может совпадать с логином");
    }

    InternetAddress mail = null;

    if (!Strings.isNullOrEmpty(form.getEmail())) {
      try {
        mail = new InternetAddress(form.getEmail());
      } catch (AddressException e) {
        errors.rejectValue("email", null, "Некорректный e-mail: " + e.getMessage());
      }
    }

    String url = null;

    if (!Strings.isNullOrEmpty(form.getUrl())) {
      url = URLUtil.fixURL(form.getUrl());
    }

    String name = Strings.emptyToNull(form.getName());

    if (name != null) {
      name = StringUtil.escapeHtml(name);
    }

    String town = null;

    if (!Strings.isNullOrEmpty(form.getTown())) {
      town = StringUtil.escapeHtml(form.getTown());
    }

    String info = null;

    if (!Strings.isNullOrEmpty(form.getInfo())) {
      info = StringUtil.escapeHtml(form.getInfo());
    }

    ipBlockDao.checkBlockIP(request.getRemoteAddr(), errors, tmpl.getCurrentUser());

    boolean emailChanged = false;

    User user = userService.getUser(nick);

    if (Strings.isNullOrEmpty(form.getOldpass())) {
      errors.rejectValue("oldpass", null, "Для изменения регистрации нужен ваш пароль");
    } else if (!user.matchPassword(form.getOldpass())) {
      errors.rejectValue("oldpass", null, "Неверный пароль");
    }

    user.checkAnonymous();

    String newEmail = null;

    if (mail != null) {
      if (user.getEmail()!=null && user.getEmail().equals(form.getEmail())) {
        newEmail = null;
      } else {
        if (userDao.getByEmail(mail.getAddress().toLowerCase(), false) != null) {
          errors.rejectValue("email", null, "такой email уже используется");
        }

        newEmail = mail.getAddress().toLowerCase();

        emailChanged = true;
      }
    }

    if (!errors.hasErrors()) {
      userDao.updateUser(
          user,
          name,
          url,
          newEmail,
          town,
          password,
          info
      );
      // Обновление token-а аудетификации после смены пароля
      if(password != null) {
        try {
          UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user.getNick(), password);
          UserDetailsImpl details = (UserDetailsImpl) userDetailsService.loadUserByUsername(user.getNick());
          token.setDetails(details);
          Authentication auth = authenticationManager.authenticate(token);
          SecurityContextHolder.getContext().setAuthentication(auth);
          rememberMeServices.loginSuccess(request, response, auth);
        } catch (Exception ex) {
          logger.error("В этом месте не должно быть исключительных ситуаций. ", ex);
        }
      }

      if (emailChanged) {
        emailService.sendEmail(user.getNick(), newEmail, false);
      }
    } else {
      return new ModelAndView("edit-reg");
    }

    if (emailChanged) {
      String msg = "Обновление регистрации прошло успешно. " +
              "Ожидайте письма на "+StringUtil.escapeHtml(newEmail)+" с кодом активации смены email.";

      return new ModelAndView("action-done", "message", msg);
    } else {
      return new ModelAndView(new RedirectView("/people/" + tmpl.getNick() + "/profile"));
    }
  }

  @InitBinder("form")
  public void requestValidator(WebDataBinder binder) {
    binder.setValidator(new EditRegisterRequestValidator());
    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor());
  }
}
