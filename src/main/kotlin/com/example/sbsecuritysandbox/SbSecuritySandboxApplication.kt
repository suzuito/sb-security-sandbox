package com.example.sbsecuritysandbox

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import java.security.Principal

@Controller
class Page {
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
