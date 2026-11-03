package com.farmacia.farmacia.security;

import com.farmacia.farmacia.repository.UsuarioRepository;
import com.farmacia.farmacia.service.AuditoriaService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http,
                                       UsuarioRepository usuarios,
                                       TentativasDeLogin tentativas,
                                       AuditoriaService auditoria,
                                       AuditoriaDeAcessos acessos) throws Exception {

        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null);

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(csrfHandler))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login.html", "/css/**", "/js/comum.js",
                                "/js/login.js", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/usuarios/**", "/gestao/**", "/gestao-*.html",
                                "/js/gestao-*.js").hasRole("ADMIN")
                        .anyRequest().authenticated())

                .formLogin(form -> form
                        .loginPage("/login.html")
                        .loginProcessingUrl("/login")
                        .successHandler((req, res, auth) -> {
                            tentativas.limpar(auth.getName());
                            acessos.registrar(req, auth.getName(), "Login", 200);
                            // Guardar a versão autenticada permite derrubar esta sessão
                            // se a senha for trocada depois.
                            req.getSession().setAttribute(
                                    UsuarioAtivoFilter.ATRIBUTO_LOGIN,
                                    ((UsuarioAutenticado) auth.getPrincipal()).getVersaoSenha());
                            res.setStatus(HttpServletResponse.SC_OK);
                        })
                        .failureHandler((req, res, ex) -> {
                            String login = req.getParameter("username");
                            tentativas.registrarFalha(login);
                            boolean bloqueado = tentativas.bloqueado(login);
                            acessos.registrar(req, login, bloqueado ? "Login bloqueado" : "Falha de login",
                                    bloqueado ? 429 : 401);
                            // A mesma resposta vale para usuários que existem e que não existem.
                            if (tentativas.bloqueado(req.getParameter("username"))) {
                                escreverErro(res, 429,
                                        "Muitas tentativas. Aguarde alguns minutos e tente de novo.");
                            } else {
                                escreverErro(res, HttpServletResponse.SC_UNAUTHORIZED,
                                        "Usuário ou senha inválidos.");
                            }
                        })
                        .permitAll())

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .addLogoutHandler((req, res, auth) -> {
                            if (auth != null) acessos.registrar(req, auth.getName(), "Logout", 200);
                        })
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
                        .deleteCookies("JSESSIONID"))

                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint((req, res, ex) -> {
                            if (ehPagina(req.getRequestURI())) {
                                res.sendRedirect("/login.html");
                            } else {
                                escreverErro(res, HttpServletResponse.SC_UNAUTHORIZED,
                                        "Sessão expirada. Entre novamente.");
                            }
                        })
                        .accessDeniedHandler((req, res, ex) -> {
                            if (ex instanceof CsrfException) {
                                escreverErro(res, HttpServletResponse.SC_FORBIDDEN,
                                        "Sessão inválida. Atualize a página (F5) e tente novamente.");
                            } else if (ehPagina(req.getRequestURI())) {
                                res.sendRedirect("/index.html");
                            } else {
                                escreverErro(res, HttpServletResponse.SC_FORBIDDEN,
                                        "Acesso restrito ao administrador.");
                            }
                        }))

                .addFilterAfter(new UsuarioAtivoFilter(usuarios), SecurityContextHolderFilter.class)
                .addFilterAfter(new AuditoriaFilter(auditoria), UsuarioAtivoFilter.class);

        return http.build();
    }

    private static boolean ehPagina(String caminho) {
        return caminho.equals("/") || caminho.endsWith(".html");
    }

    private static void escreverErro(HttpServletResponse resposta, int status, String mensagem)
            throws IOException {
        resposta.setStatus(status);
        resposta.setContentType("application/json;charset=UTF-8");
        resposta.getWriter().write("{\"mensagem\":\"" + mensagem + "\"}");
    }
}
