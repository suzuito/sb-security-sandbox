package com.example.sbsecuritysandbox

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import java.security.Principal

@Controller
class Page(
    val authorizedClientService: OAuth2AuthorizedClientService,
) {
    @GetMapping("/")
    fun getIndex(
        // principalはSpring Securityの認証情報を保持する。
        // もちろん、SecurityContextHolder（ログイン中のユーザー情報を保持するオブジェクト）から直接取得することもできる。
        // ログインしていない場合、principalはnullになる。
        principal: Principal?,
        model: Model,
    ): String {
        if (principal != null) {
            model.addAttribute("username", principal.name)
            // principalの実態はAuthentication
            val auth = principal as Authentication
            println("username is ${auth.name}")
            for (a in auth.authorities) {
                println(a)
            }
            println("detail is ${auth.details}")
            println("isAuth is ${auth.isAuthenticated}")
            // OAuth2ログインユーザーの情報を取得する
            // OAuth2AuthorizedClientServiceがOAuth2認証に関連する諸々の情報を永続化している。
            // もちろん永続化先は変更可能。デフォルトではインメモリーに永続化する。
            // principalからその情報を引き当てることができる。
            val authClient: OAuth2AuthorizedClient? = authorizedClientService.loadAuthorizedClient("github", auth.name)
            // OAuth2ログインではないユーザーの場合、nullとなる
            if (authClient != null) {
                println("OAuth2 ac ${authClient.accessToken.tokenValue}")
                println("OAuth2 client name ${authClient.clientRegistration.clientName}")
            }
        } else {
            model.addAttribute("username", "ななし")
        }
        return "index"
    }

    @GetMapping("/user")
    fun getUser(
    ): String {
        return "user"
    }

    @GetMapping("/admin")
    fun getAdmin(
    ): String {
        return "admin"
    }
}

@SpringBootApplication
class SbSecuritySandboxApplication

fun main(args: Array<String>) {
    runApplication<SbSecuritySandboxApplication>(*args)
}
