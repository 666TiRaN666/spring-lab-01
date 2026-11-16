create table app_user (
    id            bigserial primary key,
    username      varchar(60)  not null unique,
    password_hash varchar(100) not null,     -- BCrypt is 60 characters
    enabled       boolean      not null default true
);

create table user_role (
    user_id bigint      not null references app_user (id),
    role    varchar(20) not null,
    primary key (user_id, role)
);

-- the hashes are of user123 and admin123, generated with BCryptPasswordEncoder (Lab10/bcrypt-hashes.log)
insert into app_user (username, password_hash) values
    ('user',  '$2a$10$fqBzHzh9wBGcpBPuzN3tOOqvwBKDgeustHjaWR28hb/8u.kp1SPsK'),
    ('admin', '$2a$10$J/NjZdrvWBbnAA9QJ2SP0OYzaBnLGi2Qwr23BNjCP/kX47s5LOn4m');

-- two more demo users, both with the password user123:
-- reader shows that the same password gets a different hash, blocked shows enabled = false
insert into app_user (username, password_hash, enabled) values
    ('reader',  '$2a$10$y85qPBjyxIolB3tJzOFjYuUOxJ.Q1ejoS6Qyaf6FPgr.Ol/O.jtoi', true),
    ('blocked', '$2a$10$mh.RzaEF9x9v81r2vJ5WGuWb0Y8YGODLqlvVwrfxvvrOeknt2g5N.', false);

insert into user_role (user_id, role)
    select id, 'USER'  from app_user;
insert into user_role (user_id, role)
    select id, 'ADMIN' from app_user where username = 'admin';
