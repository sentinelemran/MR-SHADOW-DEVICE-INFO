# Development Checklist

- [x] Stage 0: Interview
- [x] Stage 1: Plan and repo structure
- [x] Stage 2: Telegram bot setup (secure token storage)
- [x] Stage 3: Supabase database setup
- [x] Stage 4a: Client commands and language switch
- [x] Stage 4b: Job and payment commands
- [x] Stage 4c: Stats report
- [x] Stage 5: BotFather menu, description, profile photo
- [x] Stage 5+: /about command
- [ ] Stage 6: Final testing and security check

## Commands
/client_add /client_list /client_find
/job_new /job_list /job_show /job_pay /job_status /job_done
/stats /lang /about /help

## Notes
- Secrets (bot token, database key, admin ID) live only in Cloudflare Worker settings.
- Never commit secrets or client data to this repository.
- Free-tier Supabase pauses after 7 days of inactivity; export backups monthly.
