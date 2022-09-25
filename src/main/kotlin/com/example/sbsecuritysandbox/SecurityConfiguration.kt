package com.example.sbsecuritysandbox

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.JdbcUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import javax.sql.DataSource

// Spring Securityを設定する。 Java ConfigによるBean定義である。
// Spring EnableWebSecurityアノテーションは、Spring Securityを有効化する（ために付与する・・・とのことだが、実際のところ何をしているのかは不明）。
@Configuration
@EnableWebSecurity
class SecurityConfiguration {
    @Bean
    fun hoge(hs: HttpSecurity): SecurityFilterChain {
        // H2-console用の設定です
        hs.csrf().disable()
        hs.headers().frameOptions().disable()
        // AuthorizationFilterの設定
        hs.authorizeHttpRequests()
            .antMatchers("/h2-console/**", "/error").permitAll() // h2-console用の設定です
            .mvcMatchers("/").permitAll() // index.htmlはログインなしでアクセス可
            .mvcMatchers("/login").permitAll() // login.htmlはログインなしでアクセス可
            .mvcMatchers("/logout").permitAll() // このページを開くためにはログインが必須
            .mvcMatchers("/user").authenticated() // このページを開くためにはログインが必須
            .mvcMatchers("/admin").hasRole("ADMIN") // このページを開くためにはログインが必須、かつ、adminロールも必須
        // UsernamePasswordAuthenticationFilterの設定
        // https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/form.html
        // UsernamePasswordAuthenticationFilterはログイン機能を提供する。以下を担う。
        //   - ユーザー名とクレデンシャルをリクエストから抽出する。
        //     - フォームログイン、Basic認証、ダイジェスト認証等のよく使われる認証方式はデフォルトでサポートされている。
        //   - パスワードのエンコーディング
        //   - パスワードをデータベースのものと突合し、ログイン失敗 or 成功
        //     - 成功 or 失敗したら他のページへリダイレクトさせたり
        // formLoginによりFilterがカスタマイズされる。
        // loginPage関数によりカスタムなログインページを指定できる。
        // カスタムなログインページを指定しない場合は、Spring Securityが提供するデフォルトのログインページとなる。
        // ログインページのパスは /login
        hs.formLogin()
        // LogoutFilterの設定
        hs.logout()
            .logoutSuccessUrl("/")
        val returned = hs.build()
        println("フィルタの確認だお")
        for (v in returned.filters) {
            println(v.javaClass.name)
        }
        return returned
    }

    // DaoAuthenticationProviderで使われるUserDetailsServiceのBean。
    //   DaoAuthenticationProvicerとは、UserDetailsServiceとPasswordEncoderを用いて認証を行うためのAuthenticationProvider実装。
    //   UsernamePasswordAuthenticationFilterにより提供されるログイン機能が、DaoAuthenticationProviderにより認証される。
    // で・・・UserDetailsServiceは、ユーザー名やパスワードを検索するためのもの。
    // InMemoryUserDetailsManagerはUser情報をインメモリで保持するためのもの。
    // JdbcUserDetailsManagerはUser情報をJDBC準拠のデータベースで保持するためのもの。
    @Bean
    fun fuga(
        dataSource: DataSource,
    ): UserDetailsService {
        // デフォルトユーザーを作る
        val userBuilder = User.withDefaultPasswordEncoder()
        val defaultUsers = listOf<UserDetails>(
            userBuilder.username("taro").password("taro").roles("USER").build(),
            userBuilder.username("jiro").password("jiro").roles("USER", "ADMIN").build()
        )
        // UserDetailsManager
        // val userDetailsService = InMemoryUserDetailsManager() // ユーザー情報をインメモリへ保持する。
        val userDetailsService = JdbcUserDetailsManager(dataSource) // ユーザー情報をJDBC準拠のデータベースへ保持する。
        for (defaultUser in defaultUsers) {
            userDetailsService.createUser(defaultUser)
        }
        return userDetailsService
    }

    @Bean
    fun foo(): DataSource {
        return EmbeddedDatabaseBuilder()
            .setName("sa")
            .setType(EmbeddedDatabaseType.H2)
            .addScript("schema.sql")
            .build()
    }
}