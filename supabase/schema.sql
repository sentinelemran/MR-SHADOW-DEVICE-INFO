-- Clients
create table clients (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  phone text,
  created_at timestamptz not null default now()
);

-- Jobs (one phone-rooting job per row)
create table jobs (
  id uuid primary key default gen_random_uuid(),
  client_id uuid not null references clients(id) on delete cascade,
  phone_brand text,
  phone_model text,
  android_version text,
  bootloader_status text,
  imei text,
  root_type text,
  job_date date not null default current_date,
  total_amount numeric(12,2) not null default 0,
  paid_amount numeric(12,2) not null default 0,
  due_amount numeric(12,2) generated always as (total_amount - paid_amount) stored,
  status text not null default 'pending'
    check (status in ('pending', 'in_progress', 'done', 'cancelled')),
  notes text,
  created_at timestamptz not null default now()
);

-- Bot settings (e.g. language: bn or en)
create table bot_settings (
  key text primary key,
  value text not null
);
insert into bot_settings (key, value) values ('lang', 'bn');

-- Indexes for faster search
create index jobs_client_id_idx on jobs (client_id);
create index jobs_job_date_idx on jobs (job_date);
create index clients_name_idx on clients (lower(name));

-- Security: block all public access. Only the bot (server key) can read/write.
alter table clients enable row level security;
alter table jobs enable row level security;
alter table bot_settings enable row level security;
