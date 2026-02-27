create table ticket_types (
                              id uuid primary key,
                              organization_id uuid not null references organizations(id),
                              event_id uuid not null references events(id),

                              name varchar(120) not null,
                              price numeric(12,2) not null,
                              quantity_available int not null,
                              quantity_sold int not null default 0,

                              sale_start timestamptz null,
                              sale_end timestamptz null,

                              created_at timestamptz not null,
                              updated_at timestamptz not null,
                              deleted_at timestamptz null
);

create index idx_ticket_types_org on ticket_types(organization_id);
create index idx_ticket_types_event on ticket_types(event_id);
create index idx_ticket_types_org_event on ticket_types(organization_id, event_id);
create index idx_ticket_types_org_created on ticket_types(organization_id, created_at);