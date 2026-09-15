-- Exams
-- NOTE: intentionally NO answer_key_file_path / correct_answer / rubric column
-- here. Those live only in answer_keys / question_answers, which have no
-- student-readable RLS policy. This table is safe to return in a
-- student-facing exam response as-is.
create table public.exams (
  id uuid primary key default gen_random_uuid(),
  school_id uuid not null references public.schools(id) on delete cascade,
  teacher_id uuid not null references public.profiles(id) on delete restrict,
  subject_id uuid not null references public.subjects(id) on delete restrict,
  title text not null,
  description text,
  instructions text,
  exam_date date,
  duration_minutes int,
  total_marks numeric not null default 0,
  status text not null default 'draft'
    check (status in ('draft', 'published', 'closed', 'grading', 'completed')),
  paper_file_path text,
  allow_late_submission boolean not null default false,
  allow_resubmission boolean not null default false,
  show_result_immediately boolean not null default true,
  show_question_breakdown boolean not null default true,
  show_ai_feedback boolean not null default false,
  require_teacher_approval boolean not null default false,
  result_release_mode text not null default 'immediate'
    check (result_release_mode in ('immediate', 'teacher_approval', 'scheduled')),
  result_release_at timestamptz,
  submission_deadline timestamptz,
  published_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index idx_exams_school_id on public.exams(school_id);
create index idx_exams_teacher_id on public.exams(teacher_id);
create index idx_exams_status on public.exams(status);
create trigger trg_exams_updated_at before update on public.exams
  for each row execute function public.set_updated_at();

-- Questions (public shape: text, type, marks — no answers)
create table public.questions (
  id uuid primary key default gen_random_uuid(),
  exam_id uuid not null references public.exams(id) on delete cascade,
  question_number int not null,
  question_text text not null,
  question_type text not null
    check (question_type in
      ('multiple_choice', 'true_false', 'fill_blank', 'short_answer', 'long_answer', 'mathematical', 'diagram')),
  max_marks numeric not null,
  page_number int,
  grading_mode text not null default 'ai'
    check (grading_mode in ('ai', 'manual', 'hybrid')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (exam_id, question_number)
);
create index idx_questions_exam_id on public.questions(exam_id);
create trigger trg_questions_updated_at before update on public.questions
  for each row execute function public.set_updated_at();

-- Protected answer data — split from `questions` specifically so RLS can
-- deny this table to students entirely while still exposing question text.
create table public.question_answers (
  id uuid primary key default gen_random_uuid(),
  question_id uuid not null unique references public.questions(id) on delete cascade,
  correct_answer text,
  rubric jsonb,
  grading_instructions text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create trigger trg_question_answers_updated_at before update on public.question_answers
  for each row execute function public.set_updated_at();

-- Answer-region coordinates on the paper, used to segment OCR by question.
create table public.question_regions (
  id uuid primary key default gen_random_uuid(),
  question_id uuid not null references public.questions(id) on delete cascade,
  page_number int not null,
  x numeric not null,
  y numeric not null,
  width numeric not null,
  height numeric not null,
  created_at timestamptz not null default now()
);
create index idx_question_regions_question_id on public.question_regions(question_id);

-- Official answer key file(s) — teacher/admin only, never student-readable.
create table public.answer_keys (
  id uuid primary key default gen_random_uuid(),
  exam_id uuid not null references public.exams(id) on delete cascade,
  file_path text,
  version int not null default 1,
  uploaded_by uuid not null references public.profiles(id),
  created_at timestamptz not null default now()
);
create index idx_answer_keys_exam_id on public.answer_keys(exam_id);

-- Which students an exam is assigned to, and their paper lifecycle status.
create table public.exam_assignments (
  id uuid primary key default gen_random_uuid(),
  exam_id uuid not null references public.exams(id) on delete cascade,
  student_id uuid not null references public.profiles(id) on delete cascade,
  assigned_at timestamptz not null default now(),
  due_at timestamptz,
  status text not null default 'assigned'
    check (status in ('assigned', 'downloaded', 'submitted', 'graded')),
  downloaded_at timestamptz,
  unique (exam_id, student_id)
);
create index idx_exam_assignments_exam_id on public.exam_assignments(exam_id);
create index idx_exam_assignments_student_id on public.exam_assignments(student_id);
