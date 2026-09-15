-- Schools
create table public.schools (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  address text,
  phone text,
  email text,
  logo_url text,
  timezone text not null default 'UTC',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create trigger trg_schools_updated_at before update on public.schools
  for each row execute function public.set_updated_at();

-- Profiles (id mirrors auth.users.id)
create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  school_id uuid references public.schools(id) on delete restrict,
  role text not null check (role in ('admin', 'teacher', 'student')),
  full_name text not null,
  email text not null,
  avatar_url text,
  student_code text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index idx_profiles_school_id on public.profiles(school_id);
create index idx_profiles_role on public.profiles(role);
create trigger trg_profiles_updated_at before update on public.profiles
  for each row execute function public.set_updated_at();

-- Classes
create table public.classes (
  id uuid primary key default gen_random_uuid(),
  school_id uuid not null references public.schools(id) on delete cascade,
  name text not null,
  grade text,
  section text,
  academic_year text,
  created_at timestamptz not null default now()
);
create index idx_classes_school_id on public.classes(school_id);

-- Class <-> student membership
create table public.class_students (
  id uuid primary key default gen_random_uuid(),
  class_id uuid not null references public.classes(id) on delete cascade,
  student_id uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  unique (class_id, student_id)
);
create index idx_class_students_class_id on public.class_students(class_id);
create index idx_class_students_student_id on public.class_students(student_id);

-- Class <-> teacher assignment
create table public.teacher_classes (
  id uuid primary key default gen_random_uuid(),
  teacher_id uuid not null references public.profiles(id) on delete cascade,
  class_id uuid not null references public.classes(id) on delete cascade,
  created_at timestamptz not null default now(),
  unique (teacher_id, class_id)
);
create index idx_teacher_classes_teacher_id on public.teacher_classes(teacher_id);
create index idx_teacher_classes_class_id on public.teacher_classes(class_id);

-- Subjects
create table public.subjects (
  id uuid primary key default gen_random_uuid(),
  school_id uuid not null references public.schools(id) on delete cascade,
  name text not null,
  code text,
  description text,
  created_at timestamptz not null default now()
);
create index idx_subjects_school_id on public.subjects(school_id);

-- Grading scales (e.g. 90-100 = A+) — configurable per school, never hard-coded.
create table public.grading_scales (
  id uuid primary key default gen_random_uuid(),
  school_id uuid not null references public.schools(id) on delete cascade,
  name text not null,
  is_default boolean not null default false,
  bands jsonb not null, -- [{ "min": 90, "max": 100, "grade": "A+" }, ...]
  created_at timestamptz not null default now()
);
create index idx_grading_scales_school_id on public.grading_scales(school_id);
