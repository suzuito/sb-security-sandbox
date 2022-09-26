-- ユーザー情報をH2データベースで管理する
create table if not exists users(
                      username varchar_ignorecase(50) not null primary key,
                      password varchar_ignorecase(500) not null,
                      enabled boolean not null
);

create table if not exists authorities (
                             username varchar_ignorecase(50) not null,
                             authority varchar_ignorecase(50) not null,
                             constraint fk_authorities_users foreign key(username) references users(username)
);
create unique index if not exists ix_auth_username on authorities (username,authority);

-- OAuth2情報をH2データベースで管理する
-- スキーマはこれ
-- https://docs.spring.io/spring-security/reference/servlet/appendix/database-schema.html#dbschema-oauth2-client
CREATE TABLE if not exists oauth2_authorized_client (
                                          client_registration_id varchar(100) NOT NULL,
                                          principal_name varchar(200) NOT NULL,
                                          access_token_type varchar(100) NOT NULL,
                                          access_token_value blob NOT NULL,
                                          access_token_issued_at timestamp NOT NULL,
                                          access_token_expires_at timestamp NOT NULL,
                                          access_token_scopes varchar(1000) DEFAULT NULL,
                                          refresh_token_value blob DEFAULT NULL,
                                          refresh_token_issued_at timestamp DEFAULT NULL,
                                          created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                          PRIMARY KEY (client_registration_id, principal_name)
);