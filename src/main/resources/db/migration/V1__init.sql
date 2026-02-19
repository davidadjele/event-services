create table organizations (
                               id uuid primary key,
                               name varchar(180) not null,
                               country_code varchar(2) not null,
                               currency_code varchar(3) not null,
                               status varchar(30) not null,
                               created_at timestamptz not null,
                               updated_at timestamptz not null,
                               deleted_at timestamptz null
);

create table users (
                       id uuid primary key,
                       organization_id uuid null references organizations(id),
                       email varchar(255) not null unique,
                       password_hash varchar(255) not null,
                       role varchar(40) not null,
                       status varchar(30) not null,
                       created_at timestamptz not null,
                       updated_at timestamptz not null,
                       deleted_at timestamptz null
);

create index idx_users_org on users(organization_id);
create index idx_org_created on organizations(created_at);
create index idx_user_created on users(created_at);
