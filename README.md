# Spring Security 勉強用のレポジトリ

## 基本

https://docs.spring.io/spring-security/reference/servlet/architecture.html

## Spring Security for Servlet application

### アーキテクチャ

Spring
Securityの外観を理解しておこう。参考にしたとキュメンとは[こちら](https://docs.spring.io/spring-security/reference/servlet/architecture.html)。

本題に入る前に、まずは、Servlet filterをおさらいすること。
なぜなら、Servletアプリケーション向けSpring Security機能は、Servlet
filterをベースにして実装されているため。 Servlet
filterは、Webアプリケーションのリクエスト処理の前処理や、レスポンス処理の後処理を実施するためのもの。ざっくりと理解するなら[こちら](https://www.javadrive.jp/servlet/filter/index1.html)
で十分。具体的に理解するなら[こちら](https://docs.oracle.com/cd/E18355_01/web.1013/B31859-01/filters.htm)。

さて。リクエストがサーバーに届いた後の処理フローを順に説明する。

#### リクエストはクライアントからFilterChainへ (Servletがリクエストを処理するメカニズム)

クライアントからのリクエストを最初に処理するのは、SpringBootではなくServlet containerだ。

[FilterChain](https://docs.oracle.com/javaee/7/api/javax/servlet/FilterChain.html)は
Servlet containerにより作られる。
このFilterChainがFilterの配列とServletを持つ。

リクエストは、FilterChainの中にあるFilterの配列の要素を順番に通ってゆく。やがてServletに到達する。
Servletがクライアントへレスポンスを返す。当然、いずれかのFilterでフィルタされてしまうこともある。

ここまでの処理は、Spring BootではなくServletが実行する。

#### FilterChain中のDelegatingFilterProxy -> FilterChainProxy (Servletがリクエストを処理するメカニズム)

処理をServletからSpringBootへ移譲するためのFilterが、DelegatingFilterProxyだ。
この辺りの詳細なフローは、ソースコードを読んでもようわからんかった。

#### FilterChainProxyの処理 (Spring Securityがリクエストを処理するメカニズム)

FilterChainProxyはSecurityFilterChainの配列を持つ。
SecurityFilterChainはFilterの配列を持つ（ここでやっと登場、Filterくん）。
フィルタリング処理の実態はFilterが担う。

FilterChainProxyは、リクエストパスなどを入力として、リクエストをどのSecurityFilterChainへ送るか判定する。
リクエストが送られるSecurityFilterChainは１つだけ。複数には送られない。

### 図にするとこんな感じか？

![](IMG_0399.jpg)

## Security Filter Chainの作り方を理解せよ

「アーキテクチャ」で述べた通り、SecurityFilterChainがフィルタ処理を担う。
要は、SecurityFilterChainの作り方を理解することができれば、Spring Securityをある程度使いこなせるようになる。

慣例では、Java ConfigによりSecurityFilterChainを作るっぽい。本サンプルコードでは、以下で作ってる。

[com.example.sbsecuritysandbox.SecurityConfiguration](./src/main/kotlin/com.example.sbsecuritysandbox/SecurityConfiguration.kt)

## SecurityContext（セッション情報）をどこへ永続化しているのか

https://docs.spring.io/spring-security/reference/servlet/authentication/persistence.html

当たり前だが、Spring SecurityはSecurity Context（要はセッション情報）を永続化する。ユーザーがパスワードログインした後、Spring
BootはそのSecurityContextをどこかへ永続化しているわけだ。

どこへ永続化しているのだろうか？を調べてみよう。

まず、[SecurityContextRepository](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/web/context/SecurityContextRepository.html)
がSecurityContextを永続化するとのこと。3つのSecurityContextRepository実装が提供されている（詳しい話は[こちら](https://docs.spring.io/spring-security/reference/servlet/authentication/persistence.html#securitycontextrepository)
）。
SecurityContextRepositoryのデフォルト実装は[HttpSecurityContextRepository](https://docs.spring.io/spring-security/site/docs/5.7.3/api/org/springframework/security/web/context/HttpSessionSecurityContextRepository.html)
。
このレポジトリはSecurityContextを[HttpSession](https://docs.oracle.com/javaee/7/api/javax/servlet/http/HttpSession.html)
として扱う。

HttpSessionはSpring Sessionモジュールにより扱われる。
Spring SessionモジュールはHttpSessionを様々な方法で扱うための方法を提供する。
HttpSessionを、JDBCにより永続化する方法は[こちら](https://docs.spring.io/spring-session/reference/guides/boot-jdbc.html)に書かれている。

要は、「SecurityContextを永続化する」とは「HttpSessionを永続化する」ことに他ならない。

## サンプルの動かし方

```shell
./gradlew bootRun
```