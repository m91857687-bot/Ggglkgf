-- Create a table for public profiles
create table public.profiles (
  id uuid references auth.users not null primary key,
  updated_at timestamp with time zone
);

-- Create a table for coin balances
create table public.coin_balances (
  user_id uuid references public.profiles(id) not null primary key,
  balance integer not null default 0
);

-- Create a table for transactions
create table public.coin_transactions (
  id uuid default gen_random_uuid() primary key,
  user_id uuid references public.profiles(id) not null,
  amount integer not null,
  type text not null,
  event_id text unique, -- Ensures we don't process the same S2S reward twice
  created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Turn on Security
alter table public.profiles enable row level security;
alter table public.coin_balances enable row level security;
alter table public.coin_transactions enable row level security;

-- Policies
create policy "Public profiles are viewable by everyone." on public.profiles
  for select using (true);

create policy "Users can insert their own profile." on public.profiles
  for insert with check (auth.uid() = id);

create policy "Users can update own profile." on public.profiles
  for update using (auth.uid() = id);

-- Balances: users can only read their own balance.
-- Updates/Inserts are done by triggers or edge functions (Service Role)
create policy "Users can read own balance" on public.coin_balances
  for select using (auth.uid() = user_id);

-- Transactions: users can only read their own transactions.
create policy "Users can read own transactions" on public.coin_transactions
  for select using (auth.uid() = user_id);

-- Trigger to create a profile and balance row when a new user signs up
create or replace function public.handle_new_user()
returns trigger as $$
begin
  insert into public.profiles (id) values (new.id);
  insert into public.coin_balances (user_id, balance) values (new.id, 0);
  return new;
end;
$$ language plpgsql security definer;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

-- Trigger to update balance when a new transaction is inserted
create or replace function public.update_balance_on_transaction()
returns trigger as $$
begin
  update public.coin_balances
  set balance = balance + new.amount
  where user_id = new.user_id;
  return new;
end;
$$ language plpgsql security definer;

create trigger on_transaction_inserted
  after insert on public.coin_transactions
  for each row execute procedure public.update_balance_on_transaction();
