package com.example.sbsecuritysandbox

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcOperations
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
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
        // OAuth2LoginAuthenticationFilterの設定
        hs.oauth2Login()
            // UserInfoEndpoint（OAuth2のユーザー情報取得用エンドポイント）から取得可能な情報を用いる
            .userInfoEndpoint()
            // OAuth2のユーザー情報をちょっとだけ書き換える
            .userService(customUserService())
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

    private fun customUserService(): OAuth2UserService<OAuth2UserRequest, OAuth2User> {
        // OAuth2のUser情報を取得するためのサービス
        // デフォルトがDefaultOAuth2UserService。
        val delegate = DefaultOAuth2UserService()
        return OAuth2UserService { userRequest ->
            // OAuth2ユーザー情報提供エンドポイントからユーザー情報を取得する。
            val user = delegate.loadUser(userRequest)
            // 取得されたユーザー情報を書き換える。
            println("==== OAuth2ユーザー情報を書き換える前 ====")
            println("name=${user.name}")
            println("attrs=${user.attributes}")
            println("auths=${user.authorities}")
            // デフォルトを移譲する形で、ユーザー情報を書き換える。
            val newAuthorities = HashSet(user.authorities)
            newAuthorities.clear()
            newAuthorities.add(SimpleGrantedAuthority("ROLE_USER"))
            val newAttributes = HashMap(user.attributes)
            // idを新しくする
            newAttributes["__overwritten_id"] = "${userRequest.clientRegistration.clientName}-${user.name}"
            DefaultOAuth2User(
                newAuthorities,
                newAttributes,
                "__overwritten_id",
            )
        }
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

    // Spring SecurityのOAuth2認証情報（プロバイダーから提供される諸々の情報。アクセストークンとかetc）を
    // JDBCにより永続化するためのBean
    // Spring Securityでは、デフォルトでは、認証情報をインメモリーへ保存する。
    @Bean
    fun a(
        jdbcOperations: JdbcOperations,
        clientRegistrationRepository: ClientRegistrationRepository,
    ): OAuth2AuthorizedClientService {
        return JdbcOAuth2AuthorizedClientService(
            jdbcOperations,
            clientRegistrationRepository,
        )
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