package org.zanata.action;

import java.io.Serializable;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import lombok.extern.slf4j.Slf4j;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.End;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.faces.Renderer;
import org.zanata.dao.AccountDAO;
import org.zanata.model.HAccount;
import org.zanata.model.HAccountResetPasswordKey;
import org.zanata.service.UserAccountService;

@Name("passwordResetRequest")
@Scope(ScopeType.EVENT)
@Slf4j
public class PasswordResetRequestAction implements Serializable {
    private static final long serialVersionUID = 1L;

    @In
    private AccountDAO accountDAO;

    @In
    private UserAccountService userAccountServiceImpl;

    @In(create = true)
    private Renderer renderer;

    private String username;
    private String email;
    private String activationKey;

    private HAccount account;

    public HAccount getAccount() {
        return account;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @NotEmpty
    @Size(min = 3, max = 20)
    @Pattern(regexp = "^[a-z\\d_]{3,20}$")
    public String getUsername() {
        return username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Email
    @NotEmpty
    public String getEmail() {
        return email;
    }

    @End
    public String requestReset() {
        account = accountDAO.getByUsernameAndEmail(username, email);
        HAccountResetPasswordKey key =
                userAccountServiceImpl.requestPasswordReset(account);

        if (key == null) {
            FacesMessages.instance().add("No such account found");
            return null;
        } else {
            setActivationKey(key.getKeyHash());
            renderer.render("/WEB-INF/facelets/email/password_reset.xhtml");
            log.info("Sent password reset key to {} ({})", account
                    .getPerson().getName(), account.getUsername());
            FacesMessages
                    .instance()
                    .add("You will soon receive an email with a link to reset your password.");
            return "/home.xhtml";
        }

    }

    public String getActivationKey() {
        return activationKey;
    }

    public void setActivationKey(String activationKey) {
        this.activationKey = activationKey;
    }

}
