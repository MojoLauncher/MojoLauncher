# KarabakhLauncher secure admin approval setup

This repository contains the database schema only. Admin approval is **not active** until a Supabase project and trusted Edge Functions are configured.

## Security model
- Supabase Auth authenticates accounts.
- `launcher_profiles.role` is one of `user`, `admin`, or `owner`.
- New admin requests are pending by default.
- Only a trusted server-side function may approve/reject requests or change roles.
- The Android APK must never contain the Supabase service-role key, owner password, or invitation code.

## Setup
1. Create a Supabase project.
2. Open SQL Editor and run `supabase/migrations/202611240001_roles_and_admin_requests.sql`.
3. Create the owner account through Supabase Auth using the owner's email.
4. In SQL Editor, identify that Auth user's UUID and insert the owner profile:
   ```sql
   insert into public.launcher_profiles(user_id, username, role)
   values ('OWNER_AUTH_USER_UUID', '45_abdullayefff0', 'owner');
   ```
   Replace the placeholder with the UUID from your own Supabase project. Do not share the UUID or password publicly.
5. Configure Edge Function secrets in Supabase: `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY`, and a separately chosen `ADMIN_INVITE_CODE`. Store the invite code only as a server secret; do not commit it to GitHub.
6. Deploy Edge Functions for admin-request submission and owner-only review. The review function must verify the caller's authenticated user ID has role `owner` before changing any role.

Until these steps are completed, the app's admin form is only a UI and cannot grant admin access.
