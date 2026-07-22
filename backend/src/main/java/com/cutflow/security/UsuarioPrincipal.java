package com.cutflow.security;

import com.cutflow.entity.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Identidade do usuario logado por e-mail/senha (UserDetails do Spring
 * Security). Nao carrega papel de organizacao: papeis sao contextuais a
 * organizacao ativa e verificados na camada de servico (OrganizacaoContexto),
 * nao como authorities globais.
 *
 * Contas que so entram via Google tem senhaHash nulo; nesse caso o password
 * fica vazio de proposito para nunca casar num login local.
 */
public class UsuarioPrincipal implements UserDetails {

    private final String email;
    private final String senhaHash;

    public UsuarioPrincipal(Usuario usuario) {
        this.email = usuario.getEmail();
        this.senhaHash = usuario.getSenhaHash() != null ? usuario.getSenhaHash() : "";
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
