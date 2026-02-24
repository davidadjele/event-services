create table events (
                        id uuid primary key,
                        organization_id uuid not null references organizations(id),

                        title varchar(200) not null,
                        description text null,
                        location varchar(255) null,

                        start_date timestamptz not null,
                        end_date timestamptz null,

                        status varchar(30) not null,

                        created_at timestamptz not null,
                        updated_at timestamptz not null,
                        deleted_at timestamptz null
);

create index idx_events_org on events(organization_id);
create index idx_events_org_status on events(organization_id, status);
create index idx_events_org_created on events(organization_id, created_at);
create index idx_events_start_date on events(start_date);