alter table clients add column client_no bigint generated always as identity unique;
alter table jobs add column job_no bigint generated always as identity unique;
