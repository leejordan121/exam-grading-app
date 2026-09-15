-- A student's attempt at an exam.
create table public.submissions (
  id uuid primary key default gen_random_uuid(),
  exam_id uuid not null references public.exams(id) on delete cascade,
  student_id uuid not null references public.profiles(id) on delete cascade,
  attempt_number int not null default 1,
  idempotency_key text unique,
  submitted_at timestamptz,
  status text not null default 'draft'
    check (status in
      ('draft', 'uploading', 'uploaded', 'processing', 'ocr_processing',
       'grading', 'needs_review', 'graded', 'failed')),
  ai_score numeric,
  final_score numeric,
  total_marks numeric,
  percentage numeric,
  grade text,
  needs_review boolean not null default false,
  is_late boolean not null default false,
  result_visible boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (exam_id, student_id, attempt_number)
);
create index idx_submissions_exam_id on public.submissions(exam_id);
create index idx_submissions_student_id on public.submissions(student_id);
create index idx_submissions_status on public.submissions(status);
create trigger trg_submissions_updated_at before update on public.submissions
  for each row execute function public.set_updated_at();

-- Individual scanned pages belonging to a submission.
create table public.submission_pages (
  id uuid primary key default gen_random_uuid(),
  submission_id uuid not null references public.submissions(id) on delete cascade,
  page_number int not null,
  file_path text not null,
  image_width int,
  image_height int,
  quality_score numeric,
  ocr_status text not null default 'pending'
    check (ocr_status in ('pending', 'processing', 'done', 'failed')),
  created_at timestamptz not null default now(),
  unique (submission_id, page_number)
);
create index idx_submission_pages_submission_id on public.submission_pages(submission_id);

-- Per-question AI-extracted answer + score. This is the grain-level grading
-- record; `final_score` is the teacher-approved/overridden value once
-- reviewed, `ai_score` is preserved unmodified as the original AI output.
create table public.extracted_answers (
  id uuid primary key default gen_random_uuid(),
  submission_id uuid not null references public.submissions(id) on delete cascade,
  question_id uuid not null references public.questions(id) on delete cascade,
  raw_text text,
  normalized_answer text,
  ai_score numeric,
  max_score numeric not null,
  confidence numeric,
  grading_reason text,
  needs_review boolean not null default false,
  final_score numeric,
  reviewed_by uuid references public.profiles(id),
  reviewed_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (submission_id, question_id)
);
create index idx_extracted_answers_submission_id on public.extracted_answers(submission_id);
create index idx_extracted_answers_question_id on public.extracted_answers(question_id);
create trigger trg_extracted_answers_updated_at before update on public.extracted_answers
  for each row execute function public.set_updated_at();

-- Immutable history of every manual grade override, for auditability.
create table public.grade_reviews (
  id uuid primary key default gen_random_uuid(),
  submission_id uuid not null references public.submissions(id) on delete cascade,
  question_id uuid not null references public.questions(id) on delete cascade,
  original_ai_score numeric,
  final_score numeric not null,
  reviewed_by uuid not null references public.profiles(id),
  reason text,
  created_at timestamptz not null default now()
);
create index idx_grade_reviews_submission_id on public.grade_reviews(submission_id);

-- Notifications
create table public.notifications (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  title text not null,
  message text,
  type text not null,
  related_exam_id uuid references public.exams(id) on delete cascade,
  related_submission_id uuid references public.submissions(id) on delete cascade,
  is_read boolean not null default false,
  created_at timestamptz not null default now()
);
create index idx_notifications_user_id on public.notifications(user_id);

-- Audit log
create table public.audit_logs (
  id uuid primary key default gen_random_uuid(),
  school_id uuid references public.schools(id) on delete cascade,
  user_id uuid references public.profiles(id),
  action text not null,
  entity_type text not null,
  entity_id uuid,
  metadata jsonb,
  created_at timestamptz not null default now()
);
create index idx_audit_logs_school_id on public.audit_logs(school_id);
create index idx_audit_logs_created_at on public.audit_logs(created_at);
